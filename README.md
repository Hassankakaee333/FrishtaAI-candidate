# Hassan AI — Candidate v0.5.0

تطبيق Android شخصي Chat-first مبني بـKotlin وJetpack Compose. تبقى Stable
(`ai.hassan.app`) وCandidate (`ai.hassan.app.candidate`) منفصلتين وقابلتين
للتثبيت جنبًا إلى جنب.

## الحالة المثبتة

- Candidate يتصل مباشرة عبر HTTPS بـ
  `https://hassan-cloud.hassankakaee333.workers.dev`.
- Cloudflare Worker يوفر API والمصادقة وطبقة التنسيق.
- Neon PostgreSQL هو مصدر الحقيقة للمشاريع والوظائف والحالة وبيانات Artifacts.
- GitHub Actions يشغّل Coding Jobs وAndroid Build Jobs.
- GitHub Actions Artifacts يخزن النتائج، وWorker يقدّم تنزيلها للهاتف.
- إغلاق عملية Candidate لا يوقف المهمة؛ عند العودة تُقرأ الحالة من Neon.
- Radar موجود محليًا وعلى السحابة، ولا يفعّل أي مورد تلقائيًا.
- Capability Registry مجاني أولًا ويُبقي Chat بحالة `NOT_CONFIGURED` بلا مفتاح مدفوع.
- لا يوجد `OPENAI_API_KEY` ولا دفع ولا بطاقة.

## التحقق السريع

```powershell
.\gradlew.bat testCandidateDebugUnitTest assembleCandidateDebug
cd hassan-cloud
python -m pytest -q
cd worker
npm test
npm run typecheck
npx wrangler deploy --dry-run
```

## الإنتاج

- Public health: `GET /v1/health`
- Cloud source: `hassan-cloud/worker`
- Job workflow: `hassan-cloud/.github/workflows/hassan-job.yml`
- Android settings تحتاج URL العام وBearer token فقط.
- `adb reverse` غير مطلوب، وقد تم الاختبار والقائمة فارغة.

## حدود مقصودة

لا يوجد Media Factory كامل، Root، Shizuku، Accessibility automation، أو مزوّد
LLM مدفوع. R2 غير مستخدم لأنه يتطلب تفعيل اشتراك/Checkout؛ التخزين الحالي هو
GitHub Actions Artifacts ضمن الخطة المجانية، مع مدة احتفاظ GitHub وليست أرشفة أبدية.
