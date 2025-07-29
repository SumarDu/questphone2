package neth.iecal.questphone.ui.screens.quest.setup.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neth.iecal.questphone.data.DayOfWeek

@Composable
fun DayButton(
    day: DayOfWeek,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "backgroundColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "contentColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        label = "scale"
    )

    Surface(
        onClick = { onSelected(!isSelected) },
        color = backgroundColor,
        contentColor = contentColor,
        shape = CircleShape,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = day.name.first().toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
