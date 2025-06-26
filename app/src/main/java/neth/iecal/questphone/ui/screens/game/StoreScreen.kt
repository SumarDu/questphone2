package neth.iecal.questphone.ui.screens.game

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.*
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import java.util.concurrent.TimeUnit
import neth.iecal.questphone.services.ServiceInfo
import neth.iecal.questphone.services.saveServiceInfo
import neth.iecal.questphone.ui.navigation.Screen

class StoreViewModelFactory(private val application: Application, private val appUnlockerItemDao: AppUnlockerItemDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoreViewModel(application, appUnlockerItemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class StoreViewModel(application: Application, private val appUnlockerItemDao: AppUnlockerItemDao) : AndroidViewModel(application) {
    var coins by mutableIntStateOf(User.userInfo.coins)
    var selectedCategory by mutableStateOf(Category.BOOSTERS)
    var items by mutableStateOf<List<StoreItem>>(emptyList())
    var selectedItem by mutableStateOf<StoreItem?>(null)

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            val staticItems = InventoryItem.entries.map { item ->
                StoreItem(
                    id = item.name,
                    name = item.simpleName,
                    description = item.description,
                    icon = item.icon,
                    price = item.price,
                    category = item.category,
                    isFromEnum = true,
                    onPurchase = { purchaseSelectedItem() }
                )
            }

            appUnlockerItemDao.getAll().onEach { unlockers ->
                val dynamicItems = unlockers.map { item ->
                    val hours = TimeUnit.MINUTES.toHours(item.unlockDurationMinutes.toLong())
                    val minutes = item.unlockDurationMinutes % 60
                    val durationString = when {
                        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                        hours > 0 -> "${hours}h"
                        else -> "${minutes}m"
                    }
                    StoreItem(
                        id = item.id.toString(),
                        name = item.appName,
                        description = "Unlocks ${item.appName} for $durationString",
                        icon = R.drawable.ic_launcher_foreground, // Replace with a real icon
                        price = item.price,
                        category = Category.UNLOCKERS,
                        isFromEnum = false,
                        onPurchase = { purchaseSelectedItem() }
                    )
                }
                items = (staticItems + dynamicItems).sortedBy { it.name }
            }.launchIn(viewModelScope)
        }
    }

    fun selectCategory(category: Category) {
        selectedCategory = category
    }

    fun selectItem(item: StoreItem) {
        selectedItem = item
    }

    fun deselectItem() {
        selectedItem = null
    }

    fun purchaseSelectedItem() {
        viewModelScope.launch {
            val itemToPurchase = selectedItem ?: return@launch
            if (coins >= itemToPurchase.price) {
                User.useCoins(itemToPurchase.price)
                coins = User.userInfo.coins // Manually update coins
                if (itemToPurchase.isFromEnum) {
                    val inventoryItem = InventoryItem.valueOf(itemToPurchase.id)
                    User.addItemsToInventory(hashMapOf(inventoryItem to 1))
                } else {
                    // Handle app unlocker purchase logic
                    val unlockerId = itemToPurchase.id.toIntOrNull()
                    if (unlockerId != null) {
                        val unlocker = appUnlockerItemDao.getById(unlockerId)
                        if (unlocker != null) {
                            val expiryTime = System.currentTimeMillis() + (unlocker.unlockDurationMinutes * 60 * 1000L)
                            ServiceInfo.unlockedApps[unlocker.packageName] = expiryTime
                            saveServiceInfo(getApplication())
                        }
                    }
                }
                deselectItem()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: StoreViewModel = viewModel(
        factory = StoreViewModelFactory(
            context.applicationContext as Application,
            QuestDatabaseProvider.getInstance(context).appUnlockerItemDao()
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store") },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.coin_icon),
                            contentDescription = "Coins",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${viewModel.coins}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        },
        floatingActionButton = {
            if (viewModel.selectedCategory == Category.UNLOCKERS) {
                FloatingActionButton(onClick = { navController.navigate(Screen.CreateAppUnlocker.route) }) {
                    Icon(Icons.Default.Add, contentDescription = "Create App Unlocker")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CategorySelector(
                selectedCategory = viewModel.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )
            StoreItemsList(
                items = viewModel.items.filter { it.category == viewModel.selectedCategory },
                onItemClick = { viewModel.selectItem(it) }
            )
        }

        if (viewModel.selectedItem != null) {
            PurchaseDialog(
                item = viewModel.selectedItem!!,
                onDismiss = { viewModel.deselectItem() },
                onPurchase = { viewModel.purchaseSelectedItem() },
                canAfford = viewModel.coins >= viewModel.selectedItem!!.price
            )
        }
    }
}

@Composable
fun CategorySelector(selectedCategory: Category, onCategorySelected: (Category) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(Category.entries) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category.simpleName) }
            )
        }
    }
}

@Composable
fun StoreItemsList(items: List<StoreItem>, onItemClick: (StoreItem) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        items(items) { item ->
            StoreItemCard(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
fun StoreItemCard(item: StoreItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = item.icon),
                contentDescription = item.name,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Text(text = item.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "${item.price}", style = MaterialTheme.typography.titleMedium)
            Image(
                painter = painterResource(R.drawable.coin_icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp).padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun PurchaseDialog(item: StoreItem, onDismiss: () -> Unit, onPurchase: () -> Unit, canAfford: Boolean) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Purchase Item", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Image(painter = painterResource(id = item.icon), contentDescription = item.name, modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = item.name, style = MaterialTheme.typography.titleLarge)
                Text(text = item.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                if (item.isFromEnum) {
                    val count = User.getInventoryItemCount(InventoryItem.valueOf(item.id))
                    Text(text = "You have: $count", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "Cost: ${item.price} coins",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (canAfford) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                if (!canAfford) {
                    Text(
                        text = "Not enough coins!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = onPurchase, enabled = canAfford) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
