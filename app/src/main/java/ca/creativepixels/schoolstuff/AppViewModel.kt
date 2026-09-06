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
        store.saveChildren(children)
        store.saveItems(items)
        store.saveDocuments(documents)
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
