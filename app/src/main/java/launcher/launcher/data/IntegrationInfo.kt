package launcher.launcher.data

import kotlinx.serialization.Serializable

@Serializable
data class IntegrationInfo(
    val icon: Int,
    val label: String,
    val description: String,
    val id: IntegrationId = null
)