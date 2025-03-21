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
import androidx.compose.material.icons.outlined.Person
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
import launcher.launcher.R
import launcher.launcher.ui.navigation.Screen

enum class SignUpStep {
    FORM,
    VERIFICATION
}


@Composable
fun SignUpScreen(navController: NavHostController) {
    // States
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var signUpStep by remember { mutableStateOf(SignUpStep.FORM) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Email validation
    val isEmailValid = email.contains("@") && email.contains(".")

    // Password validation
    val isPasswordValid = password.length >= 8
    val doPasswordsMatch = password == confirmPassword

    // Function to handle sign up attempt
    val handleSignUp = {
        when {
            name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                errorMessage = "Please fill in all fields"
            }
            !isEmailValid -> {
                errorMessage = "Please enter a valid email"
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

                // Simulate API call for sign up
                // In a real app, this would be a call to your authentication service
                // For this demo, we'll just simulate sending a verification code
                signUpStep = SignUpStep.VERIFICATION
                isLoading = false
            }
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
                    text = when (signUpStep) {
                        SignUpStep.FORM -> "Create an account"
                        SignUpStep.VERIFICATION -> "Verify your email"
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

                when (signUpStep) {
                    // Sign up form
                    SignUpStep.FORM -> {
                        // Name field
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; errorMessage = null },
                            label = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            isError = errorMessage != null && name.isBlank()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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
                            isError = errorMessage != null && (password.isBlank() || !isPasswordValid)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Confirm Password field
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
                                    handleSignUp()
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign up button
                        Button(
                            onClick = handleSignUp,
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
                                Text("Sign Up")
                            }
                        }
                    }

                    // Verification step
                    SignUpStep.VERIFICATION -> {
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
                            onClick = { signUpStep = SignUpStep.FORM },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to sign up")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Login option
                if (signUpStep == SignUpStep.FORM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Already have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(onClick =
                        {
                            navController.navigate(Screen.Login.route)

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
