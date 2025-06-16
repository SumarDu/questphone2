package launcher.launcher.ui.screens.onboard

import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import launcher.launcher.ui.screens.account.ForgotPasswordScreen
import launcher.launcher.ui.screens.account.LoginScreen
import launcher.launcher.ui.screens.account.LoginStep
import launcher.launcher.ui.screens.account.SignUpScreen
import launcher.launcher.utils.Supabase
import launcher.launcher.utils.isOnline
import launcher.launcher.utils.triggerProfileSync
import launcher.launcher.utils.triggerQuestSync
import launcher.launcher.utils.triggerStatsSync

@Composable
fun LoginOnboard(isNextEnabled: MutableState<Boolean>, navController: NavHostController){
    val context = LocalContext.current

    val data = context.getSharedPreferences("onboard", MODE_PRIVATE)

    val loginStep = remember { mutableStateOf(LoginStep.SIGNUP) }

//    LaunchedEffect(Unit ) {
//        val isUserLoggedIn = Supabase.supabase.auth.currentUserOrNull() != null
//        isNextEnabled.value = isUserLoggedIn
//        if (isUserLoggedIn) {
//            loginStep.value = LoginStep.COMPLETE
//        }
//    }

    LaunchedEffect(Unit) {
        Supabase.supabase.auth.sessionStatus.collectLatest { authState ->
            when (authState) {
                is SessionStatus.Authenticated -> {
                    loginStep.value = LoginStep.COMPLETE
                    isNextEnabled.value = true
                }

                is SessionStatus.NotAuthenticated -> {
                    isNextEnabled.value = false
                }
                is SessionStatus.Initializing -> {
                    Log.d("Signup", "Initializing session...")
                }

                else -> {}
            }
        }
    }

    when(loginStep.value) {
        LoginStep.LOGIN -> {
            LoginScreen(loginStep) {
                if(context.isOnline()){
                    triggerQuestSync(context.applicationContext,true)
                    triggerStatsSync(context,true)
                    triggerProfileSync(context)
                }
            }
        }
        LoginStep.SIGNUP -> {
            SignUpScreen(loginStep)

        }
        LoginStep.FORGOT_PASSWORD -> ForgotPasswordScreen(loginStep)
        LoginStep.COMPLETE ->
        {
            StandardPageContent(isNextEnabled,"A New Journey Begins Here!","Press Next to continue!")
        }

    }
}
