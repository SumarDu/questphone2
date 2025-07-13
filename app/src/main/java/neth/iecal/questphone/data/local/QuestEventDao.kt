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

    @Query("SELECT * FROM quest_events ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestEvent(): QuestEvent?

    @Query("SELECT * FROM quest_events ORDER BY startTime DESC")
    fun getAllEvents(): Flow<List<QuestEvent>>
}
