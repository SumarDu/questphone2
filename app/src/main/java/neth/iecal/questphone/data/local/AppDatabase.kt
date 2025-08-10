package neth.iecal.questphone.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import neth.iecal.questphone.data.local.AppAlias
import neth.iecal.questphone.data.local.AppAliasDao
import neth.iecal.questphone.data.local.PenaltyLog

@Database(entities = [AppAlias::class, QuestEvent::class, PenaltyLog::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
        abstract fun appAliasDao(): AppAliasDao
    abstract fun questEventDao(): QuestEventDao
    abstract fun penaltyLogDao(): PenaltyLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
