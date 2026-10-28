package com.example.despierta

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp

/**
 * Tipografía de las cifras y los títulos (2.6): Bricolage Grotesque, con licencia OFL
 * (assets/licenses). El texto corrido sigue usando la fuente del sistema.
 */
internal val DisplayFont = FontFamily(
    Font(R.font.bricolage_medium, FontWeight.Medium),
    Font(R.font.bricolage_bold, FontWeight.Bold),
    Font(R.font.bricolage_extrabold, FontWeight.ExtraBold)
)

/**
 * Texto de una línea que se encoge si no cabe en [maxWidth]. Sirve para las horas grandes:
 * "12:45" no cabe al tamaño de "5:15" en pantallas estrechas. [style] debe llevar `fontSize`,
 * `lineHeight` y `letterSpacing` definidos, para poder escalarlos juntos.
 */
@Composable
internal fun FitText(
    text: String,
    style: TextStyle,
    maxWidth: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val fitted = remember(text, style, maxWidth, density) {
        val limit = with(density) { maxWidth.roundToPx() }
        val width = measurer.measure(text, style, maxLines = 1, softWrap = false).size.width
        if (width <= limit || width == 0) {
            style
        } else {
            val scale = limit.toFloat() / width
            style.copy(
                fontSize = style.fontSize * scale,
                lineHeight = style.lineHeight * scale,
                letterSpacing = style.letterSpacing * scale
            )
        }
    }
    Text(text, style = fitted, color = color, maxLines = 1, softWrap = false, modifier = modifier)
}
