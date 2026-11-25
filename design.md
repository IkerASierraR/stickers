# design.md — Diseño UI (para Google Stitch)

## 1. Estilo visual
- **Personalidad:** divertida pero ordenada; los stickers son protagonistas.
- **Sistema:** Material 3, Android, vertical.
- **Paleta verde (inspirada en apps de mensajería, sin usar logos ni marcas de WhatsApp):**
  - Primario: `#1FAF5A`
  - Primario oscuro (barra superior): `#0B6E4F`
  - Acento suave: `#DCF8E6`
  - Fondo claro: `#F4F7F5` · Superficie: `#FFFFFF`
  - Fondo oscuro: `#0F1A16` · Superficie oscura: `#1A2621`
  - Error: `#D93025`
- **Fondo de stickers:** cuadrícula tipo tablero gris claro para que se note la transparencia.
- **Tipografía:** Roboto. Títulos 20–22 sp, texto 14–16 sp.
- **Formas:** tarjetas 16 dp, botones redondeados, espaciado base 8 dp.
- **Íconos:** Material Symbols redondeados.
- **Accesibilidad:** contraste AA en textos; botones de mínimo 48 dp; nada se comunica solo por color.

## 2. Componentes
| Componente | Uso |
|---|---|
| `CarpetaCard` | Ícono de portada, nombre, tipo (imagen/animado), contador "12/30" |
| `StickerCelda` | Sticker sobre tablero; ícono pequeño de altavoz si tiene audio |
| `ContadorStickers` | "12/30" con barra de progreso verde |
| `BotonWhatsApp` | Botón verde grande "Agregar a WhatsApp" (deshabilitado con < 3 y mensaje "Faltan N") |
| `SelectorForma` | Chips Rectángulo / Círculo |
| `BarraHerramientas` | Recortar · Quitar fondo · Texto · Dibujar · Deshacer |
| `SelectorTramo` | Tira de miniaturas con dos manijas y duración "3.2 s / 5 s" |
| `PanelTexto` | Campo, paleta de colores, tamaño |
| `PanelDibujo` | Paleta de colores, grosor, deshacer |
| `ProgresoProceso` | Diálogo con barra: "Creando sticker… 60%" |
| `GuiaSonido` | 3 pasos ilustrados para crear el sticker con sonido en WhatsApp |

## 3. Pantallas

### 3.1 Inicio
- **Barra superior:** verde oscuro, título "Mis stickers".
- **Cuerpo:** cuadrícula de 2 columnas de `CarpetaCard`.
- **Acción principal:** FAB "Nueva carpeta".
- **Estado vacío:** ilustración simple + "Crea tu primera carpeta de stickers".

### 3.2 Crear / editar carpeta
- Campo "Nombre".
- Tipo: dos tarjetas seleccionables **Imagen** / **Animado** (bloqueado al editar, con nota "No se puede cambiar").
- Ícono: "Elegir ícono" o "Usar el primer sticker".
- Botones: Guardar · Eliminar (solo al editar, con confirmación).

### 3.3 Carpeta
- **Encabezado:** ícono, nombre, tipo, `ContadorStickers`.
- **Cuerpo:** cuadrícula de 3 columnas de `StickerCelda`.
- **Abajo fijo:** `BotonWhatsApp`.
- **FAB:** "Nuevo sticker" (abre la galería de fotos o videos según el tipo). Oculto al llegar a 30.
- **Estado vacío:** "Agrega al menos 3 stickers para enviarla a WhatsApp".

### 3.4 Editor de foto
- **Centro:** lienzo grande sobre tablero con la imagen.
- **Arriba:** Cancelar · Guardar.
- **Abajo:** `SelectorForma` + `BarraHerramientas`; al tocar Texto o Dibujar se abre su panel.
- **Quitar fondo:** botón con progreso; se puede deshacer.

### 3.5 Editor de video
- **Paso 1 — Tramo:** vista previa del video + `SelectorTramo` (máx. 5 s) + botón Siguiente.
- **Paso 2 — Editar:** lienzo con el video en bucle, `SelectorForma`, Texto, Dibujar (sin Quitar fondo).
- **Guardar:** `ProgresoProceso`; al terminar, mensaje "Sticker creado · Audio guardado".

### 3.6 Detalle de sticker
- Sticker grande sobre tablero.
- Si es animado: botón ▶ "Escuchar audio" y botón "Guardar video con sonido".
- Enlace "¿Cómo crear el sticker con sonido?" → `GuiaSonido`.
- Acciones: Mover a carpeta · Eliminar.

## 4. Prompts sugeridos para Stitch
- **Inicio:** "Pantalla móvil Android Material 3 en español, app para crear stickers. Barra superior verde oscuro #0B6E4F con título 'Mis stickers'. Cuadrícula de 2 columnas con tarjetas de carpeta: ícono de portada, nombre, etiqueta 'Imagen' o 'Animado' y contador '12/30' con barra verde. Botón flotante verde #1FAF5A 'Nueva carpeta'. Fondo #F4F7F5, estilo limpio y amigable."
- **Carpeta:** "Pantalla de carpeta de stickers. Encabezado con ícono, nombre y contador 12/30. Cuadrícula de 3 columnas de stickers sobre fondo de tablero gris (transparencia); algunos con un pequeño ícono de altavoz. Botón grande verde fijo abajo 'Agregar a WhatsApp'. Botón flotante 'Nuevo sticker'."
- **Editor de foto:** "Editor de sticker móvil. Lienzo central con imagen recortada sobre tablero de transparencia. Barra superior con 'Cancelar' y 'Guardar'. Abajo chips 'Rectángulo' y 'Círculo' y una barra de herramientas con íconos: Recortar, Quitar fondo, Texto, Dibujar, Deshacer. Acentos verde #1FAF5A."
- **Editor de video (tramo):** "Pantalla para elegir tramo de video: vista previa arriba, tira de miniaturas con dos manijas verdes y texto '3.2 s / 5 s', botón 'Siguiente'."
- **Detalle de sticker:** "Detalle de sticker animado: sticker grande sobre tablero, botón 'Escuchar audio' con ícono de play, botón 'Guardar video con sonido', enlace '¿Cómo crear el sticker con sonido?', acciones 'Mover' y 'Eliminar'."
