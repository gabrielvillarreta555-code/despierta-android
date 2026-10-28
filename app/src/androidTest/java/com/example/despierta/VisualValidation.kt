package com.example.despierta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingPolicies
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Validación visual "antes y después" de la pantalla de Alarmas. No forma parte de las suites:
 * la ejecuta el workflow de validación sobre dos APK (base y candidata) en el mismo emulador,
 * con la misma hora fija, los mismos datos ficticios y los mismos temas.
 *
 * El argumento de instrumentación `etiqueta` prefija los archivos (p. ej. "pixel6-antes").
 */
class VisualValidation {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val outputDir = File(context.filesDir, "validacion")
    private val label: String = InstrumentationRegistry.getArguments().getString("etiqueta") ?: "sin-etiqueta"

    /** Capturas estáticas: con alarmas y sin alarmas, en varios temas. */
    @Test
    fun staticScreens() {
        outputDir.mkdirs()
        IdlingPolicies.setIdlingResourceTimeout(25, TimeUnit.SECONDS)
        IdlingPolicies.setMasterPolicyTimeout(30, TimeUnit.SECONDS)
        try {
            // Precalentamiento: la primera captura tras instalar a veces sale negra.
            seed(withAlarms = true)
            screen("precalentamiento", ThemeChoice.DEFAULT)
            File(outputDir, "$label-precalentamiento.png").delete()
            screen("con-alarmas-noche", ThemeChoice.DEFAULT)
            screen("con-alarmas-bosque", ThemeChoice("bosque", null))
            seed(withAlarms = false)
            screen("sin-alarmas-noche", ThemeChoice.DEFAULT)
            screen("sin-alarmas-bosque", ThemeChoice("bosque", null))
            screen("sin-alarmas-amanecer", ThemeChoice("amanecer", null))
        } finally {
            cleanUp()
        }
    }

    private fun screen(name: String, theme: ThemeChoice) {
        repeat(2) { attempt ->
            if (File(outputDir, "$label-$name.png").exists()) return
            if (attempt > 0) Thread.sleep(3_000)
            shot(name, theme)
        }
    }

    private fun shot(name: String, theme: ThemeChoice) {
        runCatching {
            saveTheme(theme)
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_DISABLE_LIVE_CLOCK_FOR_TESTS, true)
                .putExtra(MainActivity.EXTRA_FIXED_NOW_MILLIS_FOR_TESTS, FIXED_NOW_MILLIS)
            ActivityScenario.launch<MainActivity>(intent).use {
                composeRule.waitForIdle()
                Thread.sleep(1_200)
                save(name, takeScreenshot())
            }
        }.onFailure { Log.e(TAG, "validacion_fallida $name", it) }
    }

    private fun takeScreenshot(): Bitmap {
        shell("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
        repeat(10) {
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            if (bitmap != null && !isBlank(bitmap)) return bitmap
            Thread.sleep(1_000)
        }
        throw AssertionError("No se obtuvo una captura válida")
    }

    private fun save(name: String, bitmap: Bitmap) {
        FileOutputStream(File(outputDir, "$label-$name.png")).use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
        }
        Log.i(TAG, "capturada $label-$name")
    }

    private fun isBlank(bitmap: Bitmap): Boolean {
        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 24).coerceAtLeast(1)
        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                if ((bitmap.getPixel(x, y) and 0x00FFFFFF) != 0) return false
            }
        }
        return true
    }

    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().readText()
        }

    private fun saveTheme(theme: ThemeChoice) {
        val prefs = context.getSharedPreferences(DESPIERTA_UI_PREFS, Context.MODE_PRIVATE)
        saveThemeChoice(prefs, theme)
        prefs.edit().commit()
    }

    /** Datos ficticios, sin información personal: los mismos que el recorrido de capturas. */
    private fun seed(withAlarms: Boolean) {
        val store = AlarmStore(context)
        val scheduler = AlarmScheduler(context)
        store.getAll().forEach {
            scheduler.cancel(it.id)
            store.delete(it.id)
        }
        if (withAlarms) {
            val weekdays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
            store.save(store.newAlarm().copy(id = 9001, hour = 6, minute = 30, label = "Trabajo", days = weekdays, snoozeMinutes = 10, enabled = true))
            store.save(
                store.newAlarm().copy(
                    id = 9002, hour = 5, minute = 15, label = "Turno rotativo",
                    shiftPattern = ShiftPattern.from(LocalDate.of(2026, 10, 20), 14, 7), snoozeMinutes = 5, enabled = true
                )
            )
            store.save(store.newAlarm().copy(id = 9003, hour = 7, minute = 45, label = "Gimnasio", days = setOf(Calendar.SATURDAY), snoozeMinutes = 15, enabled = false))
        }
        val prefs = context.getSharedPreferences(DESPIERTA_UI_PREFS, Context.MODE_PRIVATE)
        saveSleepSchedule(prefs, SleepSchedule())
        saveNotificationPrefs(prefs, NotificationPrefs(enabled = true))
        prefs.edit().commit()
    }

    private fun cleanUp() {
        runCatching {
            val store = AlarmStore(context)
            val scheduler = AlarmScheduler(context)
            listOf(9001, 9002, 9003).forEach {
                scheduler.cancel(it)
                store.delete(it)
            }
            val prefs = context.getSharedPreferences(DESPIERTA_UI_PREFS, Context.MODE_PRIVATE)
            saveThemeChoice(prefs, ThemeChoice.DEFAULT)
            saveNotificationPrefs(prefs, NotificationPrefs())
            prefs.edit().commit()
            ReminderScheduler.refresh(context)
        }
    }

    private companion object {
        const val TAG = "DespiertaValidacion"

        /** Martes 27 de octubre de 2026, 9:42 PM en la zona del emulador (UTC). */
        val FIXED_NOW_MILLIS: Long =
            LocalDateTime.of(2026, 10, 27, 21, 42).toInstant(ZoneOffset.UTC).toEpochMilli()
    }
}
