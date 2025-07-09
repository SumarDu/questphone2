package neth.iecal.questphone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WardenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: WardenEvent): Long

    @Query("SELECT * FROM warden_events ORDER BY startTime DESC")
    fun getAllEvents(): Flow<List<WardenEvent>>

    @Query("SELECT * FROM warden_events WHERE id = :id")
    suspend fun getEventById(id: Int): WardenEvent?

    @Query("UPDATE warden_events SET \"endTime\" = :endTime WHERE id = :id")
    suspend fun updateEventEndTime(id: Int, endTime: Long)
}
