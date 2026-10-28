package com.example.despierta

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Impacto de la publicidad experimental (anuncios de PRUEBA de Google):
 *  1. memoria del proceso sin anuncios y con el banner cargado,
 *  2. una alarma suena mientras el banner se está cargando,
 *  3. sin Internet la app no falla, el banner no aparece y las alarmas siguen sonando.
 * Sin regla de Compose: con ella las animaciones y la carga quedarían congeladas.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AdsImpactCheck {
    @get:Rule
    val notificationPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val store = AlarmStore(context)
    private var alarmId = -1

    @Before
    fun setUp() {
        AlarmService.ensureNotificationChannel(context)
        // Lista vacía: el banner queda a la vista (está al final de la lista).
        store.getAll().forEach {
            AlarmScheduler(context).cancel(it.id)
            store.delete(it.id)
        }
    }

    @After
    fun cleanUp() {
        shell("svc wifi enable")
        shell("svc data enable")
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

    @Test
    fun a_memoryWithoutAndWithAds() {
        val without = launch(disableAds = true).use {
            Thread.sleep(8_000)
            pssKb()
        }
        val with = launch(disableAds = false).use {
            waitForEvent("banner_loaded", "banner_failed", timeoutMs = 45_000)
            Thread.sleep(3_000)
            saveScreenshot("anuncio-alarmas-sin-alarmas")
            pssKb()
        }
        val loaded = AdsManager.events.any { it.startsWith("banner_loaded") }
        Log.i(TAG, "memoria_pss_kb sin_anuncios=$without con_anuncios=$with diferencia=${with - without} banner_cargado=$loaded eventos=${AdsManager.events}")
        assertTrue("No se pudo medir la memoria", without > 0 && with > 0)
    }

    @Test
    fun b_alarmRingsWhileBannerLoads() {
        launch(disableAds = false).use {
            // La alarma se programa en cuanto se abre la pantalla con el banner cargándose.
            val fired = scheduleAndWaitForAlarm("Prueba con anuncios")
            Log.i(TAG, "alarma_con_anuncios sono=$fired eventos=${AdsManager.events}")
            assertTrue("La alarma no sonó mientras se cargaba el banner", fired)
        }
    }

    @Test
    fun c_offlineNoCrashAndAlarmRings() {
        shell("svc wifi disable")
        shell("svc data disable")
        Thread.sleep(2_000)
        val before = AdsManager.events.size
        launch(disableAds = false).use { scenario ->
            Thread.sleep(12_000)
            assertEquals("La pantalla principal se cerró sin Internet", Lifecycle.State.RESUMED, scenario.state)
            val fired = scheduleAndWaitForAlarm("Prueba sin Internet")
            val newEvents = AdsManager.events.drop(before)
            Log.i(TAG, "sin_internet sono=$fired eventos_nuevos=$newEvents")
            assertTrue("La alarma no sonó sin Internet", fired)
            assertTrue("Sin Internet no debería cargarse un banner", newEvents.none { it.startsWith("banner_loaded") })
        }
    }

    private fun launch(disableAds: Boolean): ActivityScenario<MainActivity> {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_DISABLE_ADS_FOR_TESTS, disableAds)
        return ActivityScenario.launch(intent)
    }

    private fun scheduleAndWaitForAlarm(label: String): Boolean {
        val alarm = store.newAlarm().copy(
            label = label,
            enabled = false,
            vibrate = false,
            soundUri = "content://invalid/despierta-test",
            snoozedUntilMillis = System.currentTimeMillis() + 6_000L
        )
        alarmId = alarm.id
        store.save(alarm)
        AlarmScheduler(context).schedule(alarm)
        val manager = context.getSystemService(NotificationManager::class.java)
        val deadline = System.currentTimeMillis() + 40_000L
        while (System.currentTimeMillis() < deadline) {
            if (manager.activeNotifications.any { it.id == AlarmService.NOTIFICATION_ID }) {
                context.startService(
                    Intent(context, AlarmService::class.java)
                        .setAction(AlarmService.ACTION_STOP)
                        .putExtra(AlarmScheduler.EXTRA_ID, alarm.id)
                )
                return true
            }
            Thread.sleep(250)
        }
        return false
    }

    private fun waitForEvent(vararg prefixes: String, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (AdsManager.events.any { e -> prefixes.any { e.startsWith(it) } }) return
            Thread.sleep(250)
        }
    }

    private fun pssKb(): Int {
        val out = shell("dumpsys meminfo ${context.packageName}")
        // Formatos: "TOTAL PSS:  54321 ..." (Android 14) o "TOTAL  54321 ..." (anteriores).
        val line = out.lines().firstOrNull { Regex("^\\s*TOTAL( PSS)?:?\\s+\\d+").containsMatchIn(it) }
        return line?.let { Regex("(\\d+)").find(it)?.value?.toIntOrNull() } ?: -1
    }

    private fun saveScreenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = java.io.File(context.filesDir, "anuncios").apply { mkdirs() }
        java.io.FileOutputStream(java.io.File(dir, "$name.png")).use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().readText()
        }

    private companion object {
        const val TAG = "DespiertaAnuncios"
    }
}
