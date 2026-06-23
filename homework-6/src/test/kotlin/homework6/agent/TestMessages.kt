package homework6.agent

import homework6.common.AgentMessage
import homework6.common.Metadata
import homework6.common.TransactionData
import java.math.BigDecimal

/** Test helper: builds an AgentMessage with sensible defaults, overridable per case. */
fun txn(
    id: String = "TXN001",
    amount: String? = "1000.00",
    currency: String? = "USD",
    country: String? = "US",
    timestamp: String = "2026-03-16T09:00:00Z",
    sourceAccount: String? = "ACC-1001",
    destinationAccount: String? = "ACC-2001",
    status: String? = "received",
): AgentMessage = AgentMessage(
    messageId = "m-$id",
    timestamp = timestamp,
    sourceAgent = "integrator",
    targetAgent = "transaction_validator",
    messageType = "transaction",
    data = TransactionData(
        transactionId = id,
        timestamp = timestamp,
        sourceAccount = sourceAccount,
        destinationAccount = destinationAccount,
        amount = amount?.let { BigDecimal(it) },
        currency = currency,
        metadata = Metadata(channel = "online", country = country),
        status = status,
    ),
)
