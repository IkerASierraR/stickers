package com.stickers.creador.media

import java.io.ByteArrayOutputStream

/**
 * Lectura y escritura del contenedor RIFF/WebP en Kotlin puro.
 *
 * Decisión T0.6: Android no codifica WebP animado y ffmpeg-kit fue retirado. En lugar de una
 * librería nativa, cada cuadro se codifica con `Bitmap.compress(WEBP)` (soporte nativo) y aquí se
 * "empaquetan" los cuadros en un WebP animado (chunks VP8X + ANIM + ANMF) según la especificación
 * oficial del contenedor WebP. Sin JNI, sin dependencias y sin tamaño extra en el APK.
 */
object WebpContenedor {

    data class Chunk(val fourcc: String, val datos: ByteArray)

    /** Información de un WebP (para el validador previo al envío). */
    data class Info(
        val ancho: Int,
        val alto: Int,
        val animado: Boolean,
        val cuadros: Int,
        val duracionesMs: List<Int>,
    ) {
        val duracionTotalMs: Int get() = duracionesMs.sum()
    }

    class FormatoInvalido(msg: String) : Exception(msg)

    // ---------------- Lectura ----------------

    fun leerChunks(bytes: ByteArray): List<Chunk> {
        if (bytes.size < 12 || str(bytes, 0) != "RIFF" || str(bytes, 8) != "WEBP") {
            throw FormatoInvalido("No es un archivo WebP")
        }
        val fin = minOf(bytes.size, 8 + le32(bytes, 4))
        val chunks = mutableListOf<Chunk>()
        var p = 12
        while (p + 8 <= fin) {
            val cc = str(bytes, p)
            val tam = le32(bytes, p + 4)
            if (tam < 0 || p + 8 + tam > bytes.size) throw FormatoInvalido("Chunk $cc truncado")
            chunks.add(Chunk(cc, bytes.copyOfRange(p + 8, p + 8 + tam)))
            p += 8 + tam + (tam and 1)
        }
        return chunks
    }

    fun info(bytes: ByteArray): Info {
        val chunks = leerChunks(bytes)
        val primero = chunks.firstOrNull() ?: throw FormatoInvalido("WebP vacío")
        return when (primero.fourcc) {
            "VP8X" -> {
                val d = primero.datos
                val animado = (d[0].toInt() and FLAG_ANIMACION) != 0
                val ancho = le24(d, 4) + 1
                val alto = le24(d, 7) + 1
                val anmf = chunks.filter { it.fourcc == "ANMF" }
                val duraciones = anmf.map { le24(it.datos, 12) }
                Info(ancho, alto, animado && anmf.isNotEmpty(), maxOf(1, anmf.size), duraciones)
            }
            "VP8 " -> {
                val (w, h) = dimensionesVp8(primero.datos)
                Info(w, h, false, 1, emptyList())
            }
            "VP8L" -> {
                val (w, h) = dimensionesVp8l(primero.datos)
                Info(w, h, false, 1, emptyList())
            }
            else -> throw FormatoInvalido("Chunk inicial desconocido: ${primero.fourcc}")
        }
    }

    private fun dimensionesVp8(d: ByteArray): Pair<Int, Int> {
        // 3 bytes de frame tag + 3 bytes de start code (9d 01 2a) + ancho/alto de 14 bits.
        if (d.size < 10) throw FormatoInvalido("VP8 truncado")
        val w = (u8(d, 6) or (u8(d, 7) shl 8)) and 0x3FFF
        val h = (u8(d, 8) or (u8(d, 9) shl 8)) and 0x3FFF
        return w to h
    }

    private fun dimensionesVp8l(d: ByteArray): Pair<Int, Int> {
        if (d.size < 5 || u8(d, 0) != 0x2F) throw FormatoInvalido("VP8L inválido")
        val bits = u8(d, 1) or (u8(d, 2) shl 8) or (u8(d, 3) shl 16) or (u8(d, 4) shl 24)
        val w = (bits and 0x3FFF) + 1
        val h = ((bits ushr 14) and 0x3FFF) + 1
        return w to h
    }

    /** Extrae los chunks de imagen (ALPH opcional + VP8/VP8L) de un WebP estático. */
    fun datosDeCuadro(webpEstatico: ByteArray): List<Chunk> {
        val chunks = leerChunks(webpEstatico)
        val imagen = chunks.filter { it.fourcc == "ALPH" || it.fourcc == "VP8 " || it.fourcc == "VP8L" }
        if (imagen.none { it.fourcc == "VP8 " || it.fourcc == "VP8L" }) {
            throw FormatoInvalido("El cuadro no contiene datos de imagen")
        }
        // Un VP8L ya incluye alfa: el ALPH solo acompaña a VP8.
        return if (imagen.any { it.fourcc == "VP8L" }) imagen.filter { it.fourcc == "VP8L" } else imagen
    }

    // ---------------- Escritura ----------------

    /**
     * Une cuadros WebP estáticos (todos del mismo tamaño) en un WebP animado en bucle infinito.
     * Cada cuadro ocupa el lienzo completo, sin mezcla con el anterior: el primer cuadro es la
     * imagen completa, como exige WhatsApp.
     */
    fun animar(cuadros: List<ByteArray>, ancho: Int, alto: Int, duracionesMs: List<Int>): ByteArray {
        require(cuadros.isNotEmpty()) { "Sin cuadros" }
        require(cuadros.size == duracionesMs.size) { "Cuadros y duraciones no coinciden" }

        val cuerpo = ByteArrayOutputStream()

        // VP8X: flags (animación + alfa), 3 reservados, ancho-1 y alto-1 en 24 bits.
        val vp8x = ByteArray(10)
        vp8x[0] = (FLAG_ANIMACION or FLAG_ALFA).toByte()
        put24(vp8x, 4, ancho - 1)
        put24(vp8x, 7, alto - 1)
        escribirChunk(cuerpo, "VP8X", vp8x)

        // ANIM: color de fondo (BGRA transparente) + número de repeticiones (0 = infinito).
        escribirChunk(cuerpo, "ANIM", ByteArray(6))

        cuadros.forEachIndexed { i, cuadro ->
            val imagen = datosDeCuadro(cuadro)
            val anmf = ByteArrayOutputStream()
            val cab = ByteArray(16)
            put24(cab, 0, 0) // X / 2
            put24(cab, 3, 0) // Y / 2
            put24(cab, 6, ancho - 1)
            put24(cab, 9, alto - 1)
            put24(cab, 12, duracionesMs[i].coerceIn(1, 0xFFFFFF))
            cab[15] = 0x02 // blending: no mezclar; disposal: ninguno
            anmf.write(cab)
            imagen.forEach { escribirChunk(anmf, it.fourcc, it.datos) }
            escribirChunk(cuerpo, "ANMF", anmf.toByteArray())
        }

        val datos = cuerpo.toByteArray()
        val salida = ByteArrayOutputStream(datos.size + 12)
        salida.write("RIFF".toByteArray(Charsets.US_ASCII))
        salida.write(le32Bytes(datos.size + 4))
        salida.write("WEBP".toByteArray(Charsets.US_ASCII))
        salida.write(datos)
        return salida.toByteArray()
    }

    fun escribirChunk(out: ByteArrayOutputStream, fourcc: String, datos: ByteArray) {
        require(fourcc.length == 4)
        out.write(fourcc.toByteArray(Charsets.US_ASCII))
        out.write(le32Bytes(datos.size))
        out.write(datos)
        if (datos.size and 1 == 1) out.write(0)
    }

    /** Crea un WebP estático mínimo a partir de chunks (útil en pruebas). */
    fun estatico(chunks: List<Chunk>): ByteArray {
        val cuerpo = ByteArrayOutputStream()
        chunks.forEach { escribirChunk(cuerpo, it.fourcc, it.datos) }
        val d = cuerpo.toByteArray()
        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        out.write(le32Bytes(d.size + 4))
        out.write("WEBP".toByteArray(Charsets.US_ASCII))
        out.write(d)
        return out.toByteArray()
    }

    /** Ancho y alto de un PNG (cabecera IHDR), para validar el ícono. */
    fun dimensionesPng(bytes: ByteArray): Pair<Int, Int>? {
        if (bytes.size < 24) return null
        val firma = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        if (!bytes.copyOfRange(0, 4).contentEquals(firma)) return null
        return be32(bytes, 16) to be32(bytes, 20)
    }

    // ---------------- Utilidades de bytes ----------------

    private const val FLAG_ANIMACION = 0x02
    private const val FLAG_ALFA = 0x10

    private fun u8(b: ByteArray, i: Int) = b[i].toInt() and 0xFF
    private fun str(b: ByteArray, i: Int) = String(b, i, 4, Charsets.US_ASCII)
    private fun le24(b: ByteArray, i: Int) = u8(b, i) or (u8(b, i + 1) shl 8) or (u8(b, i + 2) shl 16)
    private fun le32(b: ByteArray, i: Int) = le24(b, i) or (u8(b, i + 3) shl 24)
    private fun be32(b: ByteArray, i: Int) =
        (u8(b, i) shl 24) or (u8(b, i + 1) shl 16) or (u8(b, i + 2) shl 8) or u8(b, i + 3)

    private fun put24(b: ByteArray, i: Int, v: Int) {
        b[i] = (v and 0xFF).toByte()
        b[i + 1] = ((v shr 8) and 0xFF).toByte()
        b[i + 2] = ((v shr 16) and 0xFF).toByte()
    }

    private fun le32Bytes(v: Int) = byteArrayOf(
        (v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte(),
    )
}
