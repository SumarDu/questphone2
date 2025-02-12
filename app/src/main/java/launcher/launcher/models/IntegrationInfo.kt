package launcher.launcher.models

import kotlinx.serialization.Serializable

@Serializable
data class IntegrationInfo(
    val icon: Int,
    val label: String,
    val description: String,
    val id: Int? = null
)