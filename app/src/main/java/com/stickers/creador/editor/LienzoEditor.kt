package com.stickers.creador.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.stickers.creador.ui.componentes.tablero

/**
 * Lienzo cuadrado del editor sobre tablero de transparencia. Según la herramienta activa:
 * - Recortar: arrastrar y pellizcar para encuadrar la imagen.
 * - Texto: tocar un texto para seleccionarlo y arrastrarlo.
 * - Dibujar: dibujar a mano.
 */
@Composable
fun LienzoEditor(
    controlador: ControladorEditor,
    fuente: Bitmap?,
    modifier: Modifier = Modifier,
) {
    val herramienta = controlador.herramienta
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .tablero(),
    ) {
        val gestos = when (herramienta) {
            Herramienta.RECORTAR -> Modifier
                .pointerInput(herramienta) { seguirGesto(controlador) }
                .pointerInput(herramienta) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val lado = size.width.toFloat()
                        controlador.moverImagen(pan.x / lado, pan.y / lado, zoom)
                    }
                }

            Herramienta.TEXTO -> Modifier
                .pointerInput(herramienta) { seguirGesto(controlador) }
                .pointerInput(herramienta) {
                    detectTapGestures { pos ->
                        controlador.textoSeleccionado = textoEn(controlador, pos, size.width.toFloat())
                    }
                }
                .pointerInput(herramienta) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            textoEn(controlador, pos, size.width.toFloat())?.let { controlador.textoSeleccionado = it }
                        },
                        onDrag = { cambio, arrastre ->
                            cambio.consume()
                            val lado = size.width.toFloat()
                            controlador.textoSeleccionado?.let { id ->
                                controlador.moverTexto(id, arrastre.x / lado, arrastre.y / lado)
                            }
                        },
                    )
                }

            Herramienta.DIBUJAR -> Modifier.pointerInput(herramienta) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val lado = size.width.toFloat()
                        controlador.empezarTrazo(Punto(pos.x / lado, pos.y / lado))
                    },
                    onDrag = { cambio, _ ->
                        cambio.consume()
                        val lado = size.width.toFloat()
                        controlador.continuarTrazo(Punto(cambio.position.x / lado, cambio.position.y / lado))
                    },
                    onDragEnd = { controlador.terminarTrazo() },
                    onDragCancel = { controlador.terminarTrazo() },
                )
            }
        }

        Canvas(Modifier.fillMaxSize().then(gestos)) {
            val estado = controlador.estado
            val trazo = controlador.trazoEnCurso
            val seleccion = controlador.textoActual()
            drawIntoCanvas { c ->
                RenderSticker.dibujar(c.nativeCanvas, size.width, fuente, estado, trazo)
                if (herramienta == Herramienta.TEXTO && seleccion != null) {
                    val caja = RenderSticker.cajaTexto(seleccion, size.width)
                    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 2.dp.toPx()
                        color = 0xFF1FAF5A.toInt()
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 8f), 0f)
                    }
                    c.nativeCanvas.drawRect(
                        caja.left * size.width, caja.top * size.width,
                        caja.right * size.width, caja.bottom * size.width, p,
                    )
                }
            }
        }
    }
}

/** Registra el inicio y el fin de un gesto para guardar un solo paso de "Deshacer". */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.seguirGesto(controlador: ControladorEditor) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        controlador.inicioGesto()
        do {
            val evento = awaitPointerEvent(PointerEventPass.Final)
        } while (evento.changes.any { it.pressed })
        controlador.finGesto()
    }
}

private fun textoEn(controlador: ControladorEditor, pos: Offset, lado: Float): Long? {
    val x = pos.x / lado
    val y = pos.y / lado
    return controlador.estado.textos.lastOrNull { RenderSticker.cajaTexto(it, lado).contains(x, y) }?.id
}
