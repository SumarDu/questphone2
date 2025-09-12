package neth.iecal.questphone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: QuestEvent)

    @Update
    suspend fun updateEvent(event: QuestEvent)

    @Query("SELECT * FROM quest_events WHERE id = :id")
    suspend fun getEventById(id: String): QuestEvent?

    @Query("SELECT * FROM quest_events ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestEvent(): QuestEvent?

    @Query("SELECT * FROM quest_events ORDER BY startTime DESC")
        fun getAllEvents(): Flow<List<QuestEvent>>

    @Query("SELECT * FROM quest_events ORDER BY startTime ASC")
    suspend fun getAllEventsList(): List<QuestEvent>

    @Query("SELECT * FROM quest_events WHERE eventName = :eventName AND isRewardPending = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun findPendingRewardEvent(eventName: String): QuestEvent?
    
    @Query("SELECT * FROM quest_events WHERE eventName = :eventName ORDER BY startTime DESC")
    suspend fun getEventsByName(eventName: String): List<QuestEvent>
    
    @Query("SELECT * FROM quest_events WHERE synced = 0 ORDER BY startTime ASC")
    suspend fun getUnsyncedEvents(): List<QuestEvent>
    
    @Query("UPDATE quest_events SET synced = 1 WHERE id IN (:eventIds)")
    suspend fun markAsSynced(eventIds: List<String>)

    // Count of completed quest events (rewardCoins not null) within a time range
    @Query("SELECT COUNT(*) FROM quest_events WHERE rewardCoins IS NOT NULL AND endTime BETWEEN :start AND :end")
    suspend fun countCompletedEventsBetween(start: Long, end: Long): Int
}
