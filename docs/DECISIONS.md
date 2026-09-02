# Technical decisions

## D-001 — Subscription products are Human-Gated

ChatGPT Plus and Gemini subscriptions are not treated as paid API entitlement. Hassan
uses Android `ACTION_SEND` TaskPacks and accepts a response only when the owner returns
it explicitly by Share Sheet or paste. No Accessibility, scraping, cookies, or session
extraction are used. The official OpenAI API quickstart requires a separate API key and
directs API users to credits/billing, so no OpenAI API client exists in the runtime:
https://platform.openai.com/docs/quickstart

## D-002 — First Radar is deliberately narrow

The first real Radar checks public latest-release endpoints for three authoritative
open-source repositories. It records version, license, URL, time, and source evidence.
It discovers and persists only; it never activates a provider or downloads a model.

## D-003 — Preserve the modular monolith

Milestone 2 extends the existing Room database with validated 1→2 and 2→3 migrations
instead of replacing the app or introducing a distributed architecture. Stable data is preserved.

## D-004 — Approval is a local deterministic transition

Only exact normalized phrases are approvals, and only while the conversation is in
`AWAITING_USER_APPROVAL`. A model response cannot grant approval or skip the state gate.

## D-005 — Cloudflare remains optional

The Android APK is fully usable offline except for Radar. Cloudflare schemas and local
Control Plane tests are maintained, but no D1, R2, Workflow, or Worker is provisioned or
deployed without credentials and a separate verification of zero-cost eligibility.

## D-006 — Codex model and reasoning are explicit user choices

The OpenAI lead is identified as `gpt-5.6-sol`. Its reasoning effort is stored per
conversation and may be `none`, `low`, `medium`, `high`, `xhigh`, or `max`; the local
default is `medium`. The selected value is written into the ExecutionPlan and TaskPack.
Because Android `ACTION_SEND` cannot enforce settings inside another app, Hassan AI
describes these as requested settings and requires the user to verify them in Codex.
