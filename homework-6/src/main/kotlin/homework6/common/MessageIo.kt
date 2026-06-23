package homework6.common

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.isDirectory
import kotlinx.serialization.json.Json

/** JSON read/write/move helpers shared by every agent. */
object MessageIo {

    val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun read(file: Path): AgentMessage =
        json.decodeFromString(AgentMessage.serializer(), Files.readString(file))

    /** Writes a message as `<transaction_id>.json` into [dir]. */
    fun write(dir: Path, message: AgentMessage): Path {
        Files.createDirectories(dir)
        val target = dir.resolve("${message.data.transactionId}.json")
        Files.writeString(target, json.encodeToString(AgentMessage.serializer(), message))
        return target
    }

    /** Atomically moves [file] into [processing], claiming it while the agent works. */
    fun claim(file: Path, processing: Path): Path {
        Files.createDirectories(processing)
        val dest = processing.resolve(file.fileName.toString())
        Files.move(file, dest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        return dest
    }

    /** Lists `*.json` files in [dir] (sorted, stable), excluding the summary files. */
    fun listJson(dir: Path): List<Path> {
        if (!dir.isDirectory()) return emptyList()
        return Files.list(dir).use { stream -> stream.toList() }
            .filter { it.fileName.toString().endsWith(".json") }
            .filter { !it.fileName.toString().startsWith("pipeline-summary") }
            .sortedBy { it.fileName.toString() }
    }
}
