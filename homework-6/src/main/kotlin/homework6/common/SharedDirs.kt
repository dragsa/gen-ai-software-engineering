package homework6.common

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

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

    /**
     * Ensures input/processing/output/results exist and clears stale message files so a run starts
     * clean. The `.gitkeep` placeholders that keep the (otherwise empty) directories in git are
     * preserved.
     */
    fun clearAndCreate() {
        listOf(input, processing, output, results).forEach { dir ->
            Files.createDirectories(dir)
            Files.list(dir).use { stream -> stream.toList() }
                .filter { it.fileName.toString() != ".gitkeep" }
                .forEach { it.deleteIfExists() }
        }
    }

    companion object {
        fun under(base: Path): SharedDirs = SharedDirs(base.resolve("shared"))
    }
}
