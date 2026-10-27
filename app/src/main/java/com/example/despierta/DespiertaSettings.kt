package com.example.despierta

import android.content.SharedPreferences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

// Preferencias de interfaz guardadas en `despierta_ui`. Ninguna programa alarmas: solo
// describen el horario de sueño y los avisos opcionales.

internal val ALL_WEEK_DAYS: Set<Int> = (Calendar.SUNDAY..Calendar.SATURDAY).toSet()

/** Orden de lunes a domingo, con la letra corta que se muestra. */
internal val WEEK_DAYS_MONDAY_FIRST: List<Pair<Int, String>> = listOf(
    Calendar.MONDAY to "L",
    Calendar.TUESDAY to "M",
    Calendar.WEDNESDAY to "X",
    Calendar.THURSDAY to "J",
    Calendar.FRIDAY to "V",
    Calendar.SATURDAY to "S",
    Calendar.SUNDAY to "D"
)

internal val SLEEP_GOAL_OPTIONS: List<Int> = listOf(6 * 60, 6 * 60 + 30, 7 * 60, 7 * 60 + 30, 8 * 60, 8 * 60 + 30, 9 * 60)
internal val REMINDER_LEAD_OPTIONS: List<Int> = listOf(5, 10, 15, 30)
internal val SNOOZE_OPTIONS: List<Int> = listOf(5, 10, 15)

/** Horario de sueño: horas, objetivo y días en que se aplica. */
internal data class SleepSchedule(
    val plan: SleepPlan = SleepPlan.DEFAULT,
    val goalMinutes: Int = 8 * 60,
    val days: Set<Int> = ALL_WEEK_DAYS
) {
    val durationMinutes: Int get() = plan.durationMinutes

    /** Avance estimado hacia el objetivo, entre 0 y 1. */
    val goalProgress: Float
        get() = if (goalMinutes <= 0) 0f else (durationMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
}

/** Avisos opcionales. Desactivados por defecto: el usuario decide activarlos. */
internal data class NotificationPrefs(
    val enabled: Boolean = false,
    val bedtimeReminder: Boolean = true,
    val upcomingAlarm: Boolean = true,
    val leadMinutes: Int = 15,
    val vibrate: Boolean = true,
    val sound: Boolean = true
)

private const val PREF_SLEEP_BED = "sleep_bed_minutes"
private const val PREF_SLEEP_WAKE = "sleep_wake_minutes"
private const val PREF_SLEEP_GOAL = "sleep_goal_minutes"
private const val PREF_SLEEP_DAYS = "sleep_days"
private const val PREF_NOTIFY_ENABLED = "notify_enabled"
private const val PREF_NOTIFY_BEDTIME = "notify_bedtime"
private const val PREF_NOTIFY_UPCOMING = "notify_upcoming"
private const val PREF_NOTIFY_LEAD = "notify_lead_minutes"
private const val PREF_NOTIFY_VIBRATE = "notify_vibrate"
private const val PREF_NOTIFY_SOUND = "notify_sound"
private const val PREF_DEFAULT_SNOOZE = "default_snooze_minutes"

internal fun loadSleepSchedule(prefs: SharedPreferences): SleepSchedule {
    val defaults = SleepSchedule()
    val days = prefs.getString(PREF_SLEEP_DAYS, null)
        ?.split(',')
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.filter { it in ALL_WEEK_DAYS }
        ?.toSet()
        ?: defaults.days
    return SleepSchedule(
        plan = SleepPlan(
            bedMinutes = normalizeMinuteOfDay(prefs.getInt(PREF_SLEEP_BED, SleepPlan.DEFAULT.bedMinutes)),
            wakeMinutes = normalizeMinuteOfDay(prefs.getInt(PREF_SLEEP_WAKE, SleepPlan.DEFAULT.wakeMinutes))
        ),
        goalMinutes = prefs.getInt(PREF_SLEEP_GOAL, defaults.goalMinutes).takeIf { it in SLEEP_GOAL_OPTIONS }
            ?: defaults.goalMinutes,
        days = days
    )
}

internal fun saveSleepSchedule(prefs: SharedPreferences, schedule: SleepSchedule) {
    val editor = prefs.edit()
    editor.putInt(PREF_SLEEP_BED, schedule.plan.bedMinutes)
    editor.putInt(PREF_SLEEP_WAKE, schedule.plan.wakeMinutes)
    editor.putInt(PREF_SLEEP_GOAL, schedule.goalMinutes)
    editor.putString(PREF_SLEEP_DAYS, schedule.days.sorted().joinToString(","))
    editor.apply()
}

internal fun loadNotificationPrefs(prefs: SharedPreferences): NotificationPrefs {
    val defaults = NotificationPrefs()
    return NotificationPrefs(
        enabled = prefs.getBoolean(PREF_NOTIFY_ENABLED, defaults.enabled),
        bedtimeReminder = prefs.getBoolean(PREF_NOTIFY_BEDTIME, defaults.bedtimeReminder),
        upcomingAlarm = prefs.getBoolean(PREF_NOTIFY_UPCOMING, defaults.upcomingAlarm),
        leadMinutes = prefs.getInt(PREF_NOTIFY_LEAD, defaults.leadMinutes)
            .takeIf { it in REMINDER_LEAD_OPTIONS } ?: defaults.leadMinutes,
        vibrate = prefs.getBoolean(PREF_NOTIFY_VIBRATE, defaults.vibrate),
        sound = prefs.getBoolean(PREF_NOTIFY_SOUND, defaults.sound)
    )
}

internal fun saveNotificationPrefs(prefs: SharedPreferences, value: NotificationPrefs) {
    val editor = prefs.edit()
    editor.putBoolean(PREF_NOTIFY_ENABLED, value.enabled)
    editor.putBoolean(PREF_NOTIFY_BEDTIME, value.bedtimeReminder)
    editor.putBoolean(PREF_NOTIFY_UPCOMING, value.upcomingAlarm)
    editor.putInt(PREF_NOTIFY_LEAD, value.leadMinutes)
    editor.putBoolean(PREF_NOTIFY_VIBRATE, value.vibrate)
    editor.putBoolean(PREF_NOTIFY_SOUND, value.sound)
    editor.apply()
}

/** Repetición que se propone al crear una alarma nueva; cada alarma puede cambiarla. */
internal fun loadDefaultSnooze(prefs: SharedPreferences): Int =
    prefs.getInt(PREF_DEFAULT_SNOOZE, SNOOZE_OPTIONS.first()).takeIf { it in SNOOZE_OPTIONS }
        ?: SNOOZE_OPTIONS.first()

internal fun saveDefaultSnooze(prefs: SharedPreferences, minutes: Int) {
    prefs.edit().putInt(PREF_DEFAULT_SNOOZE, minutes).apply()
}

/** Calendar.SUNDAY..SATURDAY a partir de java.time. */
internal fun calendarDayOf(day: DayOfWeek): Int = day.value % 7 + 1

/**
 * Próximo recordatorio para dormir: [leadMinutes] antes de la hora de acostarse, solo en los
 * días elegidos (el día es el de la noche en que te acuestas). Null si no hay días elegidos.
 */
internal fun nextBedtimeReminderMillis(
    schedule: SleepSchedule,
    leadMinutes: Int,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long? {
    if (schedule.days.isEmpty()) return null
    val today = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    for (offset in 0L..8L) {
        val date: LocalDate = today.plusDays(offset)
        if (calendarDayOf(date.dayOfWeek) !in schedule.days) continue
        val bed = schedule.plan.bedMinutes
        val at = date.atTime(bed / 60, bed % 60)
            .atZone(zone)
            .minusMinutes(leadMinutes.toLong())
            .toInstant()
            .toEpochMilli()
        if (at > nowMillis) return at
    }
    return null
}

/** Cuándo avisar de la próxima alarma, o cuándo volver a calcular si ya está muy cerca. */
internal data class UpcomingNotice(val atMillis: Long, val postNotice: Boolean)

internal fun upcomingAlarmNotice(
    triggerAtMillis: Long,
    leadMinutes: Int,
    nowMillis: Long
): UpcomingNotice {
    val noticeAt = triggerAtMillis - leadMinutes * 60_000L
    return if (noticeAt > nowMillis) {
        UpcomingNotice(noticeAt, postNotice = true)
    } else {
        // Ya estamos dentro de la anticipación: se recalcula justo después de que suene.
        UpcomingNotice(triggerAtMillis + 60_000L, postNotice = false)
    }
}
