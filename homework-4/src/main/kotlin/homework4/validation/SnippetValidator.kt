package homework4.validation

import homework4.models.CreateSnippetRequest
import homework4.models.ValidationError

/**
 * Validates snippet creation requests.
 *
 * Documented contract:
 *   - title: required, 1..[MAX_TITLE_LENGTH] characters
 *   - content: required, non-empty
 *
 * Returns a structured list of errors. Does not throw for business-validation failures.
 */
object SnippetValidator {
    const val MAX_TITLE_LENGTH = 50

    fun validate(request: CreateSnippetRequest): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // BUG A (off-by-one): contract caps title at MAX_TITLE_LENGTH (50), but this
        // check uses `> MAX_TITLE_LENGTH + 1`, so a 51-character title is wrongly accepted.
        if (request.title.isEmpty() || request.title.length > MAX_TITLE_LENGTH) {
            errors.add(
                ValidationError(
                    field = "title",
                    message = "title must be between 1 and $MAX_TITLE_LENGTH characters",
                ),
            )
        }

        if (request.content.isEmpty()) {
            errors.add(ValidationError(field = "content", message = "content must not be empty"))
        }

        return errors
    }
}
