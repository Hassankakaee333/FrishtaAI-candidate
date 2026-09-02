# Radar 2.0

## Pipeline

```
DISCOVER → EVALUATING → TESTING → APPROVED / REJECTED → INTEGRATED (manual)
```

## User decisions (Android)

- **موافقة** → `APPROVED`
- **اختبار فقط** → `TESTING`
- **رفض** → `REJECTED` (hidden from feed, stored in Room)

Rejection memory: rejected candidates are filtered from Radar UI unless re-discovered with major changes (future).

## Server candidates

`POST /v1/radar/scan` seeds candidates (Ollama, OpenHands, llama.cpp).
`POST /v1/radar/candidates/{id}/evaluate` moves pipeline state.

## Capability Registry link

Approved candidates map to capabilities in logs — **never auto-integrate into Stable**.

## CLOUD_SERVICE type

Planned: discover free hosting/storage/workers (evaluation only, no auto-migration).
