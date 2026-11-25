package com.stickers.creador.data

import androidx.room.withTransaction
import com.stickers.creador.domain.ReglasCarpeta
import com.stickers.creador.domain.ReglasEmojis
import com.stickers.creador.domain.TipoCarpeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Error de regla de negocio con mensaje para mostrar al usuario. */
class ReglaException(mensaje: String) : Exception(mensaje)

sealed interface CambioIcono {
    data object Mantener : CambioIcono
    data object PrimerSticker : CambioIcono
    /** PNG 96x96 ya generado. */
    class Nuevo(val png: ByteArray) : CambioIcono
}

/** Archivos generados para un sticker nuevo (en temporales; el repositorio los mueve). */
class ArchivosNuevoSticker(
    val webp: File,
    val audio: File? = null,
    val videoCorto: File? = null,
)

class StickerRepositorio(
    private val db: AppDatabase,
    val archivos: ArchivosStickers,
) {
    private val carpetas = db.carpetaDao()
    private val stickers = db.stickerDao()

    fun observarCarpetas(): Flow<List<CarpetaResumen>> = carpetas.observarResumenes()
    fun observarCarpeta(id: Long): Flow<Carpeta?> = carpetas.observar(id)
    fun observarStickers(carpetaId: Long): Flow<List<Sticker>> = stickers.observarDeCarpeta(carpetaId)
    fun observarSticker(id: Long): Flow<Sticker?> = stickers.observar(id)

    suspend fun carpeta(id: Long): Carpeta? = carpetas.obtener(id)
    suspend fun sticker(id: Long): Sticker? = stickers.obtener(id)
    suspend fun stickersDe(carpetaId: Long): List<Sticker> = stickers.deCarpeta(carpetaId)
    suspend fun contar(carpetaId: Long): Int = stickers.contar(carpetaId)

    // ---------- Carpetas ----------

    suspend fun crearCarpeta(nombre: String, tipo: TipoCarpeta, iconoPng: ByteArray?): Long =
        withContext(Dispatchers.IO) {
            ReglasCarpeta.validarNombre(nombre)?.let { throw ReglaException(it) }
            val id = carpetas.insertar(
                Carpeta(
                    identificador = "pack_" + UUID.randomUUID().toString().replace("-", ""),
                    nombre = nombre.trim(),
                    tipo = tipo,
                    iconoRuta = null,
                    creadoEn = System.currentTimeMillis(),
                ),
            )
            if (iconoPng != null) {
                val f = archivos.guardar(id, ArchivosStickers.NOMBRE_ICONO, iconoPng)
                carpetas.obtener(id)?.let { carpetas.actualizar(it.copy(iconoRuta = f.absolutePath)) }
            }
            id
        }

    /** Edita nombre e ícono. El tipo no cambia (R1). Incrementa la versión (R26). */
    suspend fun editarCarpeta(id: Long, nombre: String, icono: CambioIcono) = withContext(Dispatchers.IO) {
        ReglasCarpeta.validarNombre(nombre)?.let { throw ReglaException(it) }
        val actual = carpetas.obtener(id) ?: throw ReglaException("La carpeta no existe")
        val nuevaRuta = when (icono) {
            CambioIcono.Mantener -> actual.iconoRuta
            CambioIcono.PrimerSticker -> {
                archivos.icono(id).delete(); null
            }
            is CambioIcono.Nuevo -> archivos.guardar(id, ArchivosStickers.NOMBRE_ICONO, icono.png).absolutePath
        }
        carpetas.actualizar(
            actual.copy(
                nombre = nombre.trim(),
                iconoRuta = nuevaRuta,
                version = ReglasCarpeta.siguienteVersion(actual.version),
            ),
        )
    }

    /** R7: borra la carpeta y sus stickers (la confirmación se pide en la UI). */
    suspend fun eliminarCarpeta(id: Long) = withContext(Dispatchers.IO) {
        val c = carpetas.obtener(id) ?: return@withContext
        carpetas.eliminar(c) // CASCADE borra los stickers
        archivos.borrarCarpeta(id)
    }

    private suspend fun subirVersion(id: Long) {
        carpetas.obtener(id)?.let { carpetas.actualizar(it.copy(version = ReglasCarpeta.siguienteVersion(it.version))) }
    }

    // ---------- Stickers ----------

    /** Guarda un sticker nuevo moviendo sus archivos a la carpeta (R3: máx. 30). */
    suspend fun agregarSticker(carpetaId: Long, nuevos: ArchivosNuevoSticker): Long =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                carpetas.obtener(carpetaId) ?: throw ReglaException("La carpeta no existe")
                val cantidad = stickers.contar(carpetaId)
                if (!ReglasCarpeta.puedeAgregarSticker(cantidad)) {
                    throw ReglaException("La carpeta ya tiene 30 stickers")
                }
                val base = archivos.nuevaBase()
                val webp = moverA(nuevos.webp, carpetaId, "$base.webp")
                    ?: throw ReglaException("No se pudo guardar el sticker")
                val audio = nuevos.audio?.let { moverA(it, carpetaId, "$base.m4a") }
                val video = nuevos.videoCorto?.let { moverA(it, carpetaId, "$base.mp4") }
                val id = stickers.insertar(
                    Sticker(
                        carpetaId = carpetaId,
                        webpRuta = webp,
                        audioRuta = audio,
                        videoCortoRuta = video,
                        emojis = ReglasEmojis.POR_DEFECTO,
                        orden = stickers.maxOrden(carpetaId) + 1,
                        creadoEn = System.currentTimeMillis(),
                    ),
                )
                subirVersion(carpetaId)
                id
            }
        }

    private fun moverA(origen: File, carpetaId: Long, nombre: String): String? {
        if (!origen.exists() || origen.length() == 0L) return null
        val destino = archivos.archivo(carpetaId, nombre)
        if (!origen.renameTo(destino)) {
            origen.copyTo(destino, overwrite = true)
            origen.delete()
        }
        return destino.absolutePath
    }

    /** Carpetas a las que se puede mover el sticker (R5). */
    suspend fun destinosPara(sticker: Sticker): List<Carpeta> = withContext(Dispatchers.IO) {
        val origen = carpetas.obtener(sticker.carpetaId) ?: return@withContext emptyList()
        carpetas.todas().filter {
            ReglasCarpeta.puedeMover(origen.tipo, origen.id, it.tipo, it.id, stickers.contar(it.id))
        }
    }

    suspend fun moverSticker(stickerId: Long, destinoId: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val s = stickers.obtener(stickerId) ?: throw ReglaException("El sticker no existe")
            val origen = carpetas.obtener(s.carpetaId) ?: throw ReglaException("Carpeta de origen no existe")
            val destino = carpetas.obtener(destinoId) ?: throw ReglaException("Carpeta de destino no existe")
            if (!ReglasCarpeta.puedeMover(origen.tipo, origen.id, destino.tipo, destino.id, stickers.contar(destinoId))) {
                throw ReglaException(
                    if (origen.tipo != destino.tipo) "Solo se puede mover a una carpeta del mismo tipo"
                    else "La carpeta de destino está llena",
                )
            }
            stickers.actualizar(
                s.copy(
                    carpetaId = destinoId,
                    webpRuta = archivos.mover(s.webpRuta, destinoId) ?: s.webpRuta,
                    audioRuta = archivos.mover(s.audioRuta, destinoId),
                    videoCortoRuta = archivos.mover(s.videoCortoRuta, destinoId),
                    orden = stickers.maxOrden(destinoId) + 1,
                ),
            )
            subirVersion(origen.id)
            subirVersion(destino.id)
        }
    }

    suspend fun eliminarSticker(stickerId: Long) = withContext(Dispatchers.IO) {
        val s = stickers.obtener(stickerId) ?: return@withContext
        stickers.eliminar(s)
        archivos.borrar(s.webpRuta, s.audioRuta, s.videoCortoRuta)
        subirVersion(s.carpetaId)
    }

    suspend fun cambiarEmojis(stickerId: Long, emojis: String) = withContext(Dispatchers.IO) {
        val s = stickers.obtener(stickerId) ?: return@withContext
        stickers.actualizar(s.copy(emojis = ReglasEmojis.normalizar(emojis)))
        subirVersion(s.carpetaId)
    }
}
