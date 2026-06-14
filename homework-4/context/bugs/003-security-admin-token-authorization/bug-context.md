# Bug 003 — Concerns about admin token authorization

- **Type:** security
- **Reported by:** security review request

## Concern

Write operations (`POST /snippets`) are gated by a single shared admin token sent in the
`X-Api-Token` header. A reviewer raised concerns about how this token is **stored** and how
incoming tokens are **compared** during authorization, and asked for the authorization path
to be assessed against secure-handling practices.

## Observable behavior

- The same static token authorizes every write request.
- There is no token rotation or per-client credential.

## Reproduction / how to assess

1. Exercise authorization via `POST /snippets` with and without `X-Api-Token`.
2. Review the authorization code path for how the expected token is sourced and compared.

> The specific weaknesses, their exact location, severity, and remediation are intentionally
> left for the pipeline to determine (research/security review → plan → fix → verify).
