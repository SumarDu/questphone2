package neth.iecal.questphone.ui.screens.quest.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@Composable
fun QuestRatingScale(label: String, rating: Int, onRatingSelected: (Int) -> Unit, range: IntRange = 1..10, minLabel: String = "", maxLabel: String = "") {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Rating scale row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            range.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (index <= rating) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { onRatingSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        color = if (index <= rating) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // Anchor labels
        if (minLabel.isNotEmpty() || maxLabel.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${range.first} - $minLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${range.last} - $maxLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun EmojiMoodScale(label: String, rating: Int, onRatingSelected: (Int) -> Unit) {
    val emojis = listOf("😭", "😢", "🙁", "😐", "🙂", "😊", "😁")
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            emojis.forEachIndexed { index, emoji ->
                val currentRating = index + 1
                val isSelected = currentRating == rating
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onRatingSelected(currentRating) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
fun QuestCompletionReviewDialog(
    onDismiss: () -> Unit,
    onConfirm: (difficulty: Int, mood: Int) -> Unit
) {
    var difficulty by remember { mutableStateOf(0) }

    var mood by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = { Text(text = "Quest Review") },
        text = {
            Column {
                QuestRatingScale(
                    label = "How difficult was the material?",
                    rating = difficulty,
                    onRatingSelected = { difficulty = it },
                    minLabel = "дуже легко",
                    maxLabel = "дуже складно"
                )
                Spacer(modifier = Modifier.height(16.dp))
                EmojiMoodScale(
                    label = "Mood",
                    rating = mood,
                    onRatingSelected = { mood = it }
                )
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Later")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(difficulty, mood) }) {
                Text("Confirm")
            }
        }
    )
}
