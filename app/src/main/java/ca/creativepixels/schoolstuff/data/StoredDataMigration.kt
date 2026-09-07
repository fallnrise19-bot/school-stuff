package ca.creativepixels.schoolstuff.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.util.UUID

/**
 * Gson can put null into Kotlin non-null properties when an older saved JSON object predates a
 * field. Decode through nullable storage records so model additions never crash an existing user.
 */
internal object StoredDataMigration {
    private val gson = Gson()

    fun children(raw: String): List<ChildProfile>? = decode<StoredChild>(raw)?.map { stored ->
        ChildProfile(
            id = stored.id.validId(),
            name = stored.name.orEmpty(),
            grade = stored.grade.orEmpty(),
            colorKey = stored.colorKey.orDefault("blue"),
            teacherName = stored.teacherName.orEmpty(),
            teacherEmail = stored.teacherEmail.orEmpty(),
            classroomPhone = stored.classroomPhone.orEmpty(),
            room = stored.room.orEmpty(),
            schoolName = stored.schoolName.orEmpty(),
            schoolPhone = stored.schoolPhone.orEmpty(),
            attendancePhone = stored.attendancePhone.orEmpty(),
            schoolAddress = stored.schoolAddress.orEmpty(),
            transportationType = stored.transportationType.orEmpty(),
            busNumber = stored.busNumber.orEmpty(),
            transportDriverName = stored.transportDriverName.orEmpty(),
            transportLicensePlate = stored.transportLicensePlate.orEmpty(),
            pickupInfo = stored.pickupInfo.orEmpty(),
            dropOffInfo = stored.dropOffInfo.orEmpty(),
            transportationNotes = stored.transportationNotes.orEmpty(),
            specialNotes = stored.specialNotes.orEmpty()
        )
    }

    fun items(raw: String): List<SchoolItem>? = decode<StoredItem>(raw)?.map { stored ->
        SchoolItem(
            id = stored.id.validId(),
            childId = stored.childId.orEmpty(),
            title = stored.title.orDefault("School item"),
            category = stored.category.orDefault(Category.SCHOOL_EVENT),
            emoji = stored.emoji,
            dateIso = stored.dateIso.orDefault(LocalDate.now().toString()),
            repeat = stored.repeat.orDefault(Repeat.ONE_TIME),
            reminder = stored.reminder.orDefault(Reminder.NIGHT_BEFORE),
            notes = stored.notes.orEmpty(),
            needsItemFromHome = stored.needsItemFromHome ?: false,
            completed = stored.completed ?: false,
            externalEventId = stored.externalEventId
        )
    }

    fun documents(raw: String): List<SchoolDocument>? = decode<StoredDocument>(raw)?.map { stored ->
        SchoolDocument(
            id = stored.id.validId(),
            childId = stored.childId.orEmpty(),
            title = stored.title.orDefault("School file"),
            type = stored.type.orDefault(DocumentType.FORM),
            uri = stored.uri.orEmpty(),
            addedAtMillis = stored.addedAtMillis ?: System.currentTimeMillis(),
            needsSignature = stored.needsSignature ?: false
        )
    }

    private inline fun <reified T> decode(raw: String): List<T>? = runCatching {
        val type = object : TypeToken<List<T>>() {}.type
        gson.fromJson<List<T>>(raw, type) ?: emptyList()
    }.getOrNull()

    private fun String?.validId(): String = this?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString()
    private fun String?.orDefault(default: String): String = this?.takeIf(String::isNotBlank) ?: default

    private data class StoredChild(
        val id: String? = null,
        val name: String? = null,
        val grade: String? = null,
        val colorKey: String? = null,
        val teacherName: String? = null,
        val teacherEmail: String? = null,
        val classroomPhone: String? = null,
        val room: String? = null,
        val schoolName: String? = null,
        val schoolPhone: String? = null,
        val attendancePhone: String? = null,
        val schoolAddress: String? = null,
        val transportationType: String? = null,
        val busNumber: String? = null,
        val transportDriverName: String? = null,
        val transportLicensePlate: String? = null,
        val pickupInfo: String? = null,
        val dropOffInfo: String? = null,
        val transportationNotes: String? = null,
        val specialNotes: String? = null
    )

    private data class StoredItem(
        val id: String? = null,
        val childId: String? = null,
        val title: String? = null,
        val category: String? = null,
        val emoji: String? = null,
        val dateIso: String? = null,
        val repeat: String? = null,
        val reminder: String? = null,
        val notes: String? = null,
        val needsItemFromHome: Boolean? = null,
        val completed: Boolean? = null,
        val externalEventId: String? = null
    )

    private data class StoredDocument(
        val id: String? = null,
        val childId: String? = null,
        val title: String? = null,
        val type: String? = null,
        val uri: String? = null,
        val addedAtMillis: Long? = null,
        val needsSignature: Boolean? = null
    )
}
