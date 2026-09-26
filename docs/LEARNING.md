# Учебный маршрут: HTTP → Git → Docker → CI/CD

Иди маленькими итерациями: один запрос, один тест, один коммит.

## HTTP за 20 минут

Открой главную страницу и выполни эксперименты 01–04. `GET` читает, `POST` создаёт ресурс (`201 Created` и часто `Location`), `PUT` заменяет целиком, `PATCH` меняет часть, `DELETE` удаляет (`204 No Content` без JSON-тела), `HEAD` проверяет заголовки без тела.

Проверяй не только статус: тело, обязательные поля, типы, `Content-Type`, `Location`, `X-Request-Id` и отсутствие секрета в ответе.

## Статусы как разговор

| Статус | Вопрос тестировщика |
|---|---|
| 200 | Ответ соответствует контракту? |
| 201 | Есть id и Location? |
| 204 | Тело действительно пустое? |
| 400 | Ошибка понятная и данные не изменились? |
| 401 | Кто ты? Нужен Bearer-токен. |
| 403 | Тебя знаем, но роль не разрешает действие. |
| 404 | Нет ли утечки чужого ресурса? |
| 409 | Состояние конфликтует: нет остатка или повторный ключ с другим телом. |
| 402 | Учебный платёж отклонён; реальных денег нет. |
| 429 | Есть ли `Retry-After`? |

## Git: один эксперимент — один коммит

Пример: `git switch -c test/add-negative-case`, затем `./mvnw verify`, `git status`, `git diff`, `git add src/test/`, `git commit -m "test: cover negative product validation"`, `git push -u origin test/add-negative-case`. После push открой Pull Request. Не коммить токены, пароли, `.env`, `target/` и реальные персональные данные.

## Docker: одинаковая среда

Запусти `docker compose up --build -d`, проверь `curl -i http://localhost:8080/api/health`, смотри `docker compose logs -f`, останови `docker compose down`. Dockerfile двухэтапный: Maven и JDK нужны только при сборке, финальному контейнеру нужен JRE. Процесс работает от `qalab`, filesystem read-only, лимит — 512 MB.

## CI/CD

`.github/workflows/ci.yml` запускается на PR и push в `main`: `mvnw verify`, сборка Dockerfile, контейнер с лимитом 512 MB и `scripts/smoke.py` с реальными HTTP-запросами. Smoke создаёт уникальный учебный аккаунт и удаляет только его.

Render подключается к `main`, Auto-Deploy — **After CI Checks Pass**. `render.yaml` задаёт Docker runtime, Frankfurt, Free-план, `/api/health` и `checksPass`. Free-инстанс может просыпаться после 15 минут тишины; память очищается после рестарта. Это учебный сервис, не production.

## Чеклист перед merge

- [ ] Happy path и негативный сценарий.
- [ ] Границы: 0, 1, максимум, слишком большое значение.
- [ ] Повтор и порядок действий.
- [ ] 401/403 разделены.
- [ ] Чужая песочница недоступна.
- [ ] Секреты не попали в response, логи или Git.
- [ ] `./mvnw verify` и smoke проходят.
