package neth.iecal.questphone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quest_events")
data class QuestEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventName: String,
    val startTime: Long,
    val endTime: Long,
    val colorRgba: String,
    val comments: String? = null
)
