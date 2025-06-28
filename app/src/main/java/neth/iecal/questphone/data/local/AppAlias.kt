package neth.iecal.questphone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_aliases")
data class AppAlias(
    @PrimaryKey val packageName: String,
    val alias: String
)
