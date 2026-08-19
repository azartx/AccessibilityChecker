#!/usr/bin/env bash
# ------------------------------------------------------------
# Проверка наличия локального MCP‑сервера и его установка,
# если он отсутствует.
# ------------------------------------------------------------

# Путь к файлу конфигурации opencode (можно изменить при необходимости)
CONFIG_FILE="opencode.json"

# Функция выводит сообщение в стандартный поток ошибок
log_err() { echo "ERROR: $*" >&2; }

# 1. Проверяем, установлен ли MCP‑сервер (команда npx должна находить пакет)
if npx --yes @mobilenext/mobile-mcp@latest --version >/dev/null 2>&1; then
    echo "MCP‑сервер уже установлен."
else
    echo "MCP‑сервер не найден – выполняем установку..."
    # Устанавливаем последнюю версию пакета (без сохранения в package.json)
    if ! npx --yes @mobilenext/mobile-mcp@latest; then
        log_err "Не удалось установить MCP‑сервер."
        exit 1
    fi
    echo "MCP‑сервер успешно установлен."
fi

# 2. Добавляем (или обновляем) конфигурацию в opencode.json
# Если файл отсутствует – создаём минимальный шаблон
if [ ! -f "$CONFIG_FILE" ]; then
    cat >"$CONFIG_FILE" <<EOF
{
  "\$schema": "https://opencode.ai/config.json",
  "mcp": {}
}
EOF
    echo "Создан новый файл конфигурации $CONFIG_FILE."
fi

# Формируем JSON‑блок для MCP‑сервера
MCP_BLOCK='{
    "mobile-mcp": {
      "type": "local",
      "command": [
        "npx",
        "@mobilenext/mobile-mcp@latest"
      ],
      "enabled": true
    }
}'

# Вставляем/обновляем раздел "mcp" в конфигурации
# Используем jq (утилита для работы с JSON). Если её нет – выводим инструкцию.
if command -v jq >/dev/null 2>&1; then
    tmp=$(mktemp)
    jq ".mcp = $MCP_BLOCK" "$CONFIG_FILE" >"$tmp" && mv "$tmp" "$CONFIG_FILE"
    echo "Конфигурация $CONFIG_FILE обновлена."
else
    log_err "Утилита jq не найдена. Установите её (apt-get install jq) и запустите скрипт снова."
    exit 1
fi

echo "Все операции завершены успешно."