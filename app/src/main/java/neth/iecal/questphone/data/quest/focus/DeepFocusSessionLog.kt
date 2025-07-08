package neth.iecal.questphone.data.quest.focus

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Entity(tableName = "deep_focus_session_logs")
@Serializable
data class DeepFocusSessionLog(
    @PrimaryKey(autoGenerate = true)
    @Transient // Exclude from serialization
    val id: Long = 0,
    @SerialName("client_uuid")
    val client_uuid: String = UUID.randomUUID().toString(),
    @SerialName("quest_id") 
    @Transient // Do not send to Supabase, but keep locally
        val questId: String = "",
    @SerialName("quest_name") val questName: String,
    @SerialName("session_start_time") val sessionStartTime: Long,
    @SerialName("session_duration") val sessionDuration: Long,
    @Transient // Do not send to Supabase, but keep locally
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("session_number") val sessionNumber: Int,
    @SerialName("session_type")
    val sessionType: String, // "regular" or "extra"
    val concentration: Int,
    val productivity: Int? = null,
    @SerialName("words_studied")
    val wordsStudied: Int? = null,
    val difficulty: Int? = null,
    val mood: Int? = null,
    @SerialName("study_topic")
    val studyTopic: String? = null,
    @Transient
    val synced: Boolean = false
)
