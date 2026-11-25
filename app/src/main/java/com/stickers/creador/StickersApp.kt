package com.stickers.creador

import android.app.Application

class StickersApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Los temporales de una sesión anterior interrumpida ya no sirven.
        Thread { container.archivos.limpiarTemporales() }.start()
    }
}
