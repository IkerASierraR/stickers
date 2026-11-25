package com.stickers.creador.ui.componentes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fondo de cuadrícula tipo tablero para que se note la transparencia (design.md). */
@Composable
fun Modifier.tablero(celda: Dp = 10.dp): Modifier {
    val oscuro = isSystemInDarkTheme()
    val claro = if (oscuro) Color(0xFF2A3631) else Color(0xFFF2F2F2)
    val gris = if (oscuro) Color(0xFF34423C) else Color(0xFFDDDDDD)
    val px = with(LocalDensity.current) { celda.toPx() }
    return this.drawWithCache {
        val camino = Path()
        val columnas = (size.width / px).toInt() + 1
        val filas = (size.height / px).toInt() + 1
        for (f in 0 until filas) {
            for (c in 0 until columnas) {
                if ((f + c) % 2 == 1) camino.addRect(Rect(c * px, f * px, (c + 1) * px, (f + 1) * px))
            }
        }
        onDrawBehind {
            drawRect(claro)
            drawPath(camino, gris)
        }
    }
}
