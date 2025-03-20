package launcher.launcher.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Store item types
sealed class StoreItemType {
    object Theme : StoreItemType()
    object Color : StoreItemType()
    object Font : StoreItemType()
    object Badge : StoreItemType()
    object Booster : StoreItemType()

    fun getIcon() = when(this) {
        is Theme -> Icons.Default.PlayArrow
        is Color -> Icons.Default.AccountCircle
        is Font -> Icons.Default.Info
        is Badge -> Icons.Default.Delete
        is Booster -> Icons.Default.ThumbUp
    }

    fun getTitle() = when(this) {
        is Theme -> "Themes"
        is Color -> "Colors"
        is Font -> "Fonts"
        is Badge -> "Badges"
        is Booster -> "Boosters"
    }
}

// Data model for store items
data class StoreItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val type: StoreItemType,
    val isOwned: Boolean = false,
    val isEquipped: Boolean = false,
    val previewColor: Color? = null,
    val badgeIcon: @Composable (() -> Unit)? = null
)

// View model for the store
class StoreViewModel {
    // User coins - would be linked to your user data repository in a real app
    var userCoins by mutableStateOf(1500)
        private set

    // Currently selected category
    var selectedCategory by mutableStateOf<StoreItemType>(StoreItemType.Theme)
        private set

    // Store items - in a real app, this would come from a repository
    private val _items = mutableStateListOf(
        // Themes
        StoreItem(
            id = "theme_dark",
            name = "Dark Mode",
            description = "A sleek dark theme for your app",
            price = 200,
            type = StoreItemType.Theme,
            isOwned = true,
            isEquipped = true
        ),
        StoreItem(
            id = "theme_neon",
            name = "Neon Vibes",
            description = "Bright neon accents on dark background",
            price = 500,
            type = StoreItemType.Theme
        ),
        StoreItem(
            id = "theme_minimal",
            name = "Minimalist",
            description = "Clean and simple design",
            price = 350,
            type = StoreItemType.Theme
        ),

        // Colors
        StoreItem(
            id = "color_purple",
            name = "Royal Purple",
            description = "Premium purple accent color",
            price = 150,
            type = StoreItemType.Color,
            previewColor = Color(0xFF9C27B0)
        ),
        StoreItem(
            id = "color_teal",
            name = "Ocean Teal",
            description = "Calming teal accent color",
            price = 150,
            type = StoreItemType.Color,
            previewColor = Color(0xFF009688)
        ),
        StoreItem(
            id = "color_gold",
            name = "Premium Gold",
            description = "Luxurious gold accent color",
            price = 300,
            type = StoreItemType.Color,
            previewColor = Color(0xFFFFD700)
        ),

        // Fonts
        StoreItem(
            id = "font_roboto",
            name = "Roboto Clean",
            description = "Modern sans-serif font",
            price = 100,
            type = StoreItemType.Font,
            isOwned = true,
            isEquipped = true
        ),
        StoreItem(
            id = "font_playfair",
            name = "Playfair Display",
            description = "Elegant serif font with style",
            price = 250,
            type = StoreItemType.Font
        ),
        StoreItem(
            id = "font_futuristic",
            name = "Future Tech",
            description = "Sci-fi inspired futuristic font",
            price = 400,
            type = StoreItemType.Font
        ),

        // Badges
        StoreItem(
            id = "badge_diamond",
            name = "Diamond Status",
            description = "Show off your elite status",
            price = 1000,
            type = StoreItemType.Badge,
            badgeIcon = { Icon(Icons.Default.Edit, contentDescription = "Diamond Badge", tint = Color(0xFF00BCD4)) }
        ),
        StoreItem(
            id = "badge_star",
            name = "Superstar",
            description = "You're a productivity superstar",
            price = 500,
            type = StoreItemType.Badge,
            badgeIcon = { Icon(Icons.Default.Star, contentDescription = "Star Badge", tint = Color(0xFFFFD700)) }
        ),
        StoreItem(
            id = "badge_rocket",
            name = "Rocket Launcher",
            description = "Your productivity is skyrocketing",
            price = 750,
            type = StoreItemType.Badge,
            badgeIcon = { Icon(Icons.Default.Send, contentDescription = "Rocket Badge", tint = Color(0xFFFF5722)) }
        ),

        // Boosters
        StoreItem(
            id = "booster_double",
            name = "Double Coins",
            description = "Earn double coins for 24 hours",
            price = 400,
            type = StoreItemType.Booster
        ),
        StoreItem(
            id = "booster_time",
            name = "Time Extender",
            description = "Get 25% more time on each session",
            price = 350,
            type = StoreItemType.Booster
        ),
        StoreItem(
            id = "booster_unlock",
            name = "Free Unlock",
            description = "One-time free app unlock without spending coins",
            price = 600,
            type = StoreItemType.Booster
        )
    )

    // Public store items getter
    val items: List<StoreItem>
        get() = _items.toList()

    // Get items by category
    fun getItemsByCategory(category: StoreItemType): List<StoreItem> {
        return items.filter { it.type == category }
    }

    // Select a category
    fun selectCategory(category: StoreItemType) {
        selectedCategory = category
    }

    // Purchase an item
    fun purchaseItem(itemId: String): Boolean {
        val itemIndex = _items.indexOfFirst { it.id == itemId }
        if (itemIndex == -1) return false

        val item = _items[itemIndex]

        // Check if already owned
        if (item.isOwned) return false

        // Check if user has enough coins
        if (userCoins < item.price) return false

        // Process the purchase
        userCoins -= item.price
        _items[itemIndex] = item.copy(isOwned = true)

        return true
    }

    // Equip an item
    fun equipItem(itemId: String): Boolean {
        val itemIndex = _items.indexOfFirst { it.id == itemId }
        if (itemIndex == -1) return false

        val item = _items[itemIndex]

        // Check if owned
        if (!item.isOwned) return false

        // Unequip current item of this type
        _items.forEachIndexed { index, storeItem ->
            if (storeItem.type == item.type && storeItem.isEquipped) {
                _items[index] = storeItem.copy(isEquipped = false)
            }
        }

        // Equip new item
        _items[itemIndex] = item.copy(isEquipped = true)

        return true
    }

    // Add coins (for testing or when users complete quests)
    fun addCoins(amount: Int) {
        userCoins += amount
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    navController: NavController,
    viewModel: StoreViewModel = remember { StoreViewModel() }
) {
    val coroutineScope = rememberCoroutineScope()
    var showPurchaseDialog by remember { mutableStateOf<StoreItem?>(null) }
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Success snackbar
    showSuccessMessage?.let { message ->
        LaunchedEffect(message) {
            delay(2000)
            showSuccessMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Store",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                ),
                actions = {
                    // Coins display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = Color(0xFF2A2A2A),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Coins",
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${viewModel.userCoins}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category selector
            CategorySelector(
                selectedCategory = viewModel.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            // Store items
            StoreItemsList(
                items = viewModel.getItemsByCategory(viewModel.selectedCategory),
                onItemClick = { showPurchaseDialog = it },
                onEquipClick = { item ->
                    if (viewModel.equipItem(item.id)) {
                        showSuccessMessage = "${item.name} equipped!"
                    }
                }
            )

            // Purchase dialog
            showPurchaseDialog?.let { item ->
                PurchaseDialog(
                    item = item,
                    userCoins = viewModel.userCoins,
                    onDismiss = { showPurchaseDialog = null },
                    onPurchase = {
                        if (viewModel.purchaseItem(item.id)) {
                            showSuccessMessage = "Successfully purchased ${item.name}!"
                            showPurchaseDialog = null
                        }
                    }
                )
            }

            // Success message
            AnimatedVisibility(
                visible = showSuccessMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.shadow(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = showSuccessMessage ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    selectedCategory: StoreItemType,
    onCategorySelected: (StoreItemType) -> Unit
) {
    val categories = listOf(
        StoreItemType.Theme,
        StoreItemType.Color,
        StoreItemType.Font,
        StoreItemType.Badge,
        StoreItemType.Booster
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(categories) { category ->
            CategoryItem(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: StoreItemType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFE091FF) else Color(0xFF2A2A2A)
    val contentColor = if (isSelected) Color.Black else Color.White

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        onClick = onClick,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.getIcon(),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category.getTitle(),
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StoreItemsList(
    items: List<StoreItem>,
    onItemClick: (StoreItem) -> Unit,
    onEquipClick: (StoreItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            StoreItemCard(
                item = item,
                onClick = { onItemClick(item) },
                onEquipClick = { onEquipClick(item) }
            )
        }
    }
}

@Composable
fun StoreItemCard(
    item: StoreItem,
    onClick: () -> Unit,
    onEquipClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.isOwned) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item preview/icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    // For badges with custom icon
                    item.badgeIcon != null -> {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = Color(0xFF3A3A3A),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            item.badgeIcon?.let { it() }
                        }
                    }
                    // For color preview
                    item.previewColor != null -> {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(item.previewColor, CircleShape)
                        )
                    }
                    // Default icon based on type
                    else -> {
                        Icon(
                            imageVector = item.type.getIcon(),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price or actions
            if (item.isOwned) {
                OutlinedButton(
                    onClick = onEquipClick,
                    enabled = !item.isEquipped,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (item.isEquipped) Color.Gray else Color(0xFFE091FF)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (item.isEquipped) Color.Gray else Color(0xFFE091FF)
                    )
                ) {
                    Text(
                        text = if (item.isEquipped) "Equipped" else "Equip",
                        fontSize = 14.sp
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Coins",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.price}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PurchaseDialog(
    item: StoreItem,
    userCoins: Int,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit
) {
    val hasEnoughCoins = userCoins >= item.price

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Purchase ${item.name}?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Item preview
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // For badges with custom icon
                        item.badgeIcon != null -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        color = Color(0xFF3A3A3A),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                item.badgeIcon?.let { it() }
                            }
                        }
                        // For color preview
                        item.previewColor != null -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(item.previewColor, CircleShape)
                            )
                        }
                        // Default icon based on type
                        else -> {
                            Icon(
                                imageVector = item.type.getIcon(),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = item.description,
                    fontSize = 16.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Price
                Row(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF2A2A2A),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Coins",
                        tint = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${item.price}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Not enough coins message
                if (!hasEnoughCoins) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You need ${item.price - userCoins} more coins!",
                        color = Color(0xFFFF5252),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onPurchase,
                        modifier = Modifier.weight(1f),
                        enabled = hasEnoughCoins,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE091FF),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF4A4A4A),
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Text("Purchase")
                    }
                }
            }
        }
    }
}
