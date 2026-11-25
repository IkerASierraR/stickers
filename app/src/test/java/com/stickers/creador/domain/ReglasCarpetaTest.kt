package com.stickers.creador.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasCarpetaTest {

    @Test
    fun carpetaCon30NoAceptaMas() {
        assertFalse(ReglasCarpeta.puedeAgregarSticker(30))
        assertTrue(ReglasCarpeta.puedeAgregarSticker(29))
        assertTrue(ReglasCarpeta.puedeAgregarSticker(0))
    }

    @Test
    fun carpetaCon2NoHabilitaAgregarAWhatsApp() {
        assertFalse(ReglasCarpeta.puedeEnviar(2))
        assertEquals(1, ReglasCarpeta.faltanParaEnviar(2))
        assertTrue(ReglasCarpeta.puedeEnviar(3))
        assertTrue(ReglasCarpeta.puedeEnviar(30))
        assertEquals(0, ReglasCarpeta.faltanParaEnviar(5))
    }

    @Test
    fun noSePuedeMoverAnimadoACarpetaDeImagen() {
        assertFalse(
            ReglasCarpeta.puedeMover(TipoCarpeta.ANIMADO, 1, TipoCarpeta.IMAGEN, 2, cantidadDestino = 0),
        )
    }

    @Test
    fun moverSoloAMismoTipoConEspacioYDistintaCarpeta() {
        assertTrue(ReglasCarpeta.puedeMover(TipoCarpeta.IMAGEN, 1, TipoCarpeta.IMAGEN, 2, 10))
        assertFalse(ReglasCarpeta.puedeMover(TipoCarpeta.IMAGEN, 1, TipoCarpeta.IMAGEN, 2, 30))
        assertFalse(ReglasCarpeta.puedeMover(TipoCarpeta.IMAGEN, 1, TipoCarpeta.IMAGEN, 1, 5))
    }

    @Test
    fun editarCarpetaIncrementaVersion() {
        assertEquals(2, ReglasCarpeta.siguienteVersion(1))
        assertEquals(8, ReglasCarpeta.siguienteVersion(7))
    }

    @Test
    fun nombreObligatorioYConLimite() {
        assertNotNull(ReglasCarpeta.validarNombre("   "))
        assertNotNull(ReglasCarpeta.validarNombre("a".repeat(129)))
        assertNull(ReglasCarpeta.validarNombre("Memes"))
    }

    @Test
    fun identificadorSoloCaracteresPermitidos() {
        assertTrue(ReglasCarpeta.identificadorValido("pack_0123abcd"))
        assertFalse(ReglasCarpeta.identificadorValido("pack/x"))
        assertFalse(ReglasCarpeta.identificadorValido("a..b"))
        assertFalse(ReglasCarpeta.identificadorValido(""))
    }

    @Test
    fun tramoMaximoCincoSegundos() {
        assertEquals(1_000L to 6_000L, ReglasTramo.ajustar(1_000, 9_000, 20_000))
        assertEquals(0L to 3_000L, ReglasTramo.ajustar(0, 8_000, 3_000))
        assertTrue(ReglasTramo.esValido(0, 5_000))
        assertFalse(ReglasTramo.esValido(0, 5_001))
        assertFalse(ReglasTramo.esValido(2_000, 2_000))
    }

    @Test
    fun emojisEntreUnoYTres() {
        assertTrue(ReglasEmojis.esValido("😀"))
        assertTrue(ReglasEmojis.esValido("😀,🔥,❤️"))
        assertFalse(ReglasEmojis.esValido("😀,🔥,❤️,👍"))
        assertFalse(ReglasEmojis.esValido(""))
        assertEquals("😀,🔥", ReglasEmojis.normalizar("😀🔥"))
        assertEquals(ReglasEmojis.POR_DEFECTO, ReglasEmojis.normalizar("  "))
        assertEquals(3, ReglasEmojis.separar(ReglasEmojis.normalizar("😀 🔥 ❤️ 👍")).size)
    }
}
