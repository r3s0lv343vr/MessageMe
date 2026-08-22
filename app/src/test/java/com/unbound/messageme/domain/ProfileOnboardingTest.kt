package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProfileOnboardingTest {
    @Test
    fun acceptsOrdinaryEmail() {
        assertThat(ProfileOnboarding.isValidEmail("craig@example.com")).isTrue()
        assertThat(ProfileOnboarding.isValidEmail("  a.b+tag@sub.mail.co.uk  ")).isTrue()
    }

    @Test
    fun rejectsIncompleteEmail() {
        assertThat(ProfileOnboarding.isValidEmail("")).isFalse()
        assertThat(ProfileOnboarding.isValidEmail("not-an-email")).isFalse()
        assertThat(ProfileOnboarding.isValidEmail("foo@")).isFalse()
        assertThat(ProfileOnboarding.isValidEmail("@bar.com")).isFalse()
        assertThat(ProfileOnboarding.isValidEmail("foo@bar")).isFalse()
        assertThat(ProfileOnboarding.isValidEmail("foo @bar.com")).isFalse()
    }

    @Test
    fun requiresNameAndValidEmail() {
        val result = ProfileOnboarding.validate(" ", "", "hello")
        assertThat(result.ok).isFalse()
        assertThat(result.firstNameError).isEqualTo("Enter your first name.")
        assertThat(result.lastNameError).isEqualTo("Enter your last name.")
        assertThat(result.emailError).isEqualTo("Enter a valid email.")
    }

    @Test
    fun trimsNamesAndLowercasesEmail() {
        val result = ProfileOnboarding.validate("  Craig ", "Ferguson", "Craig.J@Example.COM")
        assertThat(result.ok).isTrue()
        assertThat(result.value).isEqualTo(
            OnboardingProfile("Craig", "Ferguson", "craig.j@example.com")
        )
    }
}
