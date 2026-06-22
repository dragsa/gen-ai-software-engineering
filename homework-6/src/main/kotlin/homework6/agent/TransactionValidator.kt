package homework6.agent

import homework6.common.AgentMessage
import homework6.common.AgentRunner
import homework6.common.AuditLogger
import homework6.common.Iso4217
import homework6.common.Samples
import homework6.common.SharedDirs
import homework6.common.TransactionData

import java.math.BigDecimal
import java.nio.file.Paths

/**
 * Runtime Agent 1. Checks required fields, positive amount, and ISO 4217 currency.
 * Pure [process] is callable from the integrator; [main] runs it as a standalone process.
 */
object TransactionValidator {

    /** Returns null when valid, otherwise the first failing-check reason. */
    fun validate(data: TransactionData): String? {
        if (data.transactionId.isBlank()) return "missing transaction_id"
        if (data.sourceAccount.isNullOrBlank()) return "missing source_account"
        if (data.destinationAccount.isNullOrBlank()) return "missing destination_account"
        val amount = data.amount ?: return "missing amount"
        if (amount <= BigDecimal.ZERO) return "amount must be positive"
        if (data.currency.isNullOrBlank()) return "missing currency"
        if (!Iso4217.isValid(data.currency)) return "currency ${data.currency} not in ISO 4217 allow-list"
        return null
    }

    fun process(message: AgentMessage): AgentMessage {
        val reason = validate(message.data)
        return if (reason == null) {
            message.copy(
                sourceAgent = "transaction_validator",
                targetAgent = "fraud_detector",
                data = message.data.copy(status = "validated", reason = null),
            )
        } else {
            message.copy(
                sourceAgent = "transaction_validator",
                targetAgent = "results",
                data = message.data.copy(status = "rejected", reason = reason),
            )
        }
    }
}

/**
 * Standalone entrypoint. `--dry-run` validates sample-transactions.json without processing;
 * otherwise it drains shared/input/ through the claim-and-clear lifecycle.
 */
fun main(args: Array<String>) {
    if (args.contains("--dry-run")) {
        val samples = Samples.load(Paths.get("sample-transactions.json"))
        var valid = 0
        var invalid = 0
        println("transaction_id | result  | reason")
        println("---------------+---------+----------------------------------")
        for (data in samples) {
            val reason = TransactionValidator.validate(data)
            if (reason == null) {
                valid++
                println("${data.transactionId.padEnd(14)} | VALID   |")
            } else {
                invalid++
                println("${data.transactionId.padEnd(14)} | INVALID | $reason")
            }
        }
        println("---------------+---------+----------------------------------")
        println("total=${samples.size} valid=$valid invalid=$invalid")
        return
    }

    val dirs = SharedDirs.under(Paths.get("."))
    dirs.createAll()
    AgentRunner.runStage(dirs.input, dirs, AuditLogger(), "transaction_validator", TransactionValidator::process)
    println("TransactionValidator: drained shared/input/")
}
