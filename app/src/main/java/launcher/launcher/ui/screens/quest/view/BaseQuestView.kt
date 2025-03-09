package launcher.launcher.ui.screens.quest.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import launcher.launcher.utils.CoinHelper

@Composable
fun BaseQuestView(hideStartQuestBtn: Boolean = false, onQuestStarted: () -> Unit,progress:MutableFloatState = mutableFloatStateOf(0f), questViewBody : @Composable () -> Unit) {
    val coinHelper = CoinHelper(LocalContext.current)

    val animatedProgress by animateFloatAsState(
        targetValue = progress.floatValue,
        animationSpec = tween(durationMillis = 3000, easing = LinearEasing) // 3 seconds
    )


    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                color = Color(0xFF006064),
                size = Size(size.width, animatedProgress * size.height),
            )
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                if(!hideStartQuestBtn) {
                    Button(
                        onClick = {
                            onQuestStarted()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(text = "Start Quest")
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.Start,

                ) {
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
