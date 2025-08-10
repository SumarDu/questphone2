package neth.iecal.questphone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "penalty_logs")
data class PenaltyLog(
    @PrimaryKey val id: String,
    val occurredAt: Long,           // epoch millis when deduction happened
    val amount: Int,                // how much was deducted
    val balanceBefore: Int,         // balance before deduction
    val synced: Boolean = false     // whether synced to Supabase
)
