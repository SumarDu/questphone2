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

    suspend fun saveSwipeUpApp(packageName: String) = saveAppFor(GestureKeys.SWIPE_UP, packageName)
    suspend fun saveSwipeDownApp(packageName: String) = saveAppFor(GestureKeys.SWIPE_DOWN, packageName)
    suspend fun saveSwipeLeftApp(packageName: String) = saveAppFor(GestureKeys.SWIPE_LEFT, packageName)
    suspend fun saveSwipeRightApp(packageName: String) = saveAppFor(GestureKeys.SWIPE_RIGHT, packageName)
}
