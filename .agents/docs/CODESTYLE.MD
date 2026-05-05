# Code Style Rules

Applies to all subprojects in this repository.
These rules are enforced by code review and must be followed by all agents and contributors.

---

## Imports

### No wildcard imports

Wildcard imports (`import foo.bar.*`) are **forbidden**.

**Why:** wildcard imports cause ambiguous symbol resolution when multiple packages export the same name (e.g. `io.ktor.server.application.*` and `io.ktor.server.engine.*` both export `Application`-related symbols), leading to compilation errors that are hard to diagnose.

**Rule:** every import must name exactly one symbol.

```kotlin
// BAD — forbidden
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

// GOOD — required
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
```

This applies to all `.kt` files across all subprojects without exception.

---

## General

- Follow standard Kotlin coding conventions: https://kotlinlang.org/docs/coding-conventions.html
- One class or top-level function per file where practical.
- Package names must match the directory structure exactly.
