# Bug 002 — Search misses matching snippets

- **Type:** functional
- **Reported by:** API user

## Symptom

Searching for a snippet by title returns no results unless the query matches the stored
title's capitalization exactly. Searches that differ only in letter case come back empty,
even though a matching snippet exists.

## Reproduction

1. Create a snippet with title `Hello World`.
2. `GET /snippets?q=hello`.
3. **Observed:** empty array `[]`.
4. **Expected:** the `Hello World` snippet is returned.

> Root cause, exact location, and fix are intentionally left for the pipeline to determine
> (research → verify → plan → fix).
