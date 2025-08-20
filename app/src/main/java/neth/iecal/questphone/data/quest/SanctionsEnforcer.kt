package neth.iecal.questphone.data.quest

import java.util.Calendar
import java.util.concurrent.TimeUnit
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.useCoins
import neth.iecal.questphone.data.local.PenaltyLog

object SanctionsEnforcer {
    /**
     * Enforce unlocker bans for quests whose deadline was missed today.
     * - If quest has deadline_minutes >= 0
     * - And current time > today's deadline
     * - And quest.last_completed_at < start of today
     * Then ban selected unlockers for sanction_ban_days days.
     */
    suspend fun enforceSanctions(
        questDao: QuestDao,
        blockedDao: BlockedUnlockerDao,
        nowMillis: Long,
        penaltyLogger: suspend (PenaltyLog) -> Unit = {},
        apiInvoker: (suspend (String) -> Unit)? = null
    ) {
        val all = questDao.getAllQuestsSuspend()
        val startOfToday = startOfTodayMillis(nowMillis)
        all.forEach { q ->
            if (q.deadline_minutes >= 0 && q.sanction_ban_days > 0 && q.sanction_ban_unlocker_ids.isNotBlank()) {
                val deadline = todayDeadlineMillis(q.deadline_minutes, nowMillis)
                val completedToday = q.last_completed_at >= startOfToday
                val missed = nowMillis > deadline && !completedToday
                if (missed) {
                    val ids = q.sanction_ban_unlocker_ids.split(',')
                        .mapNotNull { it.toIntOrNull() }
                        .toSet()
                    val extendBy = TimeUnit.DAYS.toMillis(q.sanction_ban_days.toLong())
                    for (id in ids) {
                        val existing = blockedDao.getByUnlockerId(id)
                        val targetUntil = nowMillis + extendBy
                        // Keep existing active ban end time fixed; only set a new end time if there is no active ban or it expired
                        val newUntil = existing?.let { exist ->
                            if (exist.blocked_until > nowMillis) exist.blocked_until else targetUntil
                        } ?: targetUntil
                        val existingSources = existing?.sources?.split('|')?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
                        existingSources.add(q.title)
                        val mergedSources = existingSources.joinToString("|")
                        blockedDao.upsert(
                            BlockedUnlocker(
                                id = existing?.id ?: 0,
                                unlocker_id = id,
                                blocked_until = newUntil,
                                sources = mergedSources
                            )
                        )
                    }
                }
            }

            // Coin liquidation: once per missed-deadline day per quest
            run {
                if (q.deadline_minutes >= 0 && q.sanction_liquidation_percent > 0) {
                    val deadline = todayDeadlineMillis(q.deadline_minutes, nowMillis)
                    val completedToday = q.last_completed_at >= startOfToday
                    val missed = nowMillis > deadline && !completedToday
                    val alreadyDoneToday = q.last_liquidated_at >= startOfToday
                    if (missed && !alreadyDoneToday) {
                        val percent = q.sanction_liquidation_percent.coerceIn(0, 100)
                        if (percent > 0) {
                            val currentCoins = neth.iecal.questphone.data.game.User.userInfo.coins
                            val toDeduct = (currentCoins * percent) / 100
                            if (toDeduct > 0) {
                                neth.iecal.questphone.data.game.User.useCoins(toDeduct)
                                // Log sanction coin liquidation with quest context
                                val log = PenaltyLog(
                                    id = java.util.UUID.randomUUID().toString(),
                                    occurredAt = nowMillis,
                                    amount = toDeduct,
                                    balanceBefore = currentCoins,
                                    source = "quest_sanction",
                                    questId = q.id,
                                    questTitle = q.title,
                                    synced = false
                                )
                                penaltyLogger(log)
                            }
                        }
                        // Persist guard timestamp
                        val updated = q.copy(last_liquidated_at = nowMillis)
                        questDao.upsertQuest(updated)
                        // also update reference for subsequent checks in this loop
                        q.last_liquidated_at = nowMillis
                    }
                }
            }

            // Phone block API: once per missed-deadline day per quest
            run {
                if (q.deadline_minutes >= 0 && q.sanction_phone_block && q.sanction_phone_api.isNotBlank()) {
                    val deadline = todayDeadlineMillis(q.deadline_minutes, nowMillis)
                    val completedToday = q.last_completed_at >= startOfToday
                    val missed = nowMillis > deadline && !completedToday
                    val alreadyInvokedToday = q.last_phone_block_invoked_at >= startOfToday
                    if (missed && !alreadyInvokedToday) {
                        try {
                            apiInvoker?.let { invoker ->
                                invoker(q.sanction_phone_api)
                            }
                        } finally {
                            val updated = q.copy(last_phone_block_invoked_at = nowMillis)
                            questDao.upsertQuest(updated)
                            q.last_phone_block_invoked_at = nowMillis
                        }
                    }
                }
            }
        }
    }

    private fun startOfTodayMillis(now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun todayDeadlineMillis(deadlineMinutes: Int, now: Long): Long {
        val hours = deadlineMinutes / 60
        val minutes = deadlineMinutes % 60
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, hours)
        cal.set(Calendar.MINUTE, minutes)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
