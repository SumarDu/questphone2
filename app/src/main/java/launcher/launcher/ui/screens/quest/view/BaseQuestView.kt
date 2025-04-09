package launcher.launcher.ui.screens.quest.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import launcher.launcher.R
import launcher.launcher.data.game.Rewards
import launcher.launcher.data.game.User
import launcher.launcher.data.game.UserInfo
import launcher.launcher.data.game.getInventoryItemCount
import launcher.launcher.data.game.getUserInfo
import launcher.launcher.data.game.saveUserInfo
import launcher.launcher.data.game.useInventoryItem
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.utils.CoinHelper
import launcher.launcher.utils.VibrationHelper

@Composable
fun BaseQuestView(startButtonTitle: String = "Start Quest", hideStartQuestBtn: Boolean = false, onQuestStarted: () -> Unit, loadingAnimationDuration: Int = 3000,isFailed: MutableState<Boolean> = mutableStateOf(false), progress:MutableFloatState = mutableFloatStateOf(0f),onQuestCompleted: ()->Unit = {}, isQuestCompleted: MutableState<Boolean> = mutableStateOf(false), questViewBody : @Composable () -> Unit,) {
    val context = LocalContext.current
    val coinHelper = CoinHelper(context)

    val scrollState = rememberScrollState()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.floatValue,
        animationSpec = tween(durationMillis = loadingAnimationDuration, easing = LinearEasing)
    )

    val isUseQuestSkipperDialogVisible = remember { mutableStateOf(false) }


    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                color = if(!isFailed.value) Color(0xFF006064) else Color(0xFFB00023),
                size = Size(size.width, animatedProgress * size.height),
            )
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if(!isQuestCompleted.value && User.getInventoryItemCount(Rewards.QUEST_SKIPPER) > 0){
                            OutlinedButton(
                                onClick = {
                                    VibrationHelper.vibrate(50)
                                    isUseQuestSkipperDialogVisible.value = true
                                },
                                modifier = Modifier.padding(bottom = 16.dp)

                            ) {
                                Text(text = ">>")
                            }

                        }
                        if(!hideStartQuestBtn) {
                            Spacer(modifier = Modifier.width(15.dp))
                            Button(
                                onClick = {
                                    VibrationHelper.vibrate(100)
                                    onQuestStarted()
                                },
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text(text = startButtonTitle)
                            }
                        }
                    }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(innerPadding),
                horizontalAlignment = Alignment.Start,

                ) {

                if(isUseQuestSkipperDialogVisible.value){
                    Dialog(onDismissRequest = { isUseQuestSkipperDialogVisible.value = false }) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Do you want to use a QUEST SKIPPER to skip this quest for today?",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Available: ${User.getInventoryItemCount(Rewards.QUEST_SKIPPER)}",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row {
                                Button(
                                    onClick = {
                                        VibrationHelper.vibrate(200)
                                        User.useInventoryItem(Rewards.QUEST_SKIPPER)
                                        onQuestCompleted()
                                        isUseQuestSkipperDialogVisible.value = false
                                    },
                                ) {
                                    Text("Yes")
                                }
                                Spacer(modifier = Modifier.size(16.dp))

                                Button(
                                    onClick = {
                                        isUseQuestSkipperDialogVisible.value = false
                                    },
                                ) {
                                    Text("No")
                                }
                            }
                        }
                    }

                }

                // Coins display
                Text(
                    text = "${coinHelper.getCoinCount()} coins",
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
