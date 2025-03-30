package launcher.launcher.ui.screens.quest.setup.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDestruct() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    var expanded by remember { mutableStateOf(false) }
    val options = listOf<String>("Never","Select Date")

    val selectedOption = remember { mutableStateOf(options[0]) }
    Button(onClick = { expanded = true }) {
        Text(text = "Auto Destroy: " + if(selectedOption.value == options[0]) "Never" else selectedDate)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                    expanded = false
                    selectedOption.value = option
                    if(option == "Select Date"){
                        showDialog = true
                    }
                }
            )
        }
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        ) {
            val datePickerState = rememberDatePickerState()

            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )

            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let { millis ->
                    selectedDate = LocalDate.ofEpochDay(millis / 86_400_000)

                }
            }
        }
    }
}
