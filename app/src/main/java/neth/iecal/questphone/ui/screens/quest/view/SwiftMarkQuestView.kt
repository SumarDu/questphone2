package neth.iecal.questphone.ui.screens.quest.view

import android.graphics.Bitmap
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.ai.GeminiPro
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.getUserInfo
import neth.iecal.questphone.data.game.xpToRewardForQuest
import neth.iecal.questphone.data.game.addToken
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsInfo
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.ui.screens.quest.checkForRewards
import neth.iecal.questphone.ui.screens.quest.calculateQuestReward
import neth.iecal.questphone.ui.screens.quest.view.components.MdPad
import neth.iecal.questphone.ui.theme.JetBrainsMonoFont
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.formatTimeMinutes
import neth.iecal.questphone.utils.toMinutesRange
import neth.iecal.questphone.utils.isAllDayRange
import neth.iecal.questphone.utils.getCurrentDate
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

@Composable
fun SwiftMarkQuestView(
    commonQuestInfo: CommonQuestInfo
) {
    val context = LocalContext.current
    val questHelper = QuestHelper(context)
    val scope = rememberCoroutineScope()
    val dao = QuestDatabaseProvider.getInstance(context).questDao()
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState()

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val isQuestComplete = remember {
        mutableStateOf(commonQuestInfo.last_completed_on == getCurrentDate())
    }
    val isInTimeRange = remember { mutableStateOf(QuestHelper.isInTimeRange(commonQuestInfo)) }
    val isFailed = remember { mutableStateOf(questHelper.isOver(commonQuestInfo)) }
    val progress = remember { mutableFloatStateOf(if (isQuestComplete.value || isFailed.value) 1f else 0f) }

    val userInfo = getUserInfo(LocalContext.current)

    fun onQuestCompleted() {
        progress.floatValue = 1f
        commonQuestInfo.last_completed_on = getCurrentDate()
        commonQuestInfo.last_completed_at = System.currentTimeMillis()
        commonQuestInfo.quest_started_at = 0
        commonQuestInfo.synced = false
        commonQuestInfo.last_updated = System.currentTimeMillis()

        val rewardAmount = calculateQuestReward(commonQuestInfo)

        scope.launch {
            dao.upsertQuest(commonQuestInfo)
            val statsDao = StatsDatabaseProvider.getInstance(context).statsDao()
            statsDao.upsertStats(
                StatsInfo(
                    id = UUID.randomUUID().toString(),
                    quest_id = commonQuestInfo.id,
                    user_id = User.userInfo.id,
                    reward_amount = rewardAmount
                )
            )
        }
        checkForRewards(commonQuestInfo, rewardAmount)
        // Award token for completing this quest (if enabled in settings)
        if (settings.tokensEnabled) {
            User.addToken(commonQuestInfo.title)
        }
        isQuestComplete.value = true
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                imageBitmap = bitmap
                isLoading = true
                scope.launch {
                    val apiKey = settings.geminiApiKey
                    if (apiKey.isNullOrBlank()) {
                        Toast.makeText(context, "Gemini API Key is not set", Toast.LENGTH_LONG).show()
                        isLoading = false
                        return@launch
                    }
                    if (commonQuestInfo.ai_photo_proof) {
                        val (isMatch, responseText) = GeminiPro.verifyImage(commonQuestInfo.ai_photo_proof_description, apiKey, bitmap)
                        aiResponse = responseText
                        if (isMatch) {
                            onQuestCompleted()
                        } else {
                            Toast.makeText(context, "Photo does not match the required description.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val response = GeminiPro.generateFromImage("Describe this image", apiKey, bitmap)
                        aiResponse = response
                        if (response != null) {
                            onQuestCompleted()
                        } else {
                            Toast.makeText(context, "Failed to get description from AI", Toast.LENGTH_LONG).show()
                        }
                    }
                    isLoading = false
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                Toast.makeText(context, "Camera permission is required for this quest", Toast.LENGTH_LONG).show()
            }
        }
    )

    val questEndTime = remember(commonQuestInfo) {
        if (commonQuestInfo.quest_started_at > 0 && commonQuestInfo.quest_duration_minutes > 0) {
            commonQuestInfo.quest_started_at + TimeUnit.MINUTES.toMillis(commonQuestInfo.quest_duration_minutes.toLong())
        } else {
            0L
        }
    }

    var overtimeText by remember { mutableStateOf("") }

    if (questEndTime > 0) {
        LaunchedEffect(questEndTime) {
            while (true) {
                val now = System.currentTimeMillis()
                if (now > questEndTime) {
                    val overtime = Duration.ofMillis(now - questEndTime)
                    val seconds = overtime.seconds
                    overtimeText = String.format(
                        "+ %02d:%02d",
                        (seconds % 3600) / 60,
                        seconds % 60
                    )
                }
                delay(1000)
            }
        }
    }

    BaseQuestView(
        hideStartQuestBtn = isQuestComplete.value || (isFailed.value && !settings.isEditingEnabled),
        onQuestStarted = {
            if (commonQuestInfo.ai_photo_proof) {
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                        cameraLauncher.launch(null)
                    }

                    else -> {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            } else {
                onQuestCompleted()
            }
        },
        progress = progress,
        loadingAnimationDuration = 400,
        startButtonTitle = "Mark as complete",
        isFailed = isFailed,
        onQuestCompleted = { onQuestCompleted() },
        isQuestCompleted = isQuestComplete
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = commonQuestInfo.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                fontFamily = JetBrainsMonoFont,
                modifier = Modifier.padding(top = 40.dp)
            )

            if (overtimeText.isNotEmpty()) {
                Text(
                    text = overtimeText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val rewardText = if (commonQuestInfo.reward_min == commonQuestInfo.reward_max) {
                "${commonQuestInfo.reward_min} coins"
            } else {
                "${commonQuestInfo.reward_min}-${commonQuestInfo.reward_max} coins"
            }
            Text(
                text = (if (isQuestComplete.value) "Reward" else "Next Reward") + ": $rewardText + ${xpToRewardForQuest(userInfo.level)} xp",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
            )

            if (!isInTimeRange.value) {
                Text(
                    text = run {
                        val (s, e) = toMinutesRange(commonQuestInfo.time_range)
                        if (isAllDayRange(commonQuestInfo.time_range)) "Time: All day"
                        else "Time: ${formatTimeMinutes(s)} to ${formatTimeMinutes(e)}"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                )
            }

            MdPad(commonQuestInfo)

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                imageBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Captured image",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                aiResponse?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}