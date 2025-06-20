package launcher.launcher

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.jan.supabase.auth.handleDeeplinks
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.account.SetupNewPassword
import launcher.launcher.ui.screens.onboard.OnBoardScreen
import launcher.launcher.ui.screens.pet.PetDialog
import launcher.launcher.ui.theme.LauncherTheme
import launcher.launcher.utils.Supabase


class OnboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val isPetDialogVisible = remember { mutableStateOf(true) }
            val isLoginResetPassword = remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                Supabase.supabase.handleDeeplinks(intent){
                    if(it.type == "recovery"){
                        isLoginResetPassword.value = true
                    }
                    Log.d("Supabase Deeplink",it.type.toString())
                }
            }
            LauncherTheme {
                Surface {
                    val navController = rememberNavController()

                    PetDialog(
                        petId = "turtie",
                        isPetDialogVisible,
                        navController
                    )

                    NavHost(
                        navController = navController,
                        startDestination = if(isLoginResetPassword.value) Screen.ResetPass.route
                        else Screen.OnBoard.route
                    ) {

                        composable(Screen.OnBoard.route) {
                            OnBoardScreen(navController)
                        }
                        composable(
                            Screen.ResetPass.route) {
                            SetupNewPassword(navController)
                        }
                    }
                }
            }
        }
    }
}

