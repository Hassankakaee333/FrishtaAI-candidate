# نشر Hassan Cloud

## الهدف

```
Phone → Internet → Hassan Cloud (HTTPS)
```

بدون PC، adb reverse، localhost، أو Tailscale.

---

## الخيار 1 — Render (موصى به للـ MVP)

### المتطلبات
- حساب GitHub (`gh auth login`)
- رفع `hassan-cloud/` إلى مستودع

### الخطوات
1. `gh auth login`
2. ارفع `hassan-cloud/` إلى GitHub
3. [dashboard.render.com](https://dashboard.render.com) → **New** → **Blueprint**
4. اختر المستودع — يقرأ `render.yaml` تلقائياً
5. من Environment انسخ `HASSAN_BOOTSTRAP_TOKEN` (يُولَّد تلقائياً)
6. في Hassan Android → الإعدادات:
   - URL: `https://hassan-cloud-xxxx.onrender.com`
   - Token: قيمة `HASSAN_BOOTSTRAP_TOKEN`
   - Provider: `auto`

### Persistence على Render
- `HASSAN_DATA_DIR=/data` مع disk 1GB
- SQLite + ملفات artifacts تبقى بعد restart

### حدود Free tier
- قد ينام الخادم بعد عدم النشاط
- worker يعمل داخل نفس process — مناسب للـ MVP وليس لساعات طويلة بدون ترقية

**الحالة:** BLOCKED — `gh auth login` مطلوب

---

## الخيار 2 — تطوير محلي (adb reverse فقط)

للاختبار مع PC متصل:

```powershell
cd hassan-cloud
$env:HASSAN_ENV="development"
$env:HASSAN_DEV_TOKEN="your-local-dev-token"
python -m uvicorn hassan_cloud.main:app --host 0.0.0.0 --port 8787
```

```powershell
adb reverse tcp:8787 tcp:8787
```

في التطبيق: `http://127.0.0.1:8787` + token من `HASSAN_DEV_TOKEN`

**ليس Hassan Cloud الحقيقي** — للتطوير فقط.

---

## الأمان

| البيئة | Token |
|--------|-------|
| development | `HASSAN_DEV_TOKEN` أو توليد تلقائي عند أول تشغيل |
| production | `HASSAN_BOOTSTRAP_TOKEN` (Render generateValue) |

- لا تضع tokens في APK أو Git
- Tokens مخزّنة كـ SHA-256 hash في SQLite
- إنشاء/إلغاء tokens: `POST /v1/auth/tokens`, `DELETE /v1/auth/tokens/{id}`

---

## Chat / LLM

بدون `OPENAI_API_KEY`: Chat يبقى `NOT_CONFIGURED` مع رد صادق.

المهام السحابية والـ artifacts **لا تعتمد** على OpenAI.

لتفعيل ChatGPT لاحقاً (اختياري):
```
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
```

---

## اختبار محلي (تم)

```powershell
python -m pytest tests/ -v   # persistence tests PASS
```

Job workflow محلي: `POST /v1/jobs` → `state=COMPLETED` + artifact خلال ~12 ثانية.

---

## نقل المزوّد

كل المسارات عبر متغيرات البيئة:

| Variable | Default |
|----------|---------|
| `HASSAN_ENV` | development |
| `HASSAN_DATA_DIR` | ./data |
| `HASSAN_DB_PATH` | {DATA_DIR}/hassan_cloud.db |
| `HASSAN_FILE_DIR` | {DATA_DIR}/files |
| `HASSAN_WORKSPACE_DIR` | {DATA_DIR}/workspaces |

يعمل على Render، Docker، VPS، أو أي host يدعم persistent disk.
