package neth.iecal.questphone.data.quest.stats

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Entity
@Serializable
@TypeConverters(StatsConverter::class)
data class StatsInfo(
    @PrimaryKey
    val id: String,
    val quest_id: String,
    val user_id: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val reward_amount: Int = 0, // Actual coins earned for this completion
    @Transient
    val isSynced: Boolean = false
)

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}

object StatsConverter {
    @TypeConverter
    fun fromDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toDate(dateString: String): LocalDate = LocalDate.parse(dateString)
}


@Dao
interface StatsInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(statsInfo: StatsInfo)
    @Update
    suspend fun updateStats(statsInfo: StatsInfo)

    @Query("SELECT * FROM StatsInfo WHERE quest_id = :id")
    fun getStatsByQuestId(id: String): Flow<List<StatsInfo>>

    @Query("SELECT * FROM StatsInfo WHERE date = :date LIMIT 1")
    suspend fun getStatsForUserOnDate( date: LocalDate): StatsInfo?

    @Query("SELECT * FROM StatsInfo")
    fun getAllStatsForUser(): Flow<List<StatsInfo>>

    @Query("DELETE FROM StatsInfo WHERE id = :id")
    suspend fun deleteStatsById(id: String)

    @Query("DELETE FROM StatsInfo WHERE user_id = :userId")
    suspend fun deleteAllStatsForUser(userId: String)

    @Query("SELECT * FROM StatsInfo WHERE isSynced = 0 ORDER BY date DESC")
    fun getAllUnSyncedStats() : Flow<List<StatsInfo>>


    @Query("UPDATE StatsInfo SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}



@Database(
    entities = [StatsInfo::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(StatsConverter::class)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsInfoDao
}


object StatsDatabaseProvider {
    @Volatile
    private var INSTANCE: StatsDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE StatsInfo ADD COLUMN reward_amount INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun getInstance(context: Context): StatsDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                StatsDatabase::class.java,
                "stats_info_database"
            ).fallbackToDestructiveMigration().build()
            INSTANCE = instance
            instance
        }
    }
}
