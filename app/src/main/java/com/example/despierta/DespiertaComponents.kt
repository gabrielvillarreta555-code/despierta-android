package com.example.despierta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Margen superior seguro para contenido dibujado de borde a borde: barra de estado y recorte
 * de cámara, con un mínimo por si el sistema no informa del espacio (ocurre en algunos
 * emuladores y en pantallas sin barra de estado visible).
 */
@Composable
internal fun safeTopInset(minimum: Dp = 28.dp): Dp {
    val reported = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
    return if (reported > minimum) reported else minimum
}

/**
 * Componentes visuales reutilizables de Despierta y sus medidas comunes.
 * Cambiar el aspecto de la app debería ser cambiar este archivo y DespiertaTheme.kt,
 * no cada pantalla.
 */
internal object DespiertaDimens {
    /** Margen lateral de las pantallas. */
    val ScreenPadding: Dp = 16.dp
    /** Separación entre bloques de una pantalla. */
    val SectionSpacing: Dp = 16.dp
    /** Separación entre elementos de una lista. */
    val ItemSpacing: Dp = 12.dp
    /** Esquinas de tarjetas. */
    val CardCorner: Dp = 22.dp
    /** Esquinas de la tarjeta destacada (reloj). */
    val HeroCorner: Dp = 30.dp
    /** Esquinas de botones y campos. */
    val ControlCorner: Dp = 14.dp
    /** Esquinas de píldoras de selección. */
    val PillCorner: Dp = 12.dp
    /** Relleno interno de las filas de lista. */
    val RowPaddingHorizontal: Dp = 14.dp
    val RowPaddingVertical: Dp = 12.dp
    /** Icono de fila o tarjeta. */
    val RowIconSize: Dp = 38.dp
    /** Botón principal. */
    val PrimaryButtonHeight: Dp = 50.dp
    /** Botón secundario de tarjeta. */
    val CardActionHeight: Dp = 40.dp
}

@Composable
internal fun despiertaSwitchColors(): SwitchColors {
    val palette = LocalDespiertaPalette.current
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = palette.button,
        checkedBorderColor = palette.button,
        uncheckedThumbColor = Color(0xFFE9E3FF),
        // Del color del tema, no un violeta fijo: en Bosque y Amanecer desentonaba.
        uncheckedTrackColor = Color.White.copy(alpha = 0.14f),
        uncheckedBorderColor = Color.White.copy(alpha = 0.2f)
    )
}

/** Tarjeta con esquinas amplias (al estilo iOS), degradado del tema y borde de luz muy fino. */
@Composable
internal fun GlowCard(
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    corner: Dp = DespiertaDimens.CardCorner,
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

/**
 * Icono de tarjeta con el estilo elegido: solo el trazo, dentro de un cuadro redondeado
 * o con un relieve suave (degradado y borde de luz).
 */
@Composable
internal fun StyledIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = DespiertaDimens.RowIconSize
) {
    val palette = LocalDespiertaPalette.current
    val style = LocalUiOptions.current.iconStyle
    val color = tint ?: palette.cardIcon
    val shape = RoundedCornerShape(size * 0.3f)
    when (style) {
        IconStyle.MINIMAL -> Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.62f))
        }

        IconStyle.ROUNDED -> Box(
            modifier
                .size(size)
                .clip(shape)
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.55f))
        }

        IconStyle.SOFT -> Box(
            modifier
                .size(size)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Black.copy(alpha = 0.22f))
                    )
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/** Duración en milisegundos de una animación según el ajuste del usuario y de Android. */
@Composable
internal fun motion(baseMillis: Int): Int = LocalUiOptions.current.millis(baseMillis)

/** Píldora de selección única (objetivos, anticipación, repetición...). */
@Composable
internal fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDespiertaPalette.current
    val shape = RoundedCornerShape(DespiertaDimens.PillCorner)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) palette.button else Color.White.copy(alpha = 0.07f))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) palette.onButton else palette.textPrimary
        )
    }
}

@Composable
internal fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val palette = LocalDespiertaPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            color = palette.textSoft,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 6.dp)
        )
        GlowCard(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
internal fun RowDivider() {
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 64.dp))
}

@Composable
internal fun SettingTitle(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = LocalDespiertaPalette.current.textPrimary)
}

@Composable
internal fun SettingSubtitle(text: String) {
    Text(text, fontSize = 12.sp, lineHeight = 16.sp, color = LocalDespiertaPalette.current.textSoft)
}

@Composable
internal fun FootNote(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = LocalDespiertaPalette.current.textSoft.copy(alpha = 0.85f),
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
internal fun NavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDespiertaPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = DespiertaDimens.RowPaddingHorizontal, vertical = DespiertaDimens.RowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledIcon(icon)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            SettingTitle(title)
            SettingSubtitle(subtitle)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.textSoft)
    }
}

@Composable
internal fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = DespiertaDimens.RowPaddingHorizontal, vertical = DespiertaDimens.RowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledIcon(icon, tint = tint)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            SettingTitle(title)
            SettingSubtitle(subtitle)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = LocalDespiertaPalette.current.textSoft)
    }
}

@Composable
internal fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = DespiertaDimens.RowPaddingHorizontal, vertical = DespiertaDimens.RowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledIcon(icon)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            SettingTitle(title)
            SettingSubtitle(subtitle)
        }
    }
}

@Composable
internal fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
    tag: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChanged)
            .padding(horizontal = DespiertaDimens.RowPaddingHorizontal, vertical = DespiertaDimens.RowPaddingVertical)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledIcon(icon)
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            SettingTitle(title)
            SettingSubtitle(subtitle)
        }
        Switch(checked = checked, onCheckedChange = null, colors = despiertaSwitchColors())
    }
}

/** Botón principal sencillo, dentro del contenido para que nunca tape nada. */
@Composable
internal fun AddButton(label: String, tag: String, onClick: () -> Unit) {
    val palette = LocalDespiertaPalette.current
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(DespiertaDimens.ControlCorner),
        colors = ButtonDefaults.buttonColors(containerColor = palette.button, contentColor = palette.onButton),
        modifier = Modifier.fillMaxWidth().height(DespiertaDimens.PrimaryButtonHeight).testTag(tag)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

/** Botón secundario de tarjeta: mismo alto, esquinas y estilo en toda la app. */
@Composable
internal fun CardActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val palette = LocalDespiertaPalette.current
    val color = tint ?: palette.cardIcon
    Row(
        modifier
            .height(DespiertaDimens.CardActionHeight)
            .clip(RoundedCornerShape(DespiertaDimens.PillCorner))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
