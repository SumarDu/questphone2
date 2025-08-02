package neth.iecal.questphone.utils

import android.content.Context
import android.util.Log
import neth.iecal.questphone.data.settings.SettingsRepository

object CalendarSyncInitializer {
    private const val TAG = "CalendarSyncInitializer"

    /**
     * Initialize calendar sync scheduling based on current settings
     * Should be called when the app starts
     */
    fun initialize(context: Context) {
        try {
            val settingsRepository = SettingsRepository(context)
            val currentSettings = settingsRepository.settings.value
            
            // Schedule sync if auto-sync time (in minutes) is set
            currentSettings.autoSyncTimeMinutes?.let { minutes ->
                CalendarSyncScheduler.scheduleSync(context, minutes)
                val hour = minutes / 60
                val min = minutes % 60
                Log.d(TAG, "Calendar sync initialized for time %02d:%02d".format(hour, min))
            } ?: run {
                Log.d(TAG, "Calendar sync not scheduled - auto-sync disabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize calendar sync", e)
        }
    }
}
