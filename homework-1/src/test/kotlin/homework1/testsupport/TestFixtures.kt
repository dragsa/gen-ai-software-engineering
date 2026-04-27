package homework1.testsupport

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object TestFixtures {
    private val root: JsonObject by lazy {
        Json.parseToJsonElement(loadResource("fixtures/transactions.json")).jsonObject
    }

    fun payload(name: String): String = section("payloads")[name]?.toString()
        ?: error("Missing payload fixture '$name'")

    fun filter(name: String): String = section("filters")[name]?.jsonPrimitive?.content
        ?: error("Missing filter fixture '$name'")

    fun lookup(name: String): String = section("lookups")[name]?.jsonPrimitive?.content
        ?: error("Missing lookup fixture '$name'")

    fun payloadElement(name: String): JsonElement = section("payloads")[name]
        ?: error("Missing payload fixture '$name'")

    private fun section(name: String): JsonObject = root[name]?.jsonObject
        ?: error("Missing fixture section '$name'")

    private fun loadResource(path: String): String {
        val stream = object {}.javaClass.classLoader.getResourceAsStream(path)
            ?: error("Resource not found: $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
