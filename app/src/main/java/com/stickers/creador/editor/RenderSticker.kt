package com.stickers.creador.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.min

/**
 * Dibuja el sticker: imagen recortada (rectángulo o círculo, sin borde) + dibujo + texto.
 * El mismo código se usa para la vista previa en pantalla y para el resultado final de 512x512,
 * así lo que se ve es exactamente lo que se guarda (R12).
 */
object RenderSticker {

    private val pintaImagen = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val pintaBorrar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    /** Rectángulo donde se dibuja la fuente dentro de un lienzo de [lado] px. */
    fun rectImagen(anchoFuente: Int, altoFuente: Int, lado: Float, e: EstadoEditor): RectF {
        val ajuste = min(lado / anchoFuente, lado / altoFuente) * e.zoom
        val w = anchoFuente * ajuste
        val h = altoFuente * ajuste
        val cx = lado / 2f + e.despX * lado
        val cy = lado / 2f + e.despY * lado
        return RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
    }

    /**
     * Dibuja todo en [canvas] dentro del cuadrado (0,0)-(lado,lado).
     * [trazoEnCurso] es el trazo que se está dibujando con el dedo (aún no confirmado).
     */
    fun dibujar(
        canvas: Canvas,
        lado: Float,
        fuente: Bitmap?,
        e: EstadoEditor,
        trazoEnCurso: Trazo? = null,
    ) {
        canvas.save()
        canvas.clipRect(0f, 0f, lado, lado)
        if (fuente != null) {
            val capa = canvas.saveLayer(0f, 0f, lado, lado, null)
            canvas.drawBitmap(fuente, null, rectImagen(fuente.width, fuente.height, lado, e), pintaImagen)
            if (e.forma == Forma.CIRCULO) {
                // Borra todo lo que queda FUERA del círculo (relleno inverso), sin borde.
                val fuera = Path().apply {
                    addCircle(lado / 2f, lado / 2f, lado / 2f, Path.Direction.CW)
                    fillType = Path.FillType.INVERSE_WINDING
                }
                canvas.drawPath(fuera, pintaBorrar)
            }
            canvas.restoreToCount(capa)
        }
        dibujarCapas(canvas, lado, e, trazoEnCurso)
        canvas.restore()
    }

    /** Dibujo y texto (se ven durante todo el sticker en video, R16). */
    fun dibujarCapas(canvas: Canvas, lado: Float, e: EstadoEditor, trazoEnCurso: Trazo? = null) {
        e.trazos.forEach { dibujarTrazo(canvas, lado, it) }
        trazoEnCurso?.let { dibujarTrazo(canvas, lado, it) }
        e.textos.forEach { dibujarTexto(canvas, lado, it) }
    }

    /** Resultado final del sticker (512x512, fondo transparente). */
    fun renderizar(fuente: Bitmap?, e: EstadoEditor, lado: Int = 512): Bitmap {
        val salida = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        salida.eraseColor(Color.TRANSPARENT)
        dibujar(Canvas(salida), lado.toFloat(), fuente, e)
        return salida
    }

    /**
     * Capa superpuesta para el video corto con sonido (MP4 no tiene transparencia):
     * fuera del círculo se pinta blanco (fondo de chat claro) y encima van dibujo y texto.
     */
    fun capaVideoCorto(e: EstadoEditor, lado: Int = 512): Bitmap {
        val salida = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val c = Canvas(salida)
        val l = lado.toFloat()
        if (e.forma == Forma.CIRCULO) {
            c.drawColor(Color.WHITE)
            c.drawCircle(l / 2f, l / 2f, l / 2f, pintaBorrar)
        }
        dibujarCapas(c, l, e)
        return salida
    }

    /**
     * Matriz que lleva la imagen "ajustada" al lienzo a la posición elegida (zoom + desplazamiento),
     * en coordenadas normalizadas de OpenGL (NDC, eje Y hacia arriba). Para Media3.
     */
    fun matrizNdc(e: EstadoEditor): Matrix = Matrix().apply {
        setScale(e.zoom, e.zoom)
        postTranslate(2f * e.despX, -2f * e.despY)
    }

    private fun dibujarTrazo(canvas: Canvas, lado: Float, t: Trazo) {
        if (t.puntos.isEmpty()) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.color
            style = Paint.Style.STROKE
            strokeWidth = t.grosor * lado
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        if (t.puntos.size == 1) {
            val q = t.puntos[0]
            p.style = Paint.Style.FILL
            canvas.drawCircle(q.x * lado, q.y * lado, t.grosor * lado / 2f, p)
            return
        }
        val path = Path()
        val primero = t.puntos[0]
        path.moveTo(primero.x * lado, primero.y * lado)
        for (i in 1 until t.puntos.size) {
            val a = t.puntos[i - 1]
            val b = t.puntos[i]
            path.quadTo(a.x * lado, a.y * lado, (a.x + b.x) / 2f * lado, (a.y + b.y) / 2f * lado)
        }
        val ultimo = t.puntos.last()
        path.lineTo(ultimo.x * lado, ultimo.y * lado)
        canvas.drawPath(path, p)
    }

    private fun pintaTexto(t: CapaTexto, lado: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = t.tamano * lado
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun dibujarTexto(canvas: Canvas, lado: Float, t: CapaTexto) {
        if (t.texto.isBlank()) return
        val relleno = pintaTexto(t, lado).apply { color = t.color }
        val contorno = pintaTexto(t, lado).apply {
            style = Paint.Style.STROKE
            strokeWidth = relleno.textSize * 0.12f
            strokeJoin = Paint.Join.ROUND
            color = if (luminancia(t.color) > 0.5f) Color.BLACK else Color.WHITE
        }
        val lineas = t.texto.split('\n')
        val altoLinea = relleno.fontSpacing
        val inicioY = t.y * lado - altoLinea * (lineas.size - 1) / 2f -
            (relleno.ascent() + relleno.descent()) / 2f
        lineas.forEachIndexed { i, linea ->
            val y = inicioY + i * altoLinea
            canvas.drawText(linea, t.x * lado, y, contorno)
            canvas.drawText(linea, t.x * lado, y, relleno)
        }
    }

    /** Caja aproximada del texto (normalizada) para tocarlo y moverlo. */
    fun cajaTexto(t: CapaTexto, lado: Float): RectF {
        val p = pintaTexto(t, lado)
        val lineas = t.texto.split('\n')
        val ancho = (lineas.maxOfOrNull { p.measureText(it) } ?: 0f) / lado
        val alto = p.fontSpacing * lineas.size / lado
        val margen = 0.03f
        return RectF(
            t.x - ancho / 2f - margen, t.y - alto / 2f - margen,
            t.x + ancho / 2f + margen, t.y + alto / 2f + margen,
        )
    }

    private fun luminancia(c: Int): Float =
        (0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)) / 255f
}
