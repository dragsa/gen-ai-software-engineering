# 🤖 Homework 4: 4-Agent Pipeline

> **Student Name**: Andrii Gnatiuk
> **Date Submitted**: TBD
> **AI Tools Used**: Claude (Anthropic) via Cowork

---

## 📋 Project Overview

A four-agent pipeline — **Bug Research Verifier → Bug Fixer → Security Verifier → Unit Test Generator** —
that operates on a small Kotlin/Ktor sample application with seeded bugs and a security issue.
The entire pipeline runs via a single command.

See [ROADMAP.MD](ROADMAP.MD) for the phased implementation plan and [HOWTORUN.md](HOWTORUN.md)
for build/run commands.

> **Status:** Phase 0 (scaffolding) complete. Subsequent phases add the sample app, skills,
> agents, orchestration, and execution artifacts.

## 🧩 Agent Models

| Agent | Model | Justification |
|-------|-------|---------------|
| Research Verifier | _TBD (Phase 3)_ | Strong reasoning for fact-checking references |
| Bug Fixer | _TBD (Phase 3)_ | Faster/cheaper for routine edits |
| Security Verifier | _TBD (Phase 3)_ | Strong reasoning for vulnerability review |
| Unit Test Generator | _TBD (Phase 3)_ | Mid-tier for test scaffolding |

## 🛠️ Stack

Kotlin 2.3.20 / JVM 21, Gradle (Kotlin DSL), Ktor 3.4.3 (Netty), kotlinx.serialization,
kotlin-test-junit — per `.agents/docs/STACK.MD`.
