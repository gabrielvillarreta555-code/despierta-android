package com.example.despierta

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Personalización añadida en la 2.7: paisaje, tipo de letra, tamaño y forma de los iconos y
// fluidez de pantalla. Todo se guarda con el resto de la apariencia (ThemeChoice).

/** Paisaje de la cabecera de Alarmas. Bosque es el diseño original. */
internal enum class SceneStyle(val label: String) {
    BOSQUE("Bosque"),
    MONTANA("Montaña"),
    ESTRELLAS("Noche estrellada")
}

/** Tipos de letra incluidos en la app (licencia OFL, ver assets/licenses). */
internal enum class FontChoice(val label: String) {
    SISTEMA("Sistema"),
    MODERNA("Moderna"),
    REDONDEADA("Redondeada"),
    CLASICA("Clásica");

    /** Nulo = fuente del sistema. */
    val family: FontFamily?
        get() = when (this) {
            SISTEMA -> null
            MODERNA -> OutfitFamily
            REDONDEADA -> NunitoFamily
            CLASICA -> LoraFamily
        }
}

internal enum class IconSizeChoice(val label: String, val scale: Float) {
    PEQUENO("Pequeños", 0.86f),
    NORMAL("Normales", 1f),
    GRANDE("Grandes", 1.16f)
}

/** Forma del fondo de los iconos de las tarjetas (fracción del tamaño usada como radio). */
internal enum class IconShapeChoice(val label: String, val cornerFraction: Float) {
    REDONDEADA("Redondeada", 0.32f),
    CIRCULO("Círculo", 0.5f),
    CUADRADA("Cuadrada", 0.18f)
}

/** Frecuencia de pantalla que pide la app. */
internal enum class RefreshChoice(val label: String, val detail: String) {
    MAXIMA("Máxima", "La más alta del teléfono (90 o 120 Hz si existe)"),
    AHORRO("60 Hz", "Menos fluida, gasta menos batería"),
    SISTEMA("Android decide", "Despierta no pide ninguna frecuencia")
}

private val OutfitFamily = FontFamily(
    Font(R.font.outfit_300, FontWeight.Light),
    Font(R.font.outfit_400, FontWeight.Normal),
    Font(R.font.outfit_500, FontWeight.Medium),
    Font(R.font.outfit_600, FontWeight.SemiBold),
    Font(R.font.outfit_700, FontWeight.Bold),
    Font(R.font.outfit_800, FontWeight.ExtraBold)
)

private val NunitoFamily = FontFamily(
    Font(R.font.nunito_300, FontWeight.Light),
    Font(R.font.nunito_400, FontWeight.Normal),
    Font(R.font.nunito_500, FontWeight.Medium),
    Font(R.font.nunito_600, FontWeight.SemiBold),
    Font(R.font.nunito_700, FontWeight.Bold),
    Font(R.font.nunito_800, FontWeight.ExtraBold)
)

private val LoraFamily = FontFamily(
    Font(R.font.lora_400, FontWeight.Normal),
    Font(R.font.lora_500, FontWeight.Medium),
    Font(R.font.lora_600, FontWeight.SemiBold),
    Font(R.font.lora_700, FontWeight.Bold)
)

/** Tipografía de Material con todas las escalas en la familia elegida. */
internal fun typographyFor(family: FontFamily?): Typography {
    val base = Typography()
    if (family == null) return base
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family)
    )
}
