package neth.iecal.questphone.data.quest

data class CalendarQuest(
    val title: String,
    val description: String,
    val rewardMin: Int,
    val rewardMax: Int,
    val duration: Int,
    val breakMinutes: Int,
    val aiPhotoProofPrompt: String?,
    val startTime: Long,
    val endTime: Long,
    val eventId: Long,
    val rrule: String? = null,
    val until: Long? = null,
    val calendarId: Long
)
