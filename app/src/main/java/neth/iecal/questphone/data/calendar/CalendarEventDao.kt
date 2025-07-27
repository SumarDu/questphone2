package neth.iecal.questphone.data.calendar

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {
    
    @Query("SELECT * FROM calendar_events WHERE isDeleted = 0 ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>
    
    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getEventById(id: String): CalendarEvent?
    
    @Query("SELECT * FROM calendar_events WHERE startTime >= :startTime AND startTime <= :endTime AND isDeleted = 0")
    suspend fun getEventsInRange(startTime: Long, endTime: Long): List<CalendarEvent>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEvent>)
    
    @Update
    suspend fun updateEvent(event: CalendarEvent)
    
    @Delete
    suspend fun deleteEvent(event: CalendarEvent)
    
    @Query("UPDATE calendar_events SET isDeleted = 1 WHERE id = :id")
    suspend fun markEventAsDeleted(id: String)
    
    @Query("DELETE FROM calendar_events WHERE isDeleted = 1")
    suspend fun cleanupDeletedEvents()
    
    @Query("SELECT COUNT(*) FROM calendar_events WHERE isDeleted = 0")
    suspend fun getEventCount(): Int
    
    @Query("SELECT * FROM calendar_events WHERE lastModified > :timestamp AND isDeleted = 0")
    suspend fun getEventsModifiedAfter(timestamp: Long): List<CalendarEvent>
}
