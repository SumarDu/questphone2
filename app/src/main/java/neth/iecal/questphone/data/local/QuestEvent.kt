package neth.iecal.questphone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "quest_events")
data class QuestEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventName: String,
    val startTime: Long,
    val endTime: Long,
    val comments: String? = null,
    val rewardCoins: Int? = null, // Amount of coins earned from quest completion
    val preRewardCoins: Int? = null, // User's coin balance before reward was added
    val isRewardPending: Boolean = false, // Flag to indicate if the reward is pending for a completed quest
    val synced: Boolean = false // Flag to track if event has been synced to Supabase
)
