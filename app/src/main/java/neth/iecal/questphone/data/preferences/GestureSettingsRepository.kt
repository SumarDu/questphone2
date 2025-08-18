package neth.iecal.questphone.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gesture_settings")

class GestureSettingsRepository(private val context: Context) {

    object GestureKeys {
        val SWIPE_UP = stringPreferencesKey("swipe_up_app")
        val SWIPE_DOWN = stringPreferencesKey("swipe_down_app")
        val SWIPE_LEFT = stringPreferencesKey("swipe_left_app")
        val SWIPE_RIGHT = stringPreferencesKey("swipe_right_app")
        // New gestures
        val TWO_FINGER_SWIPE_UP = stringPreferencesKey("two_finger_swipe_up_app")
        val TWO_FINGER_SWIPE_DOWN = stringPreferencesKey("two_finger_swipe_down_app")
        val DOUBLE_TAP_BOTTOM_LEFT = stringPreferencesKey("double_tap_bottom_left_app")
        val DOUBLE_TAP_BOTTOM_RIGHT = stringPreferencesKey("double_tap_bottom_right_app")
        val LONG_PRESS = stringPreferencesKey("long_press_app")
        val EDGE_LEFT_SWIPE_UP = stringPreferencesKey("edge_left_swipe_up_app")
        val EDGE_LEFT_SWIPE_DOWN = stringPreferencesKey("edge_left_swipe_down_app")
        val EDGE_RIGHT_SWIPE_UP = stringPreferencesKey("edge_right_swipe_up_app")
        val EDGE_RIGHT_SWIPE_DOWN = stringPreferencesKey("edge_right_swipe_down_app")
        // Bottom quick applet
        val DOUBLE_TAP_BOTTOM_RIGHT_MODE = stringPreferencesKey("double_tap_bottom_right_mode") // "single" or "applet"
        val BOTTOM_APPLET_APPS = stringPreferencesKey("bottom_applet_apps") // csv of package names (max 6)
    }

    private fun getAppFor(key: Preferences.Key<String>): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    private suspend fun saveAppFor(key: Preferences.Key<String>, packageName: String) {
        context.dataStore.edit {
            it[key] = packageName
        }
    }

    val swipeUpApp: Flow<String?> = getAppFor(GestureKeys.SWIPE_UP)
    val swipeDownApp: Flow<String?> = getAppFor(GestureKeys.SWIPE_DOWN)
    val swipeLeftApp: Flow<String?> = getAppFor(GestureKeys.SWIPE_LEFT)
    val swipeRightApp: Flow<String?> = getAppFor(GestureKeys.SWIPE_RIGHT)
    // New gesture flows
    val twoFingerSwipeUpApp: Flow<String?> = getAppFor(GestureKeys.TWO_FINGER_SWIPE_UP)
    val twoFingerSwipeDownApp: Flow<String?> = getAppFor(GestureKeys.TWO_FINGER_SWIPE_DOWN)
    val doubleTapBottomLeftApp: Flow<String?> = getAppFor(GestureKeys.DOUBLE_TAP_BOTTOM_LEFT)
    val doubleTapBottomRightApp: Flow<String?> = getAppFor(GestureKeys.DOUBLE_TAP_BOTTOM_RIGHT)
    val longPressApp: Flow<String?> = getAppFor(GestureKeys.LONG_PRESS)
    val edgeLeftSwipeUpApp: Flow<String?> = getAppFor(GestureKeys.EDGE_LEFT_SWIPE_UP)
    val edgeLeftSwipeDownApp: Flow<String?> = getAppFor(GestureKeys.EDGE_LEFT_SWIPE_DOWN)
    val edgeRightSwipeUpApp: Flow<String?> = getAppFor(GestureKeys.EDGE_RIGHT_SWIPE_UP)
    val edgeRightSwipeDownApp: Flow<String?> = getAppFor(GestureKeys.EDGE_RIGHT_SWIPE_DOWN)
    // Bottom quick applet flows
    val doubleTapBottomRightMode: Flow<String?> = context.dataStore.data.map { it[GestureKeys.DOUBLE_TAP_BOTTOM_RIGHT_MODE] }
    val bottomAppletApps: Flow<List<String>> = context.dataStore.data.map {
        it[GestureKeys.BOTTOM_APPLET_APPS]?.split(',')?.filter { s -> s.isNotBlank() } ?: emptyList()
    }

    suspend fun saveSwipeUpApp(packageName: String) = saveAppFor(GestureKeys.SWIPE_UP, packageName)
    suspend fun saveSwipeDownApp(packageName: String) = saveAppFor(GestureKeys.SWIPE_DOWN, packageName)
    suspend fun saveSwipeLeftApp(packageName: String) = saveAppFor(GestureKeys.SWIPE_LEFT, packageName)
    suspend fun saveSwipeRightApp(packageName: String) = saveAppFor(GestureKeys.SWIPE_RIGHT, packageName)
    // New gesture setters
    suspend fun saveTwoFingerSwipeUpApp(packageName: String) = saveAppFor(GestureKeys.TWO_FINGER_SWIPE_UP, packageName)
    suspend fun saveTwoFingerSwipeDownApp(packageName: String) = saveAppFor(GestureKeys.TWO_FINGER_SWIPE_DOWN, packageName)
    suspend fun saveDoubleTapBottomLeftApp(packageName: String) = saveAppFor(GestureKeys.DOUBLE_TAP_BOTTOM_LEFT, packageName)
    suspend fun saveDoubleTapBottomRightApp(packageName: String) = saveAppFor(GestureKeys.DOUBLE_TAP_BOTTOM_RIGHT, packageName)
    suspend fun saveLongPressApp(packageName: String) = saveAppFor(GestureKeys.LONG_PRESS, packageName)
    suspend fun saveEdgeLeftSwipeUpApp(packageName: String) = saveAppFor(GestureKeys.EDGE_LEFT_SWIPE_UP, packageName)
    suspend fun saveEdgeLeftSwipeDownApp(packageName: String) = saveAppFor(GestureKeys.EDGE_LEFT_SWIPE_DOWN, packageName)
    suspend fun saveEdgeRightSwipeUpApp(packageName: String) = saveAppFor(GestureKeys.EDGE_RIGHT_SWIPE_UP, packageName)
    suspend fun saveEdgeRightSwipeDownApp(packageName: String) = saveAppFor(GestureKeys.EDGE_RIGHT_SWIPE_DOWN, packageName)
    // Bottom quick applet setters
    suspend fun saveDoubleTapBottomRightMode(mode: String) {
        context.dataStore.edit { it[GestureKeys.DOUBLE_TAP_BOTTOM_RIGHT_MODE] = mode }
    }
    suspend fun saveBottomAppletApps(packages: List<String>) {
        val capped = packages.distinct().take(6)
        context.dataStore.edit { it[GestureKeys.BOTTOM_APPLET_APPS] = capped.joinToString(",") }
    }
}
