# Cloudflare Production Architecture

## Request path

```text
Hassan Candidate
  → workers.dev HTTPS
  → token hash verification in Neon
  → durable job row in Neon
  → GitHub workflow_dispatch
  → hosted runner executes and verifies
  → upload-artifact
  → callback registers metadata and marks COMPLETED
  → phone lists and downloads through Worker
```

## Why no R2

R2 has a free usage allowance, but enabling it requires completing Cloudflare
checkout/subscription. The project rule forbids a card or payment setup, so R2 is
`BLOCKED`, not silently claimed as configured.

## Artifact download

The Worker lists the run artifact with GitHub API authentication, requests the ZIP
redirect manually, then downloads the signed storage URL without forwarding the
GitHub Authorization header. ZIP extraction reads the central directory because
GitHub uses data descriptors with zero sizes in local headers.

## Secrets

The deployed Worker expects these secrets/bindings, with values never stored in Git
or the APK:

- `DATABASE_URL`
- `GITHUB_TOKEN`
- `GITHUB_CALLBACK_SECRET`
- `HASSAN_BOOTSTRAP_TOKEN`

GitHub Actions stores `HASSAN_API_URL` and `HASSAN_CALLBACK_SECRET`.

## Failure semantics

- Dispatch failure leaves a bounded error and job failure state.
- Runner failure calls the failure callback.
- Artifact upload must complete before metadata/final completion.
- Missing/expired artifact returns 404 rather than fabricated content.
- Neon remains the authoritative job state across client/process restarts.
