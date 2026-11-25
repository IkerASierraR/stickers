package com.stickers.creador.media.video

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import com.stickers.creador.editor.EstadoEditor
import com.stickers.creador.editor.RenderSticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Recorte de tiempo, extracción de audio y video corto con Media3 Transformer (oficial de Google).
 * Transformer debe usarse desde un hilo con Looper: se ejecuta en el hilo principal, pero el
 * trabajo pesado (decodificar/codificar) lo hace Media3 en sus propios hilos.
 */
@OptIn(UnstableApi::class)
class RecortesMedia3(private val context: Context) {

    private fun itemRecortado(uri: Uri, inicioMs: Long, finMs: Long): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(inicioMs)
                    .setEndPositionMs(finMs)
                    .build(),
            )
            .build()

    /** Audio del tramo en AAC dentro de un contenedor MP4 (.m4a) (R18.2). */
    suspend fun extraerAudio(uri: Uri, inicioMs: Long, finMs: Long, salida: File, onProgreso: (Float) -> Unit) {
        val editado = EditedMediaItem.Builder(itemRecortado(uri, inicioMs, finMs))
            .setRemoveVideo(true)
            .build()
        exportar(editado, salida, onProgreso) { it.setAudioMimeType(MimeTypes.AUDIO_AAC) }
    }

    /**
     * Video corto con sonido: mismo tramo y mismo encuadre que el sticker, 512x512, con el texto
     * y el dibujo encima (R18.3).
     */
    suspend fun generarVideoCorto(
        uri: Uri,
        inicioMs: Long,
        finMs: Long,
        estado: EstadoEditor,
        salida: File,
        onProgreso: (Float) -> Unit,
    ) {
        val capa: Bitmap = RenderSticker.capaVideoCorto(estado, LADO)
        val matriz = RenderSticker.matrizNdc(estado)
        val efectosVideo: List<Effect> = listOf(
            // 1) Ajustar el cuadro completo dentro de 512x512 (igual que el lienzo del editor).
            Presentation.createForWidthAndHeight(LADO, LADO, Presentation.LAYOUT_SCALE_TO_FIT),
            // 2) Zoom y desplazamiento elegidos en el editor.
            MatrixTransformation { _ -> matriz },
            // 3) Texto, dibujo y máscara del círculo.
            OverlayEffect(ImmutableList.of<TextureOverlay>(BitmapOverlay.createStaticBitmapOverlay(capa))),
        )
        val editado = EditedMediaItem.Builder(itemRecortado(uri, inicioMs, finMs))
            .setEffects(Effects(emptyList<AudioProcessor>(), efectosVideo))
            .build()
        exportar(editado, salida, onProgreso) {
            it.setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
        }
    }

    private suspend fun exportar(
        editado: EditedMediaItem,
        salida: File,
        onProgreso: (Float) -> Unit,
        configurar: (Transformer.Builder) -> Transformer.Builder,
    ) = withContext(Dispatchers.Main) {
        salida.parentFile?.mkdirs()
        if (salida.exists()) salida.delete()
        coroutineScope {
            var transformer: Transformer? = null
            val sondeo = launch {
                val holder = ProgressHolder()
                while (isActive) {
                    val t = transformer
                    if (t != null && t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgreso(holder.progress / 100f)
                    }
                    delay(200)
                }
            }
            try {
                suspendCancellableCoroutine { cont ->
                    val t = configurar(Transformer.Builder(context))
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                if (cont.isActive) cont.resume(Unit)
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException,
                            ) {
                                if (cont.isActive) cont.resumeWithException(exportException)
                            }
                        })
                        .build()
                    transformer = t
                    // cancel() debe llamarse en el hilo del Transformer (principal).
                    cont.invokeOnCancellation {
                        android.os.Handler(android.os.Looper.getMainLooper()).post { t.cancel() }
                    }
                    t.start(editado, salida.absolutePath)
                }
                onProgreso(1f)
            } finally {
                sondeo.cancel()
            }
        }
    }

    companion object {
        const val LADO = 512
    }
}
