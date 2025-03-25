package launcher.launcher.ui.screens.account

import android.util.Log
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
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import launcher.launcher.R
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.utils.Supabase


@Composable
fun LoginScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Email validation
    val isEmailValid = email.contains("@") && email.contains(".")


    LaunchedEffect(Supabase.supabase.auth) {
        Supabase.supabase.auth.sessionStatus.collectLatest { authState ->
            when (authState) {
                is SessionStatus.Authenticated -> {
                    errorMessage = null
                    isLoading = false
                    navController.navigate(Screen.HomeScreen.route) // Navigate only once
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
                    text = "Blank Phone",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign in to continue",
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
                    onClick = {

                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill in all fields"
                        } else if (!isEmailValid) {
                            errorMessage = "Please enter a valid email"
                        } else {

                            isLoading = true
                            errorMessage = null


                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    Supabase.supabase.auth.signInWith(Email) {
                                        this.email = email
                                        this.password = password
                                    }
                                } catch (e: AuthRestException) {
                                    errorMessage = e.errorDescription
                                    isLoading = false
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
                        Text("Continue")
                    }
                }


                Spacer(modifier = Modifier.height(32.dp))

                // Sign up option

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


                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

}