package neth.iecal.questphone.ui.screens.game

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import neth.iecal.questphone.data.timer.TimerService
import neth.iecal.questphone.ui.screens.launcher.TimerViewModel
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
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
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.*
import androidx.compose.material.icons.filled.Stars
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.BlockedUnlockerDao
import neth.iecal.questphone.data.quest.SanctionsEnforcer
import neth.iecal.questphone.data.settings.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import neth.iecal.questphone.services.ServiceInfo
import neth.iecal.questphone.services.saveServiceInfo
import neth.iecal.questphone.ui.navigation.Screen

class StoreViewModelFactory(
    private val application: Application,
    private val appUnlockerItemDao: AppUnlockerItemDao,
    private val blockedUnlockerDao: BlockedUnlockerDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoreViewModel(application, appUnlockerItemDao, blockedUnlockerDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class StoreViewModel(
    application: Application,
    private val appUnlockerItemDao: AppUnlockerItemDao,
    private val blockedUnlockerDao: BlockedUnlockerDao
) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    val settings = settingsRepository.settings
    var coins by mutableIntStateOf(User.userInfo.coins)
    var diamonds by mutableIntStateOf(User.userInfo.diamonds)
    var totalTokens by mutableIntStateOf(User.getTotalTokens())
    var selectedCategory by mutableStateOf(Category.UNLOCKERS)
    var items by mutableStateOf<List<StoreItem>>(emptyList())
    var selectedItem by mutableStateOf<StoreItem?>(null)
    var showPurchaseNotAllowedDialog by mutableStateOf(false)
    var itemToDelete by mutableStateOf<StoreItem?>(null)
    var showTokensDialog by mutableStateOf(false)

    init {
        loadItems()
        refreshTokens()
    }
    
    fun refreshTokens() {
        totalTokens = User.getTotalTokens()
    }

    private fun loadItems() {
        viewModelScope.launch {
            // Clean expired blocks at startup
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val questDao = QuestDatabaseProvider.getInstance(getApplication()).questDao()
                SanctionsEnforcer.enforceSanctions(questDao, blockedUnlockerDao, now)
                blockedUnlockerDao.deleteExpired(now)
            }

            val unlockersFlow = appUnlockerItemDao.getAll()
            val blocksFlow = blockedUnlockerDao.getActive(System.currentTimeMillis())

            // Combine settings + unlockers + blocks so price updates when settings change
            settings
                .combine(unlockersFlow) { s, unlockers -> s to unlockers }
                .combine(blocksFlow) { sAndUnlockers, blocks ->
                    Triple(sAndUnlockers.first, sAndUnlockers.second, blocks)
                }
                .onEach { (s, unlockers, blocks) ->
                    val staticItems = InventoryItem.entries.map { item ->
                        val priceOverride = if (item == InventoryItem.DIAMOND_EXCHANGE) s.diamondExchangeDiamonds else item.price
                        StoreItem(
                            id = item.name,
                            name = item.simpleName,
                            description = item.description,
                            icon = item.icon,
                            price = priceOverride,
                            category = item.category,
                            isFromEnum = true,
                            onPurchase = { purchaseSelectedItem() }
                        )
                    }

                    val blocksById = blocks.associateBy { it.unlocker_id }
                    val currentTimeMinutes = getCurrentTimeInMinutes()
                    val dynamicItems = unlockers.map { item ->
                        val hours = TimeUnit.MINUTES.toHours(item.unlockDurationMinutes.toLong())
                        val minutes = item.unlockDurationMinutes % 60
                        val durationString = when {
                            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                            hours > 0 -> "${hours}h"
                            else -> "${minutes}m"
                        }
                        val block = blocksById[item.id]
                        val isOutsidePurchaseTime = if (item.purchaseStartTimeMinutes != null && item.purchaseEndTimeMinutes != null) {
                            !isTimeInRange(currentTimeMinutes, item.purchaseStartTimeMinutes, item.purchaseEndTimeMinutes)
                        } else {
                            false
                        }
                        StoreItem(
                            id = item.id.toString(),
                            name = item.appName,
                            description = "Unlocks for $durationString",
                            icon = R.drawable.ic_launcher_foreground, // Replace with a real icon
                            price = item.price,
                            category = Category.UNLOCKERS,
                            isFromEnum = false,
                            onPurchase = { purchaseSelectedItem() },
                            isBlocked = block != null,
                            blockedUntil = block?.blocked_until,
                            blockedSources = block?.sources?.split('|')?.filter { it.isNotBlank() } ?: emptyList(),
                            purchaseStartTimeMinutes = item.purchaseStartTimeMinutes,
                            purchaseEndTimeMinutes = item.purchaseEndTimeMinutes,
                            isOutsidePurchaseTime = isOutsidePurchaseTime
                        )
                    }

                    items = (staticItems + dynamicItems).sortedBy { it.name }
                }
                .launchIn(viewModelScope)
        }
    }

    fun selectCategory(category: Category) {
        selectedCategory = category
    }

    fun selectItem(item: StoreItem, timerState: neth.iecal.questphone.data.timer.TimerState) {
        if (item.isBlocked || item.isOutsidePurchaseTime) return
        if (item.category == Category.UNLOCKERS) {
            val isBreak = timerState.mode == neth.iecal.questphone.data.timer.TimerMode.BREAK
            val isBreakOvertime = timerState.mode == neth.iecal.questphone.data.timer.TimerMode.OVERTIME && timerState.isBreakOvertime

            if (isBreak || isBreakOvertime) {
                selectedItem = item
            } else {
                showPurchaseNotAllowedDialog = true
            }
        } else {
            selectedItem = item
        }
    }

    fun deselectItem() {
        selectedItem = null
    }

    fun purchaseSelectedItem() {
        viewModelScope.launch {
            val itemToPurchase = selectedItem ?: return@launch
            val isDiamondExchange = itemToPurchase.id == InventoryItem.DIAMOND_EXCHANGE.name
            val settingsSnapshot = settings.first()
            val rateDiamonds = settingsSnapshot.diamondExchangeDiamonds.coerceAtLeast(1)
            val rateCoins = settingsSnapshot.diamondExchangeCoins.coerceAtLeast(0)
            val canAfford = if (isDiamondExchange) diamonds >= itemToPurchase.price else coins >= itemToPurchase.price
            if (canAfford) {
                val prePurchaseCoins = coins
                if (isDiamondExchange) {
                    // Exchange using configurable rate: per [rateDiamonds -> rateCoins]
                    val spent = if (User.useDiamonds(itemToPurchase.price)) itemToPurchase.price else 0
                    if (spent > 0) {
                        val gainedCoins = (rateCoins * spent) / rateDiamonds
                        User.addCoins(gainedCoins)
                        coins = User.userInfo.coins
                        diamonds = User.userInfo.diamonds
                    }
                } else {
                    User.useCoins(itemToPurchase.price)
                    coins = User.userInfo.coins // Manually update coins
                }

                if (itemToPurchase.isFromEnum) {
                    val inventoryItem = InventoryItem.valueOf(itemToPurchase.id)
                    if (!isDiamondExchange) {
                        User.addItemsToInventory(hashMapOf(inventoryItem to 1))
                    }
                } else {
                    // Handle app unlocker purchase logic
                    val unlockerId = itemToPurchase.id.toIntOrNull()
                    if (unlockerId != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            val unlocker = appUnlockerItemDao.getById(unlockerId)
                            if (unlocker != null) {
                                withContext(Dispatchers.Main) {
                                    val intent = Intent(getApplication(), TimerService::class.java).apply {
                                        action = TimerService.ACTION_START_UNLOCK_TIMER
                                        putExtra(TimerService.EXTRA_UNLOCK_DURATION, unlocker.unlockDurationMinutes)
                                        putExtra(TimerService.EXTRA_PACKAGE_NAME, unlocker.packageName)
                                        putExtra(TimerService.EXTRA_REWARD_COINS, -itemToPurchase.price)
                                        putExtra(TimerService.EXTRA_PRE_REWARD_COINS, prePurchaseCoins)
                                    }
                                    getApplication<Application>().startService(intent)
                                }
                            }
                        }
                    }
                }
                deselectItem()
            }
        }
    }

    fun onDeleteItemRequest(item: StoreItem) {
        if (!item.isFromEnum) {
            itemToDelete = item
        }
    }

    fun onDeleteItemCancel() {
        itemToDelete = null
    }

    fun onDeleteItemConfirm() {
        viewModelScope.launch {
            val currentSettings = settingsRepository.settings.first()
            if (currentSettings.isItemDeletionEnabled) {
                itemToDelete?.let { item ->
                    if (!item.isFromEnum) {
                        item.id.toIntOrNull()?.let { id ->
                            appUnlockerItemDao.deleteById(id)
                        }
                    }
                }
            } else {
                Toast.makeText(getApplication(), "Item deletion is disabled in settings", Toast.LENGTH_SHORT).show()
            }
            itemToDelete = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    navController: NavController,
    timerViewModel: TimerViewModel = viewModel()
) {
    val context = LocalContext.current
    val viewModel: StoreViewModel = viewModel(
        factory = StoreViewModelFactory(
            context.applicationContext as Application,
            QuestDatabaseProvider.getInstance(context).appUnlockerItemDao(),
            QuestDatabaseProvider.getInstance(context).blockedUnlockerDao()
        )
    )
    val settings by viewModel.settings.collectAsState()
    
    // Refresh tokens when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshTokens()
    }

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
                        Spacer(modifier = Modifier.width(16.dp))
                        Image(
                            painter = painterResource(R.drawable.diamond_icon),
                            contentDescription = "Diamonds",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${User.userInfo.diamonds + User.userInfo.diamonds_pending}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(16.dp))
                        if (settings.tokensEnabled) {
                            Row(
                                modifier = Modifier.clickable {
                                    viewModel.refreshTokens()
                                    viewModel.showTokensDialog = true
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "Tokens",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFFFFD700) // Gold color for tokens
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${viewModel.totalTokens}", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (viewModel.selectedCategory == Category.UNLOCKERS && settings.isItemCreationEnabled) {
                FloatingActionButton(onClick = { navController.navigate(Screen.CreateAppUnlocker.route) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Unlocker")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category selector row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Category.entries) { category ->
                    FilterChip(
                        selected = category == viewModel.selectedCategory,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.simpleName) }
                    )
                }
            }
            
            // Items list
            StoreItemsList(
                items = viewModel.items.filter { it.category == viewModel.selectedCategory },
                onItemClick = { item -> viewModel.selectItem(item, timerViewModel.timerState.value) },
                onDeleteClick = { item -> viewModel.onDeleteItemRequest(item) },
                isDeletionEnabled = settings.isItemDeletionEnabled
            )
        }

        if (viewModel.selectedItem != null) {
            PurchaseDialog(
                item = viewModel.selectedItem!!,
                onDismiss = { viewModel.deselectItem() },
                onPurchase = { viewModel.purchaseSelectedItem() },
                canAfford = run {
                    val sel = viewModel.selectedItem
                    if (sel != null && sel.id == InventoryItem.DIAMOND_EXCHANGE.name) {
                        viewModel.diamonds >= sel.price
                    } else {
                        viewModel.coins >= (sel?.price ?: 0)
                    }
                }
            )
        }

        if (viewModel.showPurchaseNotAllowedDialog) {
            PurchaseNotAllowedDialog(
                onDismiss = { viewModel.showPurchaseNotAllowedDialog = false }
            )
        }

        if (viewModel.itemToDelete != null) {
            DeleteConfirmationDialog(
                item = viewModel.itemToDelete!!,
                onDismiss = { viewModel.onDeleteItemCancel() },
                onConfirm = { viewModel.onDeleteItemConfirm() }
            )
        }

        if (settings.tokensEnabled && viewModel.showTokensDialog) {
            TokensDialog(
                tokens = User.getTokens(),
                onDismiss = { viewModel.showTokensDialog = false }
            )
        }
    }
}

@Composable
fun StoreItemsList(
    items: List<StoreItem>,
    onItemClick: (StoreItem) -> Unit,
    onDeleteClick: (StoreItem) -> Unit,
    isDeletionEnabled: Boolean
) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        items(items) { item ->
            StoreItemCard(
                item = item,
                onClick = { onItemClick(item) },
                onDelete = { onDeleteClick(item) },
                isDeletionEnabled = isDeletionEnabled
            )
        }
    }
}

@Composable
fun StoreItemCard(item: StoreItem, onClick: () -> Unit, onDelete: () -> Unit, isDeletionEnabled: Boolean) {
    var showSources by remember { mutableStateOf(false) }
    val isDisabled = item.isBlocked || item.isOutsidePurchaseTime
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(if (isDisabled) 0.4f else 1f)
            .clickable(enabled = !isDisabled, onClick = onClick),
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
                if (item.isBlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val untilText = item.blockedUntil?.let { ts ->
                        val fmt = SimpleDateFormat("HH:mm dd.MM.yy", Locale.getDefault())
                        "Disabled until ${fmt.format(Date(ts))}"
                    } ?: "Disabled"
                    Text(
                        text = untilText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { if (item.blockedSources.isNotEmpty()) showSources = true }
                    )
                }
                if (item.isOutsidePurchaseTime && item.purchaseStartTimeMinutes != null && item.purchaseEndTimeMinutes != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Available: ${neth.iecal.questphone.utils.formatTimeMinutes(item.purchaseStartTimeMinutes)} — ${neth.iecal.questphone.utils.formatTimeMinutes(item.purchaseEndTimeMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (!item.isFromEnum && isDeletionEnabled) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Item")
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "${item.price}", style = MaterialTheme.typography.titleMedium)
            val isDiamond = item.id == InventoryItem.DIAMOND_EXCHANGE.name
            Image(
                painter = painterResource(if (isDiamond) R.drawable.diamond_icon else R.drawable.coin_icon),
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

                val isDiamond = item.id == InventoryItem.DIAMOND_EXCHANGE.name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cost: ${item.price} ${if (isDiamond) "diamonds" else "coins"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (canAfford) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(6.dp))
                    Image(
                        painter = painterResource(if (isDiamond) R.drawable.diamond_icon else R.drawable.coin_icon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (!canAfford) {
                    Text(
                        text = if (isDiamond) "Not enough diamonds!" else "Not enough coins!",
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

@Composable
fun PurchaseNotAllowedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Purchase Not Available") },
        text = { Text("You can only purchase unlockers during a break or when a break is overdue.") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(item: StoreItem, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Item") },
        text = { Text("Are you sure you want to delete '${item.name}'? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TokensDialog(tokens: Map<String, Int>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tokens")
            }
        },
        text = {
            if (tokens.isEmpty()) {
                Text(
                    text = "No tokens yet. Complete SwiftMark quests to earn tokens!",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(tokens.entries.sortedByDescending { it.value }) { (questTitle, count) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = questTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "×$count",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Helper function to get current time in minutes from midnight
private fun getCurrentTimeInMinutes(): Int {
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    return hour * 60 + minute
}

// Helper function to check if current time is within the allowed range
private fun isTimeInRange(currentMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
    return if (startMinutes <= endMinutes) {
        // Normal range (e.g., 8:00 AM to 10:00 PM)
        currentMinutes in startMinutes..endMinutes
    } else {
        // Range crosses midnight (e.g., 10:00 PM to 2:00 AM)
        currentMinutes >= startMinutes || currentMinutes <= endMinutes
    }
}
