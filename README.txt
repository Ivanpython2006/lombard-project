Инструкция по запуску проекта "Информационная система ломбарда"

1) Установите PostgreSQL и создайте пустую базу данных с именем "lombard_db".
2) При необходимости задайте DB_URL, DB_USER и DB_PASSWORD. По умолчанию используются
   jdbc:postgresql://localhost:5432/lombard_db, postgres и пароль 1.
   PowerShell: $env:DB_PASSWORD = 'ваш_пароль'
   Linux/Mac: export DB_PASSWORD='ваш_пароль'
3) Установите JDK 17 или новее и запустите приложение из корня проекта:
   PowerShell: .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
   Linux/Mac: sh mvnw spring-boot:run -Dspring-boot.run.profiles=demo
   Flyway создаст таблицы в пустой базе. Профиль demo добавит 6 клиентов, 6 займов
   и 6 залогов. Повторный запуск не создает дубликатов. Обычный запуск без профиля demo
   создает только структуру и не включает демонстрационные учетные записи.
4) Откройте http://localhost:8080.

Для старой базы сначала сделайте резервную копию. Если схема уже соответствует
V1__init.sql, при первом запуске задайте DB_BASELINE_EXISTING=true, чтобы Flyway
зарегистрировал ее как версию 1. Hibernate проверит соответствие модели схеме.
Файл lombard_db.sql — резервный дамп прежней демонстрационной базы.

Тесты и отчет JaCoCo: .\mvnw.cmd verify (Linux/Mac: sh mvnw verify).
Отчет: target/site/jacoco/index.html.

Тестовые учетные записи для входа:
Администратор: логин admin, пароль 123456
Менеджер: логин manager, пароль 123456
Клиент: логин client, пароль 123456
