package neth.iecal.questphone.ui.screens.quest.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import neth.iecal.questphone.data.quest.QuestInfoState

@Composable
fun ColorSelector(questInfoState: QuestInfoState) {
    var showDialog by remember { mutableStateOf(false) }
    val controller = rememberColorPickerController()

    Column {
        Text("Quest Color")
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(parseColor(questInfoState.colorRgba))
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Color") },
            text = {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    controller = controller,
                    onColorChanged = { colorEnvelope: ColorEnvelope ->
                        val color = colorEnvelope.color
                        questInfoState.colorRgba = "${color.red},${color.green},${color.blue},${color.alpha}"
                    }
                )
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

fun parseColor(rgba: String): Color {
    return try {
        val parts = rgba.split(",").map { it.trim().toFloat() }
        if (parts.size == 4) {
            Color(red = parts[0], green = parts[1], blue = parts[2], alpha = parts[3])
        } else {
            Color.Gray
        }
    } catch (e: Exception) {
        Color.Gray
    }
}
