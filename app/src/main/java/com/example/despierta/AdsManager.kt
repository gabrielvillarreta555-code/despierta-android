package com.example.despierta

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Publicidad experimental (rama experimental/admob). Aislada del motor de alarmas:
 * solo se inicia desde MainActivity y nunca desde AlarmReceiver, AlarmService, BootReceiver,
 * ReminderReceiver ni AlarmActivity. Con identificadores de prueba de Google mientras
 * BuildConfig.ADS_TEST_IDS sea verdadero.
 */
internal object AdsManager {
    private const val TAG = "DespiertaAnuncios"
    private val started = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Verdadero cuando el SDK está iniciado y hay consentimiento (o no hace falta). */
    var canShowAds by mutableStateOf(false)
        private set

    /** Eventos para diagnóstico y pruebas (sin datos personales). */
    val events = CopyOnWriteArrayList<String>()

    fun record(event: String) {
        events += event
        Log.i(TAG, event)
    }

    /**
     * Pide (si corresponde por región) el consentimiento con la herramienta de Google (UMP) y
     * después inicia el SDK en un hilo de fondo. No hace nada si la publicidad está desactivada.
     */
    fun start(activity: Activity, disabledForTests: Boolean) {
        if (!BuildConfig.ADS_ENABLED || disabledForTests) return
        if (!started.compareAndSet(false, true)) return
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                record("consent_updated status=${consent.consentStatus}")
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) record("consent_form_error code=${formError.errorCode}")
                    if (consent.canRequestAds()) initialize(activity.applicationContext)
                }
            },
            { error ->
                record("consent_update_error code=${error.errorCode}")
                // Con los identificadores de prueba no existe un mensaje de privacidad configurado
                // en AdMob, así que la actualización puede fallar: en esa compilación de prueba se
                // continúa para poder medir. Con identificadores reales solo se sigue si el SDK de
                // consentimiento lo permite.
                if (consent.canRequestAds() || BuildConfig.ADS_TEST_IDS) initialize(activity.applicationContext)
            }
        )
        // Si en una sesión anterior ya había consentimiento, no hace falta esperar.
        if (consent.canRequestAds()) initialize(activity.applicationContext)
    }

    private fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        Thread {
            runCatching {
                MobileAds.initialize(context) {
                    record("sdk_initialized")
                    mainHandler.post { canShowAds = true }
                }
            }.onFailure {
                record("sdk_init_failed ${it.javaClass.simpleName}")
            }
        }.start()
    }
}

/**
 * Banner de tamaño fijo (320×50) al final de una lista. Mientras carga reserva su alto; si no
 * hay conexión o falla, desaparece sin dejar hueco. Lleva la etiqueta "Publicidad".
 */
@Composable
internal fun BannerAd(modifier: Modifier = Modifier) {
    if (!AdsManager.canShowAds) return
    val palette = LocalDespiertaPalette.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var loaded by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val adView = remember {
        AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(AdSize.BANNER)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loaded = true
                    failed = false
                    AdsManager.record("banner_loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    failed = !loaded
                    AdsManager.record("banner_failed code=${error.code}")
                }
            }
            loadAd(AdRequest.Builder().build())
            AdsManager.record("banner_requested")
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }
    if (failed) return
    Column(
        modifier.fillMaxWidth().testTag("ad_banner"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loaded) {
            Text(
                "Publicidad",
                fontSize = 11.sp,
                color = palette.textSoft.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        AndroidView(factory = { adView }, modifier = Modifier.width(320.dp).height(50.dp))
    }
}
