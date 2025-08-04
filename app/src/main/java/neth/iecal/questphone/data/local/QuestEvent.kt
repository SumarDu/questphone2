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
    val comments: String? = null
)
