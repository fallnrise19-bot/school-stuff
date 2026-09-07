package ca.creativepixels.schoolstuff

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import ca.creativepixels.schoolstuff.data.*
import ca.creativepixels.schoolstuff.notifications.ReminderScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class SchoolStuffViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalStore(application)

    var children by mutableStateOf<List<ChildProfile>>(emptyList())
        private set
    var items by mutableStateOf<List<SchoolItem>>(emptyList())
        private set
    var documents by mutableStateOf<List<SchoolDocument>>(emptyList())
        private set
    var transportation by mutableStateOf<List<TransportationInfo>>(emptyList())
        private set
    var parentNotes by mutableStateOf("")
        private set
    var nightReminderHour by mutableStateOf(20)
        private set
    var nightReminderMinute by mutableStateOf(0)
        private set
    var morningReminderHour by mutableStateOf(7)
        private set
    var morningReminderMinute by mutableStateOf(0)
        private set
    var defaultReminder by mutableStateOf(Reminder.NIGHT_BEFORE)
        private set

    init {
        if (!store.hasSeeded()) {
            children = SeedData.children()
            items = SeedData.items()
            store.saveChildren(children)
            store.saveItems(items)
            store.markSeeded()
            items.forEach { ReminderScheduler.schedule(application, it) }
        } else {
            children = store.loadChildren()
            items = store.loadItems()
        }
        documents = store.loadDocuments()
        transportation = store.loadTransportation()
        parentNotes = store.getParentNotes()
        nightReminderHour = store.getNightReminderHour()
        nightReminderMinute = store.getNightReminderMinute()
        morningReminderHour = store.getMorningReminderHour()
        morningReminderMinute = store.getMorningReminderMinute()
        defaultReminder = store.getDefaultReminder()
    }

    fun child(id: String): ChildProfile? = children.firstOrNull { it.id == id }

    fun childName(id: String): String = child(id)?.name ?: "Family"

    fun addChild(name: String, grade: String) {
        if (name.isBlank()) return
        val colors = listOf("blue", "green", "pink", "yellow")
        children = children + ChildProfile(name = name.trim(), grade = grade.trim(), colorKey = colors[children.size % colors.size])
        store.saveChildren(children)
    }

    fun updateChild(profile: ChildProfile) {
        children = children.map { if (it.id == profile.id) profile else it }
        store.saveChildren(children)
    }

    fun deleteChild(childId: String) {
        children = children.filterNot { it.id == childId }
        items.filter { it.childId == childId }.forEach { ReminderScheduler.cancel(getApplication(), it.id) }
        items = items.filterNot { it.childId == childId }
        documents = documents.filterNot { it.childId == childId }
        transportation = transportation.filterNot { it.childId == childId }
        store.saveChildren(children)
        store.saveItems(items)
        store.saveDocuments(documents)
        store.saveTransportation(transportation)
    }

    fun addItem(item: SchoolItem) {
        items = items + item
        store.saveItems(items)
        ReminderScheduler.schedule(getApplication(), item)
    }

    fun updateItem(item: SchoolItem) {
        items = items.map { if (it.id == item.id) item else it }
        store.saveItems(items)
        ReminderScheduler.schedule(getApplication(), item)
    }

    fun toggleComplete(itemId: String) {
        val updated = items.firstOrNull { it.id == itemId } ?: return
        updateItem(updated.copy(completed = !updated.completed))
    }

    fun deleteItem(itemId: String) {
        ReminderScheduler.cancel(getApplication(), itemId)
        items = items.filterNot { it.id == itemId }
        store.saveItems(items)
    }

    fun addDocument(document: SchoolDocument) {
        documents = documents + document
        store.saveDocuments(documents)
    }

    fun deleteDocument(documentId: String) {
        documents = documents.filterNot { it.id == documentId }
        store.saveDocuments(documents)
    }

    fun transportationFor(childId: String): TransportationInfo? =
        transportation.firstOrNull { it.childId == childId }

    fun saveTransportation(info: TransportationInfo) {
        transportation = transportation.filterNot { it.childId == info.childId } + info
        store.saveTransportation(transportation)
    }

    fun saveParentNotes(notes: String) {
        parentNotes = notes.trim()
        store.setParentNotes(parentNotes)
    }

    fun setNightReminderTime(hour: Int, minute: Int) {
        nightReminderHour = hour.coerceIn(0, 23)
        nightReminderMinute = minute.coerceIn(0, 59)
        store.setNightReminderTime(nightReminderHour, nightReminderMinute)
        rescheduleAllReminders()
    }

    fun setMorningReminderTime(hour: Int, minute: Int) {
        morningReminderHour = hour.coerceIn(0, 23)
        morningReminderMinute = minute.coerceIn(0, 59)
        store.setMorningReminderTime(morningReminderHour, morningReminderMinute)
        rescheduleAllReminders()
    }

    fun updateDefaultReminder(value: String) {
        if (value !in Reminder.all) return
        defaultReminder = value
        store.setDefaultReminder(value)
    }

    fun rescheduleAllReminders() {
        items.forEach { ReminderScheduler.schedule(getApplication(), it) }
    }

    fun selectedCalendarIds(): Set<Long> = store.getSelectedCalendarIds()
    fun saveSelectedCalendarIds(ids: Set<Long>) = store.setSelectedCalendarIds(ids)
    fun defaultCalendarId(): Long? = store.getDefaultCalendarId()
    fun saveDefaultCalendarId(id: Long?) = store.setDefaultCalendarId(id)

    fun importCalendarEvents(events: List<ExternalCalendarEvent>): Int {
        val existingKeys = items.mapNotNull { it.externalEventId }.toSet()
        val newItems = events.mapNotNull { event ->
            val key = "${event.eventId}:${event.beginMillis}"
            if (key in existingKeys) return@mapNotNull null
            val zone = if (event.allDay) ZoneOffset.UTC else ZoneId.systemDefault()
            val date = Instant.ofEpochMilli(event.beginMillis).atZone(zone).toLocalDate()
            SchoolItem(
                childId = "",
                title = event.title,
                category = Category.SCHOOL_EVENT,
                dateIso = date.toString(),
                repeat = Repeat.ONE_TIME,
                reminder = Reminder.NONE,
                externalEventId = key
            )
        }
        if (newItems.isNotEmpty()) {
            items = items + newItems
            store.saveItems(items)
        }
        return newItems.size
    }

    fun itemsFor(date: LocalDate): List<SchoolItem> = items
        .filter { occursOn(it, date) }
        .sortedBy { childName(it.childId) }

    fun upcoming(days: Long = 14): List<Pair<LocalDate, SchoolItem>> {
        val today = LocalDate.now()
        return (0..days).flatMap { offset ->
            val date = today.plusDays(offset)
            itemsFor(date).map { date to it }
        }
    }

    fun occursOn(item: SchoolItem, date: LocalDate): Boolean {
        val start = runCatching { LocalDate.parse(item.dateIso) }.getOrNull() ?: return false
        if (date.isBefore(start)) return false
        return when (item.repeat) {
            Repeat.WEEKLY -> date.dayOfWeek == start.dayOfWeek
            Repeat.MONTHLY -> date.dayOfMonth == start.dayOfMonth.coerceAtMost(date.lengthOfMonth())
            else -> date == start
        }
    }
}
