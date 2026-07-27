# AutoSeller 1.1.0 - Анархический скупщик для 1.16.5 Java 8

## Что внутри
- /seller - меню с 3 категориями (ores, mobdrops, crops)
- Деньги Vault + очки скупщика
- Магазин бустеров x2
- Автоскупка с выбором предметов
- /sellertop и PlaceholderAPI топ

## Плейсхолдеры
%autoseller_points%
%autoseller_top_1_name% / %autoseller_top_1_points% ... до 10
%autoseller_booster_ores% и т.д.

## Как скомпилировать через GitHub (без установки Java)
1. Создай репозиторий на GitHub (например AutoSeller)
2. Залей все файлы из этого архива в репозиторий
3. Перейди во вкладку Actions -> Build AutoSeller -> Run workflow
4. Через 30-40 сек в Artifacts появится AutoSeller-1.1.0.jar - скачай
5. Если запушил в main, автоматически создастся Release с jar

## Локальная сборка
mvn clean package
jar будет в target/

## Установка на сервер
- Vault + EssentialsX (или любой Economy)
- PlaceholderAPI (опционально)
- Кинуть jar в plugins/
