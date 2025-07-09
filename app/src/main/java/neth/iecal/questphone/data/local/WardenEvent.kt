package neth.iecal.questphone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "warden_events")
data class WardenEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // "квест", "перерва", "overdue"
    val startTime: Long,
    val endTime: Long,
    val color_rgba: String
)
