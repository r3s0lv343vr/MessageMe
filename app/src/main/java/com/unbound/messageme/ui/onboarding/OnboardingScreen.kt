package com.unbound.messageme.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.unbound.messageme.domain.OnboardingValidation
import com.unbound.messageme.domain.ProfileOnboarding
import com.unbound.messageme.ui.components.WatercolorBackground
import com.unbound.messageme.ui.theme.Ink
import com.unbound.messageme.ui.theme.WaterBlue
import com.unbound.messageme.ui.theme.WaterBlueDeep

@Composable
fun OnboardingScreen(
    onContinue: (firstName: String, lastName: String, email: String) -> OnboardingValidation
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var firstError by remember { mutableStateOf<String?>(null) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var attempted by remember { mutableStateOf(false) }

    val preview = ProfileOnboarding.validate(firstName, lastName, email)
    val canContinue = preview.ok

    fun submit() {
        attempted = true
        firstError = preview.firstNameError
        lastError = preview.lastNameError
        emailError = preview.emailError
        if (!canContinue) return
        val saved = onContinue(firstName, lastName, email)
        firstError = saved.firstNameError
        lastError = saved.lastNameError
        emailError = saved.emailError
    }

    BackHandler {
        // Stay on this screen. The rest of the app is unavailable until onboarding succeeds.
    }

    WatercolorBackground {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "MessageMe",
                style = MaterialTheme.typography.displaySmall,
                color = WaterBlueDeep
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Who should these letters come from?",
                style = MaterialTheme.typography.headlineSmall,
                color = Ink
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "You must complete this before you can use MessageMe. First name, last name, and a valid email. There is no password. Until then, the rest of the app stays closed.",
                style = MaterialTheme.typography.bodyLarge,
                color = Ink.copy(alpha = 0.82f)
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    firstError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboard-first-name"),
                label = { Text("First name") },
                isError = firstError != null || (attempted && preview.firstNameError != null),
                supportingText = (firstError ?: preview.firstNameError.takeIf { attempted })?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    lastError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboard-last-name"),
                label = { Text("Last name") },
                isError = lastError != null || (attempted && preview.lastNameError != null),
                supportingText = (lastError ?: preview.lastNameError.takeIf { attempted })?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboard-email"),
                label = { Text("Email") },
                isError = emailError != null || (attempted && preview.emailError != null),
                supportingText = (emailError ?: preview.emailError.takeIf { attempted })?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() })
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { submit() },
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboard-continue"),
                colors = ButtonDefaults.buttonColors(containerColor = WaterBlue)
            ) {
                Text("Continue")
            }
        }
    }
}
