package homework2.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Category(val value: String) {
    @SerialName("account_access")   ACCOUNT_ACCESS("account_access"),
    @SerialName("technical_issue")  TECHNICAL_ISSUE("technical_issue"),
    @SerialName("billing_question") BILLING_QUESTION("billing_question"),
    @SerialName("feature_request")  FEATURE_REQUEST("feature_request"),
    @SerialName("bug_report")       BUG_REPORT("bug_report"),
    @SerialName("other")            OTHER("other");

    companion object {
        fun fromValue(s: String): Category? = entries.firstOrNull { it.value == s.lowercase() }
    }
}

@Serializable
enum class Priority(val value: String) {
    @SerialName("urgent") URGENT("urgent"),
    @SerialName("high")   HIGH("high"),
    @SerialName("medium") MEDIUM("medium"),
    @SerialName("low")    LOW("low");

    companion object {
        fun fromValue(s: String): Priority? = entries.firstOrNull { it.value == s.lowercase() }
    }
}

@Serializable
enum class Status(val value: String) {
    @SerialName("new")              NEW("new"),
    @SerialName("in_progress")      IN_PROGRESS("in_progress"),
    @SerialName("waiting_customer") WAITING_CUSTOMER("waiting_customer"),
    @SerialName("resolved")         RESOLVED("resolved"),
    @SerialName("closed")           CLOSED("closed");

    companion object {
        fun fromValue(s: String): Status? = entries.firstOrNull { it.value == s.lowercase() }
    }
}

@Serializable
enum class Source(val value: String) {
    @SerialName("web_form") WEB_FORM("web_form"),
    @SerialName("email")    EMAIL("email"),
    @SerialName("api")      API("api"),
    @SerialName("chat")     CHAT("chat"),
    @SerialName("phone")    PHONE("phone");

    companion object {
        fun fromValue(s: String): Source? = entries.firstOrNull { it.value == s.lowercase() }
    }
}

@Serializable
enum class DeviceType(val value: String) {
    @SerialName("desktop") DESKTOP("desktop"),
    @SerialName("mobile")  MOBILE("mobile"),
    @SerialName("tablet")  TABLET("tablet");

    companion object {
        fun fromValue(s: String): DeviceType? = entries.firstOrNull { it.value == s.lowercase() }
    }
}
