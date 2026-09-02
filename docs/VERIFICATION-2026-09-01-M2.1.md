# Hassan AI 0.2.1 verification — Codex reasoning selector

Date: 2026-09-01  
Device: Samsung Galaxy S25 Ultra, `SM-S938B`, Android API 36  
Candidate package: `ai.hassan.app.candidate`

## Delivered behavior

- The OpenAI lead is labeled `Codex 5.6 Sol` and carries model ID `gpt-5.6-sol`.
- The user can choose `none`, `low`, `medium`, `high`, `xhigh`, or `max` from Arabic UI labels.
- A new conversation defaults to `medium`.
- The selected effort is persisted in Room per conversation.
- ExecutionPlan displays the requested model and effort.
- Human-Gated TaskPack includes `Requested model` and `Requested reasoning effort`.
- The TaskPack states that Android Share cannot enforce settings in another app and asks the user to verify them in Codex.

Official model source: https://developers.openai.com/api/docs/models/gpt-5.6-sol

## Verified results

- Candidate unit tests: 10 passed, 0 failed.
- Galaxy instrumentation/UI tests: 8 passed, 0 failed, 0 skipped.
- The new device test selected `high` and verified the persisted value was exactly `high`.
- Candidate lint: 0 errors, 15 pre-existing/non-blocking warnings.
- Candidate APK build: passed.
- Stable APK build: passed.
- Existing Candidate was upgraded in place from Room schema 2 to 3 and cold-launched successfully in 421 ms before the test runner cleanup.
- Final Candidate 0.2.1 was reinstalled after tests and cold-launched in 483 ms.
- Final installed package reports `versionCode=3`, `versionName=0.2.1-candidate`.
- Final activity was top-resumed and the crash buffer contained no Candidate match.
- Additional money spent: 0.

## Artifacts

- `artifacts/Hassan-AI-Candidate-0.2.1-debug.apk`
  - SHA-256: `F05A1AB2CF7555785488C9F1216E65BD2D0F02402D2FA0E1D518DB36E5D4B6F0`
- `artifacts/Hassan-AI-Stable-0.2.1-debug.apk`
  - SHA-256: `315E89B59B133FA93C56DCFDBADEDD7FC0B6F359260495AF2A03D2C85FEAA7B1`

## Truth boundary

The current Android bridge launches the installed ChatGPT application through an explicit user-approved `ACTION_SEND`. It does not and cannot prove that the receiving app selected the requested model or reasoning level. Hassan AI therefore records these as requested settings, not verified external runtime settings. A future official authenticated integration would be required for programmatic enforcement.
