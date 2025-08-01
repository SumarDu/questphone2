package neth.iecal.questphone.services

import android.content.Context
import android.provider.CalendarContract
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.data.quest.CalendarQuest
import neth.iecal.questphone.data.quest.CalendarInfo
import java.util.Calendar

class CalendarSyncService(private val context: Context, private val settingsRepository: SettingsRepository) {

    fun getCalendarEvents(): List<CalendarQuest> {
        val quests = mutableListOf<CalendarQuest>()
        val contentResolver = context.contentResolver

        // Get selected calendars from settings
        val selectedCalendars = settingsRepository.settings.value.selectedCalendars

        // Get list of available calendars
        val availableCalendars = getAvailableCalendars()
        
        // If no calendars are selected, sync from all calendars
        val calendarsToSync = if (selectedCalendars.isEmpty()) {
            availableCalendars.map { it.id }
        } else {
            // Filter to only selected calendars that actually exist
            selectedCalendars.intersect(availableCalendars.map { it.id }.toSet()).toList()
        }

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.RRULE,
            CalendarContract.Instances.CALENDAR_ID
        )

        val calendar = Calendar.getInstance()
        val startTime = calendar.timeInMillis // Start from now
        calendar.add(Calendar.DAY_OF_YEAR, 30)
        val endTime = calendar.timeInMillis

        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${calendarsToSync.joinToString(",") { "?" }})"
        val selectionArgs = calendarsToSync.toTypedArray()

        val cursor = contentResolver.query(
            CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(startTime.toString())
                .appendPath(endTime.toString())
                .build(),
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleCol = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val descCol = it.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
            val startCol = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endCol = it.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val rruleCol = it.getColumnIndexOrThrow(CalendarContract.Instances.RRULE)
            val calendarIdCol = it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)

            val seenEventIds = mutableSetOf<Long>()

            while (it.moveToNext()) {
                val eventId = it.getLong(idCol)

                // Skip if we've already processed this event (deduplication for recurring events)
                if (eventId in seenEventIds) {
                    continue
                }
                seenEventIds.add(eventId)

                val title = it.getString(titleCol)
                val description = it.getString(descCol) ?: ""
                val start = it.getLong(startCol)
                val end = it.getLong(endCol)
                val rrule = it.getString(rruleCol)
                val calendarId = it.getLong(calendarIdCol)

                val (rewardMin, rewardMax, duration, breakMinutes, aiPrompt, cleanedInstructions) = parseDescription(description)
                val until = parseUntilFromRrule(rrule)

                quests.add(
                    CalendarQuest(
                        title = title,
                        description = cleanedInstructions,
                        rewardMin = rewardMin,
                        rewardMax = rewardMax,
                        duration = duration,
                        breakMinutes = breakMinutes,
                        aiPhotoProofPrompt = aiPrompt,
                        startTime = start,
                        endTime = end,
                        eventId = eventId,
                        rrule = rrule,
                        until = until,
                        calendarId = calendarId
                    )
                )
            }
        }
        return quests
    }

    private fun parseDescription(description: String): ParsedQuestParamsWithInstructions {
        val lines = description.split("\n")
        val parameterLines = lines.filter { it.trimStart().startsWith("/") }
        val instructionLines = lines.filter { !it.trimStart().startsWith("/") }
        
        val parametersText = parameterLines.joinToString(" ") { it.trimStart().removePrefix("/") }
        val cleanedInstructions = instructionLines.joinToString("\n").trim()
        
        val rewardRegex = "C(\\d+)(?:-(\\d+))?".toRegex()
        val durationRegex = "D(\\d+)".toRegex()
        val breakRegex = "B(\\d+)".toRegex()
        val aiPromptRegex = "A\\[(.*?)\\]".toRegex()

        val rewardMatch = rewardRegex.find(parametersText)
        val rewardMin = rewardMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val rewardMax = rewardMatch?.groupValues?.get(2)?.toIntOrNull() ?: rewardMin

        val duration = durationRegex.find(parametersText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val breakMinutes = breakRegex.find(parametersText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val aiPrompt = aiPromptRegex.find(parametersText)?.groupValues?.get(1)

        return ParsedQuestParamsWithInstructions(rewardMin, rewardMax, duration, breakMinutes, aiPrompt, cleanedInstructions)
    }

    private fun parseUntilFromRrule(rrule: String?): Long? {
        if (rrule.isNullOrEmpty()) return null
        
        val untilRegex = "UNTIL=([0-9T]+)".toRegex()
        val untilMatch = untilRegex.find(rrule)
        
        return untilMatch?.groupValues?.get(1)?.let { untilString ->
            try {
                // Parse UNTIL date format (YYYYMMDDTHHMMSSZ)
                val year = untilString.substring(0, 4).toInt()
                val month = untilString.substring(4, 6).toInt() - 1 // Calendar.MONTH is 0-based
                val day = untilString.substring(6, 8).toInt()
                
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day, 23, 59, 59)
                calendar.timeInMillis
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getAvailableCalendars(): List<CalendarInfo> {
        val calendars = mutableListOf<CalendarInfo>()
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME
        )

        val cursor = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idCol).toString()
                val name = it.getString(nameCol)
                val accountName = it.getString(accountCol)

                calendars.add(CalendarInfo(id, name, accountName))
            }
        }

        return calendars
    }

    private data class ParsedQuestParams(
        val rewardMin: Int,
        val rewardMax: Int,
        val duration: Int,
        val breakMinutes: Int,
        val aiPrompt: String?
    )

    private data class ParsedQuestParamsWithInstructions(
        val rewardMin: Int,
        val rewardMax: Int,
        val duration: Int,
        val breakMinutes: Int,
        val aiPrompt: String?,
        val cleanedInstructions: String
    )
}
