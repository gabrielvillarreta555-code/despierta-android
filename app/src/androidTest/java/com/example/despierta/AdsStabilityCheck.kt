package com.example.despierta

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ¿Sobrevive una alarma sonando si Google Play Services se reinicia?
 *
 * Con el SDK de anuncios conectado a Play Services, Android cierra la app cuando Play Services
 * muere ("depends on provider"). Cada escenario fuerza ese reinicio con la alarma sonando y
 * comprueba que el proceso y la alarma siguen vivos. El script ejecuta cada escenario en una
 * instrumentación aparte: si uno cierra la app, se ve como "Process crashed" sin tapar al resto.
 */
@RunWith(AndroidJUnit4::class)
class AdsStabilityCheck {
    @get:Rule
    val notificationPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val store = AlarmStore(context)
    private var alarmId = -1

    @Before
    fun setUp() {
        AlarmService.ensureNotificationChannel(context)
        store.getAll().forEach {
            AlarmScheduler(context).cancel(it.id)
            store.delete(it.id)
        }
    }

    @After
    fun cleanUp() {
        if (alarmId > 0) {
            runCatching {
                context.startService(
                    Intent(context, AlarmService::class.java)
                        .setAction(AlarmService.ACTION_STOP)
                        .putExtra(AlarmScheduler.EXTRA_ID, alarmId)
                )
            }
            AlarmScheduler(context).cancel(alarmId)
            store.delete(alarmId)
        }
    }

    /** La app la despierta solo la alarma: nunca se abrió la pantalla principal. */
    @Test
    fun soloAlarma() {
        ringThenRestartPlayServices("solo_alarma")
    }

    /** La pantalla principal está abierta con el SDK de anuncios iniciado. */
    @Test
    fun pantallaAbierta() {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            waitForSdk()
            ringThenRestartPlayServices("pantalla_abierta")
        }
    }

    /** Se abrió la pantalla principal (SDK iniciado), se cerró y después suena una alarma. */
    @Test
    fun pantallaCerrada() {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use { waitForSdk() }
        Thread.sleep(2_000)
        ringThenRestartPlayServices("pantalla_cerrada")
    }

    private fun waitForSdk() {
        val deadline = System.currentTimeMillis() + 40_000L
        while (System.currentTimeMillis() < deadline && AdsManager.events.none { it.startsWith("sdk_") }) {
            Thread.sleep(250)
        }
        Log.i(TAG, "sdk_eventos=${AdsManager.events}")
    }

    private fun ringThenRestartPlayServices(scenario: String) {
        val alarm = store.newAlarm().copy(
            label = "Prueba de estabilidad",
            enabled = false,
            vibrate = false,
            soundUri = "content://invalid/despierta-test",
            snoozedUntilMillis = System.currentTimeMillis() + 5_000L
        )
        alarmId = alarm.id
        store.save(alarm)
        AlarmScheduler(context).schedule(alarm)
        assertTrue("La alarma no empezó a sonar", waitForRinging(40_000L))
        val gmsBefore = shell("pidof com.google.android.gms").trim()
        val myPid = Process.myPid()
        Log.i(TAG, "escenario=$scenario sonando=true pid=$myPid gms_antes=$gmsBefore")
        // Reinicio forzado de Google Play Services, como en una actualización.
        shell("am force-stop com.google.android.gms")
        Thread.sleep(12_000)
        val gmsAfter = shell("pidof com.google.android.gms").trim()
        val stillRinging = isRinging()
        Log.i(TAG, "escenario=$scenario sobrevivio=true pid=${Process.myPid()} gms_despues=$gmsAfter sigue_sonando=$stillRinging")
        assertTrue("La alarma dejó de sonar tras el reinicio de Play Services ($scenario)", stillRinging)
    }

    private fun waitForRinging(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isRinging()) return true
            Thread.sleep(250)
        }
        return false
    }

    private fun isRinging(): Boolean =
        context.getSystemService(NotificationManager::class.java)
            .activeNotifications.any { it.id == AlarmService.NOTIFICATION_ID }

    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().readText()
        }

    private companion object {
        const val TAG = "DespiertaAnuncios"
    }
}
