package neth.iecal.questphone.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PenaltyLogSupabase(
    val id: String,
    val occurred_at: Long,
    val amount: Int,
    val balance_before: Int
)
