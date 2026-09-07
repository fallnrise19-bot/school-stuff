package ca.creativepixels.schoolstuff.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("school_stuff_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadChildren(): List<ChildProfile> = readList("children")
    fun saveChildren(value: List<ChildProfile>) = writeList("children", value)

    fun loadItems(): List<SchoolItem> = readList("items")
    fun saveItems(value: List<SchoolItem>) = writeList("items", value)

    fun loadDocuments(): List<SchoolDocument> = readList("documents")
    fun saveDocuments(value: List<SchoolDocument>) = writeList("documents", value)

    fun loadTransportation(): List<TransportationInfo> =
        readList<TransportationInfo?>("transportation")
            .filterNotNull()
            .map { info ->
                TransportationInfo(
                    childId = info.childId.orEmpty(),
                    mode = info.mode.orEmpty().ifBlank { TransportationMode.BUS },
                    busNumber = info.busNumber.orEmpty(),
                    pickupPoint = info.pickupPoint.orEmpty(),
                    driverName = info.driverName.orEmpty(),
                    pickupInfo = info.pickupInfo.orEmpty()
                )
            }
            .filter { it.childId.isNotBlank() }

    fun saveTransportation(value: List<TransportationInfo>) = writeList("transportation", value)

    fun getParentNotes(): String = prefs.getString("parent_notes", "").orEmpty()
    fun setParentNotes(value: String) = prefs.edit().putString("parent_notes", value).apply()

    fun getNightReminderHour(): Int = prefs.getInt("night_reminder_hour", 20).coerceIn(0, 23)
    fun getNightReminderMinute(): Int = prefs.getInt("night_reminder_minute", 0).coerceIn(0, 59)
    fun setNightReminderTime(hour: Int, minute: Int) = prefs.edit()
        .putInt("night_reminder_hour", hour.coerceIn(0, 23))
        .putInt("night_reminder_minute", minute.coerceIn(0, 59))
        .apply()

    fun getMorningReminderHour(): Int = prefs.getInt("morning_reminder_hour", 7).coerceIn(0, 23)
    fun getMorningReminderMinute(): Int = prefs.getInt("morning_reminder_minute", 0).coerceIn(0, 59)
    fun setMorningReminderTime(hour: Int, minute: Int) = prefs.edit()
        .putInt("morning_reminder_hour", hour.coerceIn(0, 23))
        .putInt("morning_reminder_minute", minute.coerceIn(0, 59))
        .apply()

    fun getDefaultReminder(): String = prefs.getString("default_reminder", Reminder.NIGHT_BEFORE)
        .orEmpty()
        .takeIf { it in Reminder.all }
        ?: Reminder.NIGHT_BEFORE

    fun setDefaultReminder(value: String) {
        if (value in Reminder.all) prefs.edit().putString("default_reminder", value).apply()
    }

    fun hasSeeded(): Boolean = prefs.getBoolean("seeded", false)
    fun markSeeded() = prefs.edit().putBoolean("seeded", true).apply()

    fun getSelectedCalendarIds(): Set<Long> =
        prefs.getStringSet("calendar_ids", emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

    fun setSelectedCalendarIds(ids: Set<Long>) =
        prefs.edit().putStringSet("calendar_ids", ids.map { it.toString() }.toSet()).apply()

    fun getDefaultCalendarId(): Long? = prefs.getLong("default_calendar_id", -1L).takeIf { it >= 0L }
    fun setDefaultCalendarId(id: Long?) {
        val editor = prefs.edit()
        if (id == null) editor.remove("default_calendar_id") else editor.putLong("default_calendar_id", id)
        editor.apply()
    }

    private inline fun <reified T> readList(key: String): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<T>>() {}.type
            gson.fromJson<List<T>>(raw, type) ?: emptyList()
        }.getOrElse { emptyList() }
    }

    private fun <T> writeList(key: String, value: List<T>) {
        prefs.edit().putString(key, gson.toJson(value)).apply()
    }
}
