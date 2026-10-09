package ca.creativepixels.schoolstuff

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import ca.creativepixels.schoolstuff.data.LocalStore
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Runs against the R8-optimized APK, not the unminified debug app. */
@RunWith(AndroidJUnit4::class)
class OptimizedRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val prefs get() = context.getSharedPreferences("school_stuff_store", Context.MODE_PRIVATE)
    private val device = UiDevice.getInstance(instrumentation)

    @Before fun startWithCleanEmulatorData() {
        prefs.edit().clear().putBoolean("seeded", true).commit()
    }

    @After fun restoreDisplayAndData() {
        device.executeShellCommand("wm size reset")
        device.executeShellCommand("wm density reset")
        device.unfreezeRotation()
        prefs.edit().clear().commit()
    }

    @Test fun existingSchoolRecordsSurviveOptimizedReadAndWrite() {
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
        assertEquals("Élodie", store.loadChildren().single().name)
        assertEquals("Return both books", store.loadItems().single().notes)
        assertEquals("Sick", store.loadAbsences().single().reason)
        assertTrue(store.loadDocuments().single().needsSignature)
        assertEquals("12", store.loadTransportation().single().busNumber)
        assertEquals("French", store.loadTeachers().single().subject)
        store.saveChildren(store.loadChildren())
        store.saveItems(store.loadItems())
        store.saveAbsences(store.loadAbsences())
        store.saveDocuments(store.loadDocuments())
        store.saveTransportation(store.loadTransportation())
        store.saveTeachers(store.loadTeachers())
        fixtures.forEach { (key, json) ->
            assertEquals("Persisted schema changed for $key", JsonParser.parseString(json),
                JsonParser.parseString(prefs.getString(key, "")!!))
        }
    }

    @Test fun cachedReceiptAndQueuedWorkerRemainCompatible() {
        val receiptClass = Class.forName("ca.creativepixels.schoolstuff.billing.CachedReceipt")
        val json = """{"json":"signed purchase payload","signature":"signature fixture","checkedAt":1791500000000}"""
        val gson = Gson()
        val receipt = gson.fromJson(json, receiptClass)
        assertEquals(JsonParser.parseString(json), JsonParser.parseString(gson.toJson(receipt)))
        // WorkManager must still resolve names already saved in its database.
        val worker = Class.forName("ca.creativepixels.schoolstuff.notifications.ReminderWorker")
        assertNotNull(worker.getConstructor(Context::class.java, androidx.work.WorkerParameters::class.java))
    }

    @Test fun previewRemainsUsableWhenRotatedAndResized() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(launch).use { scenario ->
            assertTrue(device.wait(Until.hasObject(By.text("Explore ParentBell")), 15_000))
            var activityBefore: MainActivity? = null
            scenario.onActivity { activityBefore = it }
            checkVisibleControls("phone-portrait")
            device.setOrientationLeft()
            instrumentation.waitForIdleSync()
            assertTrue(device.wait(Until.hasObject(By.text("Explore ParentBell")), 5_000))
            scenario.onActivity { assertSame("Rotation discarded activity/form state", activityBefore, it) }
            checkVisibleControls("phone-landscape")
            device.setOrientationNatural()
            device.executeShellCommand("wm density 160")
            device.executeShellCommand("wm size 1200x1920")
            instrumentation.waitForIdleSync()
            assertTrue(device.wait(Until.hasObject(By.text("Explore ParentBell")), 5_000))
            checkVisibleControls("tablet-portrait")
            device.setOrientationLeft()
            instrumentation.waitForIdleSync()
            assertTrue(device.wait(Until.hasObject(By.text("Explore ParentBell")), 5_000))
            checkVisibleControls("tablet-landscape")
            device.findObject(By.text("Kids")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Alex")), 5_000))
            device.findObject(By.text("Calendar")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Explore ParentBell")), 5_000))
            device.findObject(By.text("View subscription")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Subscription")), 5_000))
        }
    }

    private fun checkVisibleControls(name: String) {
        for (text in listOf("Explore ParentBell", "View subscription", "Home", "Calendar", "Kids", "Settings")) {
            val control = device.findObject(By.text(text))
            assertNotNull("Missing $text at $name", control)
            val bounds = control.visibleBounds
            assertTrue("$text is outside $name", bounds.left >= 0 && bounds.top >= 0 &&
                bounds.right <= device.displayWidth && bounds.bottom <= device.displayHeight && !bounds.isEmpty)
        }
        // Instrumentation output captures are intermediate CI evidence, not app assets.
        val output = context.getExternalFilesDir(null)!!
        device.takeScreenshot(File(output, "$name.png"))
    }
}
