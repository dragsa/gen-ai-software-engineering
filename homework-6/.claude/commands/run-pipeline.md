---
description: Run the multi-agent banking pipeline end-to-end and summarize results.
allowed-tools: Bash, Read
---

Run the multi-agent banking pipeline end-to-end.

Steps:
1. Check that `sample-transactions.json` exists in the homework-6 directory.
2. Clear the `shared/` directories (`input/`, `processing/`, `output/`, `results/`).
3. Run the pipeline: `./gradlew :homework-6:run --console=plain` (from the repo root).
4. Show a summary of results from `shared/results/` — read and print `shared/results/pipeline-summary.txt`.
5. Report any transactions that were **rejected** and why (read each `shared/results/TXN*.json` with `status: "rejected"` and print its `reason`).

Notes:
- The integrator recreates and clears `shared/` on each run, so step 2 is also enforced by the program.
- `shared/processing/` must be empty after a successful run (claim-and-clear lifecycle).
