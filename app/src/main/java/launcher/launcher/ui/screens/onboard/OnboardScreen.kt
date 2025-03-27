package launcher.launcher.ui.screens.onboard

import android.content.Context.MODE_PRIVATE
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import launcher.launcher.ui.navigation.Screen
import androidx.core.content.edit

// Sealed class to represent different types of onboarding pages
sealed class OnboardingContent {
    // Standard title and description page
    data class StandardPage(
        val title: String,
        val description: String
    ) : OnboardingContent()

    // Custom composable content
    data class CustomPage(
        val content: @Composable (MutableState<Boolean>) -> Unit
    ) : OnboardingContent()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    pages: List<OnboardingContent>
) {
    // Remember the pager state
    val pagerState = rememberPagerState(pageCount = { pages.size })

    // Coroutine scope for button actions
    val scope = rememberCoroutineScope()

    // Determine if we're on the first or last page
    val isFirstPage = pagerState.currentPage == 0
    val isLastPage = pagerState.currentPage == pages.size - 1
    val isNextEnabled = remember  {mutableStateOf(true)}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Horizontal Pager for swipeable pages
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = isNextEnabled.value,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { position ->
            when (val page = pages[position]) {
                is OnboardingContent.StandardPage -> {
                    StandardPageContent(
                        title = page.title,
                        description = page.description
                    )
                }
                is OnboardingContent.CustomPage -> {
                    page.content(isNextEnabled)
                }
            }
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration)
                    Color.White else Color.Gray

                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }

        // Back and Next buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedVisibility(
                visible = !isFirstPage,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Text(
                        text = "Back",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            // Spacer if no back button
            if (isFirstPage) {
                Spacer(modifier = Modifier.width(64.dp))
            }
            AnimatedVisibility(
                visible = isNextEnabled.value,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinishOnboarding()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                enabled = isNextEnabled.value
            ) {
                Text(
                    text = if (isLastPage) "Get Started" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

        }
    }
}
    }

@Composable
fun StandardPageContent(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = description,
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Usage example
@Composable
fun OnBoardScreen(navController: NavHostController) {

    val context  = LocalContext.current
        val onboardingPages = listOf(
            OnboardingContent.CustomPage{ isNextEnabled ->
                LoginOnboard(isNextEnabled,navController)
            },

            OnboardingContent.StandardPage(
                "BlankPhone",
                "Welcome to BlankPhone! Ever felt like your phone controls you instead of the other way around? BlankPhone helps you build mindful screen habits by turning screen time into a rewarding challenge."
            ),
            OnboardingContent.StandardPage(
                "How it Works?",
                "Unlock screen time by completing real-life challenges! Whether it’s doing meditation, taking a walk, or studying, you decide how to earn your screen time. Stay productive while still enjoying your favorite apps!"
            ),
            OnboardingContent.StandardPage(
                "Stay Motivated",
                "BlankPhone makes it fun! Earn XP, level up, and collect badges as you build healthier screen habits."
            ),
            OnboardingContent.StandardPage(
                "Quests",
                "Real-life tasks are called Quests in BlankPhone. Completing a quest—like exercising, reading, or meditating—earns you Coins. These coins can be used to temporarily unlock the apps that distract you the most!"
            ),
            OnboardingContent.CustomPage { isNextEnabled ->
                SelectApps(isNextEnabled)

            },
            OnboardingContent.CustomPage { _ ->
                AddQuestsScreen(navController)
            }
        )

        OnboardingScreen(
            onFinishOnboarding = {

                val data = context.getSharedPreferences("onboard", MODE_PRIVATE)
                data.edit { putBoolean("onboard", true) }
                navController.navigate(Screen.HomeScreen.route)

                // Navigate to main app screen
            },
            pages = onboardingPages
        )
}
