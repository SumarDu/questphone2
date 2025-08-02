package neth.iecal.questphone.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.quest.CalendarQuest
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.SchedulingInfo
import neth.iecal.questphone.data.SchedulingType
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.services.CalendarSyncService
import neth.iecal.questphone.utils.CalendarPermissionHelper
import java.text.SimpleDateFormat
import neth.iecal.questphone.data.DayOfWeek
import java.util.Date
import java.util.Locale
import java.util.UUID

class CalendarSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "CalendarSyncWorker"
        const val WORK_NAME = "calendar_sync_work"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Starting automatic calendar sync")

            // Check if we have calendar permissions
            if (!CalendarPermissionHelper.hasCalendarPermission(applicationContext)) {
                Log.w(TAG, "Calendar sync skipped - no permissions")
                return@withContext Result.success()
            }

            val settingsRepository = SettingsRepository(applicationContext)
            val questDao = QuestDatabaseProvider.getInstance(applicationContext).questDao()
            
            // Perform the sync
            val calendarSyncService = CalendarSyncService(applicationContext, settingsRepository)
            val calendarQuests = calendarSyncService.getCalendarEvents()
            val dbQuests = questDao.getAllQuestsSuspend()
                .filter { it.calendar_event_id != null }
                .associateBy { it.calendar_event_id!! }

            val questsToUpsert = mutableListOf<CommonQuestInfo>()
            val questsToDelete = dbQuests.toMutableMap()

            for (calendarQuest in calendarQuests) {
                val existingQuest = dbQuests[calendarQuest.eventId]
                questsToDelete.remove(calendarQuest.eventId)

                val questFromCalendar = mapCalendarQuestToCommonQuestInfo(calendarQuest, existingQuest)

                if (existingQuest == null || hasQuestChanged(existingQuest, questFromCalendar)) {
                    questsToUpsert.add(questFromCalendar)
                }
            }

            if (questsToUpsert.isNotEmpty()) {
                questDao.upsertAll(questsToUpsert)
                Log.d(TAG, "Upserted ${questsToUpsert.size} quests from calendar")
            }

            if (questsToDelete.isNotEmpty()) {
                questsToDelete.values.forEach { questDao.deleteQuest(it) }
                Log.d(TAG, "Deleted ${questsToDelete.size} outdated calendar quests")
            }

            Log.d(TAG, "Calendar sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Calendar sync failed", e)
            Result.retry()
        }
    }

    private fun mapCalendarQuestToCommonQuestInfo(
        calendarQuest: CalendarQuest,
        existingQuest: CommonQuestInfo?
    ): CommonQuestInfo {
        val (schedulingInfo, selectedDays, autoDestruct) = parseCalendarEventScheduling(calendarQuest)
        
        return CommonQuestInfo(
            id = existingQuest?.id ?: UUID.randomUUID().toString(),
            title = calendarQuest.title,
            instructions = calendarQuest.description,
            reward_min = calendarQuest.rewardMin,
            reward_max = calendarQuest.rewardMax,
            quest_duration_minutes = calendarQuest.duration,
            break_duration_minutes = calendarQuest.breakMinutes,
            ai_photo_proof = calendarQuest.aiPhotoProofPrompt != null,
            ai_photo_proof_description = calendarQuest.aiPhotoProofPrompt ?: "",
            integration_id = IntegrationId.SWIFT_MARK,
            calendar_event_id = calendarQuest.eventId,
            scheduling_info = schedulingInfo,
            selected_days = selectedDays,
            auto_destruct = autoDestruct,
            last_updated = System.currentTimeMillis()
        )
    }

    private fun parseCalendarEventScheduling(calendarQuest: CalendarQuest): Triple<SchedulingInfo, Set<neth.iecal.questphone.data.DayOfWeek>, String> {
        val rrule = calendarQuest.rrule
        
        // Check for weekly recurring event
        if (!rrule.isNullOrEmpty() && rrule.contains("FREQ=WEEKLY")) {
            val dayOfWeek = getDayOfWeekFromTimestamp(calendarQuest.startTime)
            val selectedDays = setOf(dayOfWeek)

            val schedulingInfo = SchedulingInfo(
                type = SchedulingType.WEEKLY,
                selectedDays = selectedDays
            )

            val autoDestruct = calendarQuest.until?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
            } ?: "9999-12-31"

            return Triple(schedulingInfo, selectedDays, autoDestruct)

        // Check for monthly recurring event
        } else if (!rrule.isNullOrEmpty() && rrule.contains("FREQ=MONTHLY")) {
            val intervalRegex = "INTERVAL=([0-9]+)".toRegex()
            val intervalMatch = intervalRegex.find(rrule)
            val interval = intervalMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

            if (interval > 1) {
                // Treat as a one-time quest if interval is > 1 month
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val specificDate = dateFormat.format(Date(calendarQuest.startTime))
                val schedulingInfo = SchedulingInfo(
                    type = SchedulingType.SPECIFIC_DATE,
                    specificDate = specificDate
                )
                return Triple(schedulingInfo, emptySet(), specificDate)
            }

            val byDayRegex = "BYDAY=([0-9A-Z-]+)".toRegex()
            val byDayMatch = byDayRegex.find(rrule)

            if (byDayMatch != null) {
                val byDayValue = byDayMatch.groupValues[1]
                val weekInMonth = byDayValue.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 0
                val dayOfWeekStr = byDayValue.filter { it.isLetter() }
                val dayOfWeek = mapRruleDayToAppDay(dayOfWeekStr)

                val schedulingInfo = SchedulingInfo(
                    type = SchedulingType.MONTHLY_BY_DAY,
                    monthlyDayOfWeek = dayOfWeek,
                    monthlyWeekInMonth = weekInMonth
                )

                val autoDestruct = calendarQuest.until?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                } ?: "9999-12-31"

                return Triple(schedulingInfo, emptySet(), autoDestruct)
            } else {
                val dayOfMonth = getDayOfMonthFromTimestamp(calendarQuest.startTime)

                val schedulingInfo = SchedulingInfo(
                    type = SchedulingType.MONTHLY_DATE,
                    monthlyDate = dayOfMonth
                )

                val autoDestruct = calendarQuest.until?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                } ?: "9999-12-31"

                return Triple(schedulingInfo, emptySet(), autoDestruct)
            }
        // Handle single occurrence event
        } else {
            val schedulingInfo = SchedulingInfo(
                type = SchedulingType.SPECIFIC_DATE,
                specificDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(calendarQuest.startTime))
            )

            val autoDestruct = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(calendarQuest.endTime))

            return Triple(schedulingInfo, emptySet(), autoDestruct)
        }
    }

    private fun getDayOfWeekFromTimestamp(timestamp: Long): neth.iecal.questphone.data.DayOfWeek {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> neth.iecal.questphone.data.DayOfWeek.SUN
            java.util.Calendar.MONDAY -> neth.iecal.questphone.data.DayOfWeek.MON
            java.util.Calendar.TUESDAY -> neth.iecal.questphone.data.DayOfWeek.TUE
            java.util.Calendar.WEDNESDAY -> neth.iecal.questphone.data.DayOfWeek.WED
            java.util.Calendar.THURSDAY -> neth.iecal.questphone.data.DayOfWeek.THU
            java.util.Calendar.FRIDAY -> neth.iecal.questphone.data.DayOfWeek.FRI
            java.util.Calendar.SATURDAY -> neth.iecal.questphone.data.DayOfWeek.SAT
            else -> neth.iecal.questphone.data.DayOfWeek.MON
        }
    }

    private fun getDayOfMonthFromTimestamp(timestamp: Long): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(java.util.Calendar.DAY_OF_MONTH)
    }

    private fun mapRruleDayToAppDay(dayStr: String): neth.iecal.questphone.data.DayOfWeek {
        return when (dayStr) {
            "SU" -> neth.iecal.questphone.data.DayOfWeek.SUN
            "MO" -> neth.iecal.questphone.data.DayOfWeek.MON
            "TU" -> neth.iecal.questphone.data.DayOfWeek.TUE
            "WE" -> neth.iecal.questphone.data.DayOfWeek.WED
            "TH" -> neth.iecal.questphone.data.DayOfWeek.THU
            "FR" -> neth.iecal.questphone.data.DayOfWeek.FRI
            "SA" -> neth.iecal.questphone.data.DayOfWeek.SAT
            else -> neth.iecal.questphone.data.DayOfWeek.MON
        }
    }

    private fun hasQuestChanged(
        existing: CommonQuestInfo,
        new: CommonQuestInfo
    ): Boolean {
        return existing.title != new.title ||
                existing.instructions != new.instructions ||
                existing.reward_min != new.reward_min ||
                existing.reward_max != new.reward_max ||
                existing.quest_duration_minutes != new.quest_duration_minutes ||
                existing.break_duration_minutes != new.break_duration_minutes ||
                existing.ai_photo_proof != new.ai_photo_proof ||
                existing.ai_photo_proof_description != new.ai_photo_proof_description ||
                existing.auto_destruct != new.auto_destruct ||
                existing.scheduling_info != new.scheduling_info ||
                existing.selected_days != new.selected_days
    }
}
