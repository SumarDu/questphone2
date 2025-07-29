package neth.iecal.questphone.services

import android.content.Context
import android.provider.CalendarContract
import neth.iecal.questphone.data.quest.CalendarQuest
import java.util.Calendar

class CalendarSyncService(private val context: Context) {

    fun getCalendarEvents(): List<CalendarQuest> {
        val quests = mutableListOf<CalendarQuest>()
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.RRULE
        )

        val calendar = Calendar.getInstance()
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 30)
        val endTime = calendar.timeInMillis

        val selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(startTime.toString(), endTime.toString())

        val cursor = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleCol = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val descCol = it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            val startCol = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endCol = it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)

            while (it.moveToNext()) {
                val eventId = it.getLong(idCol)
                val title = it.getString(titleCol)
                val description = it.getString(descCol) ?: ""
                val start = it.getLong(startCol)
                val end = it.getLong(endCol)

                val (rewardMin, rewardMax, duration, breakMinutes, aiPrompt) = parseDescription(description)

                quests.add(
                    CalendarQuest(
                        title = title,
                        description = description,
                        rewardMin = rewardMin,
                        rewardMax = rewardMax,
                        duration = duration,
                        breakMinutes = breakMinutes,
                        aiPhotoProofPrompt = aiPrompt,
                        startTime = start,
                        endTime = end,
                        eventId = eventId
                    )
                )
            }
        }
        return quests
    }

    private fun parseDescription(description: String): ParsedQuestParams {
        val rewardRegex = "C(\\d+)(?:-(\\d+))?".toRegex()
        val durationRegex = "D(\\d+)".toRegex()
        val breakRegex = "B(\\d+)".toRegex()
        val aiPromptRegex = "A\\[(.*?)\\]".toRegex()

        val rewardMatch = rewardRegex.find(description)
        val rewardMin = rewardMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val rewardMax = rewardMatch?.groupValues?.get(2)?.toIntOrNull() ?: rewardMin

        val duration = durationRegex.find(description)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val breakMinutes = breakRegex.find(description)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val aiPrompt = aiPromptRegex.find(description)?.groupValues?.get(1)

        return ParsedQuestParams(rewardMin, rewardMax, duration, breakMinutes, aiPrompt)
    }

    private data class ParsedQuestParams(
        val rewardMin: Int,
        val rewardMax: Int,
        val duration: Int,
        val breakMinutes: Int,
        val aiPrompt: String?
    )
}
