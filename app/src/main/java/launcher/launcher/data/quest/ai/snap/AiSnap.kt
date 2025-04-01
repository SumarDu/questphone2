package launcher.launcher.data.quest.ai.snap

import kotlinx.serialization.Serializable

@Serializable
data class AiSnap(
    val taskDescription: String,
    var spatialImageUrl : String? = null
)