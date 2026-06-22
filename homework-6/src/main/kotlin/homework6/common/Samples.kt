package homework6.common

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.builtins.ListSerializer

/** Loads the raw transaction records from sample-transactions.json. */
object Samples {
    fun load(path: Path): List<TransactionData> =
        MessageIo.json.decodeFromString(
            ListSerializer(TransactionData.serializer()),
            Files.readString(path),
        )
}
