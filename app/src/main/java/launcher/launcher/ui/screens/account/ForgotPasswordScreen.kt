package launcher.launcher.ui.screens.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.jan.supabase.auth.auth
import launcher.launcher.R
import launcher.launcher.utils.Supabase
import kotlinx.coroutines.runBlocking

enum class ForgotPasswordStep {
    EMAIL,
    VERIFICATION,
    NEW_PASSWORD
}
@Composable
fun ForgotPasswordScreen(navController: NavHostController) {
    // States
    var email by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var forgotPasswordStep by remember { mutableStateOf(ForgotPasswordStep.EMAIL) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Email validation
    val isEmailValid = email.contains("@") && email.contains(".")

    // Password validation
    val isPasswordValid = newPassword.length >= 8
    val doPasswordsMatch = newPassword == confirmPassword

    // Function to handle email submission
    val handleEmailSubmit = {
        if (email.isBlank()) {
            errorMessage = "Please enter your email"
        } else if (!isEmailValid) {
            errorMessage = "Please enter a valid email"
        } else {
            errorMessage = null
            isLoading = true

            runBlocking {
                Supabase.supabase.auth.resetPasswordForEmail(email)
            }

            // Simulate API call for password reset request
            // In a real app, this would be a call to your authentication service
            forgotPasswordStep = ForgotPasswordStep.VERIFICATION
            isLoading = false
        }
    }

    // Function to handle verification
    val handleVerification = {
        if (verificationCode.length != 6) {
            errorMessage = "Verification code must be 6 digits"
        } else {
            errorMessage = null
            isLoading = true

            // Simulate verification
            // In a real app, this would validate the code with your authentication service
            forgotPasswordStep = ForgotPasswordStep.NEW_PASSWORD
            isLoading = false
        }
    }

    // Function to handle password reset
    val handlePasswordReset = {
        when {
            newPassword.isBlank() || confirmPassword.isBlank() -> {
                errorMessage = "Please fill in all fields"
            }

            !isPasswordValid -> {
                errorMessage = "Password must be at least 8 characters"
            }

            !doPasswordsMatch -> {
                errorMessage = "Passwords don't match"
            }

            else -> {
                errorMessage = null
                isLoading = true

                // Simulate API call for password reset
                // In a real app, this would be a call to your authentication service
                isLoading = false
                navController.popBackStack()
            }
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Back button
            IconButton(
                onClick = {
                    navController.popBackStack()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back to login"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Logo or app name
                Text(
                    text = "Quest",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (forgotPasswordStep) {
                        ForgotPasswordStep.EMAIL -> "Reset Password"
                        ForgotPasswordStep.VERIFICATION -> "Verify your email"
                        ForgotPasswordStep.NEW_PASSWORD -> "Create new password"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Error message
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                when (forgotPasswordStep) {
                    // Email step
                    ForgotPasswordStep.EMAIL -> {
                        Text(
                            text = "Enter your email address and we'll send you a code to reset your password.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Email field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    handleEmailSubmit()
                                }
                            ),
                            isError = errorMessage != null && (email.isBlank() || !isEmailValid)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit button
                        Button(
                            onClick = handleEmailSubmit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Send Code")
                            }
                        }
                    }

                    // Verification step
                    ForgotPasswordStep.VERIFICATION -> {
                        Text(
                            text = "We've sent a verification code to",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Verification code field
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    verificationCode = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("6-digit code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    handleVerification()
                                }
                            ),
                            isError = errorMessage != null
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Resend code
                        TextButton(
                            onClick = { /* Handle resend code */ },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Resend code")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Verify button
                        Button(
                            onClick = handleVerification,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isLoading && verificationCode.length == 6
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Verify")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Back button
                        TextButton(
                            onClick = { forgotPasswordStep = ForgotPasswordStep.EMAIL },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to email")
                        }
                    }

                    // New password step
                    ForgotPasswordStep.NEW_PASSWORD -> {
                        Text(
                            text = "Create a new password for your account.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // New password field
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; errorMessage = null },
                            label = { Text("New Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (isPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        painter = if (isPasswordVisible)
                                            painterResource(id = R.drawable.baseline_visibility_off_24)
                                        else
                                            painterResource(id = R.drawable.baseline_visibility_24),
                                        contentDescription = if (isPasswordVisible)
                                            "Hide password"
                                        else
                                            "Show password"
                                    )
                                }
                            },
                            isError = errorMessage != null && (newPassword.isBlank() || !isPasswordValid)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Confirm password field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMessage = null },
                            label = { Text("Confirm Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (isConfirmPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    handlePasswordReset()
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    isConfirmPasswordVisible = !isConfirmPasswordVisible
                                }) {
                                    Icon(
                                        painter = if (isConfirmPasswordVisible)
                                            painterResource(id = R.drawable.baseline_visibility_off_24)
                                        else
                                            painterResource(id = R.drawable.baseline_visibility_24),
                                        contentDescription = if (isConfirmPasswordVisible)
                                            "Hide password"
                                        else
                                            "Show password"
                                    )
                                }
                            },
                            isError = errorMessage != null && (confirmPassword.isBlank() || !doPasswordsMatch)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Reset password button
                        Button(
                            onClick = { handlePasswordReset() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Reset Password")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Back button
                        TextButton(
                            onClick = { forgotPasswordStep = ForgotPasswordStep.VERIFICATION },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to verification")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Login option
                if (forgotPasswordStep == ForgotPasswordStep.EMAIL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remember your password?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Text("Login")
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
