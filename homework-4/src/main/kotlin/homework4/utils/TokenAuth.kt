package homework4.utils

/**
 * Token-based authorization for write operations.
 *
 * SECURITY ISSUE (seeded):
 *   1. Hardcoded secret — the admin token is embedded directly in source and would be
 *      committed to version control.
 *   2. Insecure comparison — `==` performs a non-constant-time String comparison, which is
 *      vulnerable to timing attacks. Token comparison should use a constant-time check and
 *      the secret should come from configuration/environment, not a literal.
 */
object TokenAuth {
    // Hardcoded secret (seeded vulnerability).
    private const val ADMIN_TOKEN = "s3cr3t-admin-token"

    fun isAuthorized(providedToken: String?): Boolean {
        if (providedToken == null) return false
        // Non-constant-time comparison (seeded vulnerability).
        return providedToken == ADMIN_TOKEN
    }
}
