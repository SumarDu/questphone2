package launcher.launcher.data.quest.health

import kotlinx.serialization.Serializable


@Serializable
data class HealthQuest(
    val type: HealthTaskType = HealthTaskType.STEPS,
    val healthGoalConfig: HealthGoalConfig = HealthGoalConfig(),
    val nextGoal: Int = healthGoalConfig.initial
)