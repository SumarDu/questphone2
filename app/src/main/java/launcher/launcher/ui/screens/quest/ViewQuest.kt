package launcher.launcher.ui.screens.quest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import launcher.launcher.Constants
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.BaseQuest
import launcher.launcher.data.quest.AppFocus
import launcher.launcher.data.quest.DeepFocus
import launcher.launcher.data.quest.FocusTimeConfig
import launcher.launcher.ui.screens.quest.view.DeepFocusQuestView
import launcher.launcher.ui.theme.JetBrainsMonoFont
import launcher.launcher.utils.CoinHelper
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.getCurrentDate

@Composable
fun ViewQuest(
    baseQuest: BaseQuest
) {
    when(baseQuest.integrationId){
        IntegrationId.APP_FOCUS -> TODO()
        IntegrationId.DEEP_FOCUS -> DeepFocusQuestView(baseQuest)
        IntegrationId.HEALTH -> TODO()
    }

}