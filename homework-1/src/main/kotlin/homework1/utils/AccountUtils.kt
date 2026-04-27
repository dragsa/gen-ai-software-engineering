package homework1.utils

private val accountPattern = Regex("^ACC-[A-Za-z0-9]{5}$")

fun isValidAccount(accountId: String): Boolean = accountPattern.matches(accountId)
