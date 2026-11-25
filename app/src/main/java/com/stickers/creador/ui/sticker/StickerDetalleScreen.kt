package com.stickers.creador.ui.sticker

import android.Manifest
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stickers.creador.AppContainer
import com.stickers.creador.data.Carpeta
import com.stickers.creador.data.Sticker
import com.stickers.creador.domain.ReglasEmojis
import com.stickers.creador.media.video.Galeria
import com.stickers.creador.ui.coloresBarra
import com.stickers.creador.ui.componentes.GuiaSonido
import com.stickers.creador.ui.componentes.StickerImagen
import com.stickers.creador.ui.componentes.tablero
import com.stickers.creador.ui.contenedor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class StickerDetalleViewModel(private val c: AppContainer, val stickerId: Long) : ViewModel() {
    val sticker: StateFlow<Sticker?> = c.repositorio.observarSticker(stickerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var destinos by mutableStateOf<List<Carpeta>?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
    var eliminado by mutableStateOf(false)
        private set

    fun cargarDestinos() = viewModelScope.launch {
        val s = c.repositorio.sticker(stickerId) ?: return@launch
        destinos = c.repositorio.destinosPara(s)
    }

    fun cerrarDestinos() {
        destinos = null
    }

    fun mover(destino: Carpeta) = viewModelScope.launch {
        destinos = null
        try {
            c.repositorio.moverSticker(stickerId, destino.id)
            mensaje = "Movido a ${destino.nombre}"
        } catch (e: Exception) {
            mensaje = e.message
        }
    }

    fun eliminar() = viewModelScope.launch {
        c.repositorio.eliminarSticker(stickerId)
        eliminado = true
    }

    fun guardarVideo(ruta: String) = viewModelScope.launch {
        mensaje = try {
            Galeria.guardarVideo(c.contexto, File(ruta))
            "Video guardado en la galería (Movies/Stickers)"
        } catch (e: Exception) {
            "No se pudo guardar el video: ${e.message}"
        }
    }

    fun cambiarEmojis(texto: String) = viewModelScope.launch {
        c.repositorio.cambiarEmojis(stickerId, texto)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerDetalleScreen(stickerId: Long, onVolver: () -> Unit) {
    val c = contenedor()
    val vm: StickerDetalleViewModel = viewModel { StickerDetalleViewModel(c, stickerId) }
    val sticker by vm.sticker.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var mostrarGuia by remember { mutableStateOf(false) }
    var editarEmojis by remember { mutableStateOf(false) }

    // Reproductor de audio del sticker (R19).
    var reproductor by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { reproductor?.release() } }
    fun alternarAudio(ruta: String) {
        val actual = reproductor
        if (actual != null) {
            actual.release(); reproductor = null; return
        }
        try {
            reproductor = MediaPlayer().apply {
                setDataSource(ruta)
                setOnCompletionListener { it.release(); reproductor = null }
                prepare()
                start()
            }
        } catch (e: Exception) {
            vm.mensaje = "No se pudo reproducir el audio"
        }
    }

    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val ruta = vm.sticker.value?.videoCortoRuta
        if (ok && ruta != null) vm.guardarVideo(ruta) else if (!ok) vm.mensaje = "Se necesita el permiso para guardar en la galería"
    }

    LaunchedEffect(vm.eliminado) { if (vm.eliminado) onVolver() }
    LaunchedEffect(vm.mensaje) { vm.mensaje?.let { snackbar.showSnackbar(it); vm.mensaje = null } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sticker") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") } },
                colors = coloresBarra(),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val s = sticker ?: run {
            Box(Modifier.fillMaxSize().padding(padding)); return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.fillMaxWidth().widthIn(max = 420.dp).aspectRatio(1f).tablero(),
                contentAlignment = Alignment.Center,
            ) {
                StickerImagen(s.webpRuta, Modifier.fillMaxSize().padding(12.dp), "Sticker")
            }

            Row(
                Modifier.fillMaxWidth().clickable { editarEmojis = true }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Emojis: ", style = MaterialTheme.typography.bodyMedium)
                Text(ReglasEmojis.separar(s.emojis).joinToString(" "), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { editarEmojis = true }) { Text("Cambiar") }
            }

            val audio = s.audioRuta?.takeIf { File(it).exists() }
            val video = s.videoCortoRuta?.takeIf { File(it).exists() }
            if (audio != null) {
                Button(onClick = { alternarAudio(audio) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(if (reproductor != null) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (reproductor != null) "Detener audio" else "Escuchar audio")
                }
            }
            if (video != null) {
                Button(
                    onClick = {
                        if (Galeria.necesitaPermiso) permiso.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        else vm.guardarVideo(video)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Rounded.SaveAlt, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Guardar video con sonido")
                }
                TextButton(onClick = { mostrarGuia = true }) {
                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, null)
                    Spacer(Modifier.size(6.dp))
                    Text("¿Cómo crear el sticker con sonido?")
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Accion(Icons.AutoMirrored.Rounded.DriveFileMove, "Mover a carpeta", Modifier.weight(1f)) { vm.cargarDestinos() }
                Accion(Icons.Rounded.Delete, "Eliminar", Modifier.weight(1f), peligro = true) { confirmarEliminar = true }
            }
        }
    }

    vm.destinos?.let { lista ->
        AlertDialog(
            onDismissRequest = { vm.cerrarDestinos() },
            title = { Text("Mover a carpeta") },
            text = {
                if (lista.isEmpty()) {
                    Text("No hay otra carpeta del mismo tipo con espacio.")
                } else {
                    Column {
                        lista.forEach { d ->
                            Text(
                                d.nombre,
                                Modifier.fillMaxWidth().clickable { vm.mover(d) }.padding(vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.cerrarDestinos() }) { Text("Cerrar") } },
        )
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("¿Eliminar sticker?") },
            text = { Text("Se borrarán el sticker, su audio y su video. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmarEliminar = false; vm.eliminar() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } },
        )
    }

    if (mostrarGuia) GuiaSonido { mostrarGuia = false }

    if (editarEmojis) {
        var texto by remember { mutableStateOf(sticker?.emojis?.replace(",", " ") ?: "") }
        AlertDialog(
            onDismissRequest = { editarEmojis = false },
            title = { Text("Emojis del sticker") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WhatsApp usa de 1 a 3 emojis para buscar el sticker.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        singleLine = true,
                        isError = !ReglasEmojis.esValido(texto),
                        supportingText = { if (!ReglasEmojis.esValido(texto)) Text("Escribe de 1 a 3 emojis") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.cambiarEmojis(texto); editarEmojis = false },
                    enabled = ReglasEmojis.esValido(texto),
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editarEmojis = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Accion(icono: ImageVector, texto: String, modifier: Modifier, peligro: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        val color = if (peligro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        Icon(icono, null, tint = color)
        Spacer(Modifier.size(6.dp))
        Text(texto, color = color)
    }
}
