package neth.iecal.questphone.data.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class CalendarManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CalendarManager"
        
        // Calendar projection for querying events
        private val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.LAST_DATE
        )
        
        private const val PROJECTION_ID_INDEX = 0
        private const val PROJECTION_TITLE_INDEX = 1
        private const val PROJECTION_DESCRIPTION_INDEX = 2
        private const val PROJECTION_DTSTART_INDEX = 3
        private const val PROJECTION_DTEND_INDEX = 4
        private const val PROJECTION_CALENDAR_ID_INDEX = 5
        private const val PROJECTION_ALL_DAY_INDEX = 6
        private const val PROJECTION_LAST_DATE_INDEX = 7
    }
    
    /**
     * Check if calendar permissions are granted
     */
    fun hasCalendarPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get calendar events for the next 30 days
     */
    suspend fun getCalendarEventsForNext30Days(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) {
            return@withContext emptyList()
        }
        
        val calendar = Calendar.getInstance()
        val startTime = calendar.timeInMillis
        
        // Add 30 days
        calendar.add(Calendar.DAY_OF_MONTH, 30)
        val endTime = calendar.timeInMillis
        
        return@withContext getCalendarEventsInRange(startTime, endTime)
    }
    
    /**
     * Get calendar events in a specific time range
     */
    suspend fun getCalendarEventsInRange(startTime: Long, endTime: Long): List<CalendarEvent> = withContext(Dispatchers.IO) {
        return@withContext getCalendarEventsInRange(startTime, endTime, emptySet())
    }
    
    /**
     * Get calendar events in a specific time range from selected calendars
     * @param selectedCalendarIds Set of calendar IDs to filter by. Empty set means all calendars.
     */
    suspend fun getCalendarEventsInRange(startTime: Long, endTime: Long, selectedCalendarIds: Set<String>): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) {
            return@withContext emptyList()
        }
        
        val events = mutableListOf<CalendarEvent>()
        val contentResolver: ContentResolver = context.contentResolver
        
        // Build selection query with calendar filtering
        val selectionBuilder = StringBuilder("(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?)")
        val selectionArgsList = mutableListOf(startTime.toString(), endTime.toString())
        
        // Add calendar ID filtering if specific calendars are selected
        if (selectedCalendarIds.isNotEmpty()) {
            val calendarPlaceholders = selectedCalendarIds.joinToString(",") { "?" }
            selectionBuilder.append(" AND ${CalendarContract.Events.CALENDAR_ID} IN ($calendarPlaceholders)")
            selectionArgsList.addAll(selectedCalendarIds)
        }
        
        val selection = selectionBuilder.toString()
        val selectionArgs = selectionArgsList.toTypedArray()
        
        val cursor: Cursor? = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            EVENT_PROJECTION,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )
        
        cursor?.use { c ->
            while (c.moveToNext()) {
                try {
                    val event = CalendarEvent(
                        id = c.getString(PROJECTION_ID_INDEX) ?: continue,
                        title = c.getString(PROJECTION_TITLE_INDEX) ?: "Untitled Event",
                        description = c.getString(PROJECTION_DESCRIPTION_INDEX) ?: "",
                        startTime = c.getLong(PROJECTION_DTSTART_INDEX),
                        endTime = c.getLong(PROJECTION_DTEND_INDEX),
                        calendarId = c.getString(PROJECTION_CALENDAR_ID_INDEX) ?: "",
                        isAllDay = c.getInt(PROJECTION_ALL_DAY_INDEX) == 1,
                        lastModified = c.getLong(PROJECTION_LAST_DATE_INDEX)
                    )
                    events.add(event)
                } catch (e: Exception) {
                    // Skip malformed events
                    continue
                }
            }
        }
        
        return@withContext events
    }
    
    /**
     * Get events that have been modified since a specific timestamp
     */
    suspend fun getModifiedEventsSince(timestamp: Long): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) {
            return@withContext emptyList()
        }
        
        val events = mutableListOf<CalendarEvent>()
        val contentResolver: ContentResolver = context.contentResolver
        
        val selection = "${CalendarContract.Events.LAST_DATE} > ?"
        val selectionArgs = arrayOf(timestamp.toString())
        
        val cursor: Cursor? = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            EVENT_PROJECTION,
            selection,
            selectionArgs,
            "${CalendarContract.Events.LAST_DATE} DESC"
        )
        
        cursor?.use { c ->
            while (c.moveToNext()) {
                try {
                    val event = CalendarEvent(
                        id = c.getString(PROJECTION_ID_INDEX) ?: continue,
                        title = c.getString(PROJECTION_TITLE_INDEX) ?: "Untitled Event",
                        description = c.getString(PROJECTION_DESCRIPTION_INDEX) ?: "",
                        startTime = c.getLong(PROJECTION_DTSTART_INDEX),
                        endTime = c.getLong(PROJECTION_DTEND_INDEX),
                        calendarId = c.getString(PROJECTION_CALENDAR_ID_INDEX) ?: "",
                        isAllDay = c.getInt(PROJECTION_ALL_DAY_INDEX) == 1,
                        lastModified = c.getLong(PROJECTION_LAST_DATE_INDEX)
                    )
                    events.add(event)
                } catch (e: Exception) {
                    continue
                }
            }
        }
        
        return@withContext events
    }
    
    /**
     * Get available calendars on the device
     */
    suspend fun getAvailableCalendars(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) {
            return@withContext emptyList()
        }
        
        val calendars = mutableListOf<DeviceCalendar>()
        val contentResolver: ContentResolver = context.contentResolver
        
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )
        
        val cursor: Cursor? = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        
        cursor?.use { c ->
            while (c.moveToNext()) {
                try {
                    val calendar = DeviceCalendar(
                        id = c.getString(0) ?: continue,
                        displayName = c.getString(1) ?: "Unknown Calendar",
                        accountName = c.getString(2) ?: "",
                        color = c.getInt(3)
                    )
                    calendars.add(calendar)
                } catch (e: Exception) {
                    continue
                }
            }
        }
        
        return@withContext calendars
    }
}

/**
 * Represents a calendar on the device
 */
data class DeviceCalendar(
    val id: String,
    val displayName: String,
    val accountName: String,
    val color: Int
)
