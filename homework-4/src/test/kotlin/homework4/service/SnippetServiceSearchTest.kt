package homework4.service

import homework4.models.Snippet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for InMemorySnippetService.search() — specifically the case-insensitive fix
 * (BUG 002: search now accepts ignoreCase = true in contains call).
 *
 * Each test covers the corrected behavior and boundary conditions.
 */
class SnippetServiceSearchTest {

    private val service = InMemorySnippetService()

    @Test
    fun `search returns results matching query in lowercase when title is mixed case`() {
        service.create("Hello World", "greeting")

        val results = service.search("hello")

        assertEquals(1, results.size)
        assertEquals("Hello World", results[0].title)
    }

    @Test
    fun `search returns results matching query in uppercase when title is mixed case`() {
        service.create("Hello World", "greeting")

        val results = service.search("HELLO")

        assertEquals(1, results.size)
        assertEquals("Hello World", results[0].title)
    }

    @Test
    fun `search returns results matching query with mixed case when title is lowercase`() {
        service.create("hello world", "greeting")

        val results = service.search("HeLLo")

        assertEquals(1, results.size)
        assertEquals("hello world", results[0].title)
    }

    @Test
    fun `search returns empty list when no snippets match`() {
        service.create("Hello World", "greeting")

        val results = service.search("goodbye")

        assertEquals(0, results.size)
    }

    @Test
    fun `search filters multiple snippets and returns only matching ones`() {
        service.create("Hello World", "greeting")
        service.create("Goodbye Moon", "farewell")
        service.create("HELLO AGAIN", "greeting2")

        val results = service.search("hello")

        assertEquals(2, results.size)
        assertTrue(results.any { it.title == "Hello World" })
        assertTrue(results.any { it.title == "HELLO AGAIN" })
    }

    @Test
    fun `search is case-insensitive for partial substring matches`() {
        service.create("JavaScript Code", "language")
        service.create("Python Guide", "language")

        val results = service.search("script")

        assertEquals(1, results.size)
        assertEquals("JavaScript Code", results[0].title)
    }

    @Test
    fun `search with exact title match case-insensitive`() {
        service.create("TestSnippet", "test")

        val results = service.search("testsnippet")

        assertEquals(1, results.size)
        assertEquals("TestSnippet", results[0].title)
    }

    @Test
    fun `search returns empty list when store is empty`() {
        val results = service.search("anything")

        assertEquals(0, results.size)
    }

    @Test
    fun `search on empty query returns all snippets`() {
        service.create("First", "content1")
        service.create("Second", "content2")

        val results = service.search("")

        assertEquals(2, results.size)
    }

    @Test
    fun `search preserves snippet data in results`() {
        val created = service.create("Sample Title", "sample content")

        val results = service.search("sample")

        assertEquals(1, results.size)
        val result = results[0]
        assertEquals(created.id, result.id)
        assertEquals("Sample Title", result.title)
        assertEquals("sample content", result.content)
    }
}
