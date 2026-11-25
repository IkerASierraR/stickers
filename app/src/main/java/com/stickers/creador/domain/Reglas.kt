package com.stickers.creador.domain

/** Tipo de carpeta. Se elige al crearla y no cambia (R1, R2). */
enum class TipoCarpeta { IMAGEN, ANIMADO }

/** Límites oficiales de WhatsApp (README de WhatsApp/stickers, verificado en T0.2). */
object LimitesWhatsApp {
    const val TAMANO_STICKER_PX = 512
    const val MAX_BYTES_ESTATICO = 100 * 1024
    const val MAX_BYTES_ANIMADO = 500 * 1024
    const val MAX_DURACION_ANIMADO_MS = 10_000
    const val MIN_DURACION_CUADRO_MS = 8
    const val TAMANO_ICONO_PX = 96
    const val MIN_ICONO_PX = 24
    const val MAX_ICONO_PX = 512
    const val MAX_BYTES_ICONO = 50 * 1024
    const val MIN_EMOJIS = 1
    const val MAX_EMOJIS = 3
    const val MIN_STICKERS = 3
    const val MAX_STICKERS = 30
    const val MAX_CARACTERES = 128

    /** Límite propio de la app para el tramo de video (R14). */
    const val MAX_TRAMO_VIDEO_MS = 5_000L
}

/** Reglas puras de carpetas (R3, R4, R5, R26). Sin dependencias de Android para poder probarlas. */
object ReglasCarpeta {

    /** R3: no se pueden agregar más stickers al llegar a 30. */
    fun puedeAgregarSticker(cantidadActual: Int): Boolean =
        cantidadActual < LimitesWhatsApp.MAX_STICKERS

    /** R4: "Agregar a WhatsApp" solo con 3 o más stickers. */
    fun puedeEnviar(cantidad: Int): Boolean =
        cantidad >= LimitesWhatsApp.MIN_STICKERS && cantidad <= LimitesWhatsApp.MAX_STICKERS

    fun faltanParaEnviar(cantidad: Int): Int =
        (LimitesWhatsApp.MIN_STICKERS - cantidad).coerceAtLeast(0)

    /** R5: mover solo a otra carpeta del mismo tipo que tenga espacio. */
    fun puedeMover(
        tipoOrigen: TipoCarpeta,
        origenId: Long,
        tipoDestino: TipoCarpeta,
        destinoId: Long,
        cantidadDestino: Int,
    ): Boolean = origenId != destinoId &&
        tipoOrigen == tipoDestino &&
        puedeAgregarSticker(cantidadDestino)

    /** R26: cada cambio incrementa la versión para que WhatsApp tome los cambios. */
    fun siguienteVersion(versionActual: Int): Int = versionActual + 1

    /** R1: nombre obligatorio y dentro del límite de WhatsApp. Devuelve el error o null. */
    fun validarNombre(nombre: String): String? {
        val limpio = nombre.trim()
        return when {
            limpio.isEmpty() -> "El nombre es obligatorio"
            limpio.length > LimitesWhatsApp.MAX_CARACTERES -> "Máximo ${LimitesWhatsApp.MAX_CARACTERES} caracteres"
            else -> null
        }
    }

    /** Identificador del paquete: solo caracteres que acepta el validador oficial. */
    fun identificadorValido(id: String): Boolean =
        id.isNotEmpty() && id.length <= LimitesWhatsApp.MAX_CARACTERES &&
            Regex("[a-zA-Z0-9_.\\- ]+").matches(id) && !id.contains("..")
}

/** Reglas del tramo de video (R14). */
object ReglasTramo {
    /** Ajusta [finMs] para que el tramo no supere 5 s ni la duración del video. */
    fun ajustar(inicioMs: Long, finMs: Long, duracionVideoMs: Long): Pair<Long, Long> {
        val inicio = inicioMs.coerceIn(0, duracionVideoMs.coerceAtLeast(0))
        val maxFin = minOf(inicio + LimitesWhatsApp.MAX_TRAMO_VIDEO_MS, duracionVideoMs)
        val fin = finMs.coerceIn(inicio, maxFin)
        return inicio to fin
    }

    fun esValido(inicioMs: Long, finMs: Long): Boolean =
        finMs > inicioMs && finMs - inicioMs <= LimitesWhatsApp.MAX_TRAMO_VIDEO_MS
}

/** Emojis por sticker (1 a 3, separados por coma en el ContentProvider). */
object ReglasEmojis {
    const val POR_DEFECTO = "😀"

    fun separar(texto: String): List<String> {
        val partes = texto.split(',', ' ').map { it.trim() }.filter { it.isNotEmpty() }
        if (partes.size > 1) return partes
        // Sin separadores: dividir por emoji (puntos de código de grafema aproximados).
        val unico = partes.firstOrNull() ?: return emptyList()
        return dividirGrafemas(unico)
    }

    fun esValido(texto: String): Boolean =
        separar(texto).size in LimitesWhatsApp.MIN_EMOJIS..LimitesWhatsApp.MAX_EMOJIS

    fun normalizar(texto: String): String {
        val lista = separar(texto).take(LimitesWhatsApp.MAX_EMOJIS)
        return if (lista.isEmpty()) POR_DEFECTO else lista.joinToString(",")
    }

    private fun dividirGrafemas(s: String): List<String> {
        val res = mutableListOf<String>()
        val actual = StringBuilder()
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            val len = Character.charCount(cp)
            val unir = actual.isNotEmpty() && (
                cp == 0x200D || // ZWJ
                    cp in 0xFE00..0xFE0F || // selectores de variación
                    cp in 0x1F3FB..0x1F3FF || // tonos de piel
                    cp == 0x20E3 ||
                    cp in 0xE0020..0xE007F ||
                    actual.codePointBefore(actual.length) == 0x200D
                )
            if (!unir && actual.isNotEmpty()) {
                res.add(actual.toString()); actual.clear()
            }
            actual.appendCodePoint(cp)
            i += len
        }
        if (actual.isNotEmpty()) res.add(actual.toString())
        return res
    }
}
