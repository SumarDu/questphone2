package neth.iecal.questphone.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class QuestEventSupabase(
    val id: Int? = null,
    val event_name: String,
    val start_time: String,
    val end_time: String?,
    val color_rgba: String
)
