# Бесплатный Render: пошагово

1. Открой Render: **New → Web Service → GitHub** и выбери `erlan2332/qa-practice-api`.
2. Проверь Docker runtime, регион `Frankfurt`, plan `Free` и health path `/api/health`.
3. Auto-Deploy: **After CI Checks Pass**.
4. Сделай первый deploy после зелёного GitHub Actions и открой `https://<имя>.onrender.com/`.

Не добавляй Postgres, persistent disk или платный instance: сервис намеренно использует временную память. Render Free засыпает после 15 минут тишины, а filesystem ephemeral. Это учебное публичное демо, не production.

Проверка после deploy: `curl -i https://<имя>.onrender.com/api/health` и `python3 scripts/smoke.py https://<имя>.onrender.com`. Если первый запрос ждёт, Free-инстанс просыпается. Render передаёт `PORT=10000`, это уже описано в `render.yaml`.
