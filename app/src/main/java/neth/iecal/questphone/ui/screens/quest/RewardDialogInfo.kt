package neth.iecal.questphone.ui.screens.quest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.InventoryItem
import neth.iecal.questphone.data.game.StreakCheckReturn
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.addCoins
import neth.iecal.questphone.data.game.addLevelUpRewards
import neth.iecal.questphone.data.game.addXp
import neth.iecal.questphone.data.game.xpFromStreak
import neth.iecal.questphone.data.game.xpToRewardForQuest
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.ui.screens.game.StreakFailedDialog
import neth.iecal.questphone.ui.screens.game.StreakUpDialog
import neth.iecal.questphone.utils.VibrationHelper
import neth.iecal.questphone.data.local.AppDatabase
import neth.iecal.questphone.data.local.QuestEvent
import neth.iecal.questphone.data.remote.SupabaseSyncService
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

enum class DialogState { COINS, LEVEL_UP, STREAK_UP, STREAK_FAILED, NONE }

/**
 * This values in here must be set to true in order to show the dialog [RewardDialogMaker] which is triggered
 * in the parent activity.
 * @property isRewardDialogVisible
 * @property currentCommonQuestInfo the quest info that led to this reward dialog be triggered. null if dialog not
 * visible.
 * @property streakData data to be used by streak dialogs. empty if not triggered by stuff related to streak
 */
object RewardDialogInfo{
    var isRewardDialogVisible by mutableStateOf(false)
    var currentCommonQuestInfo by mutableStateOf<CommonQuestInfo?>(null)
    var streakData : StreakCheckReturn? = null
    var currentDialog by mutableStateOf<DialogState>(DialogState.COINS)
    var rewardAmount by mutableStateOf(0)
    // Guard to avoid duplicate coin additions/logging for the same reward trigger
    var hasProcessedCurrentReward by mutableStateOf(false)
    // Global idempotency: track last processed session count per quest
    // This persists across navigation/recomposition so we don't re-trigger rewards on re-entry
    val lastProcessedSessionForQuest = androidx.compose.runtime.mutableStateMapOf<String, Int>()
}

/**
 * Calculate the reward amount for a quest completion without showing the dialog
 */
fun calculateQuestReward(commonQuestInfo: CommonQuestInfo): Int {
    return if (commonQuestInfo.reward_min == commonQuestInfo.reward_max) {
        commonQuestInfo.reward_min
    } else {
        (commonQuestInfo.reward_min..commonQuestInfo.reward_max).random()
    }
}

/**
 * Log quest completion event with coin reward information
 */
suspend fun logQuestCompletionWithCoins(
    context: android.content.Context,
    questInfo: CommonQuestInfo,
    rewardCoins: Int,
    preRewardCoins: Int
) {
    try {
        val questEventDao = AppDatabase.getDatabase(context).questEventDao()
        val supabaseSyncService = SupabaseSyncService(context)
        val now = System.currentTimeMillis()

        // First, try to find an active event (endTime == 0) for this quest
        val activeEvents = questEventDao.getEventsByName(questInfo.title)
        var activeEvent: QuestEvent? = null
        
        // Find the first active event (endTime == 0)
        for (event in activeEvents) {
            if (event.endTime == 0L) {
                activeEvent = event
                break
            }
        }

        if (activeEvent != null) {
            // Found an active event, update it with reward details and close it
            val updatedEvent = QuestEvent(
                id = activeEvent.id,
                eventName = activeEvent.eventName,
                startTime = activeEvent.startTime,
                endTime = now,
                comments = activeEvent.comments,
                rewardCoins = rewardCoins,
                preRewardCoins = preRewardCoins,
                isRewardPending = false
            )
            questEventDao.updateEvent(updatedEvent.copy(synced = false))
            supabaseSyncService.syncSingleQuestEvent(updatedEvent)
            android.util.Log.d("RewardDialogInfo", "Updated active quest event with reward: ${updatedEvent.id}")
        } else {
            // No active event, handle overdue completion or other cases
            // Get the current event to get its startTime (for overdue matching)
            val currentEvent = questEventDao.getLatestEvent()
            
            // Find a pending reward event that matches the quest name
            val pendingEvent = questEventDao.findPendingRewardEvent(questInfo.title)

            // If we have both events, check if their times match to ensure it's the correct one
            val eventToUpdate = if (pendingEvent != null && currentEvent != null) {
                if (pendingEvent.endTime == currentEvent.startTime) {
                    pendingEvent
                } else {
                    // Times don't match, it's not the correct pending event
                    null
                }
            } else {
                pendingEvent
            }

            if (eventToUpdate != null) {
                // Found the correct pending event, update it with reward details
                val updatedEvent = eventToUpdate.copy(
                    rewardCoins = rewardCoins,
                    preRewardCoins = preRewardCoins,
                    isRewardPending = false
                )
                questEventDao.updateEvent(updatedEvent.copy(synced = false))
                supabaseSyncService.syncSingleQuestEvent(updatedEvent)
                android.util.Log.d("RewardDialogInfo", "Updated pending quest event with reward: ${updatedEvent.id}")
            } else {
                // No correct pending event found, try to find any recent event for this quest that doesn't have reward info
                val recentEvents = questEventDao.getEventsByName(questInfo.title)
                var eventToUpdateWithReward = false
                var eventToUpdate: QuestEvent? = null
                
                // Look for events that don't have reward info yet
                for (event in recentEvents) {
                    if (event.rewardCoins == null && event.preRewardCoins == null) {
                        eventToUpdate = event
                        eventToUpdateWithReward = true
                        break
                    }
                }
                
                if (eventToUpdateWithReward && eventToUpdate != null) {
                    // Found an event without reward info, update it
                    val updatedEvent = eventToUpdate.copy(
                        rewardCoins = rewardCoins,
                        preRewardCoins = preRewardCoins
                    )
                    questEventDao.updateEvent(updatedEvent.copy(synced = false))
                    supabaseSyncService.syncSingleQuestEvent(updatedEvent)
                    android.util.Log.d("RewardDialogInfo", "Updated existing quest event with reward: ${updatedEvent.id}")
                } else {
                    // No correct pending event found, create a new one for a normally completed quest
                    // This should be rare but handles edge cases
                    val newEvent = QuestEvent(
                        eventName = questInfo.title,
                        startTime = now - 1000, // Approx start time
                        endTime = now,
                        rewardCoins = rewardCoins,
                        preRewardCoins = preRewardCoins,
                        comments = "Quest completed successfully"
                    )
                    questEventDao.insertEvent(newEvent)
                    supabaseSyncService.syncSingleQuestEvent(newEvent)
                    android.util.Log.d("RewardDialogInfo", "Logged new quest completion event: ${newEvent.id}")
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("RewardDialogInfo", "Error logging quest completion with coins", e)
    }
}

/**
 * Calculates what to reward user as well as trigger the reward dialog to be shown to the user
 */
fun checkForRewards(commonQuestInfo: CommonQuestInfo, rewardAmount: Int){
    RewardDialogInfo.isRewardDialogVisible = true
    RewardDialogInfo.currentCommonQuestInfo =  commonQuestInfo
    RewardDialogInfo.currentDialog = DialogState.COINS
    RewardDialogInfo.rewardAmount = rewardAmount
    // Reset idempotency guard for this new reward session
    RewardDialogInfo.hasProcessedCurrentReward = false
}

/**
 * Handles both showing the rewards dialog as well as rewarding user with xp, coins and bs.
 *
 */
@Composable
fun RewardDialogMaker(  ) {
    val isRewardDialogTriggered by remember { derivedStateOf { RewardDialogInfo.isRewardDialogVisible } }


    // Track current dialog state
    var currentDialog = remember { derivedStateOf { RewardDialogInfo.currentDialog } }

    if (isRewardDialogTriggered) {

        // Calculate if user leveled up and the rewards
        val oldLevel = remember { User.userInfo.level }
        var didUserLevelUp = remember { false }
        var levelUpInventoryItem = remember { hashMapOf<InventoryItem, Int>() }


        // if quest info is empty, the function was triggered by stuff like daily rewards
        val isTriggeredViaQuestCompletion = RewardDialogInfo.currentCommonQuestInfo != null

                val questInfo = RewardDialogInfo.currentCommonQuestInfo
        val coinsEarned = RewardDialogInfo.rewardAmount
        val context = LocalContext.current
        
        LaunchedEffect(Unit) {
            if (RewardDialogInfo.hasProcessedCurrentReward) {
                return@LaunchedEffect
            }
            val xp = if(isTriggeredViaQuestCompletion) xpToRewardForQuest(User.userInfo.level) else xpFromStreak(
                User.userInfo.streak.currentStreak
            )
            User.addXp(xp)
            User.lastXpEarned = xp
            
            // Capture pre-reward coin balance before adding coins
            val preRewardCoins = User.userInfo.coins
            User.addCoins(coinsEarned)
            // Mark as processed to prevent duplicate additions if recomposed
            RewardDialogInfo.hasProcessedCurrentReward = true
            
            // Log quest completion with coin information if triggered by quest completion
            if (isTriggeredViaQuestCompletion && questInfo != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    logQuestCompletionWithCoins(
                        context = context,
                        questInfo = questInfo,
                        rewardCoins = coinsEarned,
                        preRewardCoins = preRewardCoins
                    )
                }
            }

            didUserLevelUp = oldLevel != User.userInfo.level

            if (didUserLevelUp) {
                levelUpInventoryItem = User.addLevelUpRewards()
            }
        }


        // Show the appropriate dialog based on the current state
        when (currentDialog.value) {
            DialogState.COINS -> {
                CoinDialog(
                    reward = coinsEarned,
                    onDismiss = {
                        // If user leveled up, show level up dialog next, otherwise end
                        RewardDialogInfo.currentDialog =
                            if (didUserLevelUp) DialogState.LEVEL_UP else DialogState.NONE
                    }
                )
            }

            DialogState.LEVEL_UP -> {
                LevelUpDialog(
                    oldLevel = oldLevel,
                    lvUpRew = levelUpInventoryItem,
                    onDismiss = {
                        RewardDialogInfo.currentDialog = DialogState.NONE
                    }
                )
            }

            DialogState.STREAK_UP -> {
                StreakUpDialog {
                    RewardDialogInfo.currentDialog =
                        if (didUserLevelUp) DialogState.LEVEL_UP else DialogState.NONE
                }
            }
            DialogState.STREAK_FAILED ->
            {
                StreakFailedDialog() {
                    RewardDialogInfo.currentDialog = DialogState.NONE
                }
            }
            DialogState.NONE -> {
                RewardDialogInfo.apply {
                    isRewardDialogVisible = false
                    currentCommonQuestInfo = null
                    streakData = null
                }

            }

        }
    }
}

@Composable
fun CoinDialog(reward: Int, onDismiss: () -> Unit) {
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
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
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
                Text("Collect")
            }
        }
    }
}

@Composable
fun LevelUpDialog(oldLevel: Int,onDismiss: () -> Unit,lvUpRew: HashMap<InventoryItem,Int> = hashMapOf()) {
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
                painter = painterResource(R.drawable.star),
                contentDescription = "Level Up",
                tint = Color(0xFFFFC107), // Gold color
                modifier = Modifier
                    .size(50.dp)
                    .rotate(rotationAnimation.value)
            )

            Spacer(modifier = Modifier.size(16.dp))

            Text(
                text = "Level Up!",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "You advanced from level $oldLevel to level ${User.userInfo.level}",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(it.key.icon),
                            contentDescription = it.key.simpleName,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.size(4.dp))

                        Text(
                            text = "${it.key.simpleName} x ${it.value}",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }

            Spacer(Modifier.size(16.dp))
            Button(
                onClick = {
                    VibrationHelper.vibrate(50)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Text("Continue")
            }
        }
    }
}
