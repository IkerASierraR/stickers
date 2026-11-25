package com.stickers.creador.media

import com.stickers.creador.media.whatsapp.PaqueteParaValidar
import com.stickers.creador.media.whatsapp.StickerParaValidar
import com.stickers.creador.media.whatsapp.ValidadorPaquete
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorPaqueteTest {

    private fun vp8(): ByteArray {
        val d = ByteArray(30)
        d[3] = 0x9d.toByte(); d[4] = 0x01; d[5] = 0x2a
        d[6] = 0x00; d[7] = 0x02 // 512
        d[8] = 0x00; d[9] = 0x02
        return d
    }

    private val estatico = WebpContenedor.estatico(listOf(WebpContenedor.Chunk("VP8 ", vp8())))
    private val animado = WebpContenedor.animar(List(3) { estatico }, 512, 512, List(3) { 100 })
    private val png = ByteArray(40).also {
        byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()).copyInto(it)
        it[19] = 96; it[23] = 96
    }

    private fun paquete(animadoPack: Boolean, stickers: List<ByteArray>, emojis: String = "😀") = PaqueteParaValidar(
        identificador = "pack_abc",
        nombre = "Prueba",
        publicador = "Mis Stickers",
        animado = animadoPack,
        bandeja = png,
        stickers = stickers.mapIndexed { i, b -> StickerParaValidar(i + 1, b, emojis) },
    )

    @Test
    fun paqueteEstaticoValido() {
        assertTrue(ValidadorPaquete.validar(paquete(false, List(3) { estatico })).isEmpty())
    }

    @Test
    fun paqueteAnimadoValido() {
        assertTrue(ValidadorPaquete.validar(paquete(true, List(3) { animado })).isEmpty())
    }

    @Test
    fun menosDeTresStickersFalla() {
        val e = ValidadorPaquete.validar(paquete(false, List(2) { estatico }))
        assertTrue(e.any { it.startsWith("Faltan 1") })
    }

    @Test
    fun noSeMezclanTipos() {
        val e = ValidadorPaquete.validar(paquete(false, List(3) { animado }))
        assertTrue(e.any { it.contains("es animado") })
        val e2 = ValidadorPaquete.validar(paquete(true, List(3) { estatico }))
        assertTrue(e2.any { it.contains("no es animado") })
    }

    @Test
    fun estaticoDeMasDe100KbFalla() {
        val grande = WebpContenedor.estatico(listOf(WebpContenedor.Chunk("VP8 ", vp8() + ByteArray(110 * 1024))))
        val e = ValidadorPaquete.validar(paquete(false, listOf(grande, estatico, estatico)))
        assertTrue(e.any { it.contains("máximo 100 KB") })
    }

    @Test
    fun emojisObligatorios() {
        val e = ValidadorPaquete.validar(paquete(false, List(3) { estatico }, emojis = ""))
        assertTrue(e.any { it.contains("emojis") })
    }
}
