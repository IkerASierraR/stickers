package com.stickers.creador.ui.componentes

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stickers.creador.data.CarpetaResumen
import com.stickers.creador.data.Sticker
import com.stickers.creador.domain.LimitesWhatsApp
import com.stickers.creador.domain.ReglasCarpeta
import com.stickers.creador.domain.TipoCarpeta
import com.stickers.creador.ui.theme.Primario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Muestra un sticker WebP desde archivo. En Android 9+ los animados se reproducen en bucle
 * (AnimatedImageDrawable); en versiones anteriores se muestra el primer cuadro.
 */
@Composable
fun StickerImagen(ruta: String?, modifier: Modifier = Modifier, descripcion: String? = null) {
    val drawable by produceState<Drawable?>(null, ruta) {
        value = withContext(Dispatchers.IO) { cargarDrawable(ruta) }
    }
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = descripcion
            }
        },
        update = { iv ->
            if (iv.drawable !== drawable) {
                (iv.drawable as? Animatable)?.stop()
                iv.setImageDrawable(drawable)
                (drawable as? Animatable)?.start()
            }
        },
        modifier = modifier,
    )
    DisposableEffect(drawable) {
        onDispose { (drawable as? Animatable)?.stop() }
    }
}

private fun cargarDrawable(ruta: String?): Drawable? {
    if (ruta == null) return null
    val f = File(ruta)
    if (!f.exists()) return null
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeDrawable(ImageDecoder.createSource(f))
        } else {
            BitmapFactory.decodeFile(ruta)?.let { BitmapDrawable(null, it) }
        }
    } catch (_: Exception) {
        null
    }
}

/** Contador "12/30" con barra de progreso verde. */
@Composable
fun ContadorStickers(cantidad: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            "$cantidad/${LimitesWhatsApp.MAX_STICKERS}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { cantidad / LimitesWhatsApp.MAX_STICKERS.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Primario,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
        )
    }
}

@Composable
fun EtiquetaTipo(tipo: TipoCarpeta) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (tipo == TipoCarpeta.IMAGEN) Icons.Rounded.Image else Icons.Rounded.Movie,
                null, Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (tipo == TipoCarpeta.IMAGEN) "Imagen" else "Animado",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Tarjeta de carpeta: ícono de portada, nombre, tipo y contador. */
@Composable
fun CarpetaCard(resumen: CarpetaResumen, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = resumen.carpeta
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .tablero(),
            contentAlignment = Alignment.Center,
        ) {
            val portada = c.iconoRuta?.takeIf { File(it).exists() } ?: resumen.primerSticker
            if (portada != null) {
                StickerImagen(portada, Modifier.fillMaxSize().padding(16.dp), "Portada de ${c.nombre}")
            } else {
                Icon(
                    if (c.tipo == TipoCarpeta.IMAGEN) Icons.Rounded.Image else Icons.Rounded.Movie,
                    null,
                    Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(c.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            EtiquetaTipo(c.tipo)
            ContadorStickers(resumen.cantidad)
        }
    }
}

/** Sticker sobre tablero; altavoz pequeño si tiene audio. */
@Composable
fun StickerCelda(sticker: Sticker, numero: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .tablero(8.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Sticker $numero" + if (sticker.audioRuta != null) ", con audio" else "" },
    ) {
        StickerImagen(sticker.webpRuta, Modifier.fillMaxSize().padding(6.dp))
        if (sticker.audioRuta != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Tiene audio", Modifier.padding(3.dp).size(14.dp))
            }
        }
    }
}

/** Botón verde grande "Agregar a WhatsApp" (deshabilitado con menos de 3, con "Faltan N"). */
@Composable
fun BotonWhatsApp(cantidad: Int, enviando: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val habilitado = ReglasCarpeta.puedeEnviar(cantidad) && !enviando
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            enabled = habilitado,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primario, contentColor = Color.White),
            shape = RoundedCornerShape(28.dp),
        ) {
            Text(if (enviando) "Preparando…" else "Agregar a WhatsApp", style = MaterialTheme.typography.titleSmall)
        }
        val faltan = ReglasCarpeta.faltanParaEnviar(cantidad)
        if (faltan > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Faltan $faltan sticker${if (faltan == 1) "" else "s"} para enviar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Diálogo con barra: "Creando sticker… 60%". */
@Composable
fun ProgresoProceso(etapa: String, progreso: Float, onCancelar: (() -> Unit)? = null) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        dismissButton = {
            if (onCancelar != null) TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
        title = { Text("$etapa ${(progreso * 100).toInt()}%") },
        text = {
            LinearProgressIndicator(
                progress = { progreso.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = Primario,
            )
        },
    )
}

/** Guía de 3 pasos para crear el sticker con sonido dentro de WhatsApp (R21). */
@Composable
fun GuiaSonido(onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = { TextButton(onClick = onCerrar) { Text("Entendido") } },
        title = { Text("Crear el sticker con sonido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Paso(1, "Toca \"Guardar video con sonido\". El video queda en tu galería (carpeta Movies/Stickers).")
                Paso(2, "En WhatsApp abre un chat, toca el ícono de stickers y elige \"Crear\".")
                Paso(3, "Elige ese video. WhatsApp crea el sticker conservando el audio del video.")
                Text(
                    "Los paquetes que agrega esta app se envían sin sonido porque WhatsApp aún no permite audio en paquetes de otras apps. El audio queda guardado en cada sticker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun Paso(n: Int, texto: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = Primario, contentColor = Color.White, modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("$n", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.width(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** Estado vacío con ilustración simple. */
@Composable
fun EstadoVacio(texto: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(120.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Image, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
