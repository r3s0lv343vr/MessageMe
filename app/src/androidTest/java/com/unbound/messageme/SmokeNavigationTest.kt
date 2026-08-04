package com.unbound.messageme

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
class SmokeNavigationTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rule: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Test
    fun opensCalendarFromHamburger() {
        composeRule.onNodeWithText("MessageMe").assertExists()
        composeRule.onNodeWithContentDescription("Todo calendar").performClick()
        composeRule.onNodeWithText("Todo calendar").assertExists()
    }
}
