package ca.creativepixels.schoolstuff.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import ca.creativepixels.schoolstuff.data.DeviceCalendar
import ca.creativepixels.schoolstuff.data.ExternalCalendarEvent
import ca.creativepixels.schoolstuff.data.Repeat
import ca.creativepixels.schoolstuff.data.SchoolItem
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarProviderRepository(private val context: Context) {
    private val resolver = context.contentResolver

    fun hasReadPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED

    fun hasWritePermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.WRITE_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED

    fun queryCalendars(): List<DeviceCalendar> {
        if (!hasReadPermission()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val result = mutableListOf<DeviceCalendar>()
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE}=1",
            null,
            "${CalendarContract.Calendars.ACCOUNT_NAME}, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME}"
        )?.use { cursor ->
            val idI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val typeI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            val accessI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            while (cursor.moveToNext()) {
                val access = cursor.getInt(accessI)
                result += DeviceCalendar(
                    id = cursor.getLong(idI),
                    displayName = cursor.getString(nameI) ?: "Calendar",
                    accountName = cursor.getString(accountI) ?: "",
                    accountType = cursor.getString(typeI) ?: "",
                    canWrite = access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
                )
            }
        }
        return result
    }

    fun importInstances(calendarIds: Set<Long>, daysAhead: Long = 90): List<ExternalCalendarEvent> {
        if (!hasReadPermission() || calendarIds.isEmpty()) return emptyList()
        val start = System.currentTimeMillis()
        val end = start + daysAhead * 24L * 60L * 60L * 1000L
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)
        val placeholders = calendarIds.joinToString(",") { "?" }
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )
        val items = mutableListOf<ExternalCalendarEvent>()
        resolver.query(
            builder.build(),
            projection,
            "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)",
            calendarIds.map { it.toString() }.toTypedArray(),
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val eventI = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val calendarI = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val titleI = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginI = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endI = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayI = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            while (cursor.moveToNext()) {
                val title = cursor.getString(titleI)?.trim().orEmpty()
                if (title.isBlank()) continue
                items += ExternalCalendarEvent(
                    eventId = cursor.getLong(eventI),
                    calendarId = cursor.getLong(calendarI),
                    title = title,
                    beginMillis = cursor.getLong(beginI),
                    endMillis = cursor.getLong(endI),
                    allDay = cursor.getInt(allDayI) == 1
                )
            }
        }
        return items
    }

    fun upsertSchoolItem(calendarId: Long, item: SchoolItem): Boolean {
        if (!hasWritePermission()) return false
        val marker = "[School Stuff ID: ${item.id}]"
        val existingId = resolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.CALENDAR_ID}=? AND ${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf(calendarId.toString(), "%$marker%"),
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

        val date = runCatching { LocalDate.parse(item.dateIso) }.getOrElse { LocalDate.now() }
        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val details = buildString {
            if (item.notes.isNotBlank()) append(item.notes.trim()).append("\n\n")
            append(marker)
        }
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, item.title)
            put(CalendarContract.Events.DESCRIPTION, details)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.ALL_DAY, 1)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneOffset.UTC.id)
            when (item.repeat) {
                Repeat.WEEKLY -> {
                    put(CalendarContract.Events.DURATION, "P1D")
                    put(CalendarContract.Events.RRULE, "FREQ=WEEKLY")
                }
                Repeat.MONTHLY -> {
                    put(CalendarContract.Events.DURATION, "P1D")
                    put(CalendarContract.Events.RRULE, "FREQ=MONTHLY")
                }
                else -> put(CalendarContract.Events.DTEND, endMillis)
            }
        }
        return runCatching {
            if (existingId == null) {
                resolver.insert(CalendarContract.Events.CONTENT_URI, values) != null
            } else {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId)
                resolver.update(uri, values, null, null) > 0
            }
        }.getOrDefault(false)
    }
}
