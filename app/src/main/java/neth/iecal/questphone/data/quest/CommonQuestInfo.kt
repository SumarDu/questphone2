package neth.iecal.questphone.data.quest

import neth.iecal.questphone.data.game.AppUnlockerItem
import neth.iecal.questphone.data.game.AppUnlockerItemDao

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import neth.iecal.questphone.data.DayOfWeek
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.SchedulingInfo
import neth.iecal.questphone.data.quest.focus.DeepFocusSessionLog
import neth.iecal.questphone.data.quest.focus.DeepFocusSessionLogDao

import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.json
import java.util.UUID

enum class QuestPriority {
    IMPORTANT_URGENT,          // Red
    IMPORTANT_NOT_URGENT,      // Green
    NOT_IMPORTANT_URGENT,      // Beige
    STABLE,                    // Blue
    NOT_IMPORTANT_NOT_URGENT   // Light Gray
}

// Add phone block sanction fields to CommonQuestInfo
val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        fun hasColumn(name: String): Boolean {
            val cursor = database.query("PRAGMA table_info(CommonQuestInfo)")
            var found = false
            cursor.use {
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == name) { found = true; break }
                }
            }
            return found
        }
        if (!hasColumn("sanction_phone_block")) {
            database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN sanction_phone_block INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn("sanction_phone_api")) {
            database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN sanction_phone_api TEXT NOT NULL DEFAULT ''")
        }
        if (!hasColumn("last_phone_block_invoked_at")) {
            database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN last_phone_block_invoked_at INTEGER NOT NULL DEFAULT 0")
        }
    }
}

// Add sources column to blocked_unlockers for showing which quests imposed the ban
val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE blocked_unlockers ADD COLUMN sources TEXT NOT NULL DEFAULT ''")
    }
}

// Add sanctions columns and blocked_unlockers table
val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN sanction_ban_unlocker_ids TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN sanction_ban_days INTEGER NOT NULL DEFAULT 0")
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS blocked_unlockers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "unlocker_id INTEGER NOT NULL, " +
                "blocked_until INTEGER NOT NULL, " +
                "UNIQUE(unlocker_id) ON CONFLICT REPLACE)"
        )
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN deadline_minutes INTEGER NOT NULL DEFAULT -1")
    }
}

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
    var scheduling_info: SchedulingInfo = SchedulingInfo(),
    var auto_destruct: String = "9999-12-31",
    var time_range: List<Int> = listOf(0,24),
    var created_on : String = getCurrentDate(),
    var last_completed_on: String = "0001-01-01",
    var instructions: String = "",
    var quest_json: String = "",
    var is_destroyed : Boolean = false,
    var ai_photo_proof: Boolean = false,
    var ai_photo_proof_description: String = "",
    // QR validation gating
    var qr_proof: Boolean = false,
    var qr_secret_key: String = "",
    var quest_duration_minutes: Int = 0,
    var break_duration_minutes: Int = 0,
    var last_completed_at: Long = 0,
    var quest_started_at: Long = 0,
    // Minutes since midnight for daily deadline; -1 means no deadline
    var deadline_minutes: Int = -1,
    var priority: QuestPriority = QuestPriority.NOT_IMPORTANT_NOT_URGENT,
    // Sanctions persistence
    var sanction_ban_unlocker_ids: String = "", // CSV of AppUnlockerItem ids
    var sanction_ban_days: Int = 0,
    // Percentage (0..100) of coins to liquidate when deadline is missed
    @ColumnInfo(defaultValue = "0")
    var sanction_liquidation_percent: Int = 0,
    // Last time coins were liquidated for this quest (millis). Used to ensure once-per-day.
    @ColumnInfo(defaultValue = "0")
    var last_liquidated_at: Long = 0,
    // Whether to trigger phone block API after missed deadline
    @ColumnInfo(defaultValue = "0")
    var sanction_phone_block: Boolean = false,
    // API URI to call, e.g., digipaws://api/phone_lock?...  Empty means disabled
    @ColumnInfo(defaultValue = "")
    var sanction_phone_api: String = "",
    // Last time phone block API was invoked (millis). Guard to avoid repeated calls per day
    @ColumnInfo(defaultValue = "0")
    var last_phone_block_invoked_at: Long = 0,
    @Transient
    var synced: Boolean = false,
    var last_updated: Long = System.currentTimeMillis(),
    var calendar_event_id: Long? = null,


)


@Entity(tableName = "blocked_unlockers")
data class BlockedUnlocker(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val unlocker_id: Int,
    val blocked_until: Long,
    val sources: String = "" // quest titles joined by '|'
)

@Dao
interface BlockedUnlockerDao {
    @Query("SELECT * FROM blocked_unlockers WHERE blocked_until > :now")
    fun getActive(now: Long): Flow<List<BlockedUnlocker>>

    @Query("DELETE FROM blocked_unlockers WHERE blocked_until <= :now")
    suspend fun deleteExpired(now: Long)

    @Query("SELECT * FROM blocked_unlockers WHERE unlocker_id = :id LIMIT 1")
    suspend fun getByUnlockerId(id: Int): BlockedUnlocker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(block: BlockedUnlocker)
}


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
    initialAiPhotoProof: Boolean = false,
    initialAiPhotoProofDescription: String = "",
    initialQrProof: Boolean = false,
    initialQrSecretKey: String = "",
    initialQuestDurationMinutes: Int = 0,
    initialBreakDurationMinutes: Int = 0,
    initialLastCompletedAt: Long = 0,
    initialQuestStartedAt: Long = 0,
    initialSchedulingInfo: SchedulingInfo = SchedulingInfo(),
    initialPriority: QuestPriority = QuestPriority.NOT_IMPORTANT_NOT_URGENT
) {
    var id = UUID.randomUUID().toString()
    var title by mutableStateOf(initialTitle)
    var rewardMin by mutableIntStateOf(initialRewardMin)
    var rewardMax by mutableIntStateOf(initialRewardMax)
    var integrationId by mutableStateOf(initialIntegrationId)
    var selectedDays by mutableStateOf(initialSelectedDays)
    var schedulingInfo by mutableStateOf(initialSchedulingInfo)
    var instructions by mutableStateOf(initialInstructions)
    var initialAutoDestruct by mutableStateOf(initialAutoDestruct)
    var initialTimeRange by mutableStateOf(initialTimeRange)
    var aiPhotoProof by mutableStateOf(initialAiPhotoProof)
    var aiPhotoProofDescription by mutableStateOf(initialAiPhotoProofDescription)
    var qrProof by mutableStateOf(initialQrProof)
    var qrSecretKey by mutableStateOf(initialQrSecretKey)
    var questDurationMinutes by mutableIntStateOf(initialQuestDurationMinutes)
    var breakDurationMinutes by mutableIntStateOf(initialBreakDurationMinutes)
    var lastCompletedAt by mutableLongStateOf(initialLastCompletedAt)
    var questStartedAt by mutableLongStateOf(initialQuestStartedAt)
    var deadlineMinutes by mutableIntStateOf(-1)
    var priority by mutableStateOf(initialPriority)
    // Sanctions (UI-only for now)
    var sanctionSelectedUnlockerIds by mutableStateOf<Set<Int>>(emptySet())
    var sanctionBanDays by mutableIntStateOf(0)
    var sanctionLiquidationPercent by mutableIntStateOf(0)
    var sanctionPhoneBlock by mutableStateOf(false)
    var sanctionPhoneApi by mutableStateOf("")

    inline fun < reified T : Any> toBaseQuest(questInfo: T? = null) = CommonQuestInfo(
        id = id,
        title = title,
        reward_min = rewardMin,
        reward_max = rewardMax,
        integration_id = integrationId,
        selected_days = selectedDays,
        scheduling_info = schedulingInfo,
        auto_destruct = initialAutoDestruct,
        time_range = initialTimeRange,
        instructions = instructions,
        quest_json = if(questInfo!=null) json.encodeToString(questInfo) else "",
        ai_photo_proof = aiPhotoProof,
        ai_photo_proof_description = aiPhotoProofDescription,
        qr_proof = qrProof,
        qr_secret_key = qrSecretKey,
        quest_duration_minutes = questDurationMinutes,
        break_duration_minutes = breakDurationMinutes,
        last_completed_at = lastCompletedAt,
        quest_started_at = questStartedAt,
        deadline_minutes = deadlineMinutes,
        priority = priority,
        sanction_ban_unlocker_ids = sanctionSelectedUnlockerIds.joinToString(","),
        sanction_ban_days = sanctionBanDays,
        sanction_liquidation_percent = sanctionLiquidationPercent,
        sanction_phone_block = sanctionPhoneBlock,
        sanction_phone_api = sanctionPhoneApi
    )
    fun fromBaseQuest(commonQuestInfo: CommonQuestInfo){
        id = commonQuestInfo.id
        title = commonQuestInfo.title
        rewardMin = commonQuestInfo.reward_min
        rewardMax = commonQuestInfo.reward_max
        integrationId = commonQuestInfo.integration_id
        selectedDays = commonQuestInfo.selected_days
        schedulingInfo = commonQuestInfo.scheduling_info
        initialAutoDestruct = commonQuestInfo.auto_destruct
        instructions = commonQuestInfo.instructions
        initialTimeRange = commonQuestInfo.time_range
        aiPhotoProof = commonQuestInfo.ai_photo_proof
        aiPhotoProofDescription = commonQuestInfo.ai_photo_proof_description
        qrProof = commonQuestInfo.qr_proof
        qrSecretKey = commonQuestInfo.qr_secret_key
        questDurationMinutes = commonQuestInfo.quest_duration_minutes
        breakDurationMinutes = commonQuestInfo.break_duration_minutes
        lastCompletedAt = commonQuestInfo.last_completed_at
        questStartedAt = commonQuestInfo.quest_started_at
        deadlineMinutes = commonQuestInfo.deadline_minutes
        priority = commonQuestInfo.priority
        // Map sanctions to UI state
        sanctionSelectedUnlockerIds = commonQuestInfo.sanction_ban_unlocker_ids.split(',')
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }
            .toSet()
        sanctionBanDays = commonQuestInfo.sanction_ban_days
        sanctionLiquidationPercent = commonQuestInfo.sanction_liquidation_percent
        sanctionPhoneBlock = commonQuestInfo.sanction_phone_block
        sanctionPhoneApi = commonQuestInfo.sanction_phone_api
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
    fun toIntegrationId(name: String): IntegrationId {
        return try {
            IntegrationId.valueOf(name)
        } catch (e: IllegalArgumentException) {
            Log.w("QuestConverter", "Unknown integration_id '$name', defaulting to DEEP_FOCUS")
            IntegrationId.DEEP_FOCUS
        }
    }

    @TypeConverter
    fun fromPriority(p: QuestPriority): String = p.name

    @TypeConverter
    fun toPriority(name: String): QuestPriority = try {
        QuestPriority.valueOf(name)
    } catch (e: IllegalArgumentException) {
        QuestPriority.NOT_IMPORTANT_NOT_URGENT
    }

    @TypeConverter
    fun fromSchedulingInfo(schedulingInfo: SchedulingInfo): String = json.encodeToString(schedulingInfo)

    @TypeConverter
    fun toSchedulingInfo(jsonStr: String): SchedulingInfo {
        return try {
            json.decodeFromString<SchedulingInfo>(jsonStr)
        } catch (e: Exception) {
            Log.w("QuestConverter", "Failed to parse scheduling info, using default")
            SchedulingInfo()
        }
    }
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

    @Query("SELECT COUNT(*) FROM CommonQuestInfo WHERE auto_destruct = :date AND title LIKE :titleQuery")
    suspend fun getClonedQuestsCountForToday(date: String, titleQuery: String): Int

    @Query("SELECT * FROM CommonQuestInfo")
    fun getAllQuests(): Flow<List<CommonQuestInfo>>

    @Query("SELECT * FROM CommonQuestInfo WHERE auto_destruct = '9999-12-31'")
    fun getPermanentQuests(): Flow<List<CommonQuestInfo>>

    @Query("SELECT * FROM CommonQuestInfo WHERE auto_destruct != '9999-12-31'")
    fun getClonedQuests(): Flow<List<CommonQuestInfo>>

    @Query("SELECT * FROM CommonQuestInfo")
    suspend fun getAllQuestsSuspend(): List<CommonQuestInfo>

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




@Database(entities = [CommonQuestInfo::class, AppUnlockerItem::class, DeepFocusSessionLog::class, BlockedUnlocker::class], version = 28, exportSchema = false)
@TypeConverters(BaseQuestConverter::class)
abstract class QuestDatabase : RoomDatabase() {
    abstract fun appUnlockerItemDao(): AppUnlockerItemDao
    abstract fun questDao(): QuestDao
    abstract fun deepFocusSessionLogDao(): DeepFocusSessionLogDao
    abstract fun blockedUnlockerDao(): BlockedUnlockerDao

}


val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE deep_focus_session_logs ADD COLUMN concentrationDropReason TEXT")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN calendar_event_id INTEGER")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the scheduling_info column to CommonQuestInfo table
        database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN scheduling_info TEXT")
    }
}

// Add QR proof columns to CommonQuestInfo
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Boolean in Room backed by INTEGER with default 0
        database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN qr_proof INTEGER NOT NULL DEFAULT 0")
        // Secret key string for HMAC validation
        database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN qr_secret_key TEXT NOT NULL DEFAULT ''")
    }
}

// Add priority column to CommonQuestInfo
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN priority TEXT NOT NULL DEFAULT 'NOT_IMPORTANT_NOT_URGENT'")
    }
}

// Add sanction_liquidation_percent and last_liquidated_at to CommonQuestInfo
val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        fun hasColumn(name: String): Boolean {
            val cursor = database.query("PRAGMA table_info(CommonQuestInfo)")
            var found = false
            cursor.use {
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == name) { found = true; break }
                }
            }
            return found
        }
        if (!hasColumn("sanction_liquidation_percent")) {
            database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN sanction_liquidation_percent INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn("last_liquidated_at")) {
            database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN last_liquidated_at INTEGER NOT NULL DEFAULT 0")
        }
    }
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
            ).addMigrations(MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28).fallbackToDestructiveMigration().build()
            INSTANCE = instance
            instance
        }
    }
}

// No-op migration to bump identity after schema adjustments
val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Ensure last_liquidated_at exists for users who received only sanction_liquidation_percent in v26
        val cursor = database.query("PRAGMA table_info(CommonQuestInfo)")
        var hasLast = false
        cursor.use {
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == "last_liquidated_at") { hasLast = true; break }
            }
        }
        if (!hasLast) {
            database.execSQL("ALTER TABLE CommonQuestInfo ADD COLUMN last_liquidated_at INTEGER NOT NULL DEFAULT 0")
        }
    }
}
