package ca.creativepixels.schoolstuff.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ca.creativepixels.schoolstuff.data.Reminder
import ca.creativepixels.schoolstuff.data.Repeat
import ca.creativepixels.schoolstuff.data.SchoolItem
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun schedule(context: Context, item: SchoolItem) {
        cancel(context, item.id)
        when (item.reminder) {
            Reminder.NIGHT_BEFORE -> scheduleOne(context, item, "night")
            Reminder.MORNING_OF -> scheduleOne(context, item, "morning")
            Reminder.BOTH -> {
                scheduleOne(context, item, "night")
                scheduleOne(context, item, "morning")
            }
        }
    }

    fun cancel(context: Context, itemId: String) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork("school-reminder-$itemId-night")
        wm.cancelUniqueWork("school-reminder-$itemId-morning")
    }

    private fun scheduleOne(context: Context, item: SchoolItem, kind: String) {
        val now = LocalDateTime.now()
        val eventDate = nextOccurrence(item, now.toLocalDate()) ?: return
        val reminderAt = if (kind == "night") {
            LocalDateTime.of(eventDate.minusDays(1), LocalTime.of(20, 0))
        } else {
            LocalDateTime.of(eventDate, LocalTime.of(7, 0))
        }
        val adjusted = if (reminderAt.isAfter(now)) reminderAt else {
            val next = nextOccurrence(item, eventDate.plusDays(1)) ?: return
            if (kind == "night") LocalDateTime.of(next.minusDays(1), LocalTime.of(20, 0))
            else LocalDateTime.of(next, LocalTime.of(7, 0))
        }
        val delay = Duration.between(now, adjusted).toMinutes().coerceAtLeast(1)
        val data = Data.Builder()
            .putString("itemId", item.id)
            .putString("kind", kind)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "school-reminder-${item.id}-$kind",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun nextOccurrence(item: SchoolItem, from: LocalDate): LocalDate? {
        val start = runCatching { LocalDate.parse(item.dateIso) }.getOrNull() ?: return null
        return when (item.repeat) {
            Repeat.WEEKLY -> {
                val candidate = from.with(TemporalAdjusters.nextOrSame(start.dayOfWeek))
                if (candidate.isBefore(start)) start else candidate
            }
            Repeat.MONTHLY -> {
                var year = from.year
                var month = from.monthValue
                repeat(14) {
                    val day = start.dayOfMonth.coerceAtMost(java.time.YearMonth.of(year, month).lengthOfMonth())
                    val candidate = LocalDate.of(year, month, day)
                    if (!candidate.isBefore(from) && !candidate.isBefore(start)) return candidate
                    val next = java.time.YearMonth.of(year, month).plusMonths(1)
                    year = next.year
                    month = next.monthValue
                }
                null
            }
            else -> start.takeIf { !it.isBefore(from) }
        }
    }
}
