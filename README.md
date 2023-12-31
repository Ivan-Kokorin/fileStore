- Среда разработки: **OpenJDK 17**
- Версия **Java**: не ниже 17
- **Spring boot** версия 3.1.0
- База данных **H2 in-memory**
- Все дополнительные зависимости представлены в конфигурационном файле **maven** - pom.xml

Для запуска приложения достаточно скачать репозиторий и запустить приложение в IDE. \
При запуске производится инициализация таблиц базы данных. Т.к. БД in-memory, дополнительного подключения и её настройки не требуется. \
Основные настройки приложения в файле *src/main/resources/application.yml* \

Приложение реалазует три варианта сохранения файлов: в облаке S3, в файловой системе, в базе данных.
Чтобы выбрать нужный профиль, необходимо в *src/main/resources/application.yml* установить в поле *spring: profiles: active:* одно из трёх значений:
- cloud
- file-sys
- data-base
Дополнительные настройки для выбора путей сохранения, размера файла и т.д. находятся в этом же файле в разделе *storage*
Кроме данных настроек, для сохранения в облаке S3 требуется создать файл credentials с ключами доступа к управлению облаком и файл config, более подробная инструкция в разделе "Настройка" по ссылке https://cloud.yandex.ru/docs/storage/tools/aws-sdk-java
Можно использовать любое облако, совместимое с AWS SDK S3.

Фронтент-часть не реализована.

Тест контроллеров осуществляется при помощи Postman, CURL или любого другого подобного http-клиента. 

1. **Добавление файла** \
Отправить POST-запрос по адресу http://localhost:8080/files \
С телом запроса в виде multipart form-data и полем "file", в котором и размёщён сам файл. 
При успешном сохранении будет получено JSON-сообщение с данными файла.

2. **Просмотр списка файлов** \
Отправить GET-запрос по пути http://localhost:8080/files
В результате получаем массив JSON-объектов с инфморацией для пользователя \

3. **Скачивание файла** \
Отправить GET запрос по пути http://localhost:8080/files/{id}
\- где {id} - это идентификатор файла в базе данных, получить идентификатор можно из списка файлов

4. **Удаление файла** \
Отправить GET запрос по пути http://localhost:8080/files/delete/{id}
\- где {id} - это идентификатор файла в базе данных, получить идентификатор можно из списка файлов

Предполагаемая доработка: покрытие тестами основной бизнес-логики (Unit и интеграционные), дополнительная валидация входящих данных и обработка исключений, разработка фронтенд-части.
