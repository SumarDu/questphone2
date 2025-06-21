package neth.iecal.questphone.ui.screens.pet.components

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SpriteSheetAnimation(
    @DrawableRes spriteSheetRes: Int,
    frameWidth: Int,
    frameHeight: Int,
    frameCount: Int,
    frameDurationMs: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(spriteSheetRes) {
        BitmapFactory.decodeResource(context.resources, spriteSheetRes)
    }

    var currentFrame by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(frameDurationMs.toLong())
            currentFrame = (currentFrame + 1) % frameCount
        }
    }

    Canvas(modifier = modifier.size(frameWidth.dp, frameHeight.dp)) {
        drawImage(
            image = bitmap.asImageBitmap(),
            srcOffset = IntOffset(x = currentFrame * frameWidth, y = 0),
            srcSize = IntSize(frameWidth, frameHeight),
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
    }
}
