package com.unbound.messageme.domain

data class OnboardingProfile(
    val firstName: String,
    val lastName: String,
    val email: String
)

data class OnboardingValidation(
    val ok: Boolean,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val value: OnboardingProfile? = null
)

/**
 * Passwordless profile: first name, last name, and a valid email.
 * This is the only sign-in. There is no password.
 */
object ProfileOnboarding {
    private const val NAME_MAX = 80
    private const val EMAIL_MAX = 254
    private val EMAIL_PATTERN = Regex(
        "^[A-Za-z0-9.!#\$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    )

    fun isValidEmail(value: String): Boolean {
        val email = value.trim()
        if (email.length < 3 || email.length > EMAIL_MAX) return false
        if (email.contains(' ')) return false
        return EMAIL_PATTERN.matches(email)
    }

    fun isValidName(value: String): Boolean {
        val name = value.trim()
        return name.length in 1..NAME_MAX
    }

    fun validate(firstName: String, lastName: String, email: String): OnboardingValidation {
        val first = firstName.trim()
        val last = lastName.trim()
        val mail = email.trim()
        val firstError = if (!isValidName(first)) "Enter your first name." else null
        val lastError = if (!isValidName(last)) "Enter your last name." else null
        val emailError = if (!isValidEmail(mail)) "Enter a valid email." else null
        if (firstError != null || lastError != null || emailError != null) {
            return OnboardingValidation(
                ok = false,
                firstNameError = firstError,
                lastNameError = lastError,
                emailError = emailError
            )
        }
        return OnboardingValidation(
            ok = true,
            value = OnboardingProfile(
                firstName = first,
                lastName = last,
                email = mail.lowercase()
            )
        )
    }
}
