"""Custom MCP server (FastMCP) for Homework 6, Task 4.

Makes the banking pipeline queryable. Reads the terminal messages the pipeline writes to
`homework-6/shared/results/` (one `TXN*.json` per transaction, plus `pipeline-summary.*`).

Exposes:
- Tool  `get_transaction_status(transaction_id)` — current status of one transaction.
- Tool  `list_pipeline_results()`               — summary of all processed transactions.
- Resource `pipeline://summary`                  — the latest run summary as text.
"""

import json
from pathlib import Path

from fastmcp import FastMCP

# Resolve shared/results relative to THIS file (mcp/ is under homework-6/), so the server
# works regardless of the working directory the MCP client launches it from.
RESULTS_DIR = Path(__file__).resolve().parent.parent / "shared" / "results"

mcp = FastMCP("pipeline-status")


def _read_message(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


@mcp.tool
def get_transaction_status(transaction_id: str) -> dict:
    """Return the current status of a single transaction from shared/results/."""
    path = RESULTS_DIR / f"{transaction_id}.json"
    if not path.exists():
        return {"transaction_id": transaction_id, "found": False, "status": "not_found"}

    data = _read_message(path).get("data", {})
    return {
        "transaction_id": data.get("transaction_id", transaction_id),
        "found": True,
        "status": data.get("status"),
        "amount": data.get("amount"),
        "currency": data.get("currency"),
        "risk_score": data.get("risk_score"),
        "risk_reasons": data.get("risk_reasons", []),
        "reason": data.get("reason"),
    }


@mcp.tool
def list_pipeline_results() -> dict:
    """Return a summary of every processed transaction plus the run summary counts."""
    transactions = []
    for path in sorted(RESULTS_DIR.glob("TXN*.json")):
        data = _read_message(path).get("data", {})
        transactions.append(
            {
                "transaction_id": data.get("transaction_id"),
                "status": data.get("status"),
                "amount": data.get("amount"),
                "currency": data.get("currency"),
                "risk_score": data.get("risk_score"),
            }
        )

    summary_path = RESULTS_DIR / "pipeline-summary.json"
    summary = json.loads(summary_path.read_text(encoding="utf-8")) if summary_path.exists() else {}

    return {"count": len(transactions), "transactions": transactions, "summary": summary}


@mcp.resource("pipeline://summary")
def pipeline_summary() -> str:
    """Return the latest pipeline run summary as text."""
    summary_txt = RESULTS_DIR / "pipeline-summary.txt"
    if summary_txt.exists():
        return summary_txt.read_text(encoding="utf-8")
    return "No pipeline summary available. Run the pipeline first: ./gradlew :homework-6:run"


if __name__ == "__main__":
    mcp.run()
