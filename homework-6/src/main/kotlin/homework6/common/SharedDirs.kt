package homework6.common

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory

/**
 * Resolves and manages the four `shared/` subdirectories used for file-based agent communication.
 * Use [under] to anchor at `<base>/shared`, so tests can target a temp directory.
 */
data class SharedDirs(val root: Path) {
    val input: Path get() = root.resolve("input")
    val processing: Path get() = root.resolve("processing")
    val output: Path get() = root.resolve("output")
    val results: Path get() = root.resolve("results")

    fun createAll() {
        listOf(input, processing, output, results).forEach { Files.createDirectories(it) }
    }

    /** Recreates empty input/processing/output/results so a run starts from a clean state. */
    fun clearAndCreate() {
        listOf(input, processing, output, results).forEach { dir ->
            if (dir.isDirectory()) {
                Files.list(dir).use { stream -> stream.toList() }.forEach { it.deleteIfExists() }
            } else {
                Files.createDirectories(dir)
            }
        }
    }

    companion object {
        fun under(base: Path): SharedDirs = SharedDirs(base.resolve("shared"))
    }
}
