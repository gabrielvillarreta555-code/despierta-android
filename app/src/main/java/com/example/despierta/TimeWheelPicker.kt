package com.example.despierta

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Locale

internal data class SelectedTime(val hour24: Int, val minute: Int)

internal fun hour24From12(hour12: Int, isPm: Boolean): Int {
    require(hour12 in 1..12)
    return when {
        hour12 == 12 && !isPm -> 0
        hour12 == 12 && isPm -> 12
        isPm -> hour12 + 12
        else -> hour12
    }
}

internal fun hour12From24(hour24: Int): Int {
    require(hour24 in 0..23)
    val reduced = hour24 % 12
    return if (reduced == 0) 12 else reduced
}

internal fun selectWheelHour(currentHour24: Int, wheelHour: Int, is24Hour: Boolean): Int {
    require(currentHour24 in 0..23)
    return if (is24Hour) {
        require(wheelHour in 0..23)
        wheelHour
    } else {
        hour24From12(wheelHour, currentHour24 >= 12)
    }
}

internal fun selectWheelPeriod(currentHour24: Int, isPm: Boolean): Int =
    hour24From12(hour12From24(currentHour24), isPm)

internal fun parseManualTime(value: String, is24Hour: Boolean): SelectedTime? {
    val normalized = value.trim().uppercase(Locale.ROOT)
    val match = if (is24Hour) {
        Regex("^(?:([01]?\\d|2[0-3])):([0-5]\\d)$").matchEntire(normalized)
    } else {
        Regex("^(0?[1-9]|1[0-2]):([0-5]\\d)\\s*([AP])\\.?M\\.?$")
            .matchEntire(normalized)
    } ?: return null

    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    return if (is24Hour) {
        SelectedTime(hour, minute)
    } else {
        SelectedTime(hour24From12(hour, match.groupValues[3] == "P"), minute)
    }
}

internal fun formatManualTime(hour24: Int, minute: Int, is24Hour: Boolean): String {
    require(hour24 in 0..23 && minute in 0..59)
    return if (is24Hour) {
        "%02d:%02d".format(Locale.ROOT, hour24, minute)
    } else {
        formatClockTime(hour24, minute)
    }
}

/** Formato de 12 horas de toda la app: 13:00 → "1:00 PM", 00:00 → "12:00 AM". */
internal fun formatClockTime(hour24: Int, minute: Int): String {
    val (time, period) = clockParts(hour24, minute)
    return "$time $period"
}

/** Separa "1:05" y "PM" para mostrar el periodo con otro tamaño. */
internal fun clockParts(hour24: Int, minute: Int): Pair<String, String> {
    require(hour24 in 0..23 && minute in 0..59)
    return "%d:%02d".format(Locale.ROOT, hour12From24(hour24), minute) to periodOf(hour24)
}

internal fun periodOf(hour24: Int): String = if (hour24 >= 12) "PM" else "AM"

/**
 * Fila que queda en la franja central de una rueda cuando termina el desplazamiento.
 *
 * La primera fila "visible" no siempre es la centrada: si el ajuste final queda unos
 * píxeles corto (alturas en dp que no son píxeles enteros), la fila anterior sigue asomando
 * por el relleno superior y es la que informa `firstVisibleItemIndex`. Se redondea a la fila
 * más cercana, con un criterio fijo para que no alterne entre dos filas.
 */
internal fun centeredWheelIndex(
    firstVisibleIndex: Int,
    firstVisibleOffsetPx: Int,
    itemSizePx: Int,
    itemCount: Int
): Int {
    if (itemCount <= 0) return 0
    val advance = itemSizePx > 0 && firstVisibleOffsetPx * 2 >= itemSizePx
    return (firstVisibleIndex + if (advance) 1 else 0).coerceIn(0, itemCount - 1)
}

private data class WheelEntry<T>(val value: T, val text: String, val testKey: String)

@Composable
internal fun TimeWheelPicker(
    hour24: Int,
    minute: Int,
    is24Hour: Boolean,
    hapticsEnabled: Boolean,
    onTimeChanged: (hour24: Int, minute: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    fun feedback() {
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val hourEntries = if (is24Hour) {
        (0..23).map { WheelEntry(it, "%02d".format(Locale.ROOT, it), it.toString()) }
    } else {
        (1..12).map { WheelEntry(it, it.toString(), it.toString()) }
    }
    val minuteEntries = (0..59).map {
        WheelEntry(it, "%02d".format(Locale.ROOT, it), it.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Selector de hora con ruedas"
                stateDescription = formatManualTime(hour24, minute, is24Hour)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!is24Hour) {
                WheelColumn(
                    label = "Periodo",
                    entries = listOf(
                        WheelEntry(false, "AM", "am"),
                        WheelEntry(true, "PM", "pm")
                    ),
                    selected = hour24 >= 12,
                    modifier = Modifier.weight(0.9f),
                    testTag = "time_wheel_period"
                ) { isPm ->
                    val updated = selectWheelPeriod(hour24, isPm)
                    if (updated != hour24) feedback()
                    onTimeChanged(updated, minute)
                }
            }
            WheelColumn(
                label = "Hora",
                entries = hourEntries,
                selected = if (is24Hour) hour24 else hour12From24(hour24),
                modifier = Modifier.weight(1f),
                testTag = "time_wheel_hour"
            ) { wheelHour ->
                val updated = selectWheelHour(hour24, wheelHour, is24Hour)
                if (updated != hour24) feedback()
                onTimeChanged(updated, minute)
            }
            Text(":", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            WheelColumn(
                label = "Minuto",
                entries = minuteEntries,
                selected = minute,
                modifier = Modifier.weight(1f),
                testTag = "time_wheel_minute"
            ) { updated ->
                if (updated != minute) feedback()
                onTimeChanged(hour24, updated)
            }
        }
        Text(
            if (is24Hour) "Formato de 24 horas" else formatClockTime(hour24, minute),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun <T> WheelColumn(
    label: String,
    entries: List<WheelEntry<T>>,
    selected: T,
    modifier: Modifier,
    testTag: String,
    onSelected: (T) -> Unit
) {
    val selectedIndex = entries.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
    val latestSelected = rememberUpdatedState(selected)
    val latestOnSelected = rememberUpdatedState(onSelected)

    LaunchedEffect(selectedIndex) {
        if (
            !state.isScrollInProgress &&
            (state.firstVisibleItemIndex != selectedIndex || state.firstVisibleItemScrollOffset != 0)
        ) {
            state.scrollToItem(selectedIndex)
        }
    }
    LaunchedEffect(state, entries) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                // La fila elegida es la centrada, no la primera que asoma: ver centeredWheelIndex.
                val first = state.firstVisibleItemIndex
                val itemSize = state.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == first }
                    ?.size ?: 0
                val index = centeredWheelIndex(
                    firstVisibleIndex = first,
                    firstVisibleOffsetPx = state.firstVisibleItemScrollOffset,
                    itemSizePx = itemSize,
                    itemCount = entries.size
                )
                val value = entries[index].value
                if (value != latestSelected.value) {
                    latestOnSelected.value(value)
                } else if (first != index || state.firstVisibleItemScrollOffset != 0) {
                    // Alinea la rueda con el valor que ya está guardado.
                    state.scrollToItem(index)
                }
            }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(Locale.ROOT),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Box(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(156.dp)
                .clip(RoundedCornerShape(16.dp))
                .testTag(testTag)
                .semantics { stateDescription = entries[selectedIndex].text }
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        RoundedCornerShape(13.dp)
                    )
            )
            LazyColumn(
                state = state,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = 52.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(entries.size) { index ->
                    val entry = entries[index]
                    val isSelected = entry.value == selected
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("${testTag}_${entry.testKey}")
                            .semantics {
                                contentDescription = "$label ${entry.text}"
                                stateDescription = if (isSelected) "Seleccionado" else "Disponible"
                            }
                            .clickable {
                                onSelected(entry.value)
                                scope.launch { state.animateScrollToItem(index) }
                            }
                            .alpha(if (isSelected) 1f else 0.42f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            entry.text,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontSize = if (isSelected) 29.sp else 21.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
