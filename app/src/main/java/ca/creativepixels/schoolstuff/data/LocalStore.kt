package ca.creativepixels.schoolstuff.data

import android.content.Context
import com.google.gson.Gson

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("school_stuff_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadChildren(): List<ChildProfile> = readAndRepair("children", StoredDataMigration::children, ::saveChildren)
    fun saveChildren(value: List<ChildProfile>) = writeList("children", value)

    fun loadItems(): List<SchoolItem> = readAndRepair("items", StoredDataMigration::items, ::saveItems)
    fun saveItems(value: List<SchoolItem>) = writeList("items", value)

    fun loadDocuments(): List<SchoolDocument> = readAndRepair("documents", StoredDataMigration::documents, ::saveDocuments)
    fun saveDocuments(value: List<SchoolDocument>) = writeList("documents", value)

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

    private fun <T> readAndRepair(
        key: String,
        decode: (String) -> List<T>?,
        save: (List<T>) -> Unit
    ): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        val repaired = decode(raw) ?: return emptyList()
        save(repaired)
        return repaired
    }

    private fun <T> writeList(key: String, value: List<T>) {
        prefs.edit().putString(key, gson.toJson(value)).apply()
    }
}
