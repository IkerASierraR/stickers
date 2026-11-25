package com.stickers.creador.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta de design.md
val Primario = Color(0xFF1FAF5A)
val PrimarioOscuro = Color(0xFF0B6E4F)
val AcentoSuave = Color(0xFFDCF8E6)
val FondoClaro = Color(0xFFF4F7F5)
val Superficie = Color(0xFFFFFFFF)
val FondoOscuro = Color(0xFF0F1A16)
val SuperficieOscura = Color(0xFF1A2621)
val ErrorColor = Color(0xFFD93025)

private val Claro = lightColorScheme(
    primary = PrimarioOscuro, // texto/íconos sobre fondo claro con contraste AA
    onPrimary = Color.White,
    primaryContainer = AcentoSuave,
    onPrimaryContainer = Color(0xFF00391F),
    secondary = Primario,
    onSecondary = Color.White,
    secondaryContainer = AcentoSuave,
    onSecondaryContainer = Color(0xFF00391F),
    tertiary = Primario,
    background = FondoClaro,
    onBackground = Color(0xFF17201C),
    surface = Superficie,
    onSurface = Color(0xFF17201C),
    surfaceVariant = Color(0xFFE3EAE6),
    onSurfaceVariant = Color(0xFF424B47),
    surfaceContainer = Superficie,
    surfaceContainerLow = Superficie,
    surfaceContainerHigh = Color(0xFFEDF2EF),
    error = ErrorColor,
    onError = Color.White,
    outline = Color(0xFF727B77),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF6FDC97),
    onPrimary = Color(0xFF00391F),
    primaryContainer = PrimarioOscuro,
    onPrimaryContainer = AcentoSuave,
    secondary = Primario,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1F4A37),
    onSecondaryContainer = AcentoSuave,
    tertiary = Primario,
    background = FondoOscuro,
    onBackground = Color(0xFFE1E8E4),
    surface = SuperficieOscura,
    onSurface = Color(0xFFE1E8E4),
    surfaceVariant = Color(0xFF2A3631),
    onSurfaceVariant = Color(0xFFC0C9C4),
    surfaceContainer = SuperficieOscura,
    surfaceContainerLow = SuperficieOscura,
    surfaceContainerHigh = Color(0xFF24312B),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF410002),
    outline = Color(0xFF8B948F),
)

private val Tipografia = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
)

private val Formas = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun TemaStickers(oscuro: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (oscuro) Oscuro else Claro,
        typography = Tipografia,
        shapes = Formas,
        content = content,
    )
}
