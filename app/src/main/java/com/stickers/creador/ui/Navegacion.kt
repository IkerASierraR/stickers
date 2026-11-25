package com.stickers.creador.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stickers.creador.domain.TipoCarpeta
import com.stickers.creador.ui.carpeta.CarpetaScreen
import com.stickers.creador.ui.carpetaform.CarpetaFormScreen
import com.stickers.creador.ui.editor.EditorFotoScreen
import com.stickers.creador.ui.editor.EditorVideoScreen
import com.stickers.creador.ui.inicio.InicioScreen
import com.stickers.creador.ui.sticker.StickerDetalleScreen

/** Rutas de plan.md §5. */
object Rutas {
    const val INICIO = "inicio"
    const val CARPETA = "carpeta/{id}"
    const val CARPETA_FORM = "carpetaForm?id={id}"
    const val EDITOR_FOTO = "editor/foto/{carpetaId}"
    const val EDITOR_VIDEO = "editor/video/{carpetaId}"
    const val STICKER = "sticker/{id}"

    fun carpeta(id: Long) = "carpeta/$id"
    fun carpetaForm(id: Long? = null) = if (id == null) "carpetaForm" else "carpetaForm?id=$id"
    fun editorFoto(carpetaId: Long) = "editor/foto/$carpetaId"
    fun editorVideo(carpetaId: Long) = "editor/video/$carpetaId"
    fun sticker(id: Long) = "sticker/$id"
}

@Composable
fun NavegacionApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Rutas.INICIO) {
        composable(Rutas.INICIO) {
            InicioScreen(
                onAbrirCarpeta = { nav.navigate(Rutas.carpeta(it)) },
                onNuevaCarpeta = { nav.navigate(Rutas.carpetaForm()) },
            )
        }
        composable(Rutas.CARPETA, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            val id = e.arguments!!.getLong("id")
            CarpetaScreen(
                carpetaId = id,
                onVolver = { nav.popBackStack() },
                onEditar = { nav.navigate(Rutas.carpetaForm(id)) },
                onNuevoSticker = { tipo ->
                    nav.navigate(if (tipo == TipoCarpeta.IMAGEN) Rutas.editorFoto(id) else Rutas.editorVideo(id))
                },
                onAbrirSticker = { nav.navigate(Rutas.sticker(it)) },
            )
        }
        composable(
            Rutas.CARPETA_FORM,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
        ) { e ->
            val id = e.arguments?.getLong("id")?.takeIf { it > 0 }
            CarpetaFormScreen(
                carpetaId = id,
                onVolver = { nav.popBackStack() },
                onCreada = { nueva ->
                    nav.navigate(Rutas.carpeta(nueva)) { popUpTo(Rutas.INICIO) }
                },
                onEliminada = { nav.popBackStack(Rutas.INICIO, inclusive = false) },
            )
        }
        composable(Rutas.EDITOR_FOTO, arguments = listOf(navArgument("carpetaId") { type = NavType.LongType })) { e ->
            EditorFotoScreen(e.arguments!!.getLong("carpetaId"), onCerrar = { nav.popBackStack() })
        }
        composable(Rutas.EDITOR_VIDEO, arguments = listOf(navArgument("carpetaId") { type = NavType.LongType })) { e ->
            EditorVideoScreen(e.arguments!!.getLong("carpetaId"), onCerrar = { nav.popBackStack() })
        }
        composable(Rutas.STICKER, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            StickerDetalleScreen(e.arguments!!.getLong("id"), onVolver = { nav.popBackStack() })
        }
    }
}
