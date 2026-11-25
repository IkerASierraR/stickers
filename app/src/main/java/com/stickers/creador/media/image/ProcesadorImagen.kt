package com.stickers.creador.media.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.stickers.creador.domain.LimitesWhatsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** El resultado supera el límite de WhatsApp incluso con la menor calidad (R25). */
class DemasiadoGrandeException(mensaje: String) : Exception(mensaje)

/** Operaciones de imagen: decodificar, codificar WebP estático, íconos. Fuera del hilo principal. */
class ProcesadorImagen(private val context: Context) {

    /** Decodifica una imagen del Photo Picker respetando la rotación EXIF y limitando su tamaño. */
    suspend fun decodificar(uri: Uri, maxLado: Int = 1600): Bitmap = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) {
            throw IllegalArgumentException("No se pudo leer la imagen")
        }
        var muestra = 1
        while (max(opciones.outWidth, opciones.outHeight) / (muestra * 2) >= maxLado) muestra *= 2
        val decod = BitmapFactory.Options().apply {
            inSampleSize = muestra
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decod) }
            ?: throw IllegalArgumentException("No se pudo leer la imagen")
        val rotacion = try {
            resolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) {
            0f
        }
        val orientada = if (rotacion != 0f) {
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotacion) }, true)
                .also { if (it !== bmp) bmp.recycle() }
        } else bmp
        escalarSiHaceFalta(orientada, maxLado)
    }

    fun escalarSiHaceFalta(bmp: Bitmap, maxLado: Int): Bitmap {
        val lado = max(bmp.width, bmp.height)
        if (lado <= maxLado) return bmp
        val f = maxLado.toFloat() / lado
        return Bitmap.createScaledBitmap(bmp, (bmp.width * f).toInt().coerceAtLeast(1), (bmp.height * f).toInt().coerceAtLeast(1), true)
    }

    /**
     * Codifica un sticker estático (512x512) a WebP bajando la calidad hasta cumplir
     * <= 100 KB (R23, R25).
     */
    suspend fun codificarEstatico(bmp: Bitmap): ByteArray = withContext(Dispatchers.Default) {
        require(bmp.width == LimitesWhatsApp.TAMANO_STICKER_PX && bmp.height == LimitesWhatsApp.TAMANO_STICKER_PX)
        for (calidad in intArrayOf(95, 90, 85, 80, 70, 60, 50, 40, 30, 20, 10, 5)) {
            val bytes = comprimirWebp(bmp, calidad)
            if (bytes.size <= LimitesWhatsApp.MAX_BYTES_ESTATICO) return@withContext bytes
        }
        throw DemasiadoGrandeException("El sticker supera 100 KB incluso con la calidad mínima")
    }

    /** Ícono de bandeja: PNG 96x96 <= 50 KB (R24), recortado al centro. */
    suspend fun generarIcono(fuente: Bitmap): ByteArray = withContext(Dispatchers.Default) {
        val lado = LimitesWhatsApp.TAMANO_ICONO_PX
        val salida = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val c = Canvas(salida)
        val escala = max(lado.toFloat() / fuente.width, lado.toFloat() / fuente.height)
        val w = fuente.width * escala
        val h = fuente.height * escala
        c.drawBitmap(
            fuente, null,
            RectF((lado - w) / 2f, (lado - h) / 2f, (lado + w) / 2f, (lado + h) / 2f),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
        var png = ByteArrayOutputStream().use { salida.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
        if (png.size > LimitesWhatsApp.MAX_BYTES_ICONO) {
            // Muy raro a 96x96; como último recurso se reduce a 72x72 (el validador acepta 24..512).
            val menor = Bitmap.createScaledBitmap(salida, 72, 72, true)
            png = ByteArrayOutputStream().use { menor.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
        }
        png
    }

    /** Ícono a partir de una imagen de la galería. */
    suspend fun iconoDesdeUri(uri: Uri): ByteArray = generarIcono(decodificar(uri, 512))

    /** Ícono a partir de un sticker WebP (primer cuadro si es animado). */
    suspend fun iconoDesdeSticker(ruta: String): ByteArray {
        val bmp = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(ruta) }
            ?: throw IllegalStateException("No se pudo leer el sticker para el ícono")
        return generarIcono(bmp)
    }

    fun decodificarArchivo(ruta: String): Bitmap? = if (File(ruta).exists()) BitmapFactory.decodeFile(ruta) else null

    companion object {
        /** WebP con pérdida (con alfa). En API < 30 `WEBP` con calidad < 100 es con pérdida. */
        @Suppress("DEPRECATION")
        fun comprimirWebp(bmp: Bitmap, calidad: Int): ByteArray {
            val formato = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            return ByteArrayOutputStream().use {
                bmp.compress(formato, min(calidad, 99), it)
                it.toByteArray()
            }
        }
    }
}
