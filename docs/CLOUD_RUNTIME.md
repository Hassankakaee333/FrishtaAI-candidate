# Hassan Cloud Runtime

## Production

| Layer | Runtime | State |
|-------|---------|-------|
| HTTPS/API | Cloudflare Worker | stateless |
| Persistence | Neon PostgreSQL | source of truth |
| Execution | GitHub Actions | dispatched per job |
| Binary results | GitHub Actions Artifacts | retention-limited |
| Mobile client | Android Candidate | URL + Bearer token |

The Worker returns quickly after dispatch. GitHub Actions updates the durable job
through an internal callback secret. Artifact metadata is registered only after
upload succeeds.

## Local reference

FastAPI + SQLite remains available for local tests and API-contract comparison.
It is not the current public production runtime.

## Guarantees verified in this POC

- Public HTTPS and authenticated mobile access.
- Durable close/resume across Android process death.
- Coding and Android build jobs.
- Downloadable artifacts without `adb reverse`.

## Non-guarantees

- GitHub artifact retention is finite.
- The Android build fixture is not a full Candidate release pipeline.
- No paid availability SLA and no always-on private worker.
