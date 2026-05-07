package homework2.routing

import homework2.testsupport.testApp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for documentation endpoints: GET /openapi.yaml and GET /swagger.
 */
class DocumentationApiTest {

    @Test
    fun `GET openapi yaml returns 200 with YAML content`() = testApplication {
        testApp()(this)

        val response = client.get("/openapi.yaml")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("openapi"), "Expected YAML to contain 'openapi' key")
        assertTrue(body.contains("/tickets"), "Expected YAML to reference /tickets path")
    }

    @Test
    fun `GET swagger returns non-error response`() = testApplication {
        testApp()(this)

        val response = client.get("/swagger")

        assertTrue(
            response.status.value < 500,
            "Expected swagger endpoint to return non-5xx but got ${response.status}"
        )
    }
}
