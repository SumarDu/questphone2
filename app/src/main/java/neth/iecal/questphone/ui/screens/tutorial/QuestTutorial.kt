package neth.iecal.questphone.ui.screens.tutorial

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.jeziellago.compose.markdowntext.MarkdownText
import neth.iecal.questphone.utils.fetchUrlContent

@Composable
fun QuestTutorial(url: String){
    val isLoading = remember { mutableStateOf(true) }
    var response  = remember { mutableStateOf("") }
    LaunchedEffect(response.value) {
        response.value = fetchUrlContent(url) ?: "Failed to Load [Site]($url)"
        isLoading.value = false
    }
    if(isLoading.value){
        Text("Loading Docs")
    }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.verticalScroll(scrollState)) {

        MarkdownText(
            markdown = response.value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}