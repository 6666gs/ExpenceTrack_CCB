package com.expensetrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// "Ledger" palette: deep pine ink on warm paper, amber for money highlights.
private val Pine = Color(0xFF1F3D36)
private val PineLight = Color(0xFF8FC1B2)
private val Amber = Color(0xFFF2B544)
private val Paper = Color(0xFFF6F3EC)
private val PaperDim = Color(0xFFECE6DA)
private val Ink = Color(0xFF1B1C1A)
private val Night = Color(0xFF121614)
private val NightSurface = Color(0xFF1C2320)
private val Alert = Color(0xFFB3401E)
private val PineMist = Color(0xFFDCE8E2)
private val PineDeep = Color(0xFF2C4A42)

private val LightColors = lightColorScheme(
    primary = Pine, onPrimary = Paper,
    secondary = Amber, onSecondary = Ink,
    background = Paper, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = PaperDim, onSurfaceVariant = Color(0xFF55574F),
    // Containers drive the + button, nav indicator, chips and the bottom bar.
    primaryContainer = Amber, onPrimaryContainer = Ink,
    secondaryContainer = PineMist, onSecondaryContainer = Pine,
    surfaceContainer = PaperDim, surfaceContainerLow = PaperDim, surfaceContainerHigh = PaperDim,
    outline = Color(0xFF8A8C83), outlineVariant = Color(0xFFD3CDBF),
    error = Alert,
)

private val DarkColors = darkColorScheme(
    primary = PineLight, onPrimary = Night,
    secondary = Amber, onSecondary = Ink,
    background = Night, onBackground = Paper,
    surface = Night, onSurface = Paper,
    surfaceVariant = NightSurface, onSurfaceVariant = Color(0xFFB9BDB5),
    primaryContainer = Amber, onPrimaryContainer = Ink,
    secondaryContainer = PineDeep, onSecondaryContainer = Paper,
    surfaceContainer = NightSurface, surfaceContainerLow = NightSurface, surfaceContainerHigh = NightSurface,
    outline = Color(0xFF7D8279), outlineVariant = Color(0xFF333B37),
    error = Color(0xFFFF8A65),
)

/** Tabular figures keep amounts aligned in lists. */
val AmountStyle = TextStyle(fontFeatureSettings = "tnum", fontFamily = FontFamily.Default)

private val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.merge(AmountStyle).copy(fontWeight = FontWeight.Bold, fontSize = 52.sp),
        titleMedium = base.titleMedium.merge(AmountStyle).copy(fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(letterSpacing = 1.5.sp),
    )
}

@Composable
fun ExpenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
