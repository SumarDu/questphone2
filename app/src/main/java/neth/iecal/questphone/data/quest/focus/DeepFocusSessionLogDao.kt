package neth.iecal.questphone.data.quest.focus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DeepFocusSessionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DeepFocusSessionLog): Long

    @Update
    suspend fun update(log: DeepFocusSessionLog)

    @Query("UPDATE deep_focus_session_logs SET difficulty = :difficulty, mood = :mood, synced = 0 WHERE questId = :questId AND difficulty IS NULL")
    suspend fun updatePendingLogsForQuest(questId: String, difficulty: Int, mood: Int)

    @Query("SELECT * FROM deep_focus_session_logs WHERE synced = 0")
    suspend fun getUnsyncedLogs(): List<DeepFocusSessionLog>

    @Query("UPDATE deep_focus_session_logs SET synced = 1 WHERE id IN (:logIds)")
    suspend fun markAsSynced(logIds: List<Long>)
}
