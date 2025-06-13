package launcher.launcher.ui.screens.game

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import io.github.jan.supabase.auth.auth
import launcher.launcher.R
import launcher.launcher.data.game.InventoryItem
import launcher.launcher.data.game.User
import launcher.launcher.data.game.User.userInfo
import launcher.launcher.data.game.getStreakInfo
import launcher.launcher.data.game.isBoosterActive
import launcher.launcher.data.game.useInventoryItem
import launcher.launcher.data.game.xpToLevelUp
import launcher.launcher.utils.CoinHelper
import launcher.launcher.utils.Supabase
import launcher.launcher.utils.formatRemainingTime
import launcher.launcher.utils.getFullFormattedTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen() {
    val context = LocalContext.current
    val coinHelper = CoinHelper(context)
    val streakData = getStreakInfo(context)

    val totalXpForNextLevel = xpToLevelUp(userInfo.level + 1)
    val totalXpForCurrentLevel = xpToLevelUp(userInfo.level)
    val xpProgress = (userInfo.xp - totalXpForCurrentLevel).toFloat() /
            (totalXpForNextLevel - totalXpForCurrentLevel)

    val selectedInventoryItem = remember { mutableStateOf<InventoryItem?>(null) }

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
                        "${userInfo.username}}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        userInfo.full_name,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Image(
                        painter = rememberAsyncImagePainter(

                            model = ImageRequest.Builder(LocalContext.current)
                                .data(
                                    if (userInfo.has_profile)
                                        "https://hplszhlnchhfwngbojnc.supabase.co/storage/v1/object/public/profile/${Supabase.supabase.auth.currentUserOrNull()!!.id}/profile"
                                    else R.drawable.baseline_person_24
                                )
                                .crossfade(true)
                                .error(R.drawable.baseline_person_24)
                                .placeholder(R.drawable.baseline_person_24)
                                .build(),
                        ),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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

            if (userInfo.active_boosts.isNotEmpty()) {
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
                        userInfo.active_boosts.forEach {
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)) {
                    if (userInfo.inventory.isEmpty()) {
                        Text("Your inventory is empty.")
                    } else {
                        userInfo.inventory.forEach { (reward, count) ->

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable{
                                    selectedInventoryItem.value = reward
                                }
                            ) {
                                Image(
                                    painter = painterResource(reward.icon),
                                    contentDescription = reward.simpleName,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    " ${reward.simpleName} × $count",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 2.dp)

                                )
                            }

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
    reward: InventoryItem,
    onDismissRequest: () -> Unit = {}
) {
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


                Image(
                    painter = painterResource(reward.icon),
                    contentDescription = reward.simpleName,
                    modifier = Modifier.size(60.dp)
                )

                Text(
                    text = reward.simpleName,
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if(User.isBoosterActive(InventoryItem.XP_BOOSTER)){
                    Text("XP booster active until: "  + userInfo.active_boosts.getOrDefault(
                        InventoryItem.XP_BOOSTER,getFullFormattedTime()))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Close")
                    }
                    if(reward.isUsableFromInventory && !User.isBoosterActive(InventoryItem.XP_BOOSTER)){
                        Button(onClick = {
                            reward.onUse()
                            User.useInventoryItem(InventoryItem.XP_BOOSTER)
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