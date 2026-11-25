package com.stickers.creador.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.stickers.creador.AppContainer
import com.stickers.creador.StickersApp
import com.stickers.creador.ui.theme.PrimarioOscuro

/** Acceso al contenedor de dependencias desde Compose. */
@Composable
fun contenedor(): AppContainer = (LocalContext.current.applicationContext as StickersApp).container

/** Barra superior verde oscuro (design.md). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun coloresBarra(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = PrimarioOscuro,
    titleContentColor = Color.White,
    navigationIconContentColor = Color.White,
    actionIconContentColor = Color.White,
)
