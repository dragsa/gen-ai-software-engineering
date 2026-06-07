# Codebase Research — Bug 001: Oversized Title Accepted

## Reproduced Symptom

A `POST /snippets` request with a `title` of exactly 51 characters returns `201 Created`
instead of `400 Bad Request`. The documented contract (OpenAPI spec) specifies `maxLength: 50`
for the `title` field.

## Call Path Trace

1. **Entry point:** `POST /snippets` is handled in `SnippetRoutes.kt`.
   - `src/main/kotlin/homework4/routing/SnippetRoutes.kt:21` — the route is registered.
   - `src/main/kotlin/homework4/routing/SnippetRoutes.kt:31` — `SnippetValidator.validate(request)` is called to validate the incoming request.

2. **Validation logic:** `SnippetValidator.validate()` in `SnippetValidator.kt`.
   - `src/main/kotlin/homework4/validation/SnippetValidator.kt:16` — `MAX_TITLE_LENGTH` is correctly defined as `50`.
   - `src/main/kotlin/homework4/validation/SnippetValidator.kt:23` — the length check condition:
     ```kotlin
     if (request.title.isEmpty() || request.title.length > MAX_TITLE_LENGTH + 1) {
     ```

3. **Expected behavior if valid:** the snippet is created and returned with `201 Created` at
   `src/main/kotlin/homework4/routing/SnippetRoutes.kt:37`.

## Root-Cause Claim

The title length validation at `SnippetValidator.kt:23` uses **`> MAX_TITLE_LENGTH + 1`**
(i.e., `> 51`) instead of the correct **`> MAX_TITLE_LENGTH`** (i.e., `> 50`). This is a
classic off-by-one error: a title of exactly 51 characters evaluates `51 > 51` → `false`,
so the validation passes and the oversized title is accepted.

The correct condition should be `request.title.length > MAX_TITLE_LENGTH` (or equivalently
`request.title.length >= MAX_TITLE_LENGTH + 1`), which would reject any title longer than
50 characters.

## Documented Contract (confirmation)

The OpenAPI specification at `src/main/resources/openapi.yaml:83-84` confirms the intended
constraint:

```yaml
title:
  type: string
  minLength: 1
  maxLength: 50
```

## References

| Location | Verbatim Snippet |
|----------|-----------------|
| `src/main/kotlin/homework4/validation/SnippetValidator.kt:16` | `const val MAX_TITLE_LENGTH = 50` |
| `src/main/kotlin/homework4/validation/SnippetValidator.kt:23` | `if (request.title.isEmpty() \|\| request.title.length > MAX_TITLE_LENGTH + 1) {` |
| `src/main/kotlin/homework4/routing/SnippetRoutes.kt:21` | `post("/snippets") {` |
| `src/main/kotlin/homework4/routing/SnippetRoutes.kt:31` | `val errors = SnippetValidator.validate(request)` |
| `src/main/kotlin/homework4/routing/SnippetRoutes.kt:37` | `val created = service.create(request.title, request.content)` |
| `src/main/resources/openapi.yaml:83` | `maxLength: 50` |
