---
description: Validate all sample transactions without running the full pipeline.
allowed-tools: Bash
---

Validate all transactions in `sample-transactions.json` without processing them.

Steps:
1. Run the validator in dry-run mode: `./gradlew :homework-6:runValidator -Pargs="--dry-run" --console=plain` (from the repo root).
2. Report: total count, valid count, invalid count, and the reason for each rejection.
3. Show the table the validator prints (transaction_id | result | reason).

Expected for the sample data: total=8, valid=6, invalid=2 (TXN006 — currency `XYZ` not ISO 4217; TXN007 — amount `-100.00` not positive).
