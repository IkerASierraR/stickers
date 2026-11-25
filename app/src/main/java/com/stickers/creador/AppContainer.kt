package com.stickers.creador

import android.content.Context
import com.stickers.creador.data.AppDatabase
import com.stickers.creador.data.ArchivosStickers
import com.stickers.creador.data.StickerRepositorio
import com.stickers.creador.media.image.ProcesadorImagen
import com.stickers.creador.media.image.QuitarFondo
import com.stickers.creador.media.video.ProcesadorVideo
import com.stickers.creador.media.whatsapp.EnvioWhatsApp

/** Inyección manual: crea la base de datos, repositorios y servicios una sola vez. */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val contexto: Context get() = app

    val db: AppDatabase by lazy { AppDatabase.crear(app) }
    val archivos: ArchivosStickers by lazy { ArchivosStickers(app.filesDir) }
    val repositorio: StickerRepositorio by lazy { StickerRepositorio(db, archivos) }

    val procesadorImagen: ProcesadorImagen by lazy { ProcesadorImagen(app) }
    val quitarFondo: QuitarFondo by lazy { QuitarFondo() }
    val procesadorVideo: ProcesadorVideo by lazy { ProcesadorVideo(app, archivos) }
    val envioWhatsApp: EnvioWhatsApp by lazy { EnvioWhatsApp(app, repositorio, procesadorImagen) }
}
