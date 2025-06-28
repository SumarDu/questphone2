package neth.iecal.questphone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppAliasDao {
    @Query("SELECT * FROM app_aliases")
    fun getAllAliases(): Flow<List<AppAlias>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: AppAlias)

    @Query("DELETE FROM app_aliases WHERE packageName = :packageName")
    suspend fun deleteAlias(packageName: String)
}
