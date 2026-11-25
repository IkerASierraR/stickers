package com.stickers.creador.ui.carpeta

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stickers.creador.AppContainer
import com.stickers.creador.data.Carpeta
import com.stickers.creador.data.Sticker
import com.stickers.creador.domain.ReglasCarpeta
import com.stickers.creador.domain.TipoCarpeta
import com.stickers.creador.media.whatsapp.PreparacionEnvio
import com.stickers.creador.ui.coloresBarra
import com.stickers.creador.ui.componentes.BotonWhatsApp
import com.stickers.creador.ui.componentes.ContadorStickers
import com.stickers.creador.ui.componentes.EstadoVacio
import com.stickers.creador.ui.componentes.EtiquetaTipo
import com.stickers.creador.ui.componentes.StickerCelda
import com.stickers.creador.ui.componentes.StickerImagen
import com.stickers.creador.ui.componentes.tablero
import com.stickers.creador.ui.contenedor
import com.stickers.creador.ui.theme.Primario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class CarpetaViewModel(private val c: AppContainer, val carpetaId: Long) : ViewModel() {
    val carpeta: StateFlow<Carpeta?> = c.repositorio.observarCarpeta(carpetaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val stickers: StateFlow<List<Sticker>> = c.repositorio.observarStickers(carpetaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var enviando by mutableStateOf(false)
        private set
    var errores by mutableStateOf<List<String>>(emptyList())
    var intentListo by mutableStateOf<Intent?>(null)

    fun prepararEnvio() {
        enviando = true
        viewModelScope.launch {
            when (val r = c.envioWhatsApp.preparar(carpetaId)) {
                is PreparacionEnvio.Lista -> intentListo = r.intent
                is PreparacionEnvio.Errores -> errores = r.mensajes
            }
            enviando = false
        }
    }

    fun resultado(codigo: Int, datos: Intent?): String? = c.envioWhatsApp.interpretarResultado(codigo, datos)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarpetaScreen(
    carpetaId: Long,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onNuevoSticker: (TipoCarpeta) -> Unit,
    onAbrirSticker: (Long) -> Unit,
) {
    val c = contenedor()
    val vm: CarpetaViewModel = viewModel { CarpetaViewModel(c, carpetaId) }
    val carpeta by vm.carpeta.collectAsStateWithLifecycle()
    val stickers by vm.stickers.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val msg = vm.resultado(r.resultCode, r.data) ?: "Paquete agregado a WhatsApp"
        scope.launch { snackbar.showSnackbar(msg) }
    }

    LaunchedEffect(vm.intentListo) {
        val intent = vm.intentListo ?: return@LaunchedEffect
        vm.intentListo = null
        try {
            lanzador.launch(intent)
        } catch (_: ActivityNotFoundException) {
            snackbar.showSnackbar("No se encontró WhatsApp o tu versión no admite paquetes de stickers")
        }
    }

    val cant = stickers.size
    val tipo = carpeta?.tipo ?: TipoCarpeta.IMAGEN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(carpeta?.nombre ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
                },
                actions = {
                    IconButton(onClick = onEditar) { Icon(Icons.Rounded.Edit, "Editar carpeta") }
                },
                colors = coloresBarra(),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            // Oculto al llegar a 30 (R3).
            if (carpeta != null && ReglasCarpeta.puedeAgregarSticker(cant)) {
                ExtendedFloatingActionButton(
                    onClick = { onNuevoSticker(tipo) },
                    icon = {
                        Icon(if (tipo == TipoCarpeta.IMAGEN) Icons.Rounded.AddPhotoAlternate else Icons.Rounded.VideoLibrary, null)
                    },
                    text = { Text("Nuevo sticker") },
                    containerColor = Primario,
                    contentColor = Color.White,
                )
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                BotonWhatsApp(
                    cantidad = cant,
                    enviando = vm.enviando,
                    onClick = { vm.prepararEnvio() },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            carpeta?.let { Encabezado(it, stickers.firstOrNull()?.webpRuta, cant) }
            if (stickers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EstadoVacio("Agrega al menos 3 stickers para enviarla a WhatsApp")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(stickers, key = { _, s -> s.id }) { i, s ->
                        StickerCelda(s, i + 1) { onAbrirSticker(s.id) }
                    }
                }
            }
        }
    }

    if (vm.errores.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { vm.errores = emptyList() },
            title = { Text("No se puede enviar todavía") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    vm.errores.take(12).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    if (vm.errores.size > 12) Text("…y ${vm.errores.size - 12} más")
                }
            },
            confirmButton = { TextButton(onClick = { vm.errores = emptyList() }) { Text("Entendido") } },
        )
    }
}

@Composable
private fun Encabezado(carpeta: Carpeta, primerSticker: String?, cantidad: Int) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).tablero(6.dp), contentAlignment = Alignment.Center) {
            val portada = carpeta.iconoRuta?.takeIf { File(it).exists() } ?: primerSticker
            if (portada != null) StickerImagen(portada, Modifier.fillMaxSize().padding(4.dp), "Ícono de la carpeta")
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(carpeta.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            EtiquetaTipo(carpeta.tipo)
            ContadorStickers(cantidad)
        }
    }
}
