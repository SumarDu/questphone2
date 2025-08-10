package neth.iecal.questphone.ui.screens.quest.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties

@Composable
fun RatingScale(label: String, rating: Int, onRatingSelected: (Int) -> Unit, minLabel: String = "", maxLabel: String = "") {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Rating scale row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (1..10).forEach { index ->
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
                    text = "1 - $minLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "10 - $maxLabel",
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
fun SessionReviewDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    var concentration by remember { mutableStateOf(0) }
    var productivity by remember { mutableStateOf(5) }
    var wordsStudied by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = { Text(text = "Session Review") },
        text = {
            Column {
                RatingScale(
                    label = "How was your concentration level during the session?",
                    rating = concentration,
                    onRatingSelected = { concentration = it },
                    minLabel = "зовсім розсіяний",
                    maxLabel = "максимально концентрований"
                )
                Spacer(modifier = Modifier.height(16.dp))
                RatingScale(
                    label = "How productive did you feel?",
                    rating = productivity,
                    onRatingSelected = { productivity = it },
                    minLabel = "зовсім не продуктивно",
                    maxLabel = "максимально продуктивно"
                )


                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = wordsStudied,
                    onValueChange = { wordsStudied = it.filter { char -> char.isDigit() } },
                    label = { Text("Words Studied (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(concentration, productivity, wordsStudied.toIntOrNull() ?: 0) }) {
                Text("Confirm")
            }
        }
    )
}
