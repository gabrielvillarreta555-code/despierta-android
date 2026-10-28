package com.example.despierta

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Paleta completa de la interfaz. Cambiar de fondo o de colores solo reemplaza esta
 * estructura; ninguna pantalla guarda colores fijos.
 */
internal data class DespiertaPalette(
    val id: String,
    val name: String,
    val sky: List<Color>,
    val hero: List<Color>,
    val card: List<Color>,
    val active: List<Color>,
    val hill: Color,
    val accent: Color,
    val navIconColor: Color? = null,
    val cardIconColor: Color? = null,
    val buttonColor: Color? = null,
    /** 2.9: fondos sin tinte (Negro, Grafito): textos y bordes grises en vez de lavanda. */
    val neutral: Boolean = false
) {
    val accentSoft: Color get() = lerp(accent, Color.White, 0.45f)
    val onAccent: Color get() = contrastOn(accent)
    val textPrimary: Color get() = if (neutral) Color(0xFFF4F5F7) else Color(0xFFF6F2FF)
    val textSoft: Color get() = if (neutral) Color(0xFFBFC3CA) else Color(0xFFC3BBDD)
    val outline: Color get() = if (neutral) Color(0xFF4A4E56) else Color(0xFF4B3F78)
    /** Iconos de la barra inferior sin seleccionar. */
    val navIcon: Color get() = navIconColor ?: textSoft
    /** Pestaña seleccionada: el color elegido o el acento. */
    val navIconSelected: Color get() = navIconColor ?: accent
    val cardIcon: Color get() = cardIconColor ?: accent
    val button: Color get() = buttonColor ?: accent
    val onButton: Color get() = contrastOn(button)
    val skyBrush: Brush get() = Brush.verticalGradient(sky)
    val heroBrush: Brush get() = Brush.verticalGradient(hero)
    val cardBrush: Brush get() = Brush.verticalGradient(card)
    val activeBrush: Brush get() = Brush.verticalGradient(active)
    val accentBrush: Brush get() = Brush.linearGradient(listOf(accentSoft, accent))
}

/** Texto oscuro sobre colores claros y blanco sobre colores oscuros, para mantener el contraste. */
internal fun contrastOn(color: Color): Color =
    if (color.luminance() > 0.40f) Color(0xFF16112A) else Color.White

/** Contraste WCAG entre dos colores (1 a 21). */
internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/** Fondos prediseñados. El primero es el predeterminado. */
internal val DespiertaPresets: List<DespiertaPalette> = listOf(
    DespiertaPalette(
        id = "noche",
        name = "Noche",
        sky = listOf(Color(0xFF3B2B66), Color(0xFF1E1640), Color(0xFF15102B), Color(0xFF0D0A1C)),
        hero = listOf(Color(0xFF6B4E9E), Color(0xFF3A2A62), Color(0xFF1C1535)),
        card = listOf(Color(0xFF2A2150), Color(0xFF1C1634)),
        active = listOf(Color(0xFF3A2C6E), Color(0xFF231B44)),
        hill = Color(0xFF120C26),
        accent = Color(0xFFC9A7FF)
    ),
    DespiertaPalette(
        id = "amanecer",
        name = "Amanecer",
        sky = listOf(Color(0xFF5A2E4E), Color(0xFF2E1A3F), Color(0xFF1E1433), Color(0xFF140D22)),
        hero = listOf(Color(0xFF9A4F6E), Color(0xFF5A2F5E), Color(0xFF2A1A3C)),
        card = listOf(Color(0xFF3A2440), Color(0xFF2A1B33)),
        active = listOf(Color(0xFF4A2A4E), Color(0xFF2F1C36)),
        hill = Color(0xFF1A0E22),
        accent = Color(0xFFFFB38A)
    ),
    DespiertaPalette(
        id = "bosque",
        name = "Bosque",
        sky = listOf(Color(0xFF1F4A48), Color(0xFF14322F), Color(0xFF0E2320), Color(0xFF081615)),
        hero = listOf(Color(0xFF2F6E66), Color(0xFF1F4D48), Color(0xFF0F2A27)),
        card = listOf(Color(0xFF1D3D3A), Color(0xFF15302D)),
        active = listOf(Color(0xFF24504B), Color(0xFF183A36)),
        hill = Color(0xFF071512),
        accent = Color(0xFF8EE3BF)
    ),
    // 2.9: negro casi puro (ahorra batería en pantallas OLED) con acento plata.
    DespiertaPalette(
        id = "negro",
        name = "Negro",
        sky = listOf(Color(0xFF1A1A1F), Color(0xFF0E0E12), Color(0xFF07070A), Color(0xFF000000)),
        hero = listOf(Color(0xFF3A3A44), Color(0xFF202027), Color(0xFF101014)),
        card = listOf(Color(0xFF1C1C21), Color(0xFF121215)),
        active = listOf(Color(0xFF2A2A31), Color(0xFF18181C)),
        hill = Color(0xFF050506),
        accent = Color(0xFFE4E6EE),
        neutral = true
    ),
    // 2.9: gris grafito con acento gris azulado.
    DespiertaPalette(
        id = "grafito",
        name = "Grafito",
        sky = listOf(Color(0xFF4A4F57), Color(0xFF2E3238), Color(0xFF22252A), Color(0xFF17191C)),
        hero = listOf(Color(0xFF6B717B), Color(0xFF454A52), Color(0xFF2A2D32)),
        card = listOf(Color(0xFF3A3E45), Color(0xFF2C2F35)),
        active = listOf(Color(0xFF474C54), Color(0xFF33373D)),
        hill = Color(0xFF121417),
        accent = Color(0xFFB8C6DA),
        neutral = true
    )
)

/**
 * Colores que se pueden elegir. Todos son claros, así que contrastan con cualquiera de los
 * fondos oscuros, y el texto sobre ellos se oscurece automáticamente (contrastOn).
 */
internal val DespiertaAccents: List<Pair<String, Color>> = listOf(
    "Lavanda" to Color(0xFFC9A7FF),
    "Azul" to Color(0xFF82B1FF),
    "Rosa" to Color(0xFFF48FB1),
    "Naranja" to Color(0xFFFFB74D),
    "Menta" to Color(0xFF8EE3BF),
    "Blanco" to Color(0xFFF2F0F7),
    "Plata" to Color(0xFFB8C6DA)
)

internal enum class IconStyle(val label: String) {
    MINIMAL("Minimalista"),
    ROUNDED("Redondeado"),
    SOFT("Relieve suave")
}

internal enum class MotionLevel(val label: String, val factor: Float) {
    OFF("Sin animaciones", 0f),
    SUBTLE("Discretas", 0.6f),
    NORMAL("Normales", 1f)
}

internal const val DESPIERTA_UI_PREFS = "despierta_ui"
private const val PREF_THEME_PRESET = "theme_preset"
private const val PREF_THEME_ACCENT = "theme_accent"
private const val PREF_THEME_NAV_ICON = "theme_nav_icon"
private const val PREF_THEME_CARD_ICON = "theme_card_icon"
private const val PREF_THEME_BUTTON = "theme_button"
private const val PREF_THEME_ICON_STYLE = "theme_icon_style"
private const val PREF_THEME_LARGE_TEXT = "theme_large_text"
private const val PREF_THEME_MOTION = "theme_motion"
private const val PREF_THEME_SCENE = "theme_scene"
private const val PREF_THEME_FONT = "theme_font"
private const val PREF_THEME_ICON_SIZE = "theme_icon_size"
private const val PREF_THEME_ICON_SHAPE = "theme_icon_shape"
private const val PREF_THEME_REFRESH = "theme_refresh"

/** Apariencia guardada. Los colores nulos usan los del fondo elegido. */
internal data class ThemeChoice(
    val presetId: String,
    val accent: Color?,
    val navIconColor: Color? = null,
    val cardIconColor: Color? = null,
    val buttonColor: Color? = null,
    val iconStyle: IconStyle = IconStyle.ROUNDED,
    val largeText: Boolean = false,
    val motion: MotionLevel = MotionLevel.SUBTLE,
    val scene: SceneStyle = SceneStyle.BOSQUE,
    val font: FontChoice = FontChoice.SISTEMA,
    val iconSize: IconSizeChoice = IconSizeChoice.NORMAL,
    val iconShape: IconShapeChoice = IconShapeChoice.REDONDEADA,
    val refresh: RefreshChoice = RefreshChoice.MAXIMA
) {
    fun toPalette(): DespiertaPalette {
        val base = DespiertaPresets.firstOrNull { it.id == presetId } ?: DespiertaPresets.first()
        return base.copy(
            accent = accent ?: base.accent,
            navIconColor = navIconColor,
            cardIconColor = cardIconColor,
            buttonColor = buttonColor
        )
    }

    companion object {
        val DEFAULT = ThemeChoice(DespiertaPresets.first().id, null)
    }
}

private fun SharedPreferences.colorOrNull(key: String): Color? =
    if (contains(key)) Color(getInt(key, 0)) else null

private fun SharedPreferences.Editor.putColor(key: String, color: Color?) {
    if (color == null) remove(key) else putInt(key, color.toArgb())
}

internal fun loadThemeChoice(prefs: SharedPreferences): ThemeChoice {
    val preset = prefs.getString(PREF_THEME_PRESET, DespiertaPresets.first().id)
        ?: DespiertaPresets.first().id
    val defaults = ThemeChoice.DEFAULT
    return ThemeChoice(
        presetId = preset,
        accent = prefs.colorOrNull(PREF_THEME_ACCENT),
        navIconColor = prefs.colorOrNull(PREF_THEME_NAV_ICON),
        cardIconColor = prefs.colorOrNull(PREF_THEME_CARD_ICON),
        buttonColor = prefs.colorOrNull(PREF_THEME_BUTTON),
        iconStyle = IconStyle.entries.firstOrNull {
            it.name == prefs.getString(PREF_THEME_ICON_STYLE, null)
        } ?: defaults.iconStyle,
        largeText = prefs.getBoolean(PREF_THEME_LARGE_TEXT, defaults.largeText),
        motion = MotionLevel.entries.firstOrNull {
            it.name == prefs.getString(PREF_THEME_MOTION, null)
        } ?: defaults.motion,
        scene = SceneStyle.entries.firstOrNull { it.name == prefs.getString(PREF_THEME_SCENE, null) } ?: defaults.scene,
        font = FontChoice.entries.firstOrNull { it.name == prefs.getString(PREF_THEME_FONT, null) } ?: defaults.font,
        iconSize = IconSizeChoice.entries.firstOrNull { it.name == prefs.getString(PREF_THEME_ICON_SIZE, null) } ?: defaults.iconSize,
        iconShape = IconShapeChoice.entries.firstOrNull { it.name == prefs.getString(PREF_THEME_ICON_SHAPE, null) } ?: defaults.iconShape,
        refresh = RefreshChoice.entries.firstOrNull { it.name == prefs.getString(PREF_THEME_REFRESH, null) } ?: defaults.refresh
    )
}

internal fun saveThemeChoice(prefs: SharedPreferences, choice: ThemeChoice) {
    val editor = prefs.edit()
    editor.putString(PREF_THEME_PRESET, choice.presetId)
    editor.putColor(PREF_THEME_ACCENT, choice.accent)
    editor.putColor(PREF_THEME_NAV_ICON, choice.navIconColor)
    editor.putColor(PREF_THEME_CARD_ICON, choice.cardIconColor)
    editor.putColor(PREF_THEME_BUTTON, choice.buttonColor)
    editor.putString(PREF_THEME_ICON_STYLE, choice.iconStyle.name)
    editor.putBoolean(PREF_THEME_LARGE_TEXT, choice.largeText)
    editor.putString(PREF_THEME_MOTION, choice.motion.name)
    editor.putString(PREF_THEME_SCENE, choice.scene.name)
    editor.putString(PREF_THEME_FONT, choice.font.name)
    editor.putString(PREF_THEME_ICON_SIZE, choice.iconSize.name)
    editor.putString(PREF_THEME_ICON_SHAPE, choice.iconShape.name)
    editor.putString(PREF_THEME_REFRESH, choice.refresh.name)
    editor.apply()
}

/** Opciones de interfaz que no son colores. */
internal data class UiOptions(
    val iconStyle: IconStyle = IconStyle.ROUNDED,
    /** 0 desactiva las animaciones; 1 es la duración completa. */
    val motionFactor: Float = MotionLevel.SUBTLE.factor,
    val systemReducedMotion: Boolean = false,
    val iconScale: Float = 1f,
    val iconCornerFraction: Float = 0.32f,
    val scene: SceneStyle = SceneStyle.BOSQUE
) {
    /** Duración de una animación, o 0 si el usuario o Android piden reducir el movimiento. */
    fun millis(base: Int): Int = (base * motionFactor).toInt().coerceAtLeast(0)
}

internal val LocalDespiertaPalette = staticCompositionLocalOf { DespiertaPresets.first() }
internal val LocalUiOptions = staticCompositionLocalOf { UiOptions() }

/** Android permite quitar las animaciones (Accesibilidad → Quitar animaciones). */
internal fun systemReducesMotion(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)

@Composable
internal fun DespiertaTheme(
    palette: DespiertaPalette = DespiertaPresets.first(),
    choice: ThemeChoice = ThemeChoice.DEFAULT,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember { systemReducesMotion(context) }
    val options = UiOptions(
        iconStyle = choice.iconStyle,
        motionFactor = if (reducedMotion) 0f else choice.motion.factor,
        systemReducedMotion = reducedMotion,
        iconScale = choice.iconSize.scale,
        iconCornerFraction = choice.iconShape.cornerFraction,
        scene = choice.scene
    )
    val density = LocalDensity.current
    val scaledDensity = if (choice.largeText) {
        Density(density.density, density.fontScale * 1.15f)
    } else {
        density
    }
    CompositionLocalProvider(
        LocalDespiertaPalette provides palette,
        LocalUiOptions provides options,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            typography = typographyFor(choice.font.family),
            colorScheme = darkColorScheme(
                primary = palette.accent,
                onPrimary = palette.onAccent,
                primaryContainer = palette.card.first(),
                onPrimaryContainer = palette.accentSoft,
                secondary = Color(0xFF82B1FF),
                onSecondary = Color(0xFF0F1B33),
                secondaryContainer = palette.active.first(),
                onSecondaryContainer = palette.accentSoft,
                background = palette.sky.last(),
                surface = palette.card.last(),
                surfaceVariant = palette.active.first(),
                surfaceContainer = palette.card.first(),
                surfaceContainerHigh = palette.active.first(),
                onSurface = palette.textPrimary,
                onSurfaceVariant = palette.textSoft,
                outline = palette.outline,
                error = Color(0xFFFFB4AB),
                errorContainer = Color(0xFF5B2532),
                onErrorContainer = Color(0xFFFFDAD6)
            )
        ) {
            // Surface transparente con contentColor explícito: todo texto hereda el color claro,
            // también dentro de diálogos.
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                contentColor = palette.textPrimary
            ) {
                content()
            }
        }
    }
}

