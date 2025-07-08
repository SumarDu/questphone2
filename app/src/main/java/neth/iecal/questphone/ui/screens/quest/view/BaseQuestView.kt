package neth.iecal.questphone.ui.screens.quest.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.InventoryItem
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.getInventoryItemCount
import neth.iecal.questphone.data.game.useInventoryItem
import neth.iecal.questphone.utils.VibrationHelper

@Composable
fun BaseQuestView(
    startButtonTitle: String = "Start Quest",
    hideStartQuestBtn: Boolean = false,
    onQuestStarted: () -> Unit,
    loadingAnimationDuration: Int = 3000,
    isFailed: MutableState<Boolean> = mutableStateOf(false),
    progress: MutableFloatState = mutableFloatStateOf(0f),
    onQuestCompleted: () -> Unit = {},
    isQuestCompleted: MutableState<Boolean> = mutableStateOf(false),
    questStartComponent: (@Composable () -> Unit)? = null,
    questViewBody: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.floatValue,
        animationSpec = tween(durationMillis = loadingAnimationDuration, easing = LinearEasing)
    )

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                color = if (!isFailed.value) Color(0xFF006064) else Color(0xFFB00023),
                size = Size(size.width, animatedProgress * size.height),
            )
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (!hideStartQuestBtn) {
                    if (questStartComponent != null) {
                        questStartComponent()
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(15.dp))
                            Button(
                                onClick = {
                                    VibrationHelper.vibrate(100)
                                    onQuestStarted()
                                }
                            ) {
                                Text(text = startButtonTitle)
                            }
                        }
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(innerPadding),
                horizontalAlignment = Alignment.Start,

                ) {



                // Coins display
                Text(
                    text = "${User.userInfo.coins} coins",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.End)
                )

                questViewBody()
            }
        }
    }
}
