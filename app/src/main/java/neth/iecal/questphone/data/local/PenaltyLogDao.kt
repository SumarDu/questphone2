package neth.iecal.questphone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PenaltyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: PenaltyLog)

    @Query("SELECT * FROM penalty_logs WHERE synced = 0 ORDER BY occurredAt ASC")
    suspend fun getUnsynced(): List<PenaltyLog>

    @Query("UPDATE penalty_logs SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("SELECT * FROM penalty_logs ORDER BY occurredAt DESC")
    suspend fun getAll(): List<PenaltyLog>
}
