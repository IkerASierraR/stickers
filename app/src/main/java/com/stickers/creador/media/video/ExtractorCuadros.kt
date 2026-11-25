package com.stickers.creador.media.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.max

/** Lee cuadros de un video con MediaMetadataRetriever (solo el tramo elegido). */
class ExtractorCuadros(private val context: Context, private val uri: Uri) : AutoCloseable {

    private val mmr = MediaMetadataRetriever().apply { setDataSource(context, uri) }

    val duracionMs: Long =
        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

    val tieneAudio: Boolean =
        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"

    /** Ancho y alto ya rotados (como se ve el video). */
    val tamano: Pair<Int, Int> by lazy {
        val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rot = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (rot == 90 || rot == 270) h to w else w to h
    }

    /** Cuadro en [tiempoMs] escalado para que su lado mayor sea como mucho [maxLado]. */
    fun cuadro(tiempoMs: Long, maxLado: Int, exacto: Boolean = true): Bitmap? {
        val us = tiempoMs * 1000
        val opcion = if (exacto) MediaMetadataRetriever.OPTION_CLOSEST else MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        val (w, h) = tamano
        val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && w > 0 && h > 0) {
            val f = minOf(1f, maxLado.toFloat() / max(w, h))
            mmr.getScaledFrameAtTime(us, opcion, (w * f).toInt().coerceAtLeast(1), (h * f).toInt().coerceAtLeast(1))
        } else {
            mmr.getFrameAtTime(us, opcion)?.let { escalar(it, maxLado) }
        }
        return bmp?.let { asegurarArgb(it) }
    }

    /** Cuadros del tramo [inicioMs, finMs) a [fps] cuadros por segundo. */
    suspend fun cuadrosTramo(
        inicioMs: Long,
        finMs: Long,
        fps: Int,
        maxLado: Int,
        onProgreso: (Float) -> Unit = {},
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val paso = 1000.0 / fps
        val total = max(1, ((finMs - inicioMs) / paso).toInt())
        val res = ArrayList<Bitmap>(total)
        var anterior: Bitmap? = null
        for (i in 0 until total) {
            coroutineContext.ensureActive()
            val t = inicioMs + (i * paso).toLong()
            val c = cuadro(t, maxLado) ?: anterior
            if (c != null) {
                res.add(c); anterior = c
            }
            onProgreso((i + 1f) / total)
        }
        res
    }

    private fun escalar(bmp: Bitmap, maxLado: Int): Bitmap {
        val lado = max(bmp.width, bmp.height)
        if (lado <= maxLado) return bmp
        val f = maxLado.toFloat() / lado
        return Bitmap.createScaledBitmap(bmp, (bmp.width * f).toInt(), (bmp.height * f).toInt(), true)
            .also { if (it !== bmp) bmp.recycle() }
    }

    private fun asegurarArgb(bmp: Bitmap): Bitmap =
        if (bmp.config == Bitmap.Config.ARGB_8888) bmp
        else bmp.copy(Bitmap.Config.ARGB_8888, false).also { bmp.recycle() }

    override fun close() {
        try {
            mmr.release()
        } catch (_: Exception) {
        }
    }
}
