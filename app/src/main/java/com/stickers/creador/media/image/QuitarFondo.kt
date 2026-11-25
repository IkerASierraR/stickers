package com.stickers.creador.media.image

import android.graphics.Bitmap
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/**
 * Quitar fondo con ML Kit Subject Segmentation (R10). Corre en el celular: la foto no sale del
 * dispositivo. El modelo lo descarga Google Play Services la primera vez.
 */
class QuitarFondo {

    private val segmentador by lazy {
        SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build(),
        )
    }

    /** Devuelve un bitmap del mismo tamaño con el fondo transparente. */
    suspend fun quitar(fuente: Bitmap): Bitmap {
        val entrada = withContext(Dispatchers.Default) { limitar(fuente, 1280) }
        val resultado = suspendCancellableCoroutine { cont ->
            segmentador.process(InputImage.fromBitmap(entrada, 0))
                .addOnSuccessListener { r -> cont.resume(r) }
                .addOnFailureListener { e -> cont.resumeWithException(traducir(e)) }
        }
        val primerPlano = resultado.foregroundBitmap
            ?: throw IllegalStateException("No se encontró un sujeto en la foto")
        return withContext(Dispatchers.Default) {
            if (primerPlano.width == fuente.width && primerPlano.height == fuente.height) primerPlano
            else Bitmap.createScaledBitmap(primerPlano, fuente.width, fuente.height, true)
        }
    }

    private fun limitar(bmp: Bitmap, maxLado: Int): Bitmap {
        val lado = max(bmp.width, bmp.height)
        if (lado <= maxLado) return bmp
        val f = maxLado.toFloat() / lado
        return Bitmap.createScaledBitmap(bmp, (bmp.width * f).toInt(), (bmp.height * f).toInt(), true)
    }

    private fun traducir(e: Exception): Exception =
        if (e is MlKitException && e.errorCode == MlKitException.UNAVAILABLE) {
            IllegalStateException(
                "El modelo para quitar el fondo aún se está descargando en Google Play Services. " +
                    "Inténtalo de nuevo en unos minutos.",
                e,
            )
        } else {
            IllegalStateException("No se pudo quitar el fondo: ${e.message ?: "error desconocido"}", e)
        }
}
