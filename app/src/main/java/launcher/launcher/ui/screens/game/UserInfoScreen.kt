package launcher.launcher.ui.screens.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import launcher.launcher.data.game.UserInfo
import launcher.launcher.data.game.xpToLevelUp
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun UserInfoScreen(userInfo: UserInfo) {
    val context = LocalContext.current
    val totalXpForNextLevel = xpToLevelUp(userInfo.level + 1)
    val totalXpForCurrentLevel = xpToLevelUp(userInfo.level)
    val xpProgress = (userInfo.xp - totalXpForCurrentLevel).toFloat() /
            (totalXpForNextLevel - totalXpForCurrentLevel)

    // Helper function to format timestamps
    fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm")
        return dateFormat.format(Date(timestamp))
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📊 Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "💰 Coins: ${userInfo.coins}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "🔥 Streak: ${userInfo.streak} days",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Active Boosts Section (Uncommented and fixed)
//            if (userInfo.activeBoosts != null) {
//                Text(
//                    "🚀 Active Boosts",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold
//                )
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(top = 8.dp),
//                    elevation = CardDefaults.cardElevation(4.dp)
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        // Assuming activeBoosts has these properties
//                        Text("XP Multiplier: x${userInfo.activeBoosts.xpMultiplier}")
//                        if (userInfo.activeBoosts.xpBooster > 0) {
//                            Text("Streak Freezer Active Until: ${formatTimestamp(userInfo.activeBoosts.streakProtectedUntil)}")
//                        }
//                        if (userInfo.activeBoosts.luckBoostUntil > 0) {
//                            Text("Luck Boost Active Until: ${formatTimestamp(userInfo.activeBoosts.luckBoostUntil)}")
//                        }
//                    }
//                }

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
                                "• ${reward.name} × $count",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
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