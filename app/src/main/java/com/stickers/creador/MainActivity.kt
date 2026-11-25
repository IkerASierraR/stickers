package com.stickers.creador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stickers.creador.ui.NavegacionApp
import com.stickers.creador.ui.theme.TemaStickers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Barra de estado con íconos claros sobre la barra superior verde oscuro.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            TemaStickers {
                NavegacionApp()
            }
        }
    }
}
