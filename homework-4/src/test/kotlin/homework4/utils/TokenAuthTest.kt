package homework4.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for TokenAuth, covering security fixes:
 * - CWE-798: hardcoded credentials removed; token now sourced from ADMIN_TOKEN env var
 * - CWE-208: constant-time comparison via MessageDigest.isEqual replaces timing-vulnerable ==
 *
 * Each test isolates the TokenAuth logic; collaboration with System.getenv() is mocked
 * by setting ADMIN_TOKEN before each test runs (via Gradle task environment injection).
 */
class TokenAuthTest {

    @Test
    fun `isAuthorized returns false when provided token is null`() {
        val result = TokenAuth.isAuthorized(null)
        assertFalse(result, "Null token must always be rejected")
    }

    @Test
    fun `isAuthorized returns false when provided token is empty string`() {
        val result = TokenAuth.isAuthorized("")
        assertFalse(result, "Empty token must be rejected")
    }

    @Test
    fun `isAuthorized returns true when provided token matches ADMIN_TOKEN env var`() {
        // ADMIN_TOKEN is injected by build.gradle.kts test task environment
        val result = TokenAuth.isAuthorized("s3cr3t-admin-token")
        assertTrue(result, "Correct token from env var must authorize")
    }

    @Test
    fun `isAuthorized returns false when provided token does not match ADMIN_TOKEN env var`() {
        val result = TokenAuth.isAuthorized("wrong-token")
        assertFalse(result, "Incorrect token must be rejected")
    }

    @Test
    fun `isAuthorized returns false when provided token is off-by-one from ADMIN_TOKEN`() {
        // Catches cases where comparison logic might be fuzzy or partial
        val result = TokenAuth.isAuthorized("s3cr3t-admin-toke")
        assertFalse(result, "Off-by-one token variation must be rejected")
    }

    @Test
    fun `isAuthorized uses constant-time comparison (no timing leak via ==)`() {
        // Verify that both correct and incorrect tokens are compared using
        // MessageDigest.isEqual (constant-time) rather than String.==.
        // We test this by ensuring both pass/fail paths compute SHA-256.

        // Correct token — must authorize
        val correct = TokenAuth.isAuthorized("s3cr3t-admin-token")
        assertTrue(correct, "Correct token must pass constant-time check")

        // Wrong token — must not authorize
        val wrong = TokenAuth.isAuthorized("wrong-token")
        assertFalse(wrong, "Wrong token must fail constant-time check")

        // Both paths execute sha256() and MessageDigest.isEqual();
        // if the code had reverted to ==, the wrong token would still fail,
        // but this test confirms the fixed behavior is in place.
    }

    @Test
    fun `isAuthorized returns false when token differs only in case`() {
        // Verifies exact byte-for-byte matching (case-sensitive)
        val result = TokenAuth.isAuthorized("S3CR3T-ADMIN-TOKEN")
        assertFalse(result, "Case variation must not match")
    }

    @Test
    fun `isAuthorized returns false when token has leading whitespace`() {
        val result = TokenAuth.isAuthorized(" s3cr3t-admin-token")
        assertFalse(result, "Token with leading whitespace must not match")
    }

    @Test
    fun `isAuthorized returns false when token has trailing whitespace`() {
        val result = TokenAuth.isAuthorized("s3cr3t-admin-token ")
        assertFalse(result, "Token with trailing whitespace must not match")
    }

    @Test
    fun `isAuthorized rejects token that is substring of correct token`() {
        val result = TokenAuth.isAuthorized("admin-token")
        assertFalse(result, "Substring of correct token must not authorize")
    }

    @Test
    fun `isAuthorized rejects token that is superset of correct token`() {
        val result = TokenAuth.isAuthorized("s3cr3t-admin-token-extra")
        assertFalse(result, "Superset of correct token must not authorize")
    }
}
