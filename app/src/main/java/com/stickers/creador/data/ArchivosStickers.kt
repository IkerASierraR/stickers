package com.stickers.creador.data

import java.io.File
import java.util.UUID

/**
 * Gestor de archivos internos (T1.5). Todo vive en `filesDir/stickers/{carpetaId}/` (R27).
 * Nunca se guarda el original completo, solo el resultado (R28).
 */
class ArchivosStickers(filesDir: File) {

    private val raiz = File(filesDir, "stickers").apply { mkdirs() }

    fun dirCarpeta(carpetaId: Long): File = File(raiz, carpetaId.toString()).apply { mkdirs() }

    /** Nombre base único para un sticker nuevo (comparten base el .webp, .m4a y .mp4). */
    fun nuevaBase(): String = "s_" + System.currentTimeMillis() + "_" +
        UUID.randomUUID().toString().replace("-", "").take(8)

    fun archivo(carpetaId: Long, nombre: String): File = File(dirCarpeta(carpetaId), nombre)

    fun icono(carpetaId: Long): File = File(dirCarpeta(carpetaId), NOMBRE_ICONO)

    fun bandeja(carpetaId: Long): File = File(dirCarpeta(carpetaId), NOMBRE_BANDEJA)

    /** Archivo temporal de trabajo (se borra al terminar). */
    fun temporal(sufijo: String): File {
        val dir = File(raiz.parentFile, "tmp").apply { mkdirs() }
        return File(dir, "t_" + UUID.randomUUID().toString().take(8) + sufijo)
    }

    fun guardar(carpetaId: Long, nombre: String, bytes: ByteArray): File {
        val f = archivo(carpetaId, nombre)
        val tmp = File(f.parentFile, f.name + ".part")
        tmp.writeBytes(bytes)
        if (f.exists()) f.delete()
        if (!tmp.renameTo(f)) {
            tmp.copyTo(f, overwrite = true); tmp.delete()
        }
        return f
    }

    /** Mueve un archivo a la carpeta destino y devuelve la nueva ruta. */
    fun mover(ruta: String?, destinoId: Long): String? {
        if (ruta == null) return null
        val origen = File(ruta)
        if (!origen.exists()) return null
        val destino = File(dirCarpeta(destinoId), origen.name)
        if (!origen.renameTo(destino)) {
            origen.copyTo(destino, overwrite = true)
            origen.delete()
        }
        return destino.absolutePath
    }

    fun borrar(vararg rutas: String?) {
        rutas.filterNotNull().forEach { File(it).delete() }
    }

    fun borrarCarpeta(carpetaId: Long) {
        File(raiz, carpetaId.toString()).deleteRecursively()
    }

    fun limpiarTemporales() {
        File(raiz.parentFile, "tmp").deleteRecursively()
    }

    companion object {
        const val NOMBRE_ICONO = "icono.png"
        const val NOMBRE_BANDEJA = "tray.png"
    }
}
