# Предварительные настройки:
- Для работы с эмулятором используем mcp server mobile-mcp
- Для анализа выгруженной доступности и формирования конечного отчета используем чек-лист
- Для получения json логов доступности конечного экрана выполни bash команду `adb shell cat /storage/emulated/0/Android/data/com.solo4.accessibilitychecker/files/Download/current_screen_dump.json`
- После анализа json логов нужно: 1) Запросить текущее время bash командой `date +"%d_%m_%Y_%H_%M"`; 2) Сформировать отчет и сохранить в файл a11ycheck/reports/Report_TIMESTAMP.md, где в названии TIMESTAMP заменить на полученное время.
