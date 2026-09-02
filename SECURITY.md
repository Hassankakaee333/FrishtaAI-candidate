# Security posture — Milestone 1

- المفتاح الخاص لهوية الجهاز لا يغادر Android Keystore.
- StrongBox يُطلب عند توفره، والفشل ينتقل إلى hardware-backed Keystore دون تعطيل التطبيق.
- الموافقة التجريبية تمر عبر BiometricPrompt قبل التوقيع.
- Stable وCandidate لهما application IDs وdata directories منفصلة.
- لا توجد أسرار أو API keys أو وسائل دفع في المستودع أو الـAPK.
- Cloudflare backend الحالي bootstrap محلي وليس production authentication boundary.
- التحقق من توقيع الجهاز على الخادم، device attestation، replay protection، وrate limiting مطلوبة قبل أي نشر production.
- لا يستطيع التطبيق تحديث Stable ذاتيًا في Milestone 1.
