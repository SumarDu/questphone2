package launcher.launcher.ui.screens.game

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import launcher.launcher.data.game.Rewards
import launcher.launcher.data.game.User
import launcher.launcher.data.game.User.userInfo
import launcher.launcher.data.game.UserInfo
import launcher.launcher.data.game.getStreakInfo
import launcher.launcher.data.game.getUserInfo
import launcher.launcher.data.game.isBoosterActive
import launcher.launcher.data.game.xpToLevelUp
import launcher.launcher.utils.CoinHelper
import launcher.launcher.utils.formatRemainingTime
import launcher.launcher.utils.getFullFormattedTime
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun UserInfoScreen() {
    val context = LocalContext.current
    val coinHelper = CoinHelper(context)
    val streakData = getStreakInfo(context)

    val totalXpForNextLevel = xpToLevelUp(userInfo.level + 1)
    val totalXpForCurrentLevel = xpToLevelUp(userInfo.level)
    val xpProgress = (userInfo.xp - totalXpForCurrentLevel).toFloat() /
            (totalXpForNextLevel - totalXpForCurrentLevel)

    val selectedInventoryItem = remember { mutableStateOf<Rewards?>(null) }

    if(selectedInventoryItem.value!=null){
        InventoryItemInfoDialog(selectedInventoryItem.value!!, onDismissRequest = {
            selectedInventoryItem.value = null
        })
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))


            // Level and XP Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🎚 Level ${userInfo.level}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 32.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        LinearProgressIndicator(
                            progress = { xpProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "XP: ${userInfo.xp} / $totalXpForNextLevel",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Section
            Text(
                "📊 Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "💰 Coins: ${coinHelper.getCoinCount()}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "🔥 Current Streak: ${streakData.currentStreak} days",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "🔥 Longest Streak: ${streakData.longestStreak} days",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (userInfo.activeBoosts.isNotEmpty()) {
                Text(
                    "🚀 Active Boosts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        userInfo.activeBoosts.forEach {
                            Text(it.key.simpleName + ": " + formatRemainingTime(it.value))
                        }
                    }
                    }
                }

            Spacer(modifier = Modifier.height(16.dp))

            // Inventory Section
            Text(
                "🎁 Inventory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (userInfo.inventory.isEmpty()) {
                        Text("Your inventory is empty.")
                    } else {
                        userInfo.inventory.forEach { (reward, count) ->
                            Text(
                                "• ${reward.simpleName} × $count",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                                    .clickable{
                                        selectedInventoryItem.value = reward
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Achievements Section
            Text(
                "🏆 Achievements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (userInfo.achievements.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("No achievements yet. Keep playing to earn rewards!")
                    }
                }
            } else {
                userInfo.achievements.forEach {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "🥇 ${it.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                it.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Add some bottom padding
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun InventoryItemInfoDialog(
    reward: Rewards,
    onDismissRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(24.dp)
                .wrapContentSize()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = reward.simpleName,
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if(User.isBoosterActive(Rewards.XP_BOOSTER)){
                    Text("XP booster active until: "  + userInfo.activeBoosts.getOrDefault(
                        Rewards.XP_BOOSTER,getFullFormattedTime()))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Close")
                    }
                    if(reward.isUsableFromInventory && !User.isBoosterActive(Rewards.XP_BOOSTER)){
                        Button(onClick = {
                            reward.onUse()
                            onDismissRequest()
                        }) {
                            Text("Use")
                        }
                    }
                }
            }
        }

    }
}