package neth.iecal.questphone.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PenaltyLogSupabase(
    val id: String,
    val occurred_at: Long,
    val amount: Int,
    val balance_before: Int,
    val source: String,
    val quest_id: String? = null,
    val quest_title: String? = null
)
