package ca.creativepixels.schoolstuff.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ca.creativepixels.schoolstuff.trFor
import ca.creativepixels.schoolstuff.MainActivity
import ca.creativepixels.schoolstuff.R
import ca.creativepixels.schoolstuff.data.LocalStore

object ReminderNotifications {
    const val CHANNEL_ID = "school_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val language = LocalStore(context).getAppLanguage()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                trFor(language, "School reminders", "Rappels scolaires"),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = trFor(language, "School Stuff reminders", "Rappels de School Stuff") }
        )
    }

    fun areEnabled(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun build(context: Context, body: String, details: String = body) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_school_stuff)
            .setContentTitle("School Stuff")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()

    fun showTest(context: Context): Boolean {
        ensureChannel(context)
        if (!areEnabled(context)) return false
        val language = LocalStore(context).getAppLanguage()
        val body = trFor(language, "Notifications are working. One less thing for your brain to carry. ♥", "Les notifications fonctionnent. Une chose de moins à garder en tête. ♥")
        NotificationManagerCompat.from(context).notify(210021, build(context, body))
        return true
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
