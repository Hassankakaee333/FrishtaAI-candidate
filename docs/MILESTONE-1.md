# Milestone 1 verification contract

يُعد الجزء ناجحًا فقط إذا ثبت بالأوامر أو على الجهاز:

1. Gradle unit tests وlint ينجحان.
2. `assembleCandidateDebug` ينتج APK.
3. Stable وCandidate يملكان application IDs مختلفين.
4. Candidate يُثبت عبر ADB على الهاتف المصرّح.
5. التطبيق يُفتح وتظهر `MainActivity` دون crash.
6. instrumented smoke test ينجح على الجهاز.
7. Decision demo يطلب تحققًا بيومتريًا؛ إكماله نفسه يحتاج تفاعل حسن على الهاتف.
8. Backend ينجح في typecheck/tests/dry-run، وتعمل `/health` محليًا.

## مؤجل بسبب credentials أو حدود Milestone 1

- إنشاء موارد Cloudflare الحقيقية والنشر على workers.dev.
- ربط عنوان backend بالـAPK وتسجيل الجهاز عن بُعد.
- GitHub App، private repository provisioning، وFirebase/FCM/Test Lab.
- Candidate build delivery من R2.
- Radar، Media Factory، وself-evolution.
