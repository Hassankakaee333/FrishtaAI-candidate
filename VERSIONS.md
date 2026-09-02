# Build versions

تم تثبيت الإصدارات في 2026-09-01 بعد التحقق من البيئة المحلية ووثائق الإصدارات الرسمية.

| Component | Version | Rationale |
|---|---:|---|
| Hassan AI Candidate / Stable | 0.2.1 | اختيار `gpt-5.6-sol` ومستوى التفكير محفوظ لكل محادثة |
| compileSdk / targetSdk | 36 | يطابق الـbrief والـSDK المثبت محليًا |
| minSdk | 33 | جهاز واحد حديث وتقليل فروع التوافق |
| Android Gradle Plugin | 9.0.1 | مستقر ومتوافق مع API 36.1 وGradle 9.1 |
| Gradle wrapper | 9.1.0 | الإصدار المطلوب من AGP 9.0 والموجود محليًا |
| Kotlin / Compose compiler | 2.2.21 | إصدار مستقر متوافق مع AGP 9.0؛ تجنب Kotlin 2.3 مع إصدارات AGP 9.0 المبكرة |
| Compose BOM | 2026.04.01 | stable BOM مناسب لعائلة compileSdk 36 |
| Room | 2.8.4 | الإصدار المستقر لحزمة Android `androidx.room` |
| KSP | 2.3.11 | KSP2 المستقر؛ مستخدم لتوليد Room فقط |
| Cloudflare Wrangler | 4.127.1 | أحدث إصدار منشور وقت التنفيذ |
| TypeScript | 7.0.2 | أحدث إصدار منشور وقت التنفيذ |
| Zod | 4.5.4 | validation صريح لحدود HTTP |
| Vitest / Workers pool | 4.1.11 / 0.22.0 | peer versions متوافقة |

لم نستخدم Compose BOM 2026.08.00 لأنه يضم مكتبات تتطلب compileSdk 37، بينما الـbrief المجمد يطلب API 36.
