package com.stickers.creador.ui.carpetaform

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stickers.creador.AppContainer
import com.stickers.creador.data.CambioIcono
import com.stickers.creador.domain.ReglasCarpeta
import com.stickers.creador.domain.TipoCarpeta
import com.stickers.creador.ui.coloresBarra
import com.stickers.creador.ui.componentes.tablero
import com.stickers.creador.ui.contenedor
import kotlinx.coroutines.launch

class CarpetaFormViewModel(private val c: AppContainer, val carpetaId: Long?) : ViewModel() {
    var nombre by mutableStateOf("")
    var tipo by mutableStateOf(TipoCarpeta.IMAGEN)
    /** PNG 96x96 del ícono elegido en esta edición. */
    var iconoNuevo by mutableStateOf<ByteArray?>(null)
        private set
    var iconoActualRuta by mutableStateOf<String?>(null)
        private set
    var usarPrimerSticker by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var guardando by mutableStateOf(false)
        private set
    var cargado by mutableStateOf(carpetaId == null)
        private set
    var terminado by mutableStateOf<Terminado?>(null)
        private set

    sealed interface Terminado {
        data class Guardado(val id: Long, val nueva: Boolean) : Terminado
        data object Eliminado : Terminado
    }

    val esEdicion get() = carpetaId != null

    init {
        if (carpetaId != null) viewModelScope.launch {
            c.repositorio.carpeta(carpetaId)?.let {
                nombre = it.nombre
                tipo = it.tipo
                iconoActualRuta = it.iconoRuta
                usarPrimerSticker = it.iconoRuta == null
            }
            cargado = true
        }
    }

    fun elegirIcono(uri: Uri) = viewModelScope.launch {
        try {
            iconoNuevo = c.procesadorImagen.iconoDesdeUri(uri)
            usarPrimerSticker = false
        } catch (e: Exception) {
            error = "No se pudo usar esa imagen: ${e.message}"
        }
    }

    fun guardar() {
        ReglasCarpeta.validarNombre(nombre)?.let { error = it; return }
        guardando = true
        viewModelScope.launch {
            try {
                if (carpetaId == null) {
                    val id = c.repositorio.crearCarpeta(nombre, tipo, if (usarPrimerSticker) null else iconoNuevo)
                    terminado = Terminado.Guardado(id, true)
                } else {
                    val cambio = when {
                        usarPrimerSticker -> CambioIcono.PrimerSticker
                        iconoNuevo != null -> CambioIcono.Nuevo(iconoNuevo!!)
                        else -> CambioIcono.Mantener
                    }
                    c.repositorio.editarCarpeta(carpetaId, nombre, cambio)
                    terminado = Terminado.Guardado(carpetaId, false)
                }
            } catch (e: Exception) {
                error = e.message ?: "No se pudo guardar"
            } finally {
                guardando = false
            }
        }
    }

    fun eliminar() {
        val id = carpetaId ?: return
        viewModelScope.launch {
            c.repositorio.eliminarCarpeta(id)
            terminado = Terminado.Eliminado
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarpetaFormScreen(
    carpetaId: Long?,
    onVolver: () -> Unit,
    onCreada: (Long) -> Unit,
    onEliminada: () -> Unit,
) {
    val c = contenedor()
    val vm: CarpetaFormViewModel = viewModel { CarpetaFormViewModel(c, carpetaId) }
    var confirmarEliminar by remember { mutableStateOf(false) }

    val elegirIcono = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.elegirIcono(uri)
    }

    LaunchedEffect(vm.terminado) {
        when (val t = vm.terminado) {
            is CarpetaFormViewModel.Terminado.Guardado -> if (t.nueva) onCreada(t.id) else onVolver()
            CarpetaFormViewModel.Terminado.Eliminado -> onEliminada()
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.esEdicion) "Editar carpeta" else "Nueva carpeta") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
                },
                colors = coloresBarra(),
            )
        },
    ) { padding ->
        if (!vm.cargado) {
            Box(Modifier.fillMaxSize().padding(padding)); return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = vm.nombre,
                onValueChange = { vm.nombre = it.take(128); vm.error = null },
                label = { Text("Nombre") },
                singleLine = true,
                isError = vm.error != null,
                supportingText = { vm.error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Tipo", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaTipo(
                    TipoCarpeta.IMAGEN, "Imagen", "Stickers desde fotos", Icons.Rounded.Image,
                    seleccionado = vm.tipo == TipoCarpeta.IMAGEN, bloqueado = vm.esEdicion,
                    modifier = Modifier.weight(1f),
                ) { vm.tipo = TipoCarpeta.IMAGEN }
                TarjetaTipo(
                    TipoCarpeta.ANIMADO, "Animado", "Stickers desde videos", Icons.Rounded.Movie,
                    seleccionado = vm.tipo == TipoCarpeta.ANIMADO, bloqueado = vm.esEdicion,
                    modifier = Modifier.weight(1f),
                ) { vm.tipo = TipoCarpeta.ANIMADO }
            }
            if (vm.esEdicion) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(4.dp))
                    Text("No se puede cambiar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(
                    "WhatsApp no permite mezclar stickers de imagen y animados en un mismo paquete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("Ícono", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier.size(72.dp).tablero(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val bytes = vm.iconoNuevo
                    val ruta = vm.iconoActualRuta
                    val bmp = remember(bytes, ruta, vm.usarPrimerSticker) {
                        when {
                            vm.usarPrimerSticker -> null
                            bytes != null -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ruta != null -> BitmapFactory.decodeFile(ruta)
                            else -> null
                        }
                    }
                    if (bmp != null) {
                        Image(bmp.asImageBitmap(), "Ícono elegido", Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        elegirIcono.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Elegir ícono") }
                    Row(
                        Modifier.selectable(
                            selected = vm.usarPrimerSticker,
                            onClick = { vm.usarPrimerSticker = true },
                            role = Role.RadioButton,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = vm.usarPrimerSticker, onClick = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Usar el primer sticker")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.guardar() },
                enabled = !vm.guardando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Guardar") }
            if (vm.esEdicion) {
                OutlinedButton(
                    onClick = { confirmarEliminar = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) { Text("Eliminar carpeta", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("¿Eliminar carpeta?") },
            text = { Text("Se borrarán la carpeta y todos sus stickers. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmarEliminar = false; vm.eliminar() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun TarjetaTipo(
    @Suppress("UNUSED_PARAMETER") tipo: TipoCarpeta,
    titulo: String,
    detalle: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    seleccionado: Boolean,
    bloqueado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = !bloqueado,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            if (seleccionado) 2.dp else 1.dp,
            if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            disabledContainerColor = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, null)
                Spacer(Modifier.size(8.dp))
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                RadioButton(selected = seleccionado, onClick = null)
            }
            Text(detalle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
