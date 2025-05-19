package launcher.launcher.ui.screens.onboard

import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.navigation.NavHostController
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.account.ForgotPasswordScreen
import launcher.launcher.ui.screens.account.LoginScreen
import launcher.launcher.ui.screens.account.LoginStep
import launcher.launcher.ui.screens.account.SignUpScreen
import launcher.launcher.utils.Supabase
import launcher.launcher.utils.isOnline
import launcher.launcher.utils.triggerSync

@Composable
fun LoginOnboard(isNextEnabled: MutableState<Boolean>, navController: NavHostController){
    val context = LocalContext.current

    val data = context.getSharedPreferences("onboard", MODE_PRIVATE)

    val loginStep = remember { mutableStateOf(LoginStep.SIGNUP) }
    isNextEnabled.value = Supabase.supabase.auth.currentUserOrNull().let { it != null }

    val isUserLoggedIn = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
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
                if(context.isOnline()){
                    triggerSync(context.applicationContext)
                }
                navController.navigate(Screen.HomeScreen.route) {
                    popUpTo(Screen.OnBoard.route) { inclusive = true }
                }
            }
        }
        LoginStep.SIGNUP -> {
            SignUpScreen(loginStep) {
                isNextEnabled.value = true
                data.edit { putBoolean("new_user", true) }
                val userId = Supabase.supabase.auth.currentUserOrNull()?.id
                if(userId!=null){
                    Log.d("Account","creating a user profile")
                    scope.launch {
                        Supabase.supabase.postgrest["profiles"].insert(
                            mapOf(
                                "id" to userId,
                                "username" to "nethical_${userId.hashCode()}" ,
                                "full_name" to "Cool User",
                                "avatar_url" to "https://avatars.githubusercontent.com/u/79095297?v=4",
                                "quests" to "{}"
                            )
                        )
                    }
                }
            }

        }
        LoginStep.FORGOT_PASSWORD -> ForgotPasswordScreen(loginStep)
        LoginStep.COMPLETE ->
        {
            StandardPageContent("A New Journey Begins Here!","Press Next to start the setup wizard!")
        }
    }
}
