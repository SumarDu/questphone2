package neth.iecal.questphone.data.game

import androidx.compose.runtime.Stable

@Stable
data class StoreItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: Int,
    val price: Int,
    val category: Category,
    val isFromEnum: Boolean, // True if the item is from the InventoryItem enum
    val onPurchase: () -> Unit,
    val isBlocked: Boolean = false,
    val blockedUntil: Long? = null,
    val blockedSources: List<String> = emptyList(),
    val purchaseStartTimeMinutes: Int? = null,
    val purchaseEndTimeMinutes: Int? = null,
    val isOutsidePurchaseTime: Boolean = false
)
