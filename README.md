# QA Lab · практика вместо зубрёжки

Учебный магазин на **Java 21 + Spring Boot 4.1.1**. Отправляй HTTP-запросы, проверяй ответы, работай с Git, Docker и CI/CD на одном понятном проекте.

Веб-интерфейс содержит 17 последовательных экспериментов и редактор запросов. Установка Postman для старта не нужна. Это настоящий API, а не имитация ответов в браузере.

## Начни здесь

```bash
git clone https://github.com/erlan2332/qa-practice-api.git
cd qa-practice-api
docker compose up --build -d
```

Открой **http://localhost:8080**. Первое скачивание Java и зависимостей занимает несколько минут.

Нет Docker, но есть JDK 21+? Запусти `./mvnw spring-boot:run`. На Windows: `mvnw.cmd spring-boot:run`. Maven отдельно устанавливать не нужно — Wrapper скачает нужную версию.

- [Практикум Git, Docker, CI/CD](docs/LEARNING.md)
- [Как развернуть бесплатно на Render](docs/DEPLOY.md)
- [Точный контракт API](src/main/resources/static/openapi.yaml)
- [Коллекция Postman с автопроверками](src/main/resources/static/postman/qa-lab.postman_collection.json)
- [Окружение localhost](src/main/resources/static/postman/local.postman_environment.json)
- В браузере доступны `/guide.html`, `/openapi.yaml` и `/postman/qa-lab.postman_collection.json`.

## Что можно тестировать

| Область | Примеры |
|---|---|
| HTTP | GET, POST, PUT, PATCH, DELETE, HEAD, заголовки, JSON |
| Валидация | пустое имя, отрицательная цена, границы, неверный тип, неизвестное поле |
| Авторизация | регистрация, login, logout, Bearer-токен, 401 и 403 |
| Товары | CRUD, поиск, пагинация, Location, 204 без тела |
| Заказы | склад, сумма, повторный запрос с Idempotency-Key, конфликт 409 |
| Учебная оплата | APPROVED / DECLINED, 402, переходы состояний |
| Изоляция | чужие товары и заказы возвращают 404, сброс только своей песочницы |

`/api/catalog` — общая неизменяемая витрина, доступная без входа. `/api/products` — личная копия товаров: после регистрации у каждого ученика своя. `demo-1` из витрины **не является id** личного товара; используй id из `/api/products`.

### Первый запрос

```bash
curl -i http://localhost:8080/api/catalog
```

### Создать учебного пользователя

```bash
curl -i http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"student@example.test","password":"LearnOnly123!","name":"QA Student"}'
```

Из ответа скопируй `token` в Postman: **Authorization → Bearer Token**. На сайте токен сохраняется автоматически только в памяти вкладки. После обновления страницы войди снова. Все примеры паролей — только для этого одноразового тренажёра, не для реальных аккаунтов.

## Проверить проект

```bash
./mvnw verify
python3 scripts/smoke.py http://localhost:8080
docker compose logs --tail=50
docker compose down
```

Smoke-тест создаёт уникального учебного пользователя, проходит покупки/ошибки/отмену и удаляет **только свой** аккаунт. Он не сбрасывает чужие данные. Не запускай нагрузочные тесты на публичной бесплатной версии.

## Что происходит после push

```text
ветка → Pull Request → Java tests → Docker build → HTTP smoke
                                                  ↓
                         merge в dev-prod-1 → все CI checks зелёные
                                                  ↓
                         Render: After CI Checks Pass → Docker → HTTPS
```

Workflow: [`.github/workflows/ci.yml`](.github/workflows/ci.yml). Он запускается для PR в `dev-prod-1` и push в `dev-prod-1`. Render должен быть подключён к GitHub и настроен на **After CI Checks Pass**: YAML сам по себе не подключает аккаунты. Деплой ключа/пароля в репозитории нет. CI проверяет контейнер с ограничением 512 MB; финальный контейнер работает не от root. Для PR не создаются платные preview-сервисы.

## Как устроен код

```text
src/main/java/kg/qalab/
  LabController.java   HTTP-методы и статусы
  Api.java             входные/выходные DTO и валидация
  LabStore.java        правила магазина и память песочниц
  SecurityConfig.java  Bearer, ограничения запросов, security headers
  ApiErrors.java       единый формат ошибок
src/main/resources/static/   интерфейс, практикум, OpenAPI, Postman
src/test/java/kg/qalab/      интеграционные и конкурентные тесты
scripts/smoke.py             проверки настоящего HTTP-сервера
Dockerfile / compose.yaml   локальная контейнеризация
render.yaml                 конфигурация бесплатного сервиса
```

## Честные ограничения

- **Только обучение, не production.** Данные хранятся в памяти Java, не в SQL-базе. Перезапуск, сон с перезапуском или деплой очищает пользователей, товары, токены и заказы. Это намеренно: нет платной БД и зависимости от её пробного срока.
- Аккаунт удаляется после 24 часов без запросов (проверка каждые 10 минут); токен действует 12 часов. До 5 токенов на пользователя.
- Максимум 200 пользователей, по 100 товаров и 100 заказов. Глобально: 60 login/register-запросов и 1200 остальных API-запросов в минуту, кроме health. Это базовая защита учебного демо, не полноценная anti-abuse система. Не для нагрузочного тестирования.
- Тело запроса ограничено 16 KiB. Цена — целое число KGS от 1 до 1 000 000. stock: 0–1000; quantity: 1–20. Платежи **полностью вымышленные**.
- CORS для внешних сайтов не включён. Используй встроенный интерфейс, Postman Desktop или curl.
- Одна инстанция и синхронизация в памяти. Для production нужны постоянная БД с транзакциями, распределённые лимиты, мониторинг и отдельный аудит безопасности.
- Бесплатный Render засыпает после 15 минут без трафика. Лимит бесплатных часов общий для workspace; другие сервисы могут его расходовать. Не подключай платные функции для этого урока.

## Официальные источники

[Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html) · [Docker](https://docs.docker.com/get-started/) · [GitHub Actions и Maven](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven) · [Render Free](https://render.com/docs/free) · [Render: деплой после CI](https://render.com/docs/deploys#integrating-with-ci)

Условия хостинга проверены 26 сентября 2026 года; провайдер может их изменить.
