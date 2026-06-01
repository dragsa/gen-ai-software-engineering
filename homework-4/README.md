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
| Research Verifier | `claude-opus-4-6` | Accuracy-critical gate: catching a fabricated reference or mismatched snippet here prevents a wrong fix downstream, so it uses the strongest reasoning model. |
| Bug Fixer | `claude-sonnet-4-6` | Analysis is already done; the work is mechanical (apply pre-specified edits, run tests). A balanced model gives reliable edits at lower cost — the task's "faster/cheaper for routine fixes." |
| Security Verifier | `claude-opus-4-6` | Security review rewards deep, adversarial reasoning (timing side-channels, secret handling, missing-validation paths). A missed CRITICAL is the worst failure mode, so it uses the strongest model. |
| Unit Test Generator | `claude-haiku-4-5` | Test scaffolding against an explicit target plus the FIRST checklist is the most routine, high-throughput step; a fast/cheap model fits, with the FIRST skill enforcing quality. |

## 🛠️ Stack

Kotlin 2.3.20 / JVM 21, Gradle (Kotlin DSL), Ktor 3.4.3 (Netty), kotlinx.serialization,
kotlin-test-junit — per `.agents/docs/STACK.MD`.
