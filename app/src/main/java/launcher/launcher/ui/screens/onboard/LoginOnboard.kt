package launcher.launcher.ui.screens.onboard

import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.navigation.NavHostController
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.account.ForgotPasswordScreen
import launcher.launcher.ui.screens.account.LoginScreen
import launcher.launcher.ui.screens.account.LoginStep
import launcher.launcher.ui.screens.account.SignUpScreen
import launcher.launcher.utils.Supabase

@Composable
fun LoginOnboard(isNextEnabled: MutableState<Boolean>, navController: NavHostController){
    val context = LocalContext.current

    val data = context.getSharedPreferences("onboard", MODE_PRIVATE)

    val loginStep = remember { mutableStateOf(LoginStep.SIGNUP) }
    isNextEnabled.value = Supabase.supabase.auth.currentUserOrNull().let { it != null }

    val isUserLoggedIn = remember { mutableStateOf(false) }

    LaunchedEffect(isUserLoggedIn.value,isNextEnabled.value ) {
        isUserLoggedIn.value = Supabase.supabase.auth.currentUserOrNull().let { it != null }
        if (isUserLoggedIn.value) {
            isNextEnabled.value = true
            loginStep.value = LoginStep.COMPLETE
            isNextEnabled.value = true
        }
    }

    when(loginStep.value) {
        LoginStep.LOGIN -> {
            LoginScreen(loginStep) {
                data.edit { putBoolean("onboard", true) }

                navController.navigate(Screen.HomeScreen.route) {
                    popUpTo(Screen.OnBoard.route) { inclusive = true }
                }
            }
        }
        LoginStep.SIGNUP -> {
            SignUpScreen(loginStep) {
                isNextEnabled.value = true
                data.edit { putBoolean("new_user", true) }
            }

        }
        LoginStep.FORGOT_PASSWORD -> ForgotPasswordScreen(loginStep)
        LoginStep.COMPLETE ->
        {
            StandardPageContent("A New Journey Begins Here!","Press Next to start the setup wizard!")
        }
    }
}
