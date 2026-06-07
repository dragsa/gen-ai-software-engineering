package homework4.validation

import homework4.models.CreateSnippetRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for SnippetValidator, covering the off-by-one fix in title validation.
 *
 * The fix corrected the condition from `> MAX_TITLE_LENGTH + 1` to `> MAX_TITLE_LENGTH`,
 * ensuring that 51-character titles (exceeding the 50-char limit) are now properly rejected.
 */
class SnippetValidatorTest {

    // ===== Title Length Tests (Fixed Boundary) =====

    @Test
    fun `accept title of exactly 50 characters (boundary, valid)`() {
        val title = "A".repeat(50)
        val request = CreateSnippetRequest(title = title, content = "some content")
        val errors = SnippetValidator.validate(request)
        assertEquals(emptyList(), errors, "50-char title should be valid (at MAX_TITLE_LENGTH)")
    }

    @Test
    fun `reject title of exactly 51 characters (boundary, invalid — the fixed bug)`() {
        val title = "A".repeat(51)
        val request = CreateSnippetRequest(title = title, content = "some content")
        val errors = SnippetValidator.validate(request)
        assertEquals(1, errors.size, "51-char title must trigger validation error")
        assertEquals("title", errors[0].field)
        assertTrue(
            errors[0].message.contains("between 1 and 50"),
            "Error message must reference the 50-char limit"
        )
    }

    @Test
    fun `reject title of 52 characters (beyond boundary)`() {
        val title = "A".repeat(52)
        val request = CreateSnippetRequest(title = title, content = "some content")
        val errors = SnippetValidator.validate(request)
        assertEquals(1, errors.size, "52-char title must trigger validation error")
        assertEquals("title", errors[0].field)
    }

    @Test
    fun `accept title of 1 character (minimum boundary, valid)`() {
        val request = CreateSnippetRequest(title = "A", content = "some content")
        val errors = SnippetValidator.validate(request)
        assertEquals(emptyList(), errors, "1-char title should be valid")
    }

    @Test
    fun `reject empty title (minimum boundary, invalid)`() {
        val request = CreateSnippetRequest(title = "", content = "some content")
        val errors = SnippetValidator.validate(request)
        assertEquals(1, errors.size, "Empty title must trigger validation error")
        assertEquals("title", errors[0].field)
    }

    // ===== Content Validation Tests =====

    @Test
    fun `reject empty content`() {
        val request = CreateSnippetRequest(title = "Valid Title", content = "")
        val errors = SnippetValidator.validate(request)
        assertEquals(1, errors.size, "Empty content must trigger validation error")
        assertEquals("content", errors[0].field)
    }

    @Test
    fun `accept valid content (non-empty)`() {
        val request = CreateSnippetRequest(title = "Valid Title", content = "some content")
        val errors = SnippetValidator.validate(request)
        assertEquals(emptyList(), errors, "Non-empty content should be valid")
    }

    // ===== Combined Error Tests =====

    @Test
    fun `reject both empty title and empty content`() {
        val request = CreateSnippetRequest(title = "", content = "")
        val errors = SnippetValidator.validate(request)
        assertEquals(2, errors.size, "Both empty fields must trigger 2 errors")
        assertTrue(errors.any { it.field == "title" }, "Should contain title error")
        assertTrue(errors.any { it.field == "content" }, "Should contain content error")
    }

    @Test
    fun `reject oversized title and empty content together`() {
        val request = CreateSnippetRequest(title = "A".repeat(51), content = "")
        val errors = SnippetValidator.validate(request)
        assertEquals(2, errors.size, "Both invalid fields must trigger 2 errors")
        assertTrue(errors.any { it.field == "title" }, "Should contain title error")
        assertTrue(errors.any { it.field == "content" }, "Should contain content error")
    }

    // ===== Happy Path =====

    @Test
    fun `accept valid snippet request`() {
        val request = CreateSnippetRequest(
            title = "My Kotlin Snippet",
            content = "fun main() { println(\"Hello, World!\") }"
        )
        val errors = SnippetValidator.validate(request)
        assertEquals(emptyList(), errors, "Valid request should produce no errors")
    }
}
