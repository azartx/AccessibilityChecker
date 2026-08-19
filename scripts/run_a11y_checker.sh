#!/usr/bin/env bash
# ------------------------------------------------------------
# Скрипт автоматизирует подготовку эмулятора, сборку и
# установку приложений, а также запуск opencode с последующим
# мониторингом результата.
# ------------------------------------------------------------

# TODO:
# 1. При установке приложения какая то фигня + не запускается приложение
# 2. Запускать opencode и ждать ответа - плохая идея. Пусть опенкод сам себя вырубит. Научить его получать айди своего процесса и килять сеья
# 3. мобайл-мсп не работает. Нужно научить добавлять этот мсп через агента

# 0. Настройки
PACKAGE="ru.alfabank.mobile.android"
APK_CHECKER="a11ycheck/AccessibilityChecker.apk"
APK_BUILD="baseapp/build/outputs/apk/development/debug/baseapp-development-universal-debug.apk"
PROMPT_BASE="a11ycheck/baseprompt.txt"
PROMPT_USER="a11ycheck/userpath.txt"
REPORT_FILE="a11ycheck/report.txt"
PACKAGE_CHECKER="com.solo4.accessibilitychecker"
GRADLE_CMD="./gradlew :baseapp:assembleDevelopmentDebug"
OPENCODE_CMD="opencode --model MiniMaxAI/MiniMax"

# Проверяем, передан ли аргумент --justAnalyse
JUST_ANALYSE_FLAG=false
if [[ "$@" == *"--justAnalyse"* ]]; then
    JUST_ANALYSE_FLAG=true
fi

# ------------------------------------------------------------
# 1. Проверка наличия приложения‑проверщика и установка при необходимости
# ------------------------------------------------------------
if ! adb shell pm list packages | grep -q "$PACKAGE_CHECKER"; then
    echo "Приложение $PACKAGE_CHECKER не найдено – выполняется установка..."
    adb install -r "$APK_CHECKER"
else
    echo "Приложение $PACKAGE_CHECKER уже установлено."
fi

# Выполняем блок, если константа $JUST_ANALYSE_FLAG == false
if [[ $JUST_ANALYSE_FLAG == false ]]; then

# ------------------------------------------------------------
# 2. Сборка основного приложения
# ------------------------------------------------------------
echo "Запуск сборки: $GRADLE_CMD"
$GRADLE_CMD
if [ $? -ne 0 ]; then
    echo "Ошибка сборки. Прерывание скрипта."
    exit 1
fi

# ------------------------------------------------------------
# 3. Выбор и запуск первого доступного эмулятора
# ------------------------------------------------------------
AVD_LIST=$(emulator -list-avds)
if [ -z "$AVD_LIST" ]; then
    echo "Список эмуляторов пуст. Установите хотя бы один AVD."
    exit 1
fi

FIRST_AVD=$(echo "$AVD_LIST" | head -n1)
echo "Запуск эмулятора: $FIRST_AVD"
emulator -avd $FIRST_AVD -no-window
echo "Эмулятор запущен (PID=$EMULATOR_PID). Ожидание инициализации..."

# ------------------------------------------------------------
# 4. Ожидание полной инициализации эмулятора
# ------------------------------------------------------------
adb wait-for-device
while true; do
    BOOT_STATUS=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    if [ "$BOOT_STATUS" = "1" ]; then
        echo "Эмулятор полностью инициализирован."
        break
    fi
    echo "Эмулятор ещё загружается…"
    sleep 5
done

# ------------------------------------------------------------
# 5. Установка собранного приложения
# ------------------------------------------------------------
echo "Установка приложения из $APK_BUILD"
adb install -r "$APK_BUILD"
if [ $? -ne 0 ]; then
    echo "Не удалось установить приложение. Прерывание."
    exit 1
fi

fi # Конец if блока, выполняемого если JUST_ANALYSE_FLAG == true

# ------------------------------------------------------------
# 6. Запуск установленного сервиса
# ------------------------------------------------------------
echo "Запуск сервиса доступности и приложения $PACKAGE..."
adb shell settings put secure enabled_accessibility_services \
com.solo4.accessibilitychecker/com.solo4.accessibilitychecker.service.AccessibilityCheckerService
#adb shell am startservice -n \
#com.solo4.accessibilitychecker/com.solo4.accessibilitychecker.service.AccessibilityCheckerService
#adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1

# ------------------------------------------------------------
# 7. Формирование промпта для opencode
# ------------------------------------------------------------
# Проверяем, передан ли аргумент --justAnalyse
if [[ $JUST_ANALYSE_FLAG ]]; then
    # Если передан --justAnalyse - используем только settings.md и checklist.md
    if [ -f "a11ycheck/rules/settings.md" ] && [ -f "a11ycheck/rules/checklist.md" ]; then
        PROMPT_TEXT=$(cat "a11ycheck/rules/settings.md" "a11ycheck/rules/checklist.md")
        #PROMPT_TEXT="$PROMPT_TEXT\n\nЧеклист для проверки прочти из файла @a11ycheck/rules/checklist.md"
    else
        echo "Не найдены файлы промптов. Прерывание."
        exit 1
    fi
else
    # Если аргумент не передан - используем все файлы
    if [ -f "a11ycheck/rules/settings.md" ] && [ -f "a11ycheck/rules/passcode.md" ] && [ -f "a11ycheck/rules/mainscreen.md" ] && [ -f "a11ycheck/rules/USER_PATH.md" ] && [ -f "a11ycheck/rules/checklist.md" ]; then
        PROMPT_TEXT=$(cat "a11ycheck/rules/settings.md" "a11ycheck/rules/passcode.md" "a11ycheck/rules/mainscreen.md" "a11ycheck/USER_PATH.md")
        PROMPT_TEXT="$PROMPT_TEXT\n\nЧеклист для проверки прочти из файла @a11ycheck/rules/checklist.md"
    else
        echo "Не найдены файлы промптов. Прерывание."
        exit 1
    fi
fi

# Установка mcp
echo "Проверяем наличие mcp для работы с эмулятором"
/bin/sh install_mcp.sh

# ------------------------------------------------------------
# 8. Запуск opencode
# ------------------------------------------------------------
echo "Запуск opencode..."
escaped_text=$(printf "%q" "$PROMPT_TEXT")
FULL_OPENCODE="$OPENCODE_CMD --prompt $escaped_text"
echo $FULL_OPENCODE
eval $FULL_OPENCODE