package ca.creativepixels.schoolstuff

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/** Captures debug-build Java/Kotlin crashes so a device-only failure brings its stack trace home. */
internal object CrashDiagnostics {
    private const val REPORT_FILE = "last-debug-crash.txt"

    fun install(context: Context) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                File(context.filesDir, REPORT_FILE).writeText(buildReport(thread, throwable))
            }
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
            }
        }
    }

    fun showPending(activity: Activity) {
        val file = File(activity.filesDir, REPORT_FILE)
        val report = runCatching { file.takeIf(File::exists)?.readText() }.getOrNull() ?: return

        val reportView = TextView(activity).apply {
            text = report
            textSize = 12f
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
            setPadding(36, 16, 36, 8)
        }

        AlertDialog.Builder(activity)
            .setTitle("School Stuff caught the crash")
            .setMessage("Copy this report and paste it into the Codex chat. It contains the exact failure instead of making us guess again.")
            .setView(reportView)
            .setPositiveButton("Copy report") { _, _ ->
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("School Stuff crash report", report))
                file.delete()
            }
            .setNegativeButton("Dismiss") { _, _ -> file.delete() }
            .show()
    }

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val stackTrace = StringWriter().also { writer ->
            throwable.printStackTrace(PrintWriter(writer))
        }.toString()
        return buildString {
            appendLine("School Stuff ${BuildConfig.VERSION_NAME} build ${BuildConfig.VERSION_CODE}")
            appendLine("Time: ${Instant.now()}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android SDK: ${Build.VERSION.SDK_INT}")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(stackTrace)
        }
    }
}
