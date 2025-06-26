package neth.iecal.questphone.data.game

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import neth.iecal.questphone.R

@Entity(tableName = "app_unlocker_items")
data class AppUnlockerItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val appName: String,
    val packageName: String,
    val price: Int,
    val unlockDurationMinutes: Int
)

@Dao
interface AppUnlockerItemDao {
    @Query("SELECT * FROM app_unlocker_items")
    fun getAll(): Flow<List<AppUnlockerItem>>

    @Query("SELECT * FROM app_unlocker_items WHERE id = :id")
    suspend fun getById(id: Int): AppUnlockerItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AppUnlockerItem)

    @Query("DELETE FROM app_unlocker_items WHERE id = :id")
    suspend fun deleteById(id: Int)
}
