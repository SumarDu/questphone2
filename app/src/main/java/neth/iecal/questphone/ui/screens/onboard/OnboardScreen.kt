package neth.iecal.questphone.ui.screens.onboard

import android.Manifest
import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import neth.iecal.questphone.utils.VibrationHelper
import neth.iecal.questphone.MainActivity
import neth.iecal.questphone.services.AppBlockerService
import neth.iecal.questphone.ui.screens.account.SetupProfileScreen

import neth.iecal.questphone.utils.checkUsagePermission

open class OnboardingContent {
    // Standard title and description page
    data class StandardPage(
        val title: String,
        val description: String
    ) : OnboardingContent()

    // Custom composable content
    data class CustomPage(
        val onNextPressed: () -> Boolean = {true},
        val isNextEnabled: MutableState<Boolean> = mutableStateOf(true),
        val content: @Composable () -> Unit
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
    val isNextEnabled = remember { mutableStateOf(false) }

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
                        isNextEnabled = isNextEnabled,
                        title = page.title,
                        description = page.description
                    )
//                    isNextEnabled.value = true
                }

                is OnboardingContent.CustomPage -> {
                    page.content()
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
                        VibrationHelper.vibrate(50)
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
            Button(
                onClick = {
                    VibrationHelper.vibrate(50)
                    if (isLastPage) {
                        onFinishOnboarding()
                    } else {
                        val crnPage = pages[pagerState.currentPage]
                        if (crnPage is OnboardingContent.CustomPage) {
                            val result = crnPage.onNextPressed.invoke()
                            if (result) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                            return@Button
                        }
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                enabled = if(pages[pagerState.currentPage] is OnboardingContent.CustomPage){
                    (pages[pagerState.currentPage] as OnboardingContent.CustomPage).isNextEnabled.value
                }else{
                    isNextEnabled.value
                }

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
@Composable
fun StandardPageContent(
    isNextEnabled: MutableState<Boolean> ,
    title: String,
    description: String
) {

    // :pray: cheat fix for next button disappearing
    LaunchedEffect(isNextEnabled.value) {
        if(isNextEnabled.value!=true){
            isNextEnabled.value = true
        }
    }
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

    val context = LocalContext.current
    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
        }
    )
    val isTosAccepted = remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val tosp = context.getSharedPreferences("terms", MODE_PRIVATE)
        isTosAccepted.value = tosp.getBoolean("isAccepted",false)
    }
    val isNextEnabledLogin = remember {mutableStateOf(false)}
    val onboardingPages = mutableListOf(

            OnboardingContent.CustomPage(
                isNextEnabled = isNextEnabledLogin){ ->
                LoginOnboard(isNextEnabledLogin,navController)
            },

            OnboardingContent.StandardPage(
                "QuestPhone",
                "Welcome to QuestPhone! Ever felt like your phone controls you instead of the other way around? QuestPhone helps you build mindful screen habits by turning screen time into a rewarding challenge."
            ),
            OnboardingContent.CustomPage{
                SetupProfileScreen()
            },
            OnboardingContent.StandardPage(
                "How it Works?",
                "Unlock screen time by completing real-life challenges! Whether it’s doing meditation, taking a walk, or studying, you decide how to earn your screen time. Stay productive while still enjoying your favorite apps!"
            ),
            OnboardingContent.StandardPage(
                "Stay Motivated",
                "QuestPhone makes it fun! Earn XP, level up, and collect items as you build healthier screen habits."
            ),
            OnboardingContent.StandardPage(
                "Quests",
                "Real-life tasks are called Quests in QuestPhone. Completing a quest—like exercising, reading, or meditating—earns you Coins. These coins can be used to temporarily unlock the apps that distract you the most! 5 coins gives you 10 minutes to use a distracting app"
            ),
            OnboardingContent.CustomPage(
                content = {
                    OverlayPermissionScreen()
                },
                onNextPressed = {
                    val isAllowed = Settings.canDrawOverlays(context)
                    if(!isAllowed){
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
                        context.startActivity(intent)
                        return@CustomPage false
                    }
                    return@CustomPage true
                }
            ),
//            OnboardingContent.CustomPage(
//                content = {
//                    BackgroundUsagePermission()
//                },
//                onNextPressed = {
//                    if(isIgnoringBatteryOptimizations(context)){
//                        return@CustomPage true
//                    }
//                    openBatteryOptimizationSettings(context)
//                    return@CustomPage false
//                }
//            ),
            OnboardingContent.CustomPage(
                content = {
                    UsageAccessPermission()
                }, onNextPressed = {
                    if(checkUsagePermission(context)){
                        return@CustomPage true
                    }
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                    return@CustomPage false

                }
            ),
            // Notification permission page removed
            OnboardingContent.CustomPage {
                SelectApps()
            }
        )


    if(isTosAccepted.value) {
        OnboardingScreen(
            onFinishOnboarding = {
                startForegroundService(context, Intent(context, AppBlockerService::class.java))
                val data = context.getSharedPreferences("onboard", MODE_PRIVATE)
                data.edit { putBoolean("onboard", true) }
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
                (context as Activity).finish()
            },
            pages = onboardingPages
        )
    } else {
        TermsScreen(isTosAccepted)
    }
}
