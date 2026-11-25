package com.stickers.creador.media.whatsapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.stickers.creador.BuildConfig
import com.stickers.creador.data.ArchivosStickers
import com.stickers.creador.data.StickerRepositorio
import com.stickers.creador.domain.TipoCarpeta
import com.stickers.creador.media.image.ProcesadorImagen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface PreparacionEnvio {
    class Lista(val intent: Intent) : PreparacionEnvio
    class Errores(val mensajes: List<String>) : PreparacionEnvio
}

/** Prepara y lanza el Intent oficial "Agregar a WhatsApp" (T5.1–T5.3). */
class EnvioWhatsApp(
    private val context: Context,
    private val repositorio: StickerRepositorio,
    private val procesadorImagen: ProcesadorImagen,
) {

    private fun instalado(paquete: String): Boolean = try {
        context.packageManager.getPackageInfo(paquete, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** T5.3: genera tray.png (96x96) desde el ícono elegido o, si no hay, el primer sticker (R6). */
    suspend fun generarBandeja(carpetaId: Long): ByteArray? {
        val carpeta = repositorio.carpeta(carpetaId) ?: return null
        val archivos: ArchivosStickers = repositorio.archivos
        val origen = carpeta.iconoRuta?.takeIf { File(it).exists() }
            ?: repositorio.stickersDe(carpetaId).firstOrNull()?.webpRuta
            ?: return null
        val png = try {
            procesadorImagen.iconoDesdeSticker(origen)
        } catch (_: Exception) {
            return null
        }
        withContext(Dispatchers.IO) { archivos.guardar(carpetaId, ArchivosStickers.NOMBRE_BANDEJA, png) }
        return png
    }

    suspend fun preparar(carpetaId: Long): PreparacionEnvio {
        val carpeta = repositorio.carpeta(carpetaId)
            ?: return PreparacionEnvio.Errores(listOf("La carpeta no existe"))
        val stickers = repositorio.stickersDe(carpetaId)
        val bandeja = generarBandeja(carpetaId)
        val paquete = withContext(Dispatchers.IO) {
            PaqueteParaValidar(
                identificador = carpeta.identificador,
                nombre = carpeta.nombre,
                publicador = StickerContentProvider.PUBLICADOR,
                animado = carpeta.tipo == TipoCarpeta.ANIMADO,
                bandeja = bandeja,
                stickers = stickers.mapIndexed { i, s ->
                    StickerParaValidar(i + 1, File(s.webpRuta).takeIf { it.exists() }?.readBytes(), s.emojis)
                },
            )
        }
        val errores = ValidadorPaquete.validar(paquete)
        if (errores.isNotEmpty()) return PreparacionEnvio.Errores(errores)

        val normal = instalado(WHATSAPP)
        val business = instalado(WHATSAPP_BUSINESS)
        if (!normal && !business) {
            return PreparacionEnvio.Errores(listOf("WhatsApp no está instalado en este celular"))
        }
        val intent = Intent(ACCION_AGREGAR).apply {
            putExtra(EXTRA_ID, carpeta.identificador)
            putExtra(EXTRA_AUTORIDAD, BuildConfig.CONTENT_PROVIDER_AUTHORITY)
            putExtra(EXTRA_NOMBRE, carpeta.nombre)
        }
        val final = when {
            normal && business -> Intent.createChooser(intent, "Agregar a…")
            normal -> intent.setPackage(WHATSAPP)
            else -> intent.setPackage(WHATSAPP_BUSINESS)
        }
        return PreparacionEnvio.Lista(final)
    }

    /** Traduce el resultado de WhatsApp a un mensaje para el usuario (null = agregado con éxito). */
    fun interpretarResultado(codigo: Int, datos: Intent?): String? = when (codigo) {
        Activity.RESULT_OK -> null
        else -> datos?.getStringExtra("validation_error")?.let { "WhatsApp rechazó el paquete: $it" }
            ?: "No se agregó el paquete"
    }

    companion object {
        const val WHATSAPP = "com.whatsapp"
        const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
        const val ACCION_AGREGAR = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
        const val EXTRA_ID = "sticker_pack_id"
        const val EXTRA_AUTORIDAD = "sticker_pack_authority"
        const val EXTRA_NOMBRE = "sticker_pack_name"
    }
}
