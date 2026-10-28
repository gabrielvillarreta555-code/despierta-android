package com.example.despierta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

/**
 * Comprueba si el cielo de Alarmas cambia con el tiempo (estrellas que titilan): dos fotogramas
 * separados 1,7 s con las animaciones del sistema activadas y el reloj en vivo.
 *
 * Va en una clase sin regla de Compose a propósito: la regla de pruebas de Compose controla el
 * reloj de fotogramas y congela las animaciones mientras la prueba duerme, así que con ella los
 * dos fotogramas saldrían siempre iguales aunque la animación funcione.
 */
class StarsAnimationCheck {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val outputDir = File(context.filesDir, "validacion")
    private val label: String = InstrumentationRegistry.getArguments().getString("etiqueta") ?: "sin-etiqueta"

    @Test
    fun skyChangesOverTime() {
        outputDir.mkdirs()
        val previous = shell("settings get global animator_duration_scale").trim()
        shell("settings put global animator_duration_scale 1")
        try {
            val prefs = context.getSharedPreferences(DESPIERTA_UI_PREFS, Context.MODE_PRIVATE)
            saveThemeChoice(prefs, ThemeChoice.DEFAULT)
            prefs.edit().commit()
            val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActivityScenario.launch<MainActivity>(intent).use {
                Thread.sleep(6_000)
                save("anim-a", screenshot())
                Thread.sleep(1_700)
                save("anim-b", screenshot())
            }
        } finally {
            shell("settings put global animator_duration_scale ${previous.ifBlank { "0" }}")
        }
    }

    private fun screenshot(): Bitmap {
        repeat(10) {
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            if (bitmap != null) return bitmap
            Thread.sleep(500)
        }
        throw AssertionError("No se obtuvo la captura")
    }

    private fun save(name: String, bitmap: Bitmap) {
        FileOutputStream(File(outputDir, "$label-$name.png")).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        Log.i("DespiertaValidacion", "capturada $label-$name")
    }

    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().readText()
        }
}
