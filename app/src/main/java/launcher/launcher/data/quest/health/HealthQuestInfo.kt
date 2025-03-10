package launcher.launcher.data.quest.health

import kotlinx.serialization.Serializable


@Serializable
data class HealthGoal(
    val type: HealthTaskType = HealthTaskType.STEPS,
    val healthGoalConfig: HealthGoalConfig = HealthGoalConfig(),
    val nextGoal: Int = healthGoalConfig.initial
)