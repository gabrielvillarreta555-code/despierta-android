package com.example.despierta

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Paleta completa de la interfaz. Cambiar de fondo o de acento solo reemplaza esta
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
    val accent: Color
) {
    val accentSoft: Color get() = lerp(accent, Color.White, 0.45f)
    val onAccent: Color get() = Color(0xFF1A1030)
    val textPrimary: Color get() = Color(0xFFF6F2FF)
    val textSoft: Color get() = Color(0xFFC3BBDD)
    val outline: Color get() = Color(0xFF4B3F78)
    val skyBrush: Brush get() = Brush.verticalGradient(sky)
    val heroBrush: Brush get() = Brush.verticalGradient(hero)
    val cardBrush: Brush get() = Brush.verticalGradient(card)
    val activeBrush: Brush get() = Brush.verticalGradient(active)
    val accentBrush: Brush get() = Brush.linearGradient(listOf(accentSoft, accent))
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
    )
)

/** Colores de acento que el usuario puede elegir sobre cualquier fondo. */
internal val DespiertaAccents: List<Pair<String, Color>> = listOf(
    "Lavanda" to Color(0xFFC9A7FF),
    "Azul" to Color(0xFF82B1FF),
    "Rosa" to Color(0xFFF48FB1),
    "Naranja" to Color(0xFFFFB74D),
    "Menta" to Color(0xFF8EE3BF)
)

internal const val DESPIERTA_UI_PREFS = "despierta_ui"
private const val PREF_THEME_PRESET = "theme_preset"
private const val PREF_THEME_ACCENT = "theme_accent"

/** Elección guardada: fondo y, opcionalmente, un acento distinto al del fondo. */
internal data class ThemeChoice(val presetId: String, val accent: Color?) {
    fun toPalette(): DespiertaPalette {
        val base = DespiertaPresets.firstOrNull { it.id == presetId } ?: DespiertaPresets.first()
        return if (accent == null) base else base.copy(accent = accent)
    }
}

internal fun loadThemeChoice(prefs: SharedPreferences): ThemeChoice {
    val preset = prefs.getString(PREF_THEME_PRESET, DespiertaPresets.first().id)
        ?: DespiertaPresets.first().id
    val accent = if (prefs.contains(PREF_THEME_ACCENT)) {
        Color(prefs.getInt(PREF_THEME_ACCENT, 0))
    } else {
        null
    }
    return ThemeChoice(preset, accent)
}

internal fun saveThemeChoice(prefs: SharedPreferences, choice: ThemeChoice) {
    val editor = prefs.edit()
    editor.putString(PREF_THEME_PRESET, choice.presetId)
    val accent = choice.accent
    if (accent == null) {
        editor.remove(PREF_THEME_ACCENT)
    } else {
        editor.putInt(PREF_THEME_ACCENT, accent.toArgb())
    }
    editor.apply()
}

internal val LocalDespiertaPalette = staticCompositionLocalOf { DespiertaPresets.first() }

@Composable
internal fun DespiertaTheme(
    palette: DespiertaPalette = DespiertaPresets.first(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDespiertaPalette provides palette) {
        MaterialTheme(
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

@Composable
internal fun despiertaSwitchColors(): SwitchColors {
    val palette = LocalDespiertaPalette.current
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = palette.accent,
        checkedBorderColor = palette.accent,
        uncheckedThumbColor = Color(0xFFE9E3FF),
        uncheckedTrackColor = Color(0xFF3A3160),
        uncheckedBorderColor = Color(0xFF3A3160)
    )
}

/** Tarjeta redondeada con degradado del tema y borde de luz muy fino. */
@Composable
internal fun GlowCard(
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    corner: Dp = 24.dp,
    borderColor: Color = Color.White.copy(alpha = 0.07f),
    contentPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .clip(shape)
            .background(brush ?: palette.cardBrush)
            .border(BorderStroke(1.dp, borderColor), shape)
            .padding(contentPadding)
    ) {
        content()
    }
}
