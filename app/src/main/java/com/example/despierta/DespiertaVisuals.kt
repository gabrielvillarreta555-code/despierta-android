package com.example.despierta

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ---------- Paleta nocturna ----------

internal val NightDeep = Color(0xFF0D0A1C)
internal val NightBase = Color(0xFF15102B)
internal val NightRaised = Color(0xFF221A40)
internal val NightCard = Color(0xFF1D1736)
internal val NightCardHigh = Color(0xFF2A2150)
internal val NightOutline = Color(0xFF4B3F78)
internal val Lavender = Color(0xFFC9A7FF)
internal val LavenderSoft = Color(0xFFE4D4FF)
internal val LavenderDeep = Color(0xFF8A63E8)
internal val OnLavender = Color(0xFF221545)
internal val MoonLight = Color(0xFFFFE6D2)
internal val MoonWarm = Color(0xFFFFB86B)
internal val StarGold = Color(0xFFFFD45C)
internal val DawnOrange = Color(0xFFFFA24C)
internal val DuskBlue = Color(0xFFCFE3FF)
internal val TextSoft = Color(0xFFC3BBDD)

/** Fondo general: luz violeta difusa arriba que se pierde en la noche. */
internal val NightBackgroundBrush: Brush = Brush.verticalGradient(
    0f to Color(0xFF3B2B66),
    0.28f to Color(0xFF1E1640),
    0.62f to NightBase,
    1f to NightDeep
)

/** Degradado de la tarjeta principal, como la primera pantalla de referencia. */
internal val HeroBrush: Brush = Brush.verticalGradient(
    listOf(Color(0xFF6B4E9E), Color(0xFF3A2A62), Color(0xFF1C1535))
)

internal val CardBrush: Brush = Brush.verticalGradient(
    listOf(Color(0xFF2A2150), Color(0xFF1C1634))
)

internal val ActiveCardBrush: Brush = Brush.linearGradient(
    listOf(Color(0xFF3A2C6E), Color(0xFF231B44))
)

internal val LavenderBrush: Brush = Brush.linearGradient(
    listOf(Color(0xFFDCC4FF), Color(0xFFB08CFF))
)

@Composable
internal fun despiertaSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Lavender,
    checkedBorderColor = Lavender,
    uncheckedThumbColor = Color(0xFFE9E3FF),
    uncheckedTrackColor = Color(0xFF3A3160),
    uncheckedBorderColor = Color(0xFF3A3160)
)

/** Tarjeta redondeada con degradado y un borde de luz muy fino. */
@Composable
internal fun GlowCard(
    modifier: Modifier = Modifier,
    brush: Brush = CardBrush,
    corner: Dp = 24.dp,
    borderColor: Color = Color.White.copy(alpha = 0.07f),
    contentPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .clip(shape)
            .background(brush)
            .border(BorderStroke(1.dp, borderColor), shape)
            .padding(contentPadding)
    ) {
        Column { content() }
    }
}

// ---------- Luna con estrellas ----------

/**
 * Luna creciente con estrellas dibujada con Canvas; no requiere recursos gráficos.
 * Cuando [animated] es falso (pruebas instrumentadas) se dibuja estática.
 */
@Composable
internal fun MoonWithStars(modifier: Modifier = Modifier, animated: Boolean = true) {
    val glow: Float
    val twinkle: Float
    if (animated) {
        val transition = rememberInfiniteTransition(label = "moon")
        glow = transition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
            label = "moon_glow"
        ).value
        twinkle = transition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
            label = "moon_twinkle"
        ).value
    } else {
        glow = 0.9f
        twinkle = 1f
    }

    Canvas(modifier) {
        val s = min(size.width, size.height)
        val origin = Offset((size.width - s) / 2f, (size.height - s) / 2f)
        fun p(x: Float, y: Float) = Offset(origin.x + x * s, origin.y + y * s)

        // Halo cálido detrás de la luna.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(MoonWarm.copy(alpha = 0.42f * glow), Color.Transparent),
                center = p(0.42f, 0.55f),
                radius = s * 0.55f
            ),
            radius = s * 0.55f,
            center = p(0.42f, 0.55f)
        )

        // Creciente: círculo exterior menos un círculo desplazado.
        val outer = Path().apply {
            addOval(Rect(center = p(0.42f, 0.55f), radius = s * 0.34f))
        }
        val inner = Path().apply {
            addOval(Rect(center = p(0.58f, 0.43f), radius = s * 0.29f))
        }
        val crescent = Path().apply { op(outer, inner, PathOperation.Difference) }
        drawPath(
            path = crescent,
            brush = Brush.linearGradient(
                listOf(MoonWarm, Color(0xFFFFCFA0), MoonLight),
                start = p(0.15f, 0.25f),
                end = p(0.65f, 0.9f)
            )
        )

        drawStar(p(0.66f, 0.50f), s * 0.105f * twinkle)
        drawStar(p(0.55f, 0.24f), s * 0.065f * (2.05f - twinkle))
        drawStar(p(0.80f, 0.30f), s * 0.04f * twinkle)
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float) {
    val path = Path()
    val innerRadius = radius * 0.46f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = -PI / 2 + i * PI / 5
        val point = Offset(
            center.x + (r * cos(angle)).toFloat(),
            center.y + (r * sin(angle)).toFloat()
        )
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawCircle(
        brush = Brush.radialGradient(
            listOf(StarGold.copy(alpha = 0.35f), Color.Transparent),
            center = center,
            radius = radius * 1.8f
        ),
        radius = radius * 1.8f,
        center = center
    )
    drawPath(
        path,
        brush = Brush.linearGradient(
            listOf(Color(0xFFFFE89A), StarGold, Color(0xFFFFB030)),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius, center.y + radius)
        )
    )
}

// ---------- Dial de sueño ----------

/** Plan de sueño elegido por el usuario. Solo es una preferencia visual; no programa alarmas. */
internal data class SleepPlan(val bedMinutes: Int, val wakeMinutes: Int) {
    val durationMinutes: Int get() = sleepPlanDuration(bedMinutes, wakeMinutes)

    companion object {
        val DEFAULT = SleepPlan(bedMinutes = 23 * 60, wakeMinutes = 6 * 60 + 30)
    }
}

internal fun sleepPlanDuration(bedMinutes: Int, wakeMinutes: Int): Int =
    Math.floorMod(wakeMinutes - bedMinutes, MINUTES_PER_DAY)

internal fun normalizeMinuteOfDay(value: Int): Int = Math.floorMod(value, MINUTES_PER_DAY)

internal const val MINUTES_PER_DAY = 24 * 60

/**
 * Anillo de 24 horas: el arco morado va de la hora de dormir a la de despertar.
 * La medianoche queda arriba.
 */
@Composable
internal fun SleepDial(plan: SleepPlan, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = min(size.width, size.height)
        val ringWidth = s * 0.13f
        val radius = s / 2f - ringWidth / 2f - s * 0.02f
        val center = Offset(size.width / 2f, size.height / 2f)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        fun angleOf(minutes: Int) = minutes / MINUTES_PER_DAY.toFloat() * 360f - 90f
        fun pointAt(angleDeg: Float, r: Float): Offset {
            val rad = Math.toRadians(angleDeg.toDouble())
            return Offset(center.x + (r * cos(rad)).toFloat(), center.y + (r * sin(rad)).toFloat())
        }

        // Pista del anillo.
        drawCircle(
            color = Color(0xFF2B2252),
            radius = radius,
            center = center,
            style = Stroke(width = ringWidth)
        )
        // Interior oscuro.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF2A2056), Color(0xFF130E28)),
                center = center,
                radius = radius
            ),
            radius = radius - ringWidth / 2f,
            center = center
        )

        // Marcas de reloj: 96 cuartos de hora, más largas cada 3 horas.
        val tickOuter = radius - ringWidth / 2f - s * 0.035f
        for (i in 0 until 96) {
            val angle = i * 3.75f - 90f
            val major = i % 12 == 0
            val hourMark = i % 4 == 0
            val length = when {
                major -> s * 0.07f
                hourMark -> s * 0.04f
                else -> s * 0.022f
            }
            drawLine(
                color = if (major) LavenderSoft else Color.White.copy(alpha = if (hourMark) 0.55f else 0.25f),
                start = pointAt(angle, tickOuter),
                end = pointAt(angle, tickOuter - length),
                strokeWidth = if (major) s * 0.012f else s * 0.006f,
                cap = StrokeCap.Round
            )
        }

        // Arco de sueño.
        val start = angleOf(plan.bedMinutes)
        val sweep = plan.durationMinutes / MINUTES_PER_DAY.toFloat() * 360f
        if (sweep > 0f) {
            drawArc(
                brush = Brush.sweepGradient(listOf(LavenderDeep, Lavender, LavenderSoft, LavenderDeep), center),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringWidth * 0.86f, cap = StrokeCap.Round)
            )
        }

        // Extremos: luna (dormir) y sol (despertar).
        val knob = ringWidth * 0.42f
        val bedPoint = pointAt(start, radius)
        val wakePoint = pointAt(start + sweep, radius)
        drawCircle(Color(0xFF1B1538), knob * 1.25f, bedPoint)
        drawCircle(DuskBlue, knob, bedPoint)
        drawCircle(Color(0xFF1B1538), knob * 1.25f, wakePoint)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFFD37A), DawnOrange), wakePoint, knob),
            radius = knob,
            center = wakePoint
        )
    }
}
