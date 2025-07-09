package neth.iecal.questphone.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WardenEvent::class], version = 2, exportSchema = false)
abstract class WardenDatabase : RoomDatabase() {

    abstract fun wardenDao(): WardenDao

    companion object {
        @Volatile
        private var INSTANCE: WardenDatabase? = null

        fun getDatabase(context: Context): WardenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WardenDatabase::class.java,
                    "warden_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
