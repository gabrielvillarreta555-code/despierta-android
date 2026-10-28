package com.example.despierta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar

/**
 * Recorrido de capturas para revisar el diseño. No forma parte de las suites de pruebas:
 * el workflow lo ejecuta aparte y publica las imágenes en la rama `screenshots`.
 *
 * Usa solo datos ficticios y una hora fija para que las capturas sean reproducibles.
 * Cada pantalla se captura por separado: si una falla, las demás se siguen generando.
 */
class ScreenshotTour {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val outputDir = File(context.filesDir, "screenshots")
    private val captured = mutableListOf<String>()
    private val failed = mutableListOf<String>()

    @Test
    fun captureMainScreens() {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        seedDemoData()
        try {
            // Cada grupo abre la app desde cero: si una pantalla falla, las demás no se pierden.
            session {
                capture("01-alarmas")
                step("02-alarmas-lista") {
                    // El interruptor es un nodo propio; el texto de la hora se fusiona con la fila.
                    composeRule.onNodeWithTag("alarm_home").performScrollToNode(hasTestTag("alarm_toggle_$ALARM_WORK"))
                }
            }
            session {
                step("03-editor-alarma") {
                    // 2.6: Editar está en el menú "⋯" de cada fila.
                    composeRule.onNodeWithTag("alarm_home").performScrollToNode(hasTestTag("alarm_more_$ALARM_WORK"))
                    composeRule.onNodeWithTag("alarm_more_$ALARM_WORK").performClick()
                    composeRule.onNodeWithTag("alarm_edit_$ALARM_WORK").performClick()
                    // El editor tarda en quedar inactivo en emuladores lentos: una espera extra.
                    runCatching { composeRule.waitForIdle() }
                }
                step("04-editor-opciones") {
                    composeRule.onNodeWithTag("snooze_5").performScrollTo()
                }
            }
            session {
                step("05-editor-nueva") {
                    composeRule.onNodeWithTag("add_alarm").performClick()
                }
            }
            session {
                step("06-sueno") { composeRule.onNodeWithTag("nav_sleep").performClick() }
                step("07-sueno-editar") {
                    composeRule.onNodeWithTag("sleep_wake_card").performClick()
                }
                step("08-sueno-opciones") {
                    composeRule.onNodeWithTag("sleep_reminder_row").performScrollTo()
                }
            }
            session {
                step("09-turnos") { composeRule.onNodeWithTag("nav_shifts").performClick() }
            }
            session {
                step("10-ajustes") { composeRule.onNodeWithTag("nav_settings").performClick() }
                step("11-ajustes-apariencia") {
                    composeRule.onNodeWithTag("settings_appearance").performScrollTo().performClick()
                }
            }
            session {
                step("12-ajustes-notificaciones") {
                    composeRule.onNodeWithTag("nav_settings").performClick()
                    composeRule.onNodeWithTag("settings_notifications").performScrollTo().performClick()
                }
            }
            withApp(ThemeChoice("amanecer", null)) { capture("13-tema-amanecer") }
            withApp(ThemeChoice("bosque", null)) { capture("14-tema-bosque") }
            step("15-alarma-sonando") {
                val intent = Intent(context, AlarmActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(AlarmScheduler.EXTRA_ID, ALARM_WORK)
                ActivityScenario.launch<AlarmActivity>(intent).use {
                    composeRule.waitForIdle()
                    capture("15-alarma-sonando")
                }
                return@step false
            }
        } finally {
            cleanUp()
        }
        Log.i(TAG, "capturadas=${captured.joinToString()} fallidas=${failed.joinToString()}")
        assertTrue("No se generó ninguna captura. Fallidas: $failed", captured.isNotEmpty())
    }

    /** Ejecuta una acción y captura la pantalla resultante, sin detener el recorrido si falla. */
    private fun step(name: String, action: () -> Any?) {
        runCatching {
            val result = action()
            if (result != false) capture(name)
        }.onFailure {
            failed += name
            Log.e(TAG, "captura_fallida $name", it)
        }
    }

    /** Abre la app con el tema predeterminado; un fallo aquí no detiene el recorrido. */
    private fun session(block: () -> Unit) {
        runCatching { withApp(ThemeChoice.DEFAULT, block) }
            .onFailure { Log.e(TAG, "sesion_fallida", it) }
    }

    private fun withApp(theme: ThemeChoice, block: () -> Unit) {
        val prefs = context.getSharedPreferences(DESPIERTA_UI_PREFS, Context.MODE_PRIVATE)
        saveThemeChoice(prefs, theme)
        prefs.edit().commit()
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_DISABLE_LIVE_CLOCK_FOR_TESTS, true)
            .putExtra(MainActivity.EXTRA_FIXED_NOW_MILLIS_FOR_TESTS, FIXED_NOW_MILLIS)
        ActivityScenario.launch<MainActivity>(intent).use {
            composeRule.waitForIdle()
            block()
        }
    }

    private fun closeDialog() {
        runCatching { Espresso.pressBack() }
        composeRule.waitForIdle()
    }

    private fun capture(name: String) {
        // Cierra cualquier diálogo del sistema que haya aparecido encima.
        runCatching {
            instrumentation.uiAutomation
                .executeShellCommand("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
                .close()
        }
        composeRule.waitForIdle()
        // Deja que se dibuje el último fotograma antes de capturar.
        Thread.sleep(SETTLE_MILLIS)
        var bitmap: Bitmap? = null
        repeat(ATTEMPTS) {
            if (bitmap == null) {
                bitmap = instrumentation.uiAutomation.takeScreenshot()
                if (bitmap == null) Thread.sleep(RETRY_MILLIS)
            }
        }
        val image = bitmap ?: throw AssertionError("UiAutomation no devolvió la captura $name")
        FileOutputStream(File(outputDir, "$name.png")).use { out ->
            check(image.compress(Bitmap.CompressFormat.PNG, 100, out)) { "No se pudo guardar $name" }
        }
        captured += name
    }

    /** Datos ficticios, sin información personal. */
    private fun seedDemoData() {
        val store = AlarmStore(context)
        val scheduler = AlarmScheduler(context)
        store.getAll().forEach {
            scheduler.cancel(it.id)
            store.delete(it.id)
        }
        val weekdays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        store.save(
            store.newAlarm().copy(
                id = ALARM_WORK, hour = 6, minute = 30, label = "Trabajo",
                days = weekdays, snoozeMinutes = 10, enabled = true
            )
        )
        store.save(
            store.newAlarm().copy(
                id = ALARM_SHIFT, hour = 5, minute = 15, label = "Turno rotativo",
                shiftPattern = ShiftPattern.from(LocalDate.of(2026, 10, 20), 14, 7),
                snoozeMinutes = 5, enabled = true
            )
        )
        store.save(
            store.newAlarm().copy(
                id = ALARM_GYM, hour = 7, minute = 45, label = "Gimnasio",
                days = setOf(Calendar.SATURDAY), snoozeMinutes = 15, enabled = false
            )
        )
        store.save(
            store.newAlarm().copy(
                id = ALARM_ONCE, hour = 21, minute = 30, label = "Preparar mochila",
                mathChallengeEnabled = true, snoozeMinutes = 5, enabled = true
            )
        )
        val prefs = context.getSharedPreferences(DESPIERTA_UI_PREFS, Context.MODE_PRIVATE)
        saveSleepSchedule(prefs, SleepSchedule())
        saveNotificationPrefs(prefs, NotificationPrefs(enabled = true))
        prefs.edit().commit()
    }

    private fun cleanUp() {
        runCatching {
            val store = AlarmStore(context)
            val scheduler = AlarmScheduler(context)
            listOf(ALARM_WORK, ALARM_SHIFT, ALARM_GYM, ALARM_ONCE).forEach {
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
        const val TAG = "DespiertaCapturas"
        const val ALARM_WORK = 9001
        const val ALARM_SHIFT = 9002
        const val ALARM_GYM = 9003
        const val ALARM_ONCE = 9004
        const val SETTLE_MILLIS = 700L
        const val RETRY_MILLIS = 600L
        const val ATTEMPTS = 5

        /** Martes 27 de octubre de 2026, 9:42 PM en la zona del emulador (UTC). */
        val FIXED_NOW_MILLIS: Long =
            LocalDateTime.of(2026, 10, 27, 21, 42).toInstant(ZoneOffset.UTC).toEpochMilli()
    }
}
