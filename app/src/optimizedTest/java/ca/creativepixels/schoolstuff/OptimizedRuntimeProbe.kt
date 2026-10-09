package ca.creativepixels.schoolstuff

import android.content.Context
import ca.creativepixels.schoolstuff.data.LocalStore
import com.google.gson.Gson
import com.google.gson.JsonParser

/** Compiled into the optimized test APK so R8 rewrites/inlines calls together. */
object OptimizedRuntimeProbe {
    @JvmStatic fun existingSchoolRecordsSurviveOptimizedReadAndWrite(context: Context) {
        val prefs = context.getSharedPreferences("school_stuff_store", Context.MODE_PRIVATE)
        // Fixtures use the JSON keys already written by build 43 and older versions.
        val fixtures = linkedMapOf(
            "children" to """[{"id":"child-1","name":"Élodie","grade":"3","colorKey":"blue","teacherName":"Ms Smith","teacherEmail":"teacher@example.test","classroomPhone":"555-0100","room":"12","schoolName":"Example School","schoolPhone":"555-0101","attendancePhone":"555-0102","schoolAddress":"1 Example Street","specialNotes":"Allergy note"}]""",
            "items" to """[{"id":"item-1","childId":"child-1","title":"Library books","category":"Library","emoji":"📚","dateIso":"2026-10-12","repeat":"Weekly","reminder":"Both","notes":"Return both books","needsItemFromHome":true,"completed":false,"externalEventId":"calendar-1"}]""",
            "absences" to """[{"id":"absence-1","childId":"child-1","dateIso":"2026-10-05","reason":"Sick"}]""",
            "documents" to """[{"id":"document-1","childId":"child-1","title":"Permission form","type":"Forms","uri":"content://example/form.pdf","addedAtMillis":1791244800000,"needsSignature":true}]""",
            "transportation" to """[{"childId":"child-1","mode":"Bus","busNumber":"12","pickupPoint":"School gate","driverName":"Pat","pickupInfo":"7:45 AM"}]""",
            "teachers" to """[{"id":"teacher-1","childId":"child-1","name":"Ms Smith","subject":"French","email":"teacher@example.test","phone":"555-0100","room":"12"}]"""
        )
        prefs.edit().apply { fixtures.forEach { (key, json) -> putString(key, json) } }.commit()
        val store = LocalStore(context)
        check(store.loadChildren().single().name == "Élodie")
        check(store.loadItems().single().notes == "Return both books")
        check(store.loadAbsences().single().reason == "Sick")
        check(store.loadDocuments().single().needsSignature)
        check(store.loadTransportation().single().busNumber == "12")
        check(store.loadTeachers().single().subject == "French")
        store.saveChildren(store.loadChildren())
        store.saveItems(store.loadItems())
        store.saveAbsences(store.loadAbsences())
        store.saveDocuments(store.loadDocuments())
        store.saveTransportation(store.loadTransportation())
        store.saveTeachers(store.loadTeachers())
        fixtures.forEach { (key, json) ->
            check(JsonParser.parseString(json) == JsonParser.parseString(prefs.getString(key, "")!!)) { "Persisted schema changed for $key" }
        }
    }

    @JvmStatic fun cachedReceiptAndQueuedWorkerRemainCompatible(context: Context) {
        val receiptClass = Class.forName("ca.creativepixels.schoolstuff.billing.CachedReceipt")
        val json = """{"json":"signed purchase payload","signature":"signature fixture","checkedAt":1791500000000}"""
        val gson = Gson()
        val receipt = gson.fromJson(json, receiptClass)
        check(JsonParser.parseString(json) == JsonParser.parseString(gson.toJson(receipt)))
        // WorkManager must still resolve names already saved in its database.
        val worker = Class.forName("ca.creativepixels.schoolstuff.notifications.ReminderWorker")
        worker.getConstructor(Context::class.java, androidx.work.WorkerParameters::class.java)
    }

}
