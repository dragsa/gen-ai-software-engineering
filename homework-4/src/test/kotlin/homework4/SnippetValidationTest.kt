package homework4

import homework4.entrypoint.module
import homework4.service.InMemorySnippetService
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * HTTP-level integration tests for title validation, covering the off-by-one fix.
 * These tests verify end-to-end behavior when the SnippetValidator condition is applied.
 */
class SnippetValidationTest {

    @Test
    fun `reject title of exactly 51 characters (boundary, fixed bug)`() = testApplication {
        application { module(InMemorySnippetService()) }

        val title = "A".repeat(51)
        val response = client.post("/snippets") {
            header("X-Api-Token", "s3cr3t-admin-token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"$title","content":"some content"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("title"), "Error response must reference the title field")
    }

    @Test
    fun `accept title of exactly 50 characters (boundary, valid)`() = testApplication {
        application { module(InMemorySnippetService()) }

        val title = "A".repeat(50)
        val response = client.post("/snippets") {
            header("X-Api-Token", "s3cr3t-admin-token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"$title","content":"some content"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `reject title of 52 characters (beyond boundary)`() = testApplication {
        application { module(InMemorySnippetService()) }

        val title = "A".repeat(52)
        val response = client.post("/snippets") {
            header("X-Api-Token", "s3cr3t-admin-token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"$title","content":"some content"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `reject empty title`() = testApplication {
        application { module(InMemorySnippetService()) }

        val response = client.post("/snippets") {
            header("X-Api-Token", "s3cr3t-admin-token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"","content":"some content"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("title"))
    }

    @Test
    fun `reject empty content`() = testApplication {
        application { module(InMemorySnippetService()) }

        val response = client.post("/snippets") {
            header("X-Api-Token", "s3cr3t-admin-token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Valid Title","content":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("content"))
    }

    @Test
    fun `reject both empty title and empty content`() = testApplication {
        application { module(InMemorySnippetService()) }

        val response = client.post("/snippets") {
            header("X-Api-Token", "s3cr3t-admin-token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"","content":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("title"), "Error must reference title")
        assertTrue(body.contains("content"), "Error must reference content")
    }
}
