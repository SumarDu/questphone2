package neth.iecal.questphone.data.quest.health

import kotlinx.serialization.Serializable

@Serializable
data class HealthGoalConfig(
    val initial: Int = 1000,
    val final: Int = 10000,
    val increment: Int = 2000
)