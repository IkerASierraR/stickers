package com.stickers.creador.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Estado observable del editor + historial para "Deshacer". Lo comparten el editor de foto y el
 * de video. Vive dentro del ViewModel para sobrevivir a rotaciones.
 */
class ControladorEditor {

    var estado by mutableStateOf(EstadoEditor())
        private set

    private val historial = mutableStateListOf<EstadoEditor>()
    val puedeDeshacer: Boolean get() = historial.isNotEmpty()

    var herramienta by mutableStateOf(Herramienta.RECORTAR)
    var textoSeleccionado by mutableStateOf<Long?>(null)
    var colorDibujo by mutableStateOf(PaletaEditor.colores[2])
    var grosorDibujo by mutableStateOf(0.02f)
    var colorTexto by mutableStateOf(PaletaEditor.colores[0])
    var tamanoTexto by mutableStateOf(0.12f)
    var trazoEnCurso by mutableStateOf<Trazo?>(null)
        private set

    private var antesDelGesto: EstadoEditor? = null
    private var siguienteIdTexto = 1L

    /** Aplica un cambio guardando el estado anterior para deshacer. */
    fun confirmar(nuevo: EstadoEditor) {
        if (nuevo == estado) return
        historial.add(estado)
        if (historial.size > 50) historial.removeAt(0)
        estado = nuevo
    }

    fun deshacer() {
        if (historial.isEmpty()) return
        estado = historial.removeAt(historial.lastIndex)
        if (estado.textos.none { it.id == textoSeleccionado }) textoSeleccionado = null
    }

    // ---- Gestos continuos (mover/zoom, mover texto): un único paso de historial por gesto ----

    fun inicioGesto() {
        antesDelGesto = estado
    }

    fun durante(nuevo: EstadoEditor) {
        estado = nuevo
    }

    fun finGesto() {
        val antes = antesDelGesto ?: return
        antesDelGesto = null
        if (antes != estado) {
            historial.add(antes)
            if (historial.size > 50) historial.removeAt(0)
        }
    }

    // ---- Recorte ----

    fun cambiarForma(forma: Forma) = confirmar(estado.copy(forma = forma))

    fun moverImagen(dx: Float, dy: Float, factorZoom: Float) {
        val zoom = (estado.zoom * factorZoom).coerceIn(0.3f, 8f)
        durante(
            estado.copy(
                zoom = zoom,
                despX = (estado.despX + dx).coerceIn(-1.5f, 1.5f),
                despY = (estado.despY + dy).coerceIn(-1.5f, 1.5f),
            ),
        )
    }

    fun reiniciarRecorte() = confirmar(estado.copy(zoom = 1f, despX = 0f, despY = 0f))

    // ---- Texto ----

    fun agregarTexto(texto: String) {
        val capa = CapaTexto(
            id = siguienteIdTexto++,
            texto = texto,
            color = colorTexto,
            tamano = tamanoTexto,
            x = 0.5f,
            y = 0.8f,
        )
        confirmar(estado.copy(textos = estado.textos + capa))
        textoSeleccionado = capa.id
    }

    fun textoActual(): CapaTexto? = estado.textos.firstOrNull { it.id == textoSeleccionado }

    fun editarTextoSeleccionado(transformar: (CapaTexto) -> CapaTexto) {
        val id = textoSeleccionado ?: return
        confirmar(estado.copy(textos = estado.textos.map { if (it.id == id) transformar(it) else it }))
    }

    fun moverTexto(id: Long, dx: Float, dy: Float) {
        durante(
            estado.copy(
                textos = estado.textos.map {
                    if (it.id == id) it.copy(x = (it.x + dx).coerceIn(0f, 1f), y = (it.y + dy).coerceIn(0f, 1f)) else it
                },
            ),
        )
    }

    fun eliminarTextoSeleccionado() {
        val id = textoSeleccionado ?: return
        confirmar(estado.copy(textos = estado.textos.filterNot { it.id == id }))
        textoSeleccionado = null
    }

    // ---- Dibujo ----

    fun empezarTrazo(p: Punto) {
        trazoEnCurso = Trazo(listOf(p), colorDibujo, grosorDibujo)
    }

    fun continuarTrazo(p: Punto) {
        val t = trazoEnCurso ?: return
        trazoEnCurso = t.copy(puntos = t.puntos + p)
    }

    fun terminarTrazo() {
        val t = trazoEnCurso ?: return
        trazoEnCurso = null
        confirmar(estado.copy(trazos = estado.trazos + t))
    }

    fun deshacerTrazo() {
        if (estado.trazos.isNotEmpty()) confirmar(estado.copy(trazos = estado.trazos.dropLast(1)))
    }

    // ---- Quitar fondo ----

    fun marcarFondoQuitado(quitado: Boolean) = confirmar(estado.copy(fondoQuitado = quitado))
}
