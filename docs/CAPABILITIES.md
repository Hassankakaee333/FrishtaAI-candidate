# Hassan AI — Capabilities

| Capability | Status | Production provider |
|------------|--------|---------------------|
| NORMAL_CHAT | NOT_CONFIGURED | hassan-honest-chat fallback |
| INTENT_ROUTING | WORKING | Android local |
| CONVERSATION_MGMT | WORKING | Android Room |
| CLOUD_PROJECTS | WORKING | Cloudflare + Neon |
| CLOUD_JOBS | WORKING | Cloudflare + GitHub Actions |
| ARTIFACTS | WORKING | GitHub Actions Artifacts via Worker |
| CODING_WORKSPACE | VERIFIED | github-actions-coding-worker |
| CODE_REVIEW | VERIFIED_MVP | pytest report + evidence |
| ANDROID_BUILD | VERIFIED | isolated fixture APK |
| PERSISTENCE | VERIFIED | neon-persistence |
| RADAR_SCAN | PARTIAL | curated cloud + local GitHub source |
| RADAR_DAILY | WORKING | Android WorkManager |
| CAPABILITY_REGISTRY | WORKING | `/v1/providers`, `/v1/capabilities/{name}` |
| VOICE_INPUT | MISSING | — |
| IMAGE_GEN | PLACEHOLDER | — |
| BIOMETRIC_APPROVAL | WORKING | Android decision flow |

Every production provider is marked `FREE`. Radar discoveries never activate
automatically and never modify Stable.
