package com.unbound.messageme

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
class SmokeNavigationTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rule: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Before
    fun completeOnboardingAndDismissNotifications() {
        composeRule.waitForIdle()
        val needsOnboarding = runCatching {
            composeRule.waitUntil(timeoutMillis = 4_000) {
                composeRule.onAllNodesWithTag("onboard-continue").fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithText("Scheduled").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onAllNodesWithTag("onboard-continue").fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
        if (needsOnboarding) {
            composeRule.onNodeWithTag("onboard-first-name").performTextInput("Craig")
            composeRule.onNodeWithTag("onboard-last-name").performTextInput("Test")
            composeRule.onNodeWithTag("onboard-email").performTextInput("craig@example.com")
            composeRule.onNodeWithTag("onboard-continue").performClick()
            composeRule.waitForIdle()
        }
        val permissionPrompt = runCatching {
            composeRule.waitUntil(timeoutMillis = 4_000) {
                composeRule.onAllNodesWithText("Not Now").fetchSemanticsNodes().isNotEmpty()
            }
            true
        }.getOrDefault(false)
        if (permissionPrompt) {
            composeRule.onNodeWithText("Not Now").performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun opensCalendarFromHamburger() {
        composeRule.onNodeWithText("Scheduled").assertExists()
        composeRule.onNodeWithContentDescription("Todo calendar").performClick()
        composeRule.onNodeWithText("Calendar").assertExists()
    }

    @Test
    fun opensReceivedTab() {
        composeRule.onNodeWithText("Received").performClick()
        composeRule.onNodeWithText("No messages received on this day.").assertExists()
    }
}
