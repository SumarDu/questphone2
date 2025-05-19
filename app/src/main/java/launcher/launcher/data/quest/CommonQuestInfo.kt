package launcher.launcher.data.quest

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import launcher.launcher.data.DayOfWeek
import launcher.launcher.data.IntegrationId
import launcher.launcher.utils.getCurrentDate
import launcher.launcher.utils.json

/**
 * Stores information about quests which are common to all integration types
 *
 * @property title this should be unique as it also acts as a primary key
 * @property reward the coins rewarded for that quest
 * @property integrationId id
 * @property selectedDays the days on which it can be performed
 * @property autoDestruct format yyyy-mm-dd
 * @property timeRange format startHour,endHour, says when between what time range the quest is to be completed
 * @property createdOn
 * @property questJson stores additional integration specific information here
 */
@Entity
@Serializable
@TypeConverters(BaseQuestConverter::class)
data class CommonQuestInfo(
    @PrimaryKey
    var title: String = "",
    val reward: Int = 5,
    var integrationId : IntegrationId = IntegrationId.DEEP_FOCUS,
    var selectedDays: Set<DayOfWeek> = emptySet(),
    var autoDestruct: String = "9999-12-31",
    var timeRange: List<Int> = listOf(0,24),
    var createdOn : String = getCurrentDate(),
    var lastCompletedOn: String = "0001-01-01",
    var instructions: String = "",
    var questJson: String = "",
    var isDestroyed : Boolean = false
)


@Stable
class QuestInfoState(
    initialTitle: String = "",
    initialInstructions: String = "",
    initialReward: Int = 5,
    initialIntegrationId: IntegrationId = IntegrationId.DEEP_FOCUS,
    initialSelectedDays: Set<DayOfWeek> = emptySet(),
    initialAutoDestruct: String = "9999-12-31",
    initialTimeRange: List<Int> = listOf(0,24),
) {
    var title by mutableStateOf(initialTitle)
    var reward by mutableIntStateOf(initialReward)
    var integrationId by mutableStateOf(initialIntegrationId)
    var selectedDays by mutableStateOf(initialSelectedDays)
    var instructions by mutableStateOf(initialInstructions)
    var initialAutoDestruct by mutableStateOf(initialAutoDestruct)
    var initialTimeRange by mutableStateOf(initialTimeRange)
    inline fun < reified T : Any> toBaseQuest(questInfo: T? = null) = CommonQuestInfo(
        title = title,
        reward = reward,
        integrationId = integrationId,
        selectedDays = selectedDays,
        autoDestruct = initialAutoDestruct,
        timeRange = initialTimeRange,
        instructions = instructions,
        questJson = if(questInfo!=null) json.encodeToString(questInfo) else ""
    )
}


object BaseQuestConverter {

    @TypeConverter
    fun fromDayOfWeekSet(set: Set<DayOfWeek>): String = json.encodeToString(set.toList())

    @TypeConverter
    fun toDayOfWeekSet(jsonStr: String): Set<DayOfWeek> =
        json.decodeFromString<List<DayOfWeek>>(jsonStr).toSet()

    @TypeConverter
    fun fromTimeRange(range: List<Int>): String = json.encodeToString(range)

    @TypeConverter
    fun toTimeRange(jsonStr: String): List<Int> = json.decodeFromString(jsonStr)

    @TypeConverter
    fun fromIntegrationId(id: IntegrationId): String = id.name

    @TypeConverter
    fun toIntegrationId(name: String): IntegrationId = IntegrationId.valueOf(name)
}

@Dao
interface QuestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuest(quest: CommonQuestInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(quests: List<CommonQuestInfo>)

    @Query("SELECT * FROM CommonQuestInfo WHERE title = :title")
    suspend fun getQuest(title: String): CommonQuestInfo?

    @Query("SELECT * FROM CommonQuestInfo")
    fun getAllQuests(): Flow<List<CommonQuestInfo>>

    @Delete
    suspend fun deleteQuest(quest: CommonQuestInfo)

    @Query("DELETE FROM CommonQuestInfo WHERE title = :title")
    suspend fun deleteQuestByTitle(title: String)

    @Query("DELETE FROM CommonQuestInfo")
    suspend fun clearAll()


}

@androidx.room.Database(
    entities = [CommonQuestInfo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(BaseQuestConverter::class)
abstract class QuestDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao
}


object QuestDatabaseProvider {
    @Volatile
    private var INSTANCE: QuestDatabase? = null

    fun getInstance(context: Context): QuestDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                QuestDatabase::class.java,
                "quest_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
