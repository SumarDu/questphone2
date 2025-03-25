package launcher.launcher.ui.screens.account

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import launcher.launcher.R
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.utils.Supabase

enum class SignUpStep {
    FORM,
    VERIFICATION
}

@Composable
fun SignUpScreen(navController: NavHostController) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var signUpStep by remember { mutableStateOf(SignUpStep.FORM) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Email and password validation
    val isEmailValid = email.contains("@") && email.contains(".")
    val isPasswordValid = password.length >= 8
    val doPasswordsMatch = password == confirmPassword

    // Supabase session status
    val sessionStatus by Supabase.supabase.auth.sessionStatus.collectAsStateWithLifecycle(initialValue = SessionStatus.Initializing)

    // Handle session status changes
    LaunchedEffect(sessionStatus) {
        when (sessionStatus) {
            is SessionStatus.Authenticated -> {
                navController.navigate(Screen.OnBoard.route) {
                    popUpTo(Screen.SignUp.route) { inclusive = true } // Clear sign-up from stack
                }
            }
            is SessionStatus.RefreshFailure -> {
                errorMessage = "Session expired. Please log in again."
            }
            is SessionStatus.Initializing -> {
                Log.d("Signup", "Initializing session...")
            }
            else -> {}
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
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
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
                    SignUpStep.FORM -> {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Email field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            isError = errorMessage != null && (email.isBlank() || !isEmailValid)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                                        painter = painterResource(
                                            if (isPasswordVisible) R.drawable.baseline_visibility_off_24
                                            else R.drawable.baseline_visibility_24
                                        ),
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
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
                            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                    Icon(
                                        painter = painterResource(
                                            if (isConfirmPasswordVisible) R.drawable.baseline_visibility_off_24
                                            else R.drawable.baseline_visibility_24
                                        ),
                                        contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            isError = errorMessage != null && (confirmPassword.isBlank() || !doPasswordsMatch)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign up button
                        Button(
                            onClick = {
                                when {
                                    email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
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
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            try {
                                                Supabase.supabase.auth.signUpWith(Email) {
                                                    this.email = email
                                                    this.password = password
                                                }
                                                signUpStep = SignUpStep.VERIFICATION
                                            } catch (e: AuthRestException) {
                                                errorMessage = e.message ?: "Sign-up failed"
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                }

                            },
                            modifier = Modifier
                                .fillMaxWidth(),
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

                    SignUpStep.VERIFICATION -> {
                        Text(
                            text = "We've sent a verification email to",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Resend email button
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        Supabase.supabase.auth.resendEmail(
                                            email = email,
                                            type = OtpType.Email.SIGNUP
                                        )
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to resend email: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            enabled = !isLoading
                        ) {
                            Text("Resend email")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

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
                        TextButton(onClick = { navController.navigate(Screen.Login.route) }) {
                            Text("Login")
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}