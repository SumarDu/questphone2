package neth.iecal.questphone.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class QuestEventSupabase(
    val id: Int? = null,
    val event_name: String,
    val start_time: Long,
    val end_time: Long?,
    val color_rgba: String
)
