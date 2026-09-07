package ca.creativepixels.schoolstuff.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import ca.creativepixels.schoolstuff.trFor
import ca.creativepixels.schoolstuff.data.LocalStore
import ca.creativepixels.schoolstuff.data.Repeat

class ReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val itemId = inputData.getString("itemId") ?: return Result.success()
        val kind = inputData.getString("kind") ?: "morning"
        val store = LocalStore(applicationContext)
        val item = store.loadItems().firstOrNull { it.id == itemId } ?: return Result.success()
        val child = store.loadChildren().firstOrNull { it.id == item.childId }
        val language = store.getAppLanguage()
        val who = child?.name ?: trFor(language, "School", "École")

        ReminderNotifications.ensureChannel(applicationContext)

        val body = if (kind == "night") {
            trFor(language, "$who: ${item.title} tomorrow", "$who : ${item.title} demain")
        } else {
            trFor(language, "$who: ${item.title} today", "$who : ${item.title} aujourd’hui")
        }
        val notes = item.notes.orEmpty()
        val details = body + if (notes.isBlank()) "" else " • $notes"
        val notification = ReminderNotifications.build(applicationContext, body, details)

        if (ReminderNotifications.areEnabled(applicationContext)) {
            NotificationManagerCompat.from(applicationContext).notify((item.id + kind).hashCode(), notification)
        }

        if (item.repeat == Repeat.WEEKLY || item.repeat == Repeat.MONTHLY) {
            ReminderScheduler.schedule(applicationContext, item)
        }
        return Result.success()
    }
}
