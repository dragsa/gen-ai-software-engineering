# Bug 001 — Oversized title accepted

- **Type:** functional
- **Reported by:** API user

## Symptom

The API accepts a snippet `title` that is longer than the documented maximum length.
According to the API docs, `title` must be **1–50 characters**, but a slightly longer title
is stored successfully instead of being rejected.

## Reproduction

1. `POST /snippets` with a valid `X-Api-Token` and a `title` of **51 characters**.
2. **Observed:** `201 Created` (the snippet is stored).
3. **Expected:** `400 Bad Request` with a `title` validation error.

> Root cause, exact location, and fix are intentionally left for the pipeline to determine
> (research → verify → plan → fix).
