package com.stickers.creador.media.video

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Copia el video corto con sonido a la galería (MediaStore) para usarlo en WhatsApp (R19, R21). */
object Galeria {

    /** En Android 9 o anterior se necesita el permiso WRITE_EXTERNAL_STORAGE. */
    val necesitaPermiso: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    suspend fun guardarVideo(context: Context, archivo: File): Unit = withContext(Dispatchers.IO) {
        require(archivo.exists()) { "El video ya no existe" }
        val resolver = context.contentResolver
        val nombre = "sticker_${System.currentTimeMillis()}.mp4"
        val valores = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, nombre)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Stickers")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Stickers")
                dir.mkdirs()
                @Suppress("DEPRECATION")
                put(MediaStore.Video.Media.DATA, File(dir, nombre).absolutePath)
            }
        }
        val coleccion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(coleccion, valores) ?: throw IllegalStateException("No se pudo crear el video en la galería")
        try {
            resolver.openOutputStream(uri)?.use { out -> archivo.inputStream().use { it.copyTo(out) } }
                ?: throw IllegalStateException("No se pudo escribir el video")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
