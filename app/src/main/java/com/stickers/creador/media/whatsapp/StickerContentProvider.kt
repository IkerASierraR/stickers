package com.stickers.creador.media.whatsapp

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.stickers.creador.BuildConfig
import com.stickers.creador.StickersApp
import com.stickers.creador.data.ArchivosStickers
import com.stickers.creador.data.Carpeta
import com.stickers.creador.domain.ReglasEmojis
import com.stickers.creador.domain.TipoCarpeta
import java.io.File

/**
 * ContentProvider que lee WhatsApp para importar los paquetes. Adaptado del sample oficial
 * `WhatsApp/stickers` (StickerContentProvider.java): mismas columnas, rutas y tipos MIME, pero
 * los datos salen de Room y los archivos de `filesDir/stickers/{carpetaId}/` en lugar de assets.
 */
class StickerContentProvider : ContentProvider() {

    private val matcher = UriMatcher(UriMatcher.NO_MATCH)

    private val container get() = (context!!.applicationContext as StickersApp).container

    override fun onCreate(): Boolean {
        val authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY
        check(authority.startsWith(context!!.packageName)) {
            "La autoridad ($authority) debe empezar por el nombre del paquete (${context!!.packageName})"
        }
        matcher.addURI(authority, METADATA, METADATA_CODE)
        matcher.addURI(authority, "$METADATA/*", METADATA_CODE_FOR_SINGLE_PACK)
        matcher.addURI(authority, "$STICKERS/*", STICKERS_CODE)
        // El sample registra cada archivo; aquí se acepta cualquier nombre y se valida al abrirlo.
        matcher.addURI(authority, "$STICKERS_ASSET/*/*", STICKERS_ASSET_CODE)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val dao = container.db.carpetaDao()
        return when (matcher.match(uri)) {
            METADATA_CODE -> cursorPaquetes(uri, dao.todasSync())
            METADATA_CODE_FOR_SINGLE_PACK ->
                cursorPaquetes(uri, listOfNotNull(dao.porIdentificadorSync(uri.lastPathSegment.orEmpty())))
            STICKERS_CODE -> cursorStickers(uri, uri.lastPathSegment.orEmpty())
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    private fun cursorPaquetes(uri: Uri, carpetas: List<Carpeta>): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                STICKER_PACK_IDENTIFIER_IN_QUERY,
                STICKER_PACK_NAME_IN_QUERY,
                STICKER_PACK_PUBLISHER_IN_QUERY,
                STICKER_PACK_ICON_IN_QUERY,
                ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
                IOS_APP_DOWNLOAD_LINK_IN_QUERY,
                PUBLISHER_EMAIL,
                PUBLISHER_WEBSITE,
                PRIVACY_POLICY_WEBSITE,
                LICENSE_AGREEMENT_WEBSITE,
                IMAGE_DATA_VERSION,
                AVOID_CACHE,
                ANIMATED_STICKER_PACK,
            ),
        )
        carpetas.forEach { c ->
            cursor.addRow(
                arrayOf<Any>(
                    c.identificador,
                    c.nombre,
                    PUBLICADOR,
                    ArchivosStickers.NOMBRE_BANDEJA,
                    "", "", "", "", "", "",
                    // R26: la versión cambia con cada edición para que WhatsApp actualice.
                    c.version.toString(),
                    0,
                    if (c.tipo == TipoCarpeta.ANIMADO) 1 else 0,
                ),
            )
        }
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    private fun cursorStickers(uri: Uri, identificador: String): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                STICKER_FILE_NAME_IN_QUERY,
                STICKER_FILE_EMOJI_IN_QUERY,
                STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY,
            ),
        )
        val carpeta = container.db.carpetaDao().porIdentificadorSync(identificador)
        if (carpeta != null) {
            container.db.stickerDao().deCarpetaSync(carpeta.id).forEach { s ->
                cursor.addRow(
                    arrayOf<Any?>(
                        File(s.webpRuta).name,
                        ReglasEmojis.normalizar(s.emojis),
                        null,
                    ),
                )
            }
        }
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        if (matcher.match(uri) != STICKERS_ASSET_CODE) return null
        val segmentos = uri.pathSegments
        if (segmentos.size != 3) return null
        val identificador = segmentos[1]
        val nombre = segmentos[2]
        // Solo nombres simples (sin rutas) para no exponer otros archivos.
        if (nombre.contains('/') || nombre.contains("..")) return null
        val carpeta = container.db.carpetaDao().porIdentificadorSync(identificador) ?: return null
        val archivos = container.archivos
        val esBandeja = nombre == ArchivosStickers.NOMBRE_BANDEJA
        val esSticker = nombre.endsWith(".webp") &&
            container.db.stickerDao().deCarpetaSync(carpeta.id).any { File(it.webpRuta).name == nombre }
        if (!esBandeja && !esSticker) return null
        val archivo = archivos.archivo(carpeta.id, nombre)
        if (!archivo.exists()) return null
        val pfd = ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    override fun getType(uri: Uri): String {
        val authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY
        return when (matcher.match(uri)) {
            METADATA_CODE -> "vnd.android.cursor.dir/vnd.$authority.$METADATA"
            METADATA_CODE_FOR_SINGLE_PACK -> "vnd.android.cursor.item/vnd.$authority.$METADATA"
            STICKERS_CODE -> "vnd.android.cursor.dir/vnd.$authority.$STICKERS"
            STICKERS_ASSET_CODE ->
                if (uri.lastPathSegment == ArchivosStickers.NOMBRE_BANDEJA) "image/png" else "image/webp"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Not supported")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Not supported")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Not supported")

    companion object {
        const val PUBLICADOR = "Mis Stickers"

        // Constantes exactas del sample oficial.
        const val STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier"
        const val STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name"
        const val STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher"
        const val STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon"
        const val ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link"
        const val IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_download_link"
        const val PUBLISHER_EMAIL = "sticker_pack_publisher_email"
        const val PUBLISHER_WEBSITE = "sticker_pack_publisher_website"
        const val PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website"
        const val LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website"
        const val IMAGE_DATA_VERSION = "image_data_version"
        const val AVOID_CACHE = "whatsapp_will_not_cache_stickers"
        const val ANIMATED_STICKER_PACK = "animated_sticker_pack"

        const val STICKER_FILE_NAME_IN_QUERY = "sticker_file_name"
        const val STICKER_FILE_EMOJI_IN_QUERY = "sticker_emoji"
        const val STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY = "sticker_accessibility_text"

        private const val METADATA = "metadata"
        const val STICKERS = "stickers"
        const val STICKERS_ASSET = "stickers_asset"

        private const val METADATA_CODE = 1
        private const val METADATA_CODE_FOR_SINGLE_PACK = 2
        private const val STICKERS_CODE = 3
        private const val STICKERS_ASSET_CODE = 4
    }
}
