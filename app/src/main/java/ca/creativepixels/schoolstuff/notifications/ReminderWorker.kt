package ca.creativepixels.schoolstuff.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import ca.creativepixels.schoolstuff.R
import ca.creativepixels.schoolstuff.data.LocalStore
import ca.creativepixels.schoolstuff.data.Repeat

class ReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val itemId = inputData.getString("itemId") ?: return Result.success()
        val kind = inputData.getString("kind") ?: "morning"
        val store = LocalStore(applicationContext)
        val item = store.loadItems().firstOrNull { it.id == itemId } ?: return Result.success()
        val child = store.loadChildren().firstOrNull { it.id == item.childId }
        val who = child?.name ?: "School"

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "school_reminders",
            "School reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "School Stuff reminders" }
        manager.createNotificationChannel(channel)

        val body = if (kind == "night") "$who: ${item.title} tomorrow" else "$who: ${item.title} today"
        val notification = NotificationCompat.Builder(applicationContext, "school_reminders")
            .setSmallIcon(R.drawable.ic_school_stuff)
            .setContentTitle("School Stuff")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body + if (item.notes.isBlank()) "" else " • ${item.notes}"))
            .setAutoCancel(true)
            .build()

        if (android.os.Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify((item.id + kind).hashCode(), notification)
        }

        if (item.repeat == Repeat.WEEKLY || item.repeat == Repeat.MONTHLY) {
            ReminderScheduler.schedule(applicationContext, item)
        }
        return Result.success()
    }
}
