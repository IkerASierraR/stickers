# plan.md — Plan técnico

## 1. Stack
| Capa | Elección | Motivo |
|---|---|---|
| Lenguaje | Kotlin | Pedido del usuario |
| UI | Jetpack Compose + Material 3 | Igual que el proyecto anterior |
| Navegación | Navigation Compose | Oficial, simple |
| Base de datos | Room | Carpetas y stickers |
| Galería | Photo Picker de Android | No requiere permisos de almacenamiento |
| Quitar fondo | ML Kit Subject Segmentation | Gratis, corre en el celular |
| Recorte de video y audio | Media3 Transformer | Oficial de Google: recorta tramo, extrae audio, genera MP4 corto |
| Video → WebP animado | **Empaquetador propio en Kotlin** (`media/WebpContenedor.kt`) | Decidido en T0.6: cada cuadro se codifica con `Bitmap.compress(WEBP)` nativo y se une en un WebP animado (VP8X + ANIM + ANMF). Sin JNI ni librerías externas |
| Envío a WhatsApp | API oficial de paquetes (`WhatsApp/stickers`, ContentProvider + Intent) | Única vía soportada |
| Arquitectura | MVVM + repositorio, inyección manual (`AppContainer`) | Sencilla para programar con IA |
| Pruebas | JUnit 4 | Validaciones y reglas |

Costo: 0. Sin servidores.

## 2. Arquitectura
```
ui/            Pantallas Compose + ViewModels
editor/        Lienzo: recorte, texto, dibujo, vista previa (Compose Canvas)
media/
  image/       Recorte, quitar fondo, exportar WebP estático
  video/       Tramo, recorte, frames → WebP animado, audio, MP4 corto
  whatsapp/    ContentProvider del paquete, validador de formato, Intent
domain/        Reglas puras (límites de carpeta, tipos, validaciones)
data/          Room + archivos en almacenamiento interno
AppContainer   Crea DB, repositorios y servicios
```

## 3. Entidades (Room)
### Carpeta
| Campo | Tipo | Nota |
|---|---|---|
| id | Long PK | |
| identificador | String único | ID del paquete para WhatsApp |
| nombre | String | ≤ 128 caracteres |
| tipo | Enum `IMAGEN` / `ANIMADO` | Fijo tras crear |
| iconoRuta | String? | `null` = usar primer sticker |
| version | Int | Se incrementa al editar (para que WhatsApp actualice) |
| creadoEn | Long | |

### Sticker
| Campo | Tipo | Nota |
|---|---|---|
| id | Long PK | |
| carpetaId | Long FK → Carpeta (CASCADE) | |
| webpRuta | String | 512×512 |
| audioRuta | String? | Solo animados (`.m4a`, ≤ 5 s) |
| videoCortoRuta | String? | Solo animados (`.mp4` con sonido) |
| emojis | String | Si WhatsApp los exige (verificar) |
| orden | Int | |
| creadoEn | Long | |

Archivos en `filesDir/stickers/{carpetaId}/`.

## 4. Flujos técnicos
### 4.1 Foto → sticker
1. Photo Picker → `Bitmap`.
2. (Opcional) ML Kit Subject Segmentation → bitmap con fondo transparente.
3. Recorte rectángulo/círculo (máscara, sin borde).
4. Capas de texto y dibujo (Compose Canvas) → render final.
5. Escalar a 512×512 con fondo transparente → `Bitmap.compress(WEBP_LOSSY)` bajando calidad hasta ≤ 100 KB.

### 4.2 Video → sticker animado + audio + video corto
1. Photo Picker (video) → selector de tramo (≤ 5 s) con miniaturas.
2. Recorte rectángulo/círculo + capas de texto y dibujo (se dibujan sobre cada cuadro).
3. **Sticker:** extraer cuadros del tramo (≈ 10–15 fps), aplicar recorte y capas, escalar a 512×512, codificar WebP animado. Bajar fps/calidad hasta ≤ 500 KB.
4. **Audio:** Media3 Transformer con video removido y recorte de tiempo → `.m4a`.
5. **Video corto:** Media3 Transformer con recorte de tiempo y efectos de recorte → `.mp4` con sonido.
6. Todo en segundo plano (corrutinas) con barra de progreso.

### 4.3 Envío a WhatsApp
- `ContentProvider` que expone carpetas y stickers según el formato del repositorio oficial.
- Validador previo: 3–30 stickers, tamaños, dimensiones, ícono 96×96.
- Intent `com.whatsapp.intent.action.ENABLE_STICKER_PACK`.
- Copiar del sample oficial de Android en lugar de escribirlo desde cero.

### 4.4 Sonido (R20–R22)
- Botón "Guardar video con sonido" → copia el MP4 corto a la galería (MediaStore).
- Mostrar una guía de 3 pasos: WhatsApp → Stickers → Crear → elegir ese video.
- `audioRuta` queda listo para una futura versión si WhatsApp abre el sonido a apps externas.

## 5. Pantallas y navegación
| Ruta | Pantalla |
|---|---|
| `inicio` | Lista de carpetas |
| `carpeta/{id}` | Cuadrícula de stickers + "Agregar a WhatsApp" |
| `carpetaForm/{id?}` | Crear/editar carpeta |
| `editor/foto/{carpetaId}` | Editor de foto |
| `editor/video/{carpetaId}` | Tramo + editor de video |
| `sticker/{id}` | Detalle: vista grande, audio, guardar video, mover, eliminar |

## 6. Riesgos
| Riesgo | Mitigación |
|---|---|
| **Codificar WebP animado en Android** no tiene soporte nativo; `ffmpeg-kit` fue retirado | **Resuelto (T0.6):** Android sí codifica WebP estático. `WebpContenedor.animar()` toma esos cuadros (chunks ALPH + VP8/VP8L) y escribe el contenedor animado según la especificación oficial de WebP. Cuadros: `MediaMetadataRetriever` a 12 fps; ajuste automático bajando fps (12→4) y calidad (búsqueda binaria) hasta ≤ 500 KB. Pendiente confirmar en WhatsApp real (C2) |
| Sticker animado supera 500 KB | Ajuste automático de fps, calidad y duración |
| WhatsApp cambia formato o reglas | Validar contra el README oficial al inicio; validador centralizado |
| El editor de WhatsApp no conserva el audio del video compartido | Verificar en T0.7; si falla, el audio queda guardado igual y se informa al usuario |
| ML Kit necesita descargar su modelo la primera vez | Lo gestiona Google Play Services; mostrar aviso si aún no está disponible |
| Rendimiento con videos largos | Solo se procesa el tramo elegido (≤ 5 s) |

## 7. Privacidad
- Nada sale del celular salvo lo que yo envío a WhatsApp.
- Photo Picker: sin permisos amplios de almacenamiento.
- Sin anuncios ni analítica.

## 8. Decisiones de implementación
- **Versiones:** AGP 9.2.1, Kotlin 2.4.10 (Compose plugin), KSP 2.3.12, Room 2.8.5, Compose BOM 2026.09.00, Navigation 2.9.7, Media3 1.8.0, ML Kit Subject Segmentation 16.0.0-beta1. compileSdk 37, minSdk 26.
- **Editor:** `editor/RenderSticker` dibuja con `android.graphics.Canvas` tanto la vista previa como el resultado de 512×512 (lo que se ve es lo que se guarda). Estado inmutable `EstadoEditor` + historial para Deshacer.
- **Video corto:** Media3 Transformer con `Presentation` (ajuste a 512×512) + `MatrixTransformation` (mismo zoom/desplazamiento del editor) + `OverlayEffect` (texto, dibujo y máscara del círculo en blanco, porque MP4 no tiene transparencia).
- **Audio:** Transformer con `setRemoveVideo(true)` → AAC en `.m4a`.
- **ContentProvider:** columnas y rutas idénticas al sample oficial; los datos salen de Room y los archivos de `filesDir/stickers/{carpetaId}/`. `image_data_version` = `Carpeta.version`, que sube con cada cambio (R26).
- **Emojis:** WhatsApp exige 1–3 por sticker; por defecto 😀, editable en el detalle.
