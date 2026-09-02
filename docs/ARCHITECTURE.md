# Hassan AI — Architecture

## Android Candidate

- UI: Jetpack Compose وRTL عربي.
- State: `MainViewModel` + `HassanRepository` + Room.
- Cloud client: `HassanCloudApi` للمصادقة والمشاريع والوظائف وArtifacts.
- Resume: `MainActivity.onResume` و`CloudJobSyncWorker` يعيدان المزامنة.
- Production package: `ai.hassan.app.candidate`.

## Production cloud

```text
Android Candidate
    │ HTTPS + Bearer token
    ▼
Cloudflare Worker (Hono/TypeScript)
    ├── Neon PostgreSQL: projects, jobs, tokens, artifact metadata, radar
    ├── GitHub Actions: coding and android_build execution
    └── GitHub Actions Artifacts: logs, reports, ZIPs, APKs
```

Cloudflare Worker لا يحتفظ بحالة تنفيذ داخل الذاكرة. Neon هو مصدر الحقيقة، لذلك
تستمر المهمة بعد إغلاق التطبيق أو انتهاء طلب HTTP. عند اكتمال Workflow تُرفع
الملفات أولًا، ثم تُسجل metadata، ثم تنتقل المهمة إلى `COMPLETED`.

## Public API v0.5

```text
GET  /v1/health
POST /v1/auth/verify | /v1/auth/tokens | DELETE /v1/auth/tokens/{id}
GET/POST /v1/projects | GET /v1/projects/{id}/workspace
POST /v1/jobs | GET /v1/jobs | GET /v1/jobs/{id} | POST /v1/jobs/{id}/cancel
GET  /v1/artifacts | GET /v1/files/{artifact_id}
GET  /v1/providers | GET /v1/capabilities/{name}
POST /v1/radar/scan | GET /v1/radar/candidates
POST /v1/radar/candidates/{id}/evaluate
```

## Local reference server

`hassan-cloud/hassan_cloud` يبقى تطبيق FastAPI مرجعيًا للتطوير المحلي ولعقد
API المشترك. الإنتاج الحالي يستخدم Worker + Neon + GitHub Actions، وليس SQLite
أو filesystem محليًا.

## Security boundaries

- Bearer tokens محفوظة كـSHA-256 فقط في Neon.
- GitHub token وNeon URL وcallback secret هي Cloudflare/GitHub secrets وليست في Git أو APK.
- Callback داخلي منفصل عن token الهاتف.
- CORS العام غير مفعّل؛ Android الأصلي لا يحتاج CORS.
- ردود API تحمل `no-store` وheaders حماية أساسية.
- Stable لم يُعدّل ولم يُحذف.

## Status labels

`WORKING` | `PARTIAL` | `HUMAN_GATED` | `NOT_CONFIGURED` |
`NOT_VERIFIED` | `BLOCKED`
