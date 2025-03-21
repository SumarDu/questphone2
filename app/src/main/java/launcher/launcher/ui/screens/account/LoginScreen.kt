package launcher.launcher.ui.screens.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import launcher.launcher.R
import launcher.launcher.ui.navigation.Screen

enum class LoginStep {
    EMAIL,
    VERIFICATION
}

@Composable
fun LoginScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loginStep by remember { mutableStateOf(LoginStep.EMAIL) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Email validation
    val isEmailValid = email.contains("@") && email.contains(".")

    // Function to handle login attempt
    val handleLogin = {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in all fields"
        } else if (!isEmailValid) {
            errorMessage = "Please enter a valid email"
        } else {
            errorMessage = null
            isLoading = true

            // Simulate API call for login
            // In a real app, this would be a call to your authentication service
            // For this demo, we'll just simulate sending a verification code
            loginStep = LoginStep.VERIFICATION
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
            isLoading = false
            navController.navigate(Screen.HomeScreen.route)
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                    text = when (loginStep) {
                        LoginStep.EMAIL -> "Sign in to continue"
                        LoginStep.VERIFICATION -> "Verify your email"
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

                when (loginStep) {
                    // Email & Password Step
                    LoginStep.EMAIL -> {
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
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            isError = errorMessage != null && email.isBlank() || (email.isNotBlank() && !isEmailValid)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (isPasswordVisible)
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
                                    handleLogin()
                                }
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
                            isError = errorMessage != null && password.isBlank()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Forgot password
                        TextButton(
                            onClick = {
                                navController.navigate(Screen.ForgetPassword.route)

                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Forgot password?")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login button
                        Button(
                            onClick = handleLogin,
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
                                Text("Continue")
                            }
                        }
                    }

                    // Verification step
                    LoginStep.VERIFICATION -> {
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
                            onClick = { loginStep = LoginStep.EMAIL },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to login")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Sign up option
                if (loginStep == LoginStep.EMAIL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(onClick = {
                            navController.navigate(Screen.SignUp.route)
                        }) {
                            Text("Sign up")
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

