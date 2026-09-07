package ca.creativepixels.schoolstuff.data

import java.time.LocalDate
import java.util.UUID

data class ChildProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val grade: String = "",
    val colorKey: String = "blue",
    val teacherName: String = "",
    val teacherEmail: String = "",
    val classroomPhone: String = "",
    val room: String = "",
    val schoolName: String = "",
    val schoolPhone: String = "",
    val attendancePhone: String = "",
    val schoolAddress: String = "",
    val specialNotes: String = ""
)

data class SchoolItem(
    val id: String = UUID.randomUUID().toString(),
    val childId: String = "",
    val title: String,
    val category: String = Category.SCHOOL_EVENT,
    val dateIso: String = LocalDate.now().toString(),
    val repeat: String = Repeat.ONE_TIME,
    val reminder: String = Reminder.NIGHT_BEFORE,
    val notes: String = "",
    val needsItemFromHome: Boolean = false,
    val completed: Boolean = false,
    val externalEventId: String? = null
)

data class SchoolDocument(
    val id: String = UUID.randomUUID().toString(),
    val childId: String,
    val title: String,
    val type: String = DocumentType.FORM,
    val uri: String,
    val addedAtMillis: Long = System.currentTimeMillis(),
    val needsSignature: Boolean = false
)

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val canWrite: Boolean
)

data class ExternalCalendarEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean
)

object Category {
    const val FOOD_DAY = "Food Day"
    const val FORM_DUE = "Form Due"
    const val SPIRIT_DAY = "Spirit Day"
    const val BRING_ITEM = "Bring Item"
    const val SCHOOL_EVENT = "School Event"
    const val LIBRARY = "Library"
    const val GYM = "Gym"
    const val HOMEWORK = "Homework"

    val all = listOf(
        FOOD_DAY, FORM_DUE, SPIRIT_DAY, BRING_ITEM,
        SCHOOL_EVENT, LIBRARY, GYM, HOMEWORK
    )
}

object Repeat {
    const val ONE_TIME = "One-time"
    const val WEEKLY = "Weekly"
    const val MONTHLY = "Monthly"
    val all = listOf(ONE_TIME, WEEKLY, MONTHLY)
}

object Reminder {
    const val NONE = "None"
    const val NIGHT_BEFORE = "Night Before"
    const val MORNING_OF = "Morning Of"
    const val BOTH = "Both"
    val all = listOf(NONE, NIGHT_BEFORE, MORNING_OF, BOTH)
}

object DocumentType {
    const val FORM = "Forms"
    const val REPORT_CARD = "Report Cards"
    const val MEDICAL = "Medical / ICP"
    const val ARTWORK = "Artwork"
    const val PHOTO = "Photos"
    val all = listOf(FORM, REPORT_CARD, MEDICAL, ARTWORK, PHOTO)
}
