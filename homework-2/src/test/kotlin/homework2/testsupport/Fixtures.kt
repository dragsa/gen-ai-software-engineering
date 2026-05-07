package homework2.testsupport

/**
 * Loads test fixture files from src/test/resources/fixtures/.
 * Provides inline JSON payload builders for API tests.
 */
object Fixtures {

    fun text(name: String): String =
        bytes(name).toString(Charsets.UTF_8)

    fun bytes(name: String): ByteArray {
        val path = "fixtures/$name"
        val stream = object {}.javaClass.classLoader.getResourceAsStream(path)
            ?: error("Fixture not found: $path")
        return stream.use { it.readBytes() }
    }

    fun validCreateRequest(
        customerId: String    = "cust-001",
        email: String         = "alice@example.com",
        name: String          = "Alice Smith",
        subject: String       = "Cannot login to my account",
        description: String   = "I have been unable to login since yesterday morning",
        category: String?     = null,
        priority: String?     = null
    ): String {
        val categoryField  = if (category != null) """"category": "$category",""" else ""
        val priorityField  = if (priority != null) """"priority": "$priority",""" else ""
        return """
            {
              "customer_id": "$customerId",
              "customer_email": "$email",
              "customer_name": "$name",
              "subject": "$subject",
              "description": "$description",
              $categoryField
              $priorityField
              "metadata": { "source": "api" }
            }
        """.trimIndent()
    }

    fun minimalCreateRequest(): String = validCreateRequest()
}
