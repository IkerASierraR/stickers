package com.stickers.creador.editor

/** Forma del recorte (R9, R15). Sin borde blanco. */
enum class Forma { RECTANGULO, CIRCULO }

/** Punto normalizado: 0..1 relativo al lado del lienzo cuadrado. */
data class Punto(val x: Float, val y: Float)

data class CapaTexto(
    val id: Long,
    val texto: String,
    val color: Int,
    /** Tamaño de letra como fracción del lado del lienzo. */
    val tamano: Float,
    /** Centro normalizado. */
    val x: Float,
    val y: Float,
)

data class Trazo(
    val puntos: List<Punto>,
    val color: Int,
    /** Grosor como fracción del lado del lienzo. */
    val grosor: Float,
)

/**
 * Estado completo e inmutable del editor. La imagen se coloca "ajustada" (contain) en el lienzo
 * cuadrado y luego se aplica [zoom] y el desplazamiento normalizado [despX]/[despY].
 * Cada cambio confirmado guarda el estado anterior para "Deshacer".
 */
data class EstadoEditor(
    val forma: Forma = Forma.RECTANGULO,
    val zoom: Float = 1f,
    val despX: Float = 0f,
    val despY: Float = 0f,
    val textos: List<CapaTexto> = emptyList(),
    val trazos: List<Trazo> = emptyList(),
    val fondoQuitado: Boolean = false,
)

/** Herramienta activa en la barra (design.md: Recortar · Quitar fondo · Texto · Dibujar · Deshacer). */
enum class Herramienta { RECORTAR, TEXTO, DIBUJAR }

object PaletaEditor {
    val colores: List<Int> = listOf(
        0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFD93025.toInt(), 0xFFFF9800.toInt(),
        0xFFFFEB3B.toInt(), 0xFF1FAF5A.toInt(), 0xFF2196F3.toInt(), 0xFF9C27B0.toInt(),
        0xFFE91E63.toInt(),
    )
    const val TAMANO_TEXTO_MIN = 0.05f
    const val TAMANO_TEXTO_MAX = 0.25f
    const val GROSOR_MIN = 0.005f
    const val GROSOR_MAX = 0.06f
}
