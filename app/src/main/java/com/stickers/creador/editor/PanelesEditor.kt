package com.stickers.creador.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Chips Rectángulo / Círculo. */
@Composable
fun SelectorForma(forma: Forma, onCambio: (Forma) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = forma == Forma.RECTANGULO,
            onClick = { onCambio(Forma.RECTANGULO) },
            label = { Text("Rectángulo") },
            leadingIcon = { Icon(Icons.Rounded.CropSquare, null, Modifier.size(FilterChipDefaults.IconSize)) },
        )
        FilterChip(
            selected = forma == Forma.CIRCULO,
            onClick = { onCambio(Forma.CIRCULO) },
            label = { Text("Círculo") },
            leadingIcon = { Icon(Icons.Rounded.Circle, null, Modifier.size(FilterChipDefaults.IconSize)) },
        )
    }
}

/** Recortar · Quitar fondo · Texto · Dibujar · Deshacer. */
@Composable
fun BarraHerramientas(
    controlador: ControladorEditor,
    mostrarQuitarFondo: Boolean,
    quitandoFondo: Boolean,
    onQuitarFondo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BotonHerramienta(Icons.Rounded.Crop, "Recortar", controlador.herramienta == Herramienta.RECORTAR) {
            controlador.herramienta = Herramienta.RECORTAR
        }
        if (mostrarQuitarFondo) {
            Column(
                Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(enabled = !quitandoFondo, onClick = onQuitarFondo)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .width(64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (quitandoFondo) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Rounded.AutoFixHigh, null,
                        tint = if (controlador.estado.fondoQuitado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    if (controlador.estado.fondoQuitado) "Fondo quitado" else "Quitar fondo",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
        BotonHerramienta(Icons.Rounded.TextFields, "Texto", controlador.herramienta == Herramienta.TEXTO) {
            controlador.herramienta = Herramienta.TEXTO
        }
        BotonHerramienta(Icons.Rounded.Brush, "Dibujar", controlador.herramienta == Herramienta.DIBUJAR) {
            controlador.herramienta = Herramienta.DIBUJAR
        }
        BotonHerramienta(
            Icons.AutoMirrored.Rounded.Undo, "Deshacer", seleccionado = false,
            habilitado = controlador.puedeDeshacer,
        ) { controlador.deshacer() }
    }
}

@Composable
private fun BotonHerramienta(
    icono: ImageVector,
    texto: String,
    seleccionado: Boolean,
    habilitado: Boolean = true,
    onClick: () -> Unit,
) {
    val color = when {
        !habilitado -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        seleccionado -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (seleccionado) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .width(60.dp)
            .height(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icono, null, tint = color)
        Text(texto, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** Paleta de colores accesible: el color elegido se marca con un ícono, no solo con color. */
@Composable
fun PaletaColores(seleccionado: Int, onElegir: (Int) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PaletaEditor.colores.forEach { c ->
            val elegido = c == seleccionado
            Box48(
                color = Color(c),
                elegido = elegido,
                onClick = { onElegir(c) },
            )
        }
    }
}

@Composable
private fun Box48(color: Color, elegido: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(BorderStroke(if (elegido) 3.dp else 1.dp, MaterialTheme.colorScheme.outline), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (elegido) "Color elegido" else "Elegir color" },
        contentAlignment = Alignment.Center,
    ) {
        if (elegido) {
            val claro = (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f) > 0.5f
            Icon(Icons.Rounded.Check, null, tint = if (claro) Color.Black else Color.White)
        }
    }
}

/** Panel de texto: escribir, color, tamaño, agregar/eliminar. */
@Composable
fun PanelTexto(controlador: ControladorEditor, modifier: Modifier = Modifier) {
    val actual = controlador.textoActual()
    var nuevo by remember { mutableStateOf("") }
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (actual == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nuevo,
                    onValueChange = { nuevo = it },
                    label = { Text("Escribe un texto") },
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    maxLines = 3,
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { controlador.agregarTexto(nuevo.trim()); nuevo = "" },
                    enabled = nuevo.isNotBlank(),
                ) { Text("Agregar") }
            }
            PaletaColores(controlador.colorTexto) { controlador.colorTexto = it }
            Text("Tamaño", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = controlador.tamanoTexto,
                onValueChange = { controlador.tamanoTexto = it },
                valueRange = PaletaEditor.TAMANO_TEXTO_MIN..PaletaEditor.TAMANO_TEXTO_MAX,
            )
        } else {
            var edicion by remember(actual.id) { mutableStateOf(actual.texto) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = edicion,
                    onValueChange = {
                        edicion = it
                        if (it.isNotBlank()) controlador.editarTextoSeleccionado { c -> c.copy(texto = it) }
                    },
                    label = { Text("Texto seleccionado") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                )
                Spacer(Modifier.width(4.dp))
                androidx.compose.material3.IconButton(onClick = { controlador.eliminarTextoSeleccionado() }) {
                    Icon(Icons.Rounded.Delete, "Eliminar texto")
                }
            }
            PaletaColores(actual.color) { c -> controlador.editarTextoSeleccionado { it.copy(color = c) } }
            Text("Tamaño", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = actual.tamano,
                onValueChange = { v -> controlador.editarTextoSeleccionado { it.copy(tamano = v) } },
                valueRange = PaletaEditor.TAMANO_TEXTO_MIN..PaletaEditor.TAMANO_TEXTO_MAX,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { controlador.textoSeleccionado = null }) { Text("Nuevo texto") }
            }
            Text(
                "Arrastra el texto en el lienzo para moverlo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Panel de dibujo: color, grosor, deshacer trazo. */
@Composable
fun PanelDibujo(controlador: ControladorEditor, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PaletaColores(controlador.colorDibujo) { controlador.colorDibujo = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Grosor", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(12.dp))
            Slider(
                value = controlador.grosorDibujo,
                onValueChange = { controlador.grosorDibujo = it },
                valueRange = PaletaEditor.GROSOR_MIN..PaletaEditor.GROSOR_MAX,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { controlador.deshacerTrazo() },
                enabled = controlador.estado.trazos.isNotEmpty(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Undo, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Trazo")
            }
        }
    }
}

/** Panel de recorte: forma + reiniciar encuadre. */
@Composable
fun PanelRecorte(controlador: ControladorEditor, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectorForma(controlador.estado.forma, { controlador.cambiarForma(it) }, Modifier.weight(1f))
            TextButton(onClick = { controlador.reiniciarRecorte() }) { Text("Centrar") }
        }
        Text(
            "Arrastra y pellizca la imagen para encuadrarla.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
