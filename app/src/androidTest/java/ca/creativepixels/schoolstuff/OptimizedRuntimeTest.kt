package ca.creativepixels.schoolstuff

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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

    @Test fun existingSchoolRecordsSurviveOptimizedReadAndWrite() =
        runOptimizedProbe("existingSchoolRecordsSurviveOptimizedReadAndWrite")

    @Test fun cachedReceiptAndQueuedWorkerRemainCompatible() =
        runOptimizedProbe("cachedReceiptAndQueuedWorkerRemainCompatible")

    private fun runOptimizedProbe(method: String) {
        try {
            Class.forName("ca.creativepixels.schoolstuff.OptimizedRuntimeProbe")
                .getMethod(method, Context::class.java).invoke(null, context)
        } catch (failure: java.lang.reflect.InvocationTargetException) {
            throw failure.targetException
        }
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
        device.executeShellCommand("screencap -p /sdcard/Download/parentbell-$name.png")
    }
}
