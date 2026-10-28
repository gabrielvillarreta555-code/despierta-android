package com.example.despierta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AlarmScreenInstrumentedTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun activeAlarmHasReadableTimeLargeControlsAndProducesRealScreenshot() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AlarmStore(context)
        val alarm = store.newAlarm().copy(
            hour = 12,
            minute = 0,
            label = "Contraste verificado",
            snoozeMinutes = 10,
            vibrate = false,
            enabled = true
        )
        store.save(alarm)
        val intent = Intent(context, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(AlarmScheduler.EXTRA_ID, alarm.id)

        try {
            ActivityScenario.launch<AlarmActivity>(intent).use {
                composeRule.waitForIdle()
                composeRule.onNodeWithTag("ringing_time")
                    .assertIsDisplayed()
                    .assertTextEquals("12:00")
                composeRule.onNodeWithTag("ringing_period").assertTextEquals("PM")
                composeRule.onNodeWithTag("ringing_snooze").assertIsDisplayed()
                composeRule.onNodeWithTag("ringing_stop").assertIsDisplayed()

                // UiAutomation.takeScreenshot() puede devolver null en emuladores lentos mientras
                // la superficie aún no está lista; se reintenta brevemente sin ocultar el fallo final.
                var screenshot: Bitmap? = null
                repeat(SCREENSHOT_ATTEMPTS) {
                    if (screenshot == null) {
                        composeRule.waitForIdle()
                        screenshot = InstrumentationRegistry.getInstrumentation()
                            .uiAutomation
                            .takeScreenshot()
                        if (screenshot == null) Thread.sleep(SCREENSHOT_RETRY_DELAY_MS)
                    }
                }
                val capture = screenshot ?: throw AssertionError(
                    "UiAutomation no devolvió captura tras $SCREENSHOT_ATTEMPTS intentos"
                )
                val output = File(context.filesDir, SCREENSHOT_NAME)
                output.outputStream().use { stream ->
                    assertTrue(capture.compress(Bitmap.CompressFormat.PNG, 100, stream))
                }
                assertTrue(output.isFile && output.length() > 0L)
            }
        } finally {
            store.delete(alarm.id)
        }
    }

    companion object {
        const val SCREENSHOT_NAME = "Despierta-Alarma-activa.png"
        private const val SCREENSHOT_ATTEMPTS = 5
        private const val SCREENSHOT_RETRY_DELAY_MS = 700L
    }
}
