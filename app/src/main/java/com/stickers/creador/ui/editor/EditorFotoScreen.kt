package com.stickers.creador.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stickers.creador.AppContainer
import com.stickers.creador.data.ArchivosNuevoSticker
import com.stickers.creador.editor.BarraHerramientas
import com.stickers.creador.editor.ControladorEditor
import com.stickers.creador.editor.Herramienta
import com.stickers.creador.editor.LienzoEditor
import com.stickers.creador.editor.PanelDibujo
import com.stickers.creador.editor.PanelRecorte
import com.stickers.creador.editor.PanelTexto
import com.stickers.creador.editor.RenderSticker
import com.stickers.creador.ui.coloresBarra
import com.stickers.creador.ui.componentes.tablero
import com.stickers.creador.ui.contenedor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorFotoViewModel(private val c: AppContainer, private val carpetaId: Long) : ViewModel() {
    val controlador = ControladorEditor()

    var pickerLanzado = false
    var original by mutableStateOf<Bitmap?>(null)
        private set
    private var sinFondo: Bitmap? = null
    var cargando by mutableStateOf(false)
        private set
    var quitandoFondo by mutableStateOf(false)
        private set
    var guardando by mutableStateOf(false)
        private set
    var vistaPrevia by mutableStateOf<Bitmap?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
    var guardado by mutableStateOf(false)
        private set

    /** Imagen que se edita: sin fondo si se aplicó "Quitar fondo" (se puede deshacer). */
    val fuente: Bitmap?
        get() = if (controlador.estado.fondoQuitado) sinFondo ?: original else original

    fun cargar(uri: Uri) {
        cargando = true
        viewModelScope.launch {
            try {
                original = c.procesadorImagen.decodificar(uri)
            } catch (e: Exception) {
                mensaje = "No se pudo abrir la foto: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    fun alternarFondo() {
        if (controlador.estado.fondoQuitado) {
            controlador.marcarFondoQuitado(false); return
        }
        if (sinFondo != null) {
            controlador.marcarFondoQuitado(true); return
        }
        val bmp = original ?: return
        quitandoFondo = true
        viewModelScope.launch {
            try {
                sinFondo = c.quitarFondo.quitar(bmp)
                controlador.marcarFondoQuitado(true)
            } catch (e: Exception) {
                mensaje = e.message ?: "No se pudo quitar el fondo"
            } finally {
                quitandoFondo = false
            }
        }
    }

    /** R12: vista previa del resultado final (512x512) antes de guardar. */
    fun prepararVistaPrevia() {
        val f = fuente ?: return
        val estado = controlador.estado
        viewModelScope.launch {
            vistaPrevia = withContext(Dispatchers.Default) { RenderSticker.renderizar(f, estado) }
        }
    }

    fun cerrarVistaPrevia() {
        vistaPrevia = null
    }

    fun guardar() {
        val final = vistaPrevia ?: return
        guardando = true
        viewModelScope.launch {
            try {
                val bytes = c.procesadorImagen.codificarEstatico(final)
                val tmp = c.archivos.temporal(".webp")
                withContext(Dispatchers.IO) { tmp.writeBytes(bytes) }
                c.repositorio.agregarSticker(carpetaId, ArchivosNuevoSticker(tmp))
                guardado = true
            } catch (e: Exception) {
                mensaje = e.message ?: "No se pudo guardar el sticker"
            } finally {
                guardando = false
                vistaPrevia = null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorFotoScreen(carpetaId: Long, onCerrar: () -> Unit) {
    val c = contenedor()
    val vm: EditorFotoViewModel = viewModel { EditorFotoViewModel(c, carpetaId) }
    val snackbar = remember { SnackbarHostState() }
    var confirmarSalir by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) onCerrar() else vm.cargar(uri)
    }
    LaunchedEffect(Unit) {
        if (!vm.pickerLanzado) {
            vm.pickerLanzado = true
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
    LaunchedEffect(vm.guardado) { if (vm.guardado) onCerrar() }
    LaunchedEffect(vm.mensaje) {
        vm.mensaje?.let { snackbar.showSnackbar(it); vm.mensaje = null }
    }

    val salir = { if (vm.original != null) confirmarSalir = true else onCerrar() }
    BackHandler(enabled = vm.original != null) { confirmarSalir = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sticker desde foto") },
                navigationIcon = { IconButton(onClick = salir) { Icon(Icons.Rounded.Close, "Cancelar") } },
                actions = {
                    TextButton(
                        onClick = { vm.prepararVistaPrevia() },
                        enabled = vm.original != null && !vm.guardando,
                    ) { Text("Guardar", color = Color.White) }
                },
                colors = coloresBarra(),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val original = vm.original
        if (original == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (vm.cargando) CircularProgressIndicator()
            }
            return@Scaffold
        }
        EditorCuerpo(
            controlador = vm.controlador,
            fuente = vm.fuente,
            mostrarQuitarFondo = true,
            quitandoFondo = vm.quitandoFondo,
            onQuitarFondo = { vm.alternarFondo() },
            modifier = Modifier.padding(padding),
        )
    }

    vm.vistaPrevia?.let { previa ->
        DialogoVistaPrevia(
            previa = previa,
            guardando = vm.guardando,
            onGuardar = { vm.guardar() },
            onCancelar = { vm.cerrarVistaPrevia() },
        )
    }

    if (confirmarSalir) {
        AlertDialog(
            onDismissRequest = { confirmarSalir = false },
            title = { Text("¿Descartar el sticker?") },
            text = { Text("Se perderán los cambios.") },
            confirmButton = { TextButton(onClick = { confirmarSalir = false; onCerrar() }) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { confirmarSalir = false }) { Text("Seguir editando") } },
        )
    }
}

/** Lienzo + panel de la herramienta activa + barra de herramientas (compartido con video). */
@Composable
fun EditorCuerpo(
    controlador: ControladorEditor,
    fuente: Bitmap?,
    mostrarQuitarFondo: Boolean,
    quitandoFondo: Boolean,
    onQuitarFondo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().imePadding()) {
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            LienzoEditor(controlador, fuente, Modifier.widthIn(max = 480.dp))
        }
        Surface(tonalElevation = 2.dp) {
            Column(Modifier.navigationBarsPadding()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 12.dp),
                ) {
                    when (controlador.herramienta) {
                        Herramienta.RECORTAR -> PanelRecorte(controlador)
                        Herramienta.TEXTO -> PanelTexto(controlador)
                        Herramienta.DIBUJAR -> PanelDibujo(controlador)
                    }
                }
                HorizontalDivider(Modifier.padding(top = 8.dp))
                BarraHerramientas(
                    controlador = controlador,
                    mostrarQuitarFondo = mostrarQuitarFondo,
                    quitandoFondo = quitandoFondo,
                    onQuitarFondo = onQuitarFondo,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
fun DialogoVistaPrevia(previa: Bitmap, guardando: Boolean, onGuardar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text("Vista previa") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(240.dp).tablero(), contentAlignment = Alignment.Center) {
                    Image(previa.asImageBitmap(), "Vista previa del sticker", Modifier.fillMaxSize())
                }
                Text("512 × 512 px", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !guardando) {
                if (guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Guardar sticker")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar, enabled = !guardando) { Text("Seguir editando") } },
    )
}
