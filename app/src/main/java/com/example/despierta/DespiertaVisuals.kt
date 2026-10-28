package com.example.despierta

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Colores de luna y sol: no dependen del fondo elegido.
internal val MoonLight = Color(0xFFFFE6D2)
internal val MoonWarm = Color(0xFFFFB86B)
internal val StarGold = Color(0xFFFFD45C)
internal val DawnOrange = Color(0xFFFFA24C)
internal val DuskBlue = Color(0xFFCFE3FF)

/** Luna creciente con estrellas dibujada con Canvas; no usa imágenes. */
@Composable
internal fun MoonWithStars(modifier: Modifier = Modifier, animated: Boolean = true) {
    // Los valores animados se leen dentro del dibujo: la luna se redibuja sin recomponer
    // la pantalla en cada fotograma.
    val glowState: State<Float>
    val twinkleState: State<Float>
    if (animated) {
        val transition = rememberInfiniteTransition(label = "moon")
        glowState = transition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
            label = "moon_glow"
        )
        twinkleState = transition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
            label = "moon_twinkle"
        )
    } else {
        glowState = remember { mutableFloatStateOf(0.9f) }
        twinkleState = remember { mutableFloatStateOf(1f) }
    }

    Canvas(modifier) {
        val glow = glowState.value
        val twinkle = twinkleState.value
        val s = min(size.width, size.height)
        val origin = Offset((size.width - s) / 2f, (size.height - s) / 2f)
        fun p(x: Float, y: Float) = Offset(origin.x + x * s, origin.y + y * s)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(MoonWarm.copy(alpha = 0.42f * glow), Color.Transparent),
                center = p(0.42f, 0.55f),
                radius = s * 0.55f
            ),
            radius = s * 0.55f,
            center = p(0.42f, 0.55f)
        )

        val outer = Path().apply { addOval(Rect(center = p(0.42f, 0.55f), radius = s * 0.34f)) }
        val inner = Path().apply { addOval(Rect(center = p(0.58f, 0.43f), radius = s * 0.29f)) }
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
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
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

/** Siluetas de montañas: el paisaje nocturno de la cabecera. */
@Composable
internal fun NightLandscape(hill: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val back = Path().apply {
            moveTo(0f, h * 0.62f)
            lineTo(w * 0.18f, h * 0.38f)
            lineTo(w * 0.32f, h * 0.55f)
            lineTo(w * 0.52f, h * 0.30f)
            lineTo(w * 0.72f, h * 0.52f)
            lineTo(w * 0.86f, h * 0.40f)
            lineTo(w, h * 0.58f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(back, color = hill.copy(alpha = 0.55f))
        val front = Path().apply {
            moveTo(0f, h * 0.78f)
            lineTo(w * 0.22f, h * 0.60f)
            lineTo(w * 0.40f, h * 0.74f)
            lineTo(w * 0.64f, h * 0.58f)
            lineTo(w * 0.84f, h * 0.72f)
            lineTo(w, h * 0.64f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(front, color = hill)
    }
}

// ---------- Plan de sueño (solo preferencia visual) ----------

/** Plan de sueño elegido por el usuario. No programa alarmas por sí mismo. */
internal data class SleepPlan(val bedMinutes: Int, val wakeMinutes: Int) {
    val durationMinutes: Int get() = sleepPlanDuration(bedMinutes, wakeMinutes)

    companion object {
        val DEFAULT = SleepPlan(bedMinutes = 23 * 60, wakeMinutes = 6 * 60 + 30)
    }
}

internal const val MINUTES_PER_DAY = 24 * 60

internal fun sleepPlanDuration(bedMinutes: Int, wakeMinutes: Int): Int =
    Math.floorMod(wakeMinutes - bedMinutes, MINUTES_PER_DAY)

internal fun normalizeMinuteOfDay(value: Int): Int = Math.floorMod(value, MINUTES_PER_DAY)

/** Qué extremo del anillo se está moviendo. */
internal enum class DialHandle { BED, WAKE }

private class DialGeometry(val center: Offset, val radius: Float, val ringWidth: Float)

private fun dialGeometry(width: Float, height: Float): DialGeometry {
    val s = min(width, height)
    val ringWidth = s * 0.13f
    val radius = s / 2f - ringWidth / 2f - s * 0.02f
    return DialGeometry(Offset(width / 2f, height / 2f), radius, ringWidth)
}

private fun minutesToAngle(minutes: Int): Float = minutes / MINUTES_PER_DAY.toFloat() * 360f - 90f

private fun ringPoint(center: Offset, radius: Float, angleDeg: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    return Offset(center.x + (radius * cos(rad)).toFloat(), center.y + (radius * sin(rad)).toFloat())
}

/** Convierte un punto del anillo en minutos del día, redondeados a 5 minutos. */
private fun pointToMinutes(center: Offset, point: Offset): Int {
    val deg = Math.toDegrees(atan2((point.y - center.y).toDouble(), (point.x - center.x).toDouble())).toFloat()
    val fromTop = ((deg + 90f) % 360f + 360f) % 360f
    val minutes = fromTop / 360f * MINUTES_PER_DAY
    return (Math.round(minutes / 5f) * 5) % MINUTES_PER_DAY
}

/**
 * Anillo de 24 horas: el arco va de la hora de dormir a la de despertar.
 * Si [onHandleMoved] no es nulo, cada extremo se puede arrastrar alrededor del anillo.
 */
@Composable
internal fun SleepDial(
    plan: SleepPlan,
    accent: Color,
    accentSoft: Color,
    modifier: Modifier = Modifier,
    onHandleMoved: ((DialHandle, Int) -> Unit)? = null
) {
    val latestPlan by rememberUpdatedState(plan)
    val latestOnMoved by rememberUpdatedState(onHandleMoved)
    val dragModifier = if (onHandleMoved != null) {
        Modifier.pointerInput(Unit) {
            var handle: DialHandle? = null
            detectDragGestures(
                onDragStart = { offset ->
                    val geo = dialGeometry(size.width.toFloat(), size.height.toFloat())
                    val bed = ringPoint(geo.center, geo.radius, minutesToAngle(latestPlan.bedMinutes))
                    val wakeAngle = minutesToAngle(latestPlan.bedMinutes + latestPlan.durationMinutes)
                    val wake = ringPoint(geo.center, geo.radius, wakeAngle)
                    val toBed = (offset - bed).getDistance()
                    val toWake = (offset - wake).getDistance()
                    handle = if (toBed <= toWake) DialHandle.BED else DialHandle.WAKE
                },
                onDragEnd = { handle = null },
                onDragCancel = { handle = null },
                onDrag = { change, _ ->
                    change.consume()
                    val current = handle
                    if (current != null) {
                        val geo = dialGeometry(size.width.toFloat(), size.height.toFloat())
                        latestOnMoved?.invoke(current, pointToMinutes(geo.center, change.position))
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Canvas(modifier.then(dragModifier)) {
        val geo = dialGeometry(size.width, size.height)
        val center = geo.center
        val radius = geo.radius
        val ringWidth = geo.ringWidth
        val s = min(size.width, size.height)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        drawCircle(color = Color(0xFF2B2252), radius = radius, center = center, style = Stroke(width = ringWidth))
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF2A2056), Color(0xFF130E28)),
                center = center,
                radius = radius
            ),
            radius = radius - ringWidth / 2f,
            center = center
        )

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
                color = if (major) accentSoft else Color.White.copy(alpha = if (hourMark) 0.55f else 0.25f),
                start = ringPoint(center, tickOuter, angle),
                end = ringPoint(center, tickOuter - length, angle),
                strokeWidth = if (major) s * 0.012f else s * 0.006f,
                cap = StrokeCap.Round
            )
        }

        val startAngle = minutesToAngle(plan.bedMinutes)
        val sweep = plan.durationMinutes / MINUTES_PER_DAY.toFloat() * 360f
        if (sweep > 0f) {
            drawArc(
                brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.7f), accent, accentSoft, accent.copy(alpha = 0.7f)), center),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringWidth * 0.86f, cap = StrokeCap.Round)
            )
        }

        val knob = ringWidth * 0.42f
        val bedPoint = ringPoint(center, radius, startAngle)
        val wakePoint = ringPoint(center, radius, startAngle + sweep)
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

// ---------- Paisaje de la cabecera (2.6) ----------

/**
 * Escena nocturna a sangre para la cabecera de Alarmas: resplandor en el horizonte, estrellas,
 * tres cordilleras con profundidad y pinos en primer plano. Los colores salen del tema
 * (Noche, Amanecer o Bosque), así que se adapta sin imágenes. El suelo se funde con el fondo.
 */
@Composable
internal fun NightScene(palette: DespiertaPalette, modifier: Modifier = Modifier, twinkle: () -> Float = { -1f }) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // Resplandor del horizonte: halo amplio del tema y una franja luminosa con el acento,
        // justo detrás de la cordillera lejana (da profundidad, como un atardecer).
        drawRect(
            brush = Brush.radialGradient(
                listOf(palette.hero.first().copy(alpha = 0.65f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.48f),
                radius = w * 0.9f
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.6f to lerp(palette.accent, palette.hero.first(), 0.45f).copy(alpha = 0.42f),
                1f to Color.Transparent,
                startY = h * 0.22f,
                endY = h * 0.52f
            )
        )

        // Estrellas en posiciones fijas (generador determinista, mismas en cada dibujo).
        var seed = 0x2F6E2B1
        fun next(): Float {
            seed = seed * 1103515245 + 12345
            return ((seed ushr 8) and 0xFFFF) / 65535f
        }
        // Las estrellas titilan (fase leída al dibujar: no recompone la pantalla).
        val phase = twinkle()
        repeat(46) { i ->
            val x = next() * w
            val y = (0.04f + next() * 0.40f) * h
            val r = (0.7f + next() * 1.3f) * density
            val base = 0.25f + next() * 0.6f
            val alpha = if (phase < 0f) base else base * (0.35f + 0.65f * ((sin(phase * 2f * PI.toFloat() + i * 1.9f) + 1f) / 2f))
            drawCircle(Color.White.copy(alpha = alpha), r, Offset(x, y))
        }

        fun ridge(base: Float, peaks: List<Pair<Float, Float>>): Path = Path().apply {
            moveTo(0f, h * base)
            var prev = Offset(0f, h * base)
            for ((px, py) in peaks) {
                val point = Offset(w * px, h * py)
                val mid = Offset((prev.x + point.x) / 2f, (prev.y + point.y) / 2f)
                @Suppress("DEPRECATION")
                quadraticBezierTo(prev.x, prev.y, mid.x, mid.y)
                prev = point
            }
            lineTo(w, prev.y)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        // Cordillera lejana, media y cercana: más clara cuanto más lejos.
        val far = lerp(palette.hero.first(), palette.sky.first(), 0.25f)
        val mid = lerp(palette.hero[1], palette.hill, 0.35f)
        drawPath(
            ridge(0.52f, listOf(0.1f to 0.38f, 0.22f to 0.47f, 0.36f to 0.33f, 0.52f to 0.46f, 0.68f to 0.36f, 0.84f to 0.47f, 1.05f to 0.40f)),
            color = far.copy(alpha = 0.85f)
        )
        drawPath(
            ridge(0.62f, listOf(0.08f to 0.56f, 0.24f to 0.48f, 0.42f to 0.60f, 0.58f to 0.50f, 0.76f to 0.59f, 0.92f to 0.51f, 1.05f to 0.57f)),
            color = mid
        )

        // Suelo cercano que se funde con el fondo de la pantalla.
        val ground = Brush.verticalGradient(
            0f to palette.hill,
            0.55f to palette.hill,
            1f to palette.hill.copy(alpha = 0f),
            startY = h * 0.64f,
            endY = h
        )
        drawPath(
            ridge(0.72f, listOf(0.2f to 0.68f, 0.5f to 0.74f, 0.8f to 0.67f, 1.05f to 0.71f)),
            brush = ground
        )

        // Neblina baja entre las cordilleras y los pinos: separa planos y suaviza la transición.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to lerp(palette.hero.first(), palette.accent, 0.25f).copy(alpha = 0.16f),
                1f to Color.Transparent,
                startY = h * 0.56f,
                endY = h * 0.88f
            )
        )

        // Pinos en los bordes, de tres pisos cada uno. Se disuelven en la neblina hacia abajo,
        // sin un corte duro donde termina el paisaje.
        val treeColor = lerp(palette.hill, Color.Black, 0.22f)
        val treeBrush = Brush.verticalGradient(
            0f to treeColor,
            0.78f to treeColor,
            1f to treeColor.copy(alpha = 0f),
            startY = 0f,
            endY = h
        )
        fun pine(cx: Float, baseY: Float, height: Float) {
            val half = height * 0.32f
            for (tier in 0 until 3) {
                val top = baseY - height + tier * height * 0.24f
                val bottom = top + height * 0.46f
                val spread = half * (0.55f + tier * 0.25f)
                drawPath(
                    Path().apply {
                        moveTo(cx, top)
                        lineTo(cx + spread, bottom)
                        lineTo(cx - spread, bottom)
                        close()
                    },
                    brush = treeBrush
                )
            }
            // Tronco corto bajo la copa; se desvanece con la neblina (sin barras en los bordes).
            drawRect(
                brush = treeBrush,
                topLeft = Offset(cx - height * 0.03f, baseY - height * 0.1f),
                size = Size(height * 0.06f, height * 0.14f)
            )
        }
        // Pinos que enmarcan los lados, sobre el suelo cercano.
        val treeBase = h * 0.80f
        listOf(
            0.02f to 0.44f, 0.08f to 0.52f, 0.145f to 0.38f, 0.205f to 0.30f,
            0.80f to 0.30f, 0.86f to 0.40f, 0.925f to 0.50f, 0.985f to 0.42f
        ).forEach { (x, th) -> pine(w * x, treeBase, h * th) }
    }
}
