package com.stickers.creador.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stickers.creador.AppContainer
import com.stickers.creador.data.ArchivosNuevoSticker
import com.stickers.creador.domain.LimitesWhatsApp
import com.stickers.creador.domain.ReglasTramo
import com.stickers.creador.editor.ControladorEditor
import com.stickers.creador.media.video.ExtractorCuadros
import com.stickers.creador.ui.coloresBarra
import com.stickers.creador.ui.componentes.ProgresoProceso
import com.stickers.creador.ui.contenedor
import com.stickers.creador.ui.theme.Primario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class EditorVideoViewModel(private val c: AppContainer, private val carpetaId: Long) : ViewModel() {
    val controlador = ControladorEditor()

    var pickerLanzado = false
    var uri by mutableStateOf<Uri?>(null)
        private set
    var duracionMs by mutableStateOf(0L)
        private set
    var miniaturas by mutableStateOf<List<Bitmap>>(emptyList())
        private set
    var inicioMs by mutableStateOf(0L)
        private set
    var finMs by mutableStateOf(0L)
        private set
    var paso by mutableIntStateOf(1)
        private set
    var cuadrosVista by mutableStateOf<List<Bitmap>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set

    var etapa by mutableStateOf("")
        private set
    var progreso by mutableStateOf(0f)
        private set
    var procesando by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf<String?>(null)
    /** Mensaje final "Sticker creado · Audio guardado". */
    var terminado by mutableStateOf<String?>(null)
        private set

    private var trabajo: Job? = null

    fun cargar(u: Uri) {
        uri = u
        cargando = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ExtractorCuadros(contextoApp, u).use { ex ->
                        duracionMs = ex.duracionMs
                        val (i, f) = ReglasTramo.ajustar(0, LimitesWhatsApp.MAX_TRAMO_VIDEO_MS, ex.duracionMs)
                        inicioMs = i; finMs = f
                        val n = 8
                        miniaturas = (0 until n).mapNotNull { k ->
                            ex.cuadro(ex.duracionMs * k / n, 160, exacto = false)
                        }
                    }
                }
                if (duracionMs <= 0) mensaje = "No se pudo leer la duración del video"
            } catch (e: Exception) {
                mensaje = "No se pudo abrir el video: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    /** Ajusta el tramo: máximo 5 s y mínimo 0,5 s (R14). */
    fun cambiarTramo(nuevoInicio: Long, nuevoFin: Long) {
        var i = nuevoInicio
        var f = nuevoFin
        val max = LimitesWhatsApp.MAX_TRAMO_VIDEO_MS
        if (f - i > max) {
            if (i != inicioMs) f = i + max else i = f - max
        }
        if (f - i < MIN_TRAMO_MS) {
            if (i != inicioMs) i = (f - MIN_TRAMO_MS).coerceAtLeast(0) else f = (i + MIN_TRAMO_MS).coerceAtMost(duracionMs)
        }
        val (ai, af) = ReglasTramo.ajustar(i, f, duracionMs)
        inicioMs = ai; finMs = af
    }

    fun siguiente() {
        val u = uri ?: return
        if (!ReglasTramo.esValido(inicioMs, finMs)) {
            mensaje = "Elige un tramo de hasta 5 segundos"; return
        }
        cargando = true
        viewModelScope.launch {
            try {
                cuadrosVista = withContext(Dispatchers.IO) {
                    ExtractorCuadros(contextoApp, u).use { ex ->
                        ex.cuadrosTramo(inicioMs, finMs, fps = 6, maxLado = 480)
                    }
                }
                paso = 2
            } catch (e: Exception) {
                mensaje = "No se pudo leer el tramo: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    fun volverAlTramo() {
        paso = 1
    }

    fun guardar() {
        val u = uri ?: return
        procesando = true
        progreso = 0f
        trabajo = viewModelScope.launch {
            try {
                val r = c.procesadorVideo.procesar(u, inicioMs, finMs, controlador.estado) { e, p ->
                    etapa = e; progreso = p
                }
                c.repositorio.agregarSticker(carpetaId, ArchivosNuevoSticker(r.webp, r.audio, r.videoCorto))
                val partes = mutableListOf("Sticker creado")
                if (r.audio != null) partes += "Audio guardado"
                terminado = partes.joinToString(" · ") +
                    if (r.avisos.isNotEmpty()) "\n" + r.avisos.joinToString("\n") else ""
            } catch (e: CancellationException) {
                mensaje = "Cancelado"
            } catch (e: Exception) {
                mensaje = e.message ?: "No se pudo crear el sticker"
            } finally {
                procesando = false
            }
        }
    }

    fun cancelar() {
        trabajo?.cancel()
    }

    private val contextoApp get() = c.contexto

    companion object {
        const val MIN_TRAMO_MS = 500L
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorVideoScreen(carpetaId: Long, onCerrar: () -> Unit) {
    val c = contenedor()
    val vm: EditorVideoViewModel = viewModel { EditorVideoViewModel(c, carpetaId) }
    val snackbar = remember { SnackbarHostState() }
    var confirmarSalir by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) onCerrar() else vm.cargar(uri)
    }
    LaunchedEffect(Unit) {
        if (!vm.pickerLanzado) {
            vm.pickerLanzado = true
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }
    }
    LaunchedEffect(vm.mensaje) {
        vm.mensaje?.let { snackbar.showSnackbar(it); vm.mensaje = null }
    }

    BackHandler(enabled = vm.uri != null) {
        if (vm.paso == 2) vm.volverAlTramo() else confirmarSalir = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.paso == 1) "Paso 1 · Elige el tramo" else "Paso 2 · Edita") },
                navigationIcon = {
                    IconButton(onClick = { if (vm.uri != null) confirmarSalir = true else onCerrar() }) {
                        Icon(Icons.Rounded.Close, "Cancelar")
                    }
                },
                actions = {
                    if (vm.paso == 2) {
                        TextButton(onClick = { vm.guardar() }, enabled = !vm.procesando) {
                            Text("Guardar", color = Color.White)
                        }
                    }
                },
                colors = coloresBarra(),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val uri = vm.uri
        when {
            uri == null || (vm.paso == 1 && vm.duracionMs <= 0) -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { if (vm.cargando) CircularProgressIndicator() }

            vm.paso == 1 -> PasoTramo(vm, uri, Modifier.padding(padding))

            else -> {
                // Vista previa en bucle del tramo (cuadros a 6 fps) bajo el texto y el dibujo.
                var indice by remember { mutableIntStateOf(0) }
                val cuadros = vm.cuadrosVista
                LaunchedEffect(cuadros) {
                    while (cuadros.isNotEmpty()) {
                        delay(1000L / 6)
                        indice = (indice + 1) % cuadros.size
                    }
                }
                EditorCuerpo(
                    controlador = vm.controlador,
                    fuente = cuadros.getOrNull(indice % cuadros.size.coerceAtLeast(1)),
                    mostrarQuitarFondo = false, // R17: no aplica a video
                    quitandoFondo = false,
                    onQuitarFondo = {},
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    if (vm.procesando) {
        ProgresoProceso(vm.etapa.ifEmpty { "Creando sticker…" }, vm.progreso, onCancelar = { vm.cancelar() })
    }

    vm.terminado?.let { texto ->
        AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text("¡Listo!") },
            text = { Text(texto) },
            confirmButton = { TextButton(onClick = onCerrar) { Text("Aceptar") } },
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

@Composable
private fun PasoTramo(vm: EditorVideoViewModel, uri: Uri, modifier: Modifier) {
    val duracion = vm.duracionMs
    Column(modifier.fillMaxSize().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Vista previa del tramo en bucle.
        var vista by remember { mutableStateOf<VideoView?>(null) }
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = false
                            mp.setVolume(1f, 1f)
                            seekTo(vm.inicioMs.toInt())
                            start()
                        }
                        vista = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        LaunchedEffect(vista) {
            val v = vista ?: return@LaunchedEffect
            while (true) {
                delay(100)
                try {
                    if (v.currentPosition >= vm.finMs || v.currentPosition < vm.inicioMs - 300 || !v.isPlaying) {
                        v.seekTo(vm.inicioMs.toInt())
                        if (!v.isPlaying) v.start()
                    }
                } catch (_: Exception) {
                }
            }
        }
        LaunchedEffect(vm.inicioMs) {
            try { vista?.seekTo(vm.inicioMs.toInt()) } catch (_: Exception) {}
        }

        // Selector de tramo: tira de miniaturas con dos manijas.
        Text(
            String.format(Locale.US, "%.1f s / 5 s", (vm.finMs - vm.inicioMs) / 1000f),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
            Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 10.dp).clip(RoundedCornerShape(8.dp))) {
                vm.miniaturas.forEach { bmp ->
                    Image(
                        bmp.asImageBitmap(), null,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            RangeSlider(
                value = vm.inicioMs.toFloat()..vm.finMs.toFloat(),
                onValueChange = { r -> vm.cambiarTramo(r.start.toLong(), r.endInclusive.toLong()) },
                valueRange = 0f..duracion.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Primario,
                    activeTrackColor = Primario.copy(alpha = 0.45f),
                    inactiveTrackColor = Color.Black.copy(alpha = 0.35f),
                ),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text(String.format(Locale.US, "Inicio %.1f s", vm.inicioMs / 1000f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text(String.format(Locale.US, "Fin %.1f s", vm.finMs / 1000f), style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { vm.siguiente() },
            enabled = !vm.cargando,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            if (vm.cargando) CircularProgressIndicator(Modifier.height(20.dp).aspectRatio(1f), strokeWidth = 2.dp, color = Color.White)
            else Text("Siguiente")
        }
    }
}
