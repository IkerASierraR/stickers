package com.stickers.creador.media.whatsapp

import com.stickers.creador.domain.LimitesWhatsApp
import com.stickers.creador.domain.ReglasCarpeta
import com.stickers.creador.domain.ReglasEmojis
import com.stickers.creador.media.WebpContenedor

class StickerParaValidar(
    val numero: Int,
    val bytes: ByteArray?,
    val emojis: String,
)

class PaqueteParaValidar(
    val identificador: String,
    val nombre: String,
    val publicador: String,
    val animado: Boolean,
    val bandeja: ByteArray?,
    val stickers: List<StickerParaValidar>,
)

/**
 * Validador previo al envío (T5.2), con las mismas reglas que `StickerPackValidator` del sample
 * oficial de WhatsApp, pero con mensajes claros en español. Kotlin puro (se prueba con JUnit).
 */
object ValidadorPaquete {

    fun validar(p: PaqueteParaValidar): List<String> {
        val errores = mutableListOf<String>()
        val max = LimitesWhatsApp.MAX_CARACTERES

        if (!ReglasCarpeta.identificadorValido(p.identificador)) errores += "El identificador del paquete no es válido"
        if (p.nombre.isBlank()) errores += "La carpeta necesita un nombre"
        if (p.nombre.length > max) errores += "El nombre supera $max caracteres"
        if (p.publicador.isBlank() || p.publicador.length > max) errores += "El autor del paquete no es válido"

        val n = p.stickers.size
        if (n < LimitesWhatsApp.MIN_STICKERS) {
            errores += "Faltan ${LimitesWhatsApp.MIN_STICKERS - n} stickers (mínimo ${LimitesWhatsApp.MIN_STICKERS})"
        }
        if (n > LimitesWhatsApp.MAX_STICKERS) errores += "Máximo ${LimitesWhatsApp.MAX_STICKERS} stickers por carpeta"

        // Ícono de bandeja
        val bandeja = p.bandeja
        if (bandeja == null) {
            errores += "No se pudo generar el ícono de la carpeta"
        } else {
            if (bandeja.size > LimitesWhatsApp.MAX_BYTES_ICONO) errores += "El ícono pesa más de 50 KB"
            val dim = WebpContenedor.dimensionesPng(bandeja)
            if (dim == null) {
                errores += "El ícono no es un PNG válido"
            } else {
                val rango = LimitesWhatsApp.MIN_ICONO_PX..LimitesWhatsApp.MAX_ICONO_PX
                if (dim.first !in rango || dim.second !in rango) errores += "El ícono debe medir entre 24 y 512 px"
            }
        }

        p.stickers.forEach { s -> errores += validarSticker(s, p.animado) }
        return errores
    }

    fun validarSticker(s: StickerParaValidar, animado: Boolean): List<String> {
        val e = mutableListOf<String>()
        val nombre = "Sticker ${s.numero}"
        if (!ReglasEmojis.esValido(s.emojis)) e += "$nombre: debe tener de 1 a 3 emojis"
        val bytes = s.bytes
        if (bytes == null) {
            e += "$nombre: falta el archivo"
            return e
        }
        val limite = if (animado) LimitesWhatsApp.MAX_BYTES_ANIMADO else LimitesWhatsApp.MAX_BYTES_ESTATICO
        if (bytes.size > limite) e += "$nombre: pesa ${bytes.size / 1024} KB (máximo ${limite / 1024} KB)"
        val info = try {
            WebpContenedor.info(bytes)
        } catch (ex: WebpContenedor.FormatoInvalido) {
            e += "$nombre: no es un WebP válido"
            return e
        }
        val lado = LimitesWhatsApp.TAMANO_STICKER_PX
        if (info.ancho != lado || info.alto != lado) e += "$nombre: mide ${info.ancho}x${info.alto} (debe ser 512x512)"
        if (animado) {
            if (!info.animado || info.cuadros < 2) e += "$nombre: no es animado (la carpeta es animada)"
            if (info.duracionesMs.any { it < LimitesWhatsApp.MIN_DURACION_CUADRO_MS }) {
                e += "$nombre: hay cuadros de menos de 8 ms"
            }
            if (info.duracionTotalMs > LimitesWhatsApp.MAX_DURACION_ANIMADO_MS) e += "$nombre: dura más de 10 s"
        } else if (info.animado) {
            e += "$nombre: es animado (la carpeta es de imagen)"
        }
        return e
    }
}
