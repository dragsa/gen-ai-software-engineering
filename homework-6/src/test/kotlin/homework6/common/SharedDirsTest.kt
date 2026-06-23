package homework6.common

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedDirsTest {

    @Test
    fun underResolvesSharedSubdir() {
        val base = createTempDirectory("hw6-shared")
        val dirs = SharedDirs.under(base)
        assertEquals(base.resolve("shared").resolve("input"), dirs.input)
    }

    @Test
    fun clearAndCreatePreservesGitkeepButRemovesMessages() {
        val base = createTempDirectory("hw6-shared")
        val dirs = SharedDirs.under(base)
        dirs.createAll()

        // A placeholder that must survive, and a stale message that must not.
        Files.writeString(dirs.input.resolve(".gitkeep"), "")
        Files.writeString(dirs.input.resolve("TXN001.json"), "{}")

        dirs.clearAndCreate()

        assertTrue(dirs.input.resolve(".gitkeep").exists(), ".gitkeep must be preserved")
        assertFalse(dirs.input.resolve("TXN001.json").exists(), "stale message must be cleared")
        assertTrue(dirs.processing.exists() && dirs.output.exists() && dirs.results.exists())
    }
}
