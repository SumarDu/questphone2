package launcher.launcher.ui.screens.quest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import launcher.launcher.R
import launcher.launcher.data.game.Rewards
import launcher.launcher.data.game.UserInfo
import launcher.launcher.data.game.addXP
import launcher.launcher.data.game.getUserInfo
import launcher.launcher.data.game.saveUserInfo
import launcher.launcher.data.game.xpToRewardForQuest
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.utils.VibrationHelper

enum class DialogState { COINS, LEVEL_UP, NONE }

@Composable
fun RewardDialogMaker(baseQuest: BasicQuestInfo, onAllDialogsDismissed: () -> Unit = {}) {
    val context = LocalContext.current
    var userInfo = remember {  getUserInfo(context) }

    // Calculate if user leveled up and the rewards
    val oldLevel = remember { userInfo.level }
    var didUserLevelUp = remember { false }
    var levelUpRewards = remember { hashMapOf<Rewards, Int>() }

    val coinsEarned = baseQuest.reward

    LaunchedEffect(userInfo) {
        didUserLevelUp = oldLevel != userInfo.level

        if(didUserLevelUp){
            levelUpRewards.put(Rewards.QUEST_SKIPPER,1)
            if(userInfo.level % 2==0){
                levelUpRewards.put(Rewards.XP_BOOSTER,1)
            }
            if(userInfo.level % 5==0){
                levelUpRewards.put(Rewards.STREAK_FREEZER,1)
            }
            levelUpRewards.forEach {
                userInfo.inventory.put(it.key,it.value+userInfo.inventory.getOrDefault(it.key,0))
            }
            userInfo.inventory.putAll(levelUpRewards)
        }
    }
    // Track current dialog state
    var currentDialog by remember { mutableStateOf(DialogState.COINS) }

    // Show the appropriate dialog based on the current state
    when (currentDialog) {
        DialogState.COINS -> {
            RewardDialog(
                reward = coinsEarned,
                onDismiss = {
                    // If user leveled up, show level up dialog next, otherwise end
                    currentDialog = if (didUserLevelUp) DialogState.LEVEL_UP else DialogState.NONE
                }
            )
        }
        DialogState.LEVEL_UP -> {
            LevelUpDialog(
                oldLevel = oldLevel,
                newLevel = userInfo.level,
                onDismiss = {
                    currentDialog = DialogState.NONE
                }
            )
        }
        DialogState.NONE -> {
            // All dialogs have been dismissed
            LaunchedEffect(key1 = true) {
                onAllDialogsDismissed()
                saveUserInfo(userInfo, context)
            }
        }
    }
}

@Composable
fun RewardDialog(reward: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animation for coin
            val bounceAnimation = remember {
                Animatable(0f)
            }

            LaunchedEffect(key1 = true) {
                bounceAnimation.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.3f,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            // Apply bounce animation to the coin image
            Image(
                painter = painterResource(R.drawable.coin_icon),
                contentDescription = "coin",
                modifier = Modifier
                    .size(50.dp)
                    .scale(1f + (bounceAnimation.value * 0.2f))
                    .offset(y = (-20 * bounceAnimation.value).dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.size(16.dp))

            // Animated text appearance
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically()
            ) {
                Text(
                    text = "You won $reward ${if (reward > 1) "coins" else "coin"}!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Button(
                onClick = {
                    VibrationHelper.vibrate(50)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Text("Collect", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun LevelUpDialog(oldLevel: Int, newLevel: Int, onDismiss: () -> Unit,lvUpRew: HashMap<Rewards,Int> = hashMapOf()) {
    Dialog(onDismissRequest = onDismiss) {

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated level up icon
            val rotationAnimation = remember { Animatable(0f) }

            LaunchedEffect(key1 = true) {
                rotationAnimation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(1000)
                )
            }

            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Level Up",
                tint = Color(0xFFFFC107), // Gold color
                modifier = Modifier
                    .size(50.dp)
                    .rotate(rotationAnimation.value)
            )

            Spacer(modifier = Modifier.size(16.dp))

            Text(
                text = "Level Up!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "You advanced from level $oldLevel to level $newLevel",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Only show new abilities text if applicable
            if (lvUpRew.isNotEmpty()) {
                Text(
                    text = "Rewards",
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                lvUpRew.forEach {
                    Text(
                        text = "${it.key.name} x ${it.value}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                    )
                }
            }

            Button(
                onClick = {
                    VibrationHelper.vibrate(50)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Text("Continue", fontSize = 16.sp)
            }
        }
    }
}

//// Optional: Add more dialog types as needed
//@Composable
//fun AchievementUnlockedDialog(achievement: Achievement, onDismiss: () -> Unit) {
//    // Implementation similar to the other dialogs
//}