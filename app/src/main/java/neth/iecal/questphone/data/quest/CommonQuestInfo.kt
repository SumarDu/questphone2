package neth.iecal.questphone.data.quest

import neth.iecal.questphone.data.game.AppUnlockerItem
import neth.iecal.questphone.data.game.AppUnlockerItemDao

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Dao
import androidx.room.Database
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
import kotlinx.serialization.Transient
import neth.iecal.questphone.data.DayOfWeek
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.json
import java.util.UUID

/**
 * Stores information about quests which are common to all integration types
 *
 * @property title this should be unique as it also acts as a primary key
 * @property reward the coins rewarded for that quest
 * @property integration_id id
 * @property selected_days the days on which it can be performed
 * @property auto_destruct format yyyy-mm-dd
 * @property time_range format startHour,endHour, says when between what time range the quest is to be completed
 * @property created_on
 * @property quest_json stores additional integration specific information here
 */
@Entity
@Serializable
@TypeConverters(BaseQuestConverter::class)
data class CommonQuestInfo(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    val reward_min: Int = 5,
    val reward_max: Int = 5,
    var integration_id : IntegrationId = IntegrationId.DEEP_FOCUS,
    var selected_days: Set<DayOfWeek> = emptySet(),
    var auto_destruct: String = "9999-12-31",
    var time_range: List<Int> = listOf(0,24),
    var created_on : String = getCurrentDate(),
    var last_completed_on: String = "0001-01-01",
    var instructions: String = "",
    var quest_json: String = "",
    var is_destroyed : Boolean = false,
    @Transient
    var synced: Boolean = false,
    var last_updated: Long = System.currentTimeMillis(),  // Epoch millis
)


@Stable
class QuestInfoState(
    initialTitle: String = "",
    initialInstructions: String = "",
    initialRewardMin: Int = 5,
    initialRewardMax: Int = 5,
    initialIntegrationId: IntegrationId = IntegrationId.DEEP_FOCUS,
    initialSelectedDays: Set<DayOfWeek> = emptySet(),
    initialAutoDestruct: String = "9999-12-31",
    initialTimeRange: List<Int> = listOf(0,24),
) {
    var id = UUID.randomUUID().toString()
    var title by mutableStateOf(initialTitle)
    var rewardMin by mutableIntStateOf(initialRewardMin)
    var rewardMax by mutableIntStateOf(initialRewardMax)
    var integrationId by mutableStateOf(initialIntegrationId)
    var selectedDays by mutableStateOf(initialSelectedDays)
    var instructions by mutableStateOf(initialInstructions)
    var initialAutoDestruct by mutableStateOf(initialAutoDestruct)
    var initialTimeRange by mutableStateOf(initialTimeRange)
    inline fun < reified T : Any> toBaseQuest(questInfo: T? = null) = CommonQuestInfo(
        id = id,
        title = title,
        reward_min = rewardMin,
        reward_max = rewardMax,
        integration_id = integrationId,
        selected_days = selectedDays,
        auto_destruct = initialAutoDestruct,
        time_range = initialTimeRange,
        instructions = instructions,
        quest_json = if(questInfo!=null) json.encodeToString(questInfo) else ""
    )
    fun fromBaseQuest(commonQuestInfo: CommonQuestInfo){
        id = commonQuestInfo.id
        title = commonQuestInfo.title
        rewardMin = commonQuestInfo.reward_min
        rewardMax = commonQuestInfo.reward_max
        integrationId = commonQuestInfo.integration_id
        selectedDays = commonQuestInfo.selected_days
        initialAutoDestruct = commonQuestInfo.auto_destruct
        instructions = commonQuestInfo.instructions
        initialTimeRange = commonQuestInfo.time_range
    }
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


    @Query("SELECT * FROM CommonQuestInfo WHERE id = :id")
    suspend fun getQuestById(id: String): CommonQuestInfo?

    @Query("SELECT * FROM CommonQuestInfo")
    fun getAllQuests(): Flow<List<CommonQuestInfo>>

    @Query("SELECT * FROM CommonQuestInfo WHERE synced = 0")
    fun getUnSyncedQuests(): Flow<List<CommonQuestInfo>>

    @Delete
    suspend fun deleteQuest(quest: CommonQuestInfo)

    @Query("DELETE FROM CommonQuestInfo WHERE title = :title")
    suspend fun deleteQuestByTitle(title: String)

    @Query("UPDATE CommonQuestInfo SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM CommonQuestInfo")
    suspend fun clearAll()


    @Query("SELECT COUNT(*) FROM CommonQuestInfo")
    suspend fun getRowCount(): Int

}



@Database(entities = [CommonQuestInfo::class, AppUnlockerItem::class], version = 3, exportSchema = false)
@TypeConverters(BaseQuestConverter::class)
abstract class QuestDatabase : RoomDatabase() {
    abstract fun appUnlockerItemDao(): AppUnlockerItemDao
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
            ).fallbackToDestructiveMigration().build()
            INSTANCE = instance
            instance
        }
    }
}

