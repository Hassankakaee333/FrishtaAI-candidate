# Hassan Cloud Security

## Tokens

| Environment | Variable | Storage |
|-------------|----------|---------|
| development | `HASSAN_DEV_TOKEN` | env only |
| production POC | `HASSAN_BOOTSTRAP_TOKEN` | Render generateValue |

- Server stores **SHA-256 hash** only (`auth/tokens.py`)
- Never commit tokens to Git
- Never embed production tokens in APK
- Health endpoint does **not** expose tokens

## API

- `POST /v1/auth/tokens` — create device token (requires valid token)
- `DELETE /v1/auth/tokens/{id}` — revoke

## Development token `hassan-phone-token-2026`

**Deprecated for public use.** Development only.

## Chat / LLM keys

`OPENAI_API_KEY` stays on server env if user opts in — never in APK.
