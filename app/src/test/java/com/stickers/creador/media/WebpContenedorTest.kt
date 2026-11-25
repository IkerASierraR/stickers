package com.stickers.creador.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebpContenedorTest {

    /** VP8 mínimo con cabecera de 512x512 (los datos de imagen no se decodifican aquí). */
    private fun vp8(ancho: Int, alto: Int, relleno: Int = 20): ByteArray {
        val d = ByteArray(10 + relleno)
        d[3] = 0x9d.toByte(); d[4] = 0x01; d[5] = 0x2a
        d[6] = (ancho and 0xFF).toByte(); d[7] = ((ancho shr 8) and 0x3F).toByte()
        d[8] = (alto and 0xFF).toByte(); d[9] = ((alto shr 8) and 0x3F).toByte()
        return d
    }

    private fun cuadroConAlfa(): ByteArray {
        val vp8x = ByteArray(10)
        vp8x[0] = 0x10
        return WebpContenedor.estatico(
            listOf(
                WebpContenedor.Chunk("VP8X", vp8x),
                WebpContenedor.Chunk("ALPH", ByteArray(7) { 1 }), // tamaño impar: prueba el relleno
                WebpContenedor.Chunk("VP8 ", vp8(512, 512)),
            ),
        )
    }

    @Test
    fun estaticoVp8SeLeeComo512() {
        val webp = WebpContenedor.estatico(listOf(WebpContenedor.Chunk("VP8 ", vp8(512, 512))))
        val info = WebpContenedor.info(webp)
        assertEquals(512, info.ancho)
        assertEquals(512, info.alto)
        assertFalse(info.animado)
        assertEquals(1, info.cuadros)
    }

    @Test
    fun animarGeneraWebpAnimadoValido() {
        val cuadros = List(5) { cuadroConAlfa() }
        val animado = WebpContenedor.animar(cuadros, 512, 512, List(5) { 100 })
        val info = WebpContenedor.info(animado)
        assertTrue(info.animado)
        assertEquals(512, info.ancho)
        assertEquals(512, info.alto)
        assertEquals(5, info.cuadros)
        assertEquals(500, info.duracionTotalMs)

        // Tamaño RIFF coherente con el archivo.
        val riff = (animado[4].toInt() and 0xFF) or ((animado[5].toInt() and 0xFF) shl 8) or
            ((animado[6].toInt() and 0xFF) shl 16) or ((animado[7].toInt() and 0xFF) shl 24)
        assertEquals(animado.size - 8, riff)

        val chunks = WebpContenedor.leerChunks(animado)
        assertEquals(listOf("VP8X", "ANIM") + List(5) { "ANMF" }, chunks.map { it.fourcc })
        // Cada ANMF contiene ALPH + VP8.
        val anmf = chunks[2].datos
        assertEquals("ALPH", String(anmf, 16, 4, Charsets.US_ASCII))
    }

    @Test
    fun datosDeCuadroVp8lDescartaAlph() {
        val vp8l = byteArrayOf(0x2F, 0xFF.toByte(), 0xC1.toByte(), 0x7F, 0x00, 0, 0, 0)
        val webp = WebpContenedor.estatico(listOf(WebpContenedor.Chunk("VP8L", vp8l)))
        val info = WebpContenedor.info(webp)
        assertEquals(512, info.ancho)
        assertEquals(512, info.alto)
        assertEquals(listOf("VP8L"), WebpContenedor.datosDeCuadro(webp).map { it.fourcc })
    }

    @Test(expected = WebpContenedor.FormatoInvalido::class)
    fun archivoQueNoEsWebpFalla() {
        WebpContenedor.info(ByteArray(30))
    }

    @Test
    fun dimensionesPng() {
        val png = ByteArray(24)
        byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()).copyInto(png)
        png[19] = 96; png[23] = 96
        assertEquals(96 to 96, WebpContenedor.dimensionesPng(png))
    }
}
