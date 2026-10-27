package com.example.despierta

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.Instant
import java.time.ZoneId

/**
 * Avisos opcionales: recordatorio para dormir y aviso de la próxima alarma.
 *
 * Es independiente del motor de alarmas: usa su propio receptor, su propio canal y sus propias
 * PendingIntent. Solo lee las alarmas guardadas para saber cuál es la próxima; nunca las
 * programa, cancela ni modifica. Se usan avisos no exactos, así que Android puede retrasarlos
 * unos minutos para ahorrar batería.
 */
internal object ReminderScheduler {
    private const val ACTION_BEDTIME = "com.example.despierta.reminder.BEDTIME"
    private const val ACTION_UPCOMING = "com.example.despierta.reminder.UPCOMING"
    private const val EXTRA_POST = "post"
    private const val REQUEST_BEDTIME = 9101
    private const val REQUEST_UPCOMING = 9102
    private const val NOTIFICATION_BEDTIME = 9201
    private const val NOTIFICATION_UPCOMING = 9202

    /** Recalcula y reprograma los avisos según las preferencias actuales. */
    fun refresh(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(DESPIERTA_UI_PREFS, Context.MODE_PRIVATE)
        val notify = loadNotificationPrefs(prefs)
        val alarmManager = app.getSystemService(AlarmManager::class.java) ?: return

        cancel(app, alarmManager)
        if (!notify.enabled) return

        if (notify.bedtimeReminder) {
            val schedule = loadSleepSchedule(prefs)
            nextBedtimeReminderMillis(schedule, notify.leadMinutes, nowMillis)?.let { at ->
                val intent = reminderIntent(app, ACTION_BEDTIME)
                setInexact(alarmManager, at, pending(app, REQUEST_BEDTIME, intent))
            }
        }

        if (notify.upcomingAlarm) {
            val next = runCatching { findNextAlarm(AlarmStore(app).getAll(), nowMillis) }.getOrNull()
            if (next != null) {
                val notice = upcomingAlarmNotice(next.triggerAtMillis, notify.leadMinutes, nowMillis)
                // Misma acción para avisar o solo recalcular: así una única PendingIntent
                // sustituye a la anterior y se cancela siempre.
                val intent = reminderIntent(app, ACTION_UPCOMING)
                    .putExtra(EXTRA_POST, notice.postNotice)
                setInexact(alarmManager, notice.atMillis, pending(app, REQUEST_UPCOMING, intent))
            }
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && manager.areNotificationsEnabled()
    }

    internal fun handle(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_BEDTIME -> bedtimeContent(context)?.let { post(context, NOTIFICATION_BEDTIME, it) }
            ACTION_UPCOMING -> if (intent.getBooleanExtra(EXTRA_POST, false)) {
                upcomingContent(context)?.let { post(context, NOTIFICATION_UPCOMING, it) }
            }
            else -> Unit // Arranque, actualización o cambio de hora: solo recalcular.
        }
        refresh(context)
    }

    /**
     * El receptor está exportado para recibir el reinicio del sistema, así que el texto nunca se
     * toma del Intent: se reconstruye siempre a partir de los datos guardados.
     */
    private fun bedtimeContent(context: Context): Pair<String, String>? {
        val prefs = context.getSharedPreferences(DESPIERTA_UI_PREFS, Context.MODE_PRIVATE)
        val notify = loadNotificationPrefs(prefs)
        if (!notify.enabled || !notify.bedtimeReminder) return null
        val bed = loadSleepSchedule(prefs).plan.bedMinutes
        return "Prepárate para dormir" to
            "Tu hora de dormir es a las ${formatClockTime(bed / 60, bed % 60)}."
    }

    private fun upcomingContent(context: Context): Pair<String, String>? {
        val prefs = context.getSharedPreferences(DESPIERTA_UI_PREFS, Context.MODE_PRIVATE)
        val notify = loadNotificationPrefs(prefs)
        if (!notify.enabled || !notify.upcomingAlarm) return null
        val next = runCatching { findNextAlarm(AlarmStore(context).getAll()) }.getOrNull() ?: return null
        val at = Instant.ofEpochMilli(next.triggerAtMillis).atZone(ZoneId.systemDefault())
        return "Próxima alarma" to
            "«${next.alarm.label}» sonará a las ${formatClockTime(at.hour, at.minute)}."
    }

    private fun post(context: Context, id: Int, content: Pair<String, String>) {
        if (!canPostNotifications(context)) {
            AlarmLog.warning("reminder_not_posted", "notifications_disabled")
            return
        }
        val prefs = context.getSharedPreferences(DESPIERTA_UI_PREFS, Context.MODE_PRIVATE)
        val notify = loadNotificationPrefs(prefs)
        if (!notify.enabled) return
        val channelId = ensureChannel(context, notify.sound, notify.vibrate)
        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(content.first)
            .setContentText(content.second)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching {
            context.getSystemService(NotificationManager::class.java)?.notify(id, notification)
        }.onFailure { AlarmLog.error("reminder_post_failed", "id=$id", it) }
    }

    /**
     * Android fija el sonido y la vibración de un canal al crearlo, así que cada combinación
     * usa su propio canal de recordatorios (siempre separado del canal de alarmas).
     */
    private fun ensureChannel(context: Context, sound: Boolean, vibrate: Boolean): String {
        val id = "reminders_v1_" + (if (sound) "s" else "n") + (if (vibrate) "v" else "n")
        val manager = context.getSystemService(NotificationManager::class.java) ?: return id
        if (manager.getNotificationChannel(id) == null) {
            val channel = NotificationChannel(
                id,
                "Recordatorios" + when {
                    sound && vibrate -> ""
                    sound -> " sin vibración"
                    vibrate -> " silenciosos"
                    else -> " discretos"
                },
                if (sound) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Recordatorios para dormir y avisos de próximas alarmas"
                enableVibration(vibrate)
                if (!sound) setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
        return id
    }

    private fun reminderIntent(context: Context, action: String): Intent =
        Intent(context, ReminderReceiver::class.java).setAction(action)

    private fun pending(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun setInexact(alarmManager: AlarmManager, atMillis: Long, pendingIntent: PendingIntent) {
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }.onFailure { AlarmLog.error("reminder_schedule_failed", "at=$atMillis", it) }
    }

    private fun cancel(context: Context, alarmManager: AlarmManager) {
        for ((code, action) in listOf(REQUEST_BEDTIME to ACTION_BEDTIME, REQUEST_UPCOMING to ACTION_UPCOMING)) {
            alarmManager.cancel(pending(context, code, reminderIntent(context, action)))
        }
    }
}

/** Receptor de los avisos opcionales. También recalcula tras reiniciar o cambiar la hora. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching { ReminderScheduler.handle(context, intent) }
            .onFailure { AlarmLog.error("reminder_receive_failed", "action=${intent.action}", it) }
    }
}
