package neth.iecal.questphone

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

import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.ui.screens.account.SetupNewPassword
import neth.iecal.questphone.ui.screens.onboard.OnBoardScreen
import neth.iecal.questphone.ui.screens.pet.PetDialog
import neth.iecal.questphone.ui.theme.LauncherTheme



class OnboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            LauncherTheme {
                Surface {
                    val navController = rememberNavController()
                    val isPetDialogVisible = remember { mutableStateOf(true) }

                    PetDialog(
                        petId = "turtie",
                        isPetDialogVisible,
                        navController
                    )

                    NavHost(
                        navController = navController,
                        startDestination = Screen.OnBoard.route
                    ) {

                        composable(Screen.OnBoard.route) {
                            OnBoardScreen(navController)
                        }
                        composable(
                            Screen.ResetPass.route
                        ) {
                            SetupNewPassword(navController)
                        }
                    }
                }
            }
        }
    }
}

