package launcher.launcher.models

import kotlinx.serialization.Serializable

/**
 * Represents information about an app.
 *
 * @property name The name of the app.
 * @property packageNamIe The package name of the app.
 */
@Serializable
data class AppInfo(
    val name: String,
    val packageName: String
)