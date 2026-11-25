package com.stickers.creador.media.video

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.stickers.creador.data.ArchivosStickers
import com.stickers.creador.domain.LimitesWhatsApp
import com.stickers.creador.domain.ReglasTramo
import com.stickers.creador.editor.EstadoEditor
import com.stickers.creador.editor.RenderSticker
import com.stickers.creador.media.WebpContenedor
import com.stickers.creador.media.image.DemasiadoGrandeException
import com.stickers.creador.media.image.ProcesadorImagen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/** Archivos generados a partir de un video (en temporales). */
class ResultadoVideo(
    val webp: File,
    val audio: File?,
    val videoCorto: File?,
    val avisos: List<String>,
)

/**
 * Video → sticker animado + audio + video corto (R18). Todo fuera del hilo principal, con progreso.
 */
class ProcesadorVideo(private val context: Context, private val archivos: ArchivosStickers) {

    private val media3 = RecortesMedia3(context)

    suspend fun procesar(
        uri: Uri,
        inicioMs: Long,
        finMs: Long,
        estado: EstadoEditor,
        onProgreso: (etapa: String, progreso: Float) -> Unit,
    ): ResultadoVideo {
        require(ReglasTramo.esValido(inicioMs, finMs)) { "El tramo debe durar entre 0 y 5 segundos" }
        val avisos = mutableListOf<String>()
        val tieneAudio: Boolean

        // 1) Sticker animado (cuadros → WebP animado ≤ 500 KB).
        onProgreso("Leyendo el video…", 0f)
        val webp = archivos.temporal(".webp")
        val cuadros: List<Bitmap>
        ExtractorCuadros(context, uri).use { ex ->
            tieneAudio = ex.tieneAudio
            val fuentes = ex.cuadrosTramo(inicioMs, finMs, FPS_BASE, maxLado = 720) { p ->
                onProgreso("Leyendo el video…", p * 0.35f)
            }
            if (fuentes.isEmpty()) throw IllegalStateException("No se pudieron leer cuadros del video")
            cuadros = withContext(Dispatchers.Default) {
                fuentes.mapIndexed { i, f ->
                    coroutineContext.ensureActive()
                    RenderSticker.renderizar(f, estado, LimitesWhatsApp.TAMANO_STICKER_PX).also {
                        f.recycle()
                        onProgreso("Creando sticker…", 0.35f + 0.1f * (i + 1) / fuentes.size)
                    }
                }
            }
        }
        val bytes = codificarAnimado(cuadros) { p -> onProgreso("Creando sticker…", 0.45f + 0.2f * p) }
        cuadros.forEach { it.recycle() }
        withContext(Dispatchers.IO) { webp.writeBytes(bytes) }

        // 2) Audio del tramo.
        var audio: File? = null
        if (tieneAudio) {
            val f = archivos.temporal(".m4a")
            try {
                media3.extraerAudio(uri, inicioMs, finMs, f) { p -> onProgreso("Guardando audio…", 0.65f + 0.1f * p) }
                audio = f.takeIf { it.exists() && it.length() > 0 }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                f.delete()
                avisos.add("No se pudo guardar el audio: ${e.message}")
            }
        } else {
            avisos.add("El video no tiene audio")
        }

        // 3) Video corto con sonido (para el sticker con sonido dentro de WhatsApp).
        var video: File? = null
        val fv = archivos.temporal(".mp4")
        try {
            media3.generarVideoCorto(uri, inicioMs, finMs, estado, fv) { p ->
                onProgreso("Creando video con sonido…", 0.75f + 0.25f * p)
            }
            video = fv.takeIf { it.exists() && it.length() > 0 }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fv.delete()
            avisos.add("No se pudo crear el video con sonido: ${e.message}")
        }

        onProgreso("Listo", 1f)
        return ResultadoVideo(webp, audio, video, avisos)
    }

    /**
     * Ajuste automático (R25): prueba menos cuadros por segundo y menor calidad hasta que el
     * WebP animado pese ≤ 500 KB. Los cuadros codificados se reutilizan entre intentos.
     */
    suspend fun codificarAnimado(cuadros: List<Bitmap>, onProgreso: (Float) -> Unit): ByteArray =
        withContext(Dispatchers.Default) {
            val cache = HashMap<Long, ByteArray>()
            fun cuadro(i: Int, q: Int): ByteArray = cache.getOrPut(i.toLong() * 1000 + q) {
                ProcesadorImagen.comprimirWebp(cuadros[i], q)
            }

            val lado = LimitesWhatsApp.TAMANO_STICKER_PX
            val opcionesFps = FPS_OPCIONES.filter { it <= FPS_BASE }
            opcionesFps.forEachIndexed { n, fps ->
                coroutineContext.ensureActive()
                onProgreso(n.toFloat() / opcionesFps.size)
                val indices = indicesPara(cuadros.size, fps)
                val duracion = (1000f / fps).roundToInt().coerceAtLeast(LimitesWhatsApp.MIN_DURACION_CUADRO_MS)

                fun intentar(q: Int): ByteArray =
                    WebpContenedor.animar(indices.map { cuadro(it, q) }, lado, lado, List(indices.size) { duracion })

                val minimo = intentar(CALIDADES.first())
                if (minimo.size > LimitesWhatsApp.MAX_BYTES_ANIMADO) return@forEachIndexed

                // Búsqueda binaria de la mayor calidad que entra en el límite.
                var mejor = minimo
                var lo = 1
                var hi = CALIDADES.lastIndex
                while (lo <= hi) {
                    coroutineContext.ensureActive()
                    val mid = (lo + hi) / 2
                    val r = intentar(CALIDADES[mid])
                    if (r.size <= LimitesWhatsApp.MAX_BYTES_ANIMADO) {
                        mejor = r; lo = mid + 1
                    } else {
                        hi = mid - 1
                    }
                }
                onProgreso(1f)
                return@withContext mejor
            }
            throw DemasiadoGrandeException(
                "El sticker animado supera 500 KB incluso con la menor calidad. Prueba un tramo más corto o con menos movimiento.",
            )
        }

    /** Índices de cuadros para bajar de [FPS_BASE] a [fps]; siempre al menos 2 (animado). */
    private fun indicesPara(total: Int, fps: Int): List<Int> {
        if (total <= 1) return listOf(0, 0)
        val salto = FPS_BASE.toFloat() / fps
        val res = mutableListOf<Int>()
        var x = 0f
        while (x.roundToInt() < total) {
            res.add(x.roundToInt()); x += salto
        }
        return if (res.size >= 2) res.distinct().let { if (it.size >= 2) it else listOf(0, total - 1) } else listOf(0, total - 1)
    }

    companion object {
        const val FPS_BASE = 12
        val FPS_OPCIONES = listOf(12, 10, 8, 6, 5, 4)
        val CALIDADES = listOf(8, 15, 25, 35, 45, 55, 65, 75, 85)
    }
}
