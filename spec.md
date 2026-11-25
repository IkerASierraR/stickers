# spec.md — Creador de Stickers para WhatsApp (MVP)

## 1. Visión
App Android personal, gratis y local para crear stickers de WhatsApp a partir de fotos y videos de la galería: recortarlos, decorarlos con texto y dibujos, quitarles el fondo, guardar el audio de los videos y organizarlos en carpetas que se envían a WhatsApp como paquetes.

## 2. Problema
Crear stickers propios es lento: hay que recortar, ajustar tamaño y formato y armar paquetes con otras apps llenas de anuncios. Quiero hacerlo rápido, a mi manera y conservando el audio de los videos ahora que WhatsApp tiene stickers con sonido.

## 3. Usuario
- **Único usuario:** yo. Sin cuentas, login ni roles.
- Beneficio principal: ahorrar tiempo.

## 4. Conceptos
| Concepto | Descripción |
|---|---|
| Carpeta | Paquete de stickers: nombre, ícono de portada, tipo (**imagen** o **animado**) y de 3 a 30 stickers para poder enviarse. |
| Sticker de imagen | Creado desde una foto. Estático. |
| Sticker animado | Creado desde un tramo de video de hasta 5 s. Guarda también el audio de ese tramo. |
| Audio del sticker | Clip de audio del mismo tramo del video (hasta 5 s), guardado junto al sticker. |
| Ícono de portada | Imagen pequeña que WhatsApp muestra en la bandeja de stickers. |

## 5. Reglas de negocio
### 5.1 Carpetas
- **R1.** Una carpeta tiene nombre (obligatorio), ícono y tipo. El tipo se elige al crearla y no cambia.
- **R2.** WhatsApp no permite mezclar stickers estáticos y animados en un mismo paquete; por eso cada carpeta es de un solo tipo.
- **R3.** Máximo 30 stickers por carpeta. Al llegar a 30 no se pueden agregar más.
- **R4.** "Agregar a WhatsApp" solo se habilita con 3 o más stickers.
- **R5.** Cada sticker pertenece a una sola carpeta. Se puede mover a otra carpeta del mismo tipo que tenga espacio.
- **R6.** Si no elijo ícono, se usa el primer sticker de la carpeta como ícono.
- **R7.** Eliminar una carpeta pide confirmación y borra sus stickers.

### 5.2 Creación desde foto
- **R8.** Elijo una foto de la galería.
- **R9.** Recorte en **rectángulo** o **círculo**, simple, **sin borde blanco**.
- **R10.** Opción **Quitar fondo** automático (en el celular, sin enviar la foto a ningún servidor).
- **R11.** Puedo agregar **texto** (escribir, elegir color y tamaño, moverlo) y **dibujar a mano** (color, grosor, deshacer).
- **R12.** Vista previa antes de guardar.

### 5.3 Creación desde video
- **R13.** Elijo un video de la galería.
- **R14.** Elijo **inicio y fin** del tramo, máximo **5 segundos** (coincide con el límite de audio de WhatsApp para stickers animados).
- **R15.** Recorte en **rectángulo** o **círculo**, sin borde blanco.
- **R16.** Puedo agregar texto y dibujo encima (se ven durante todo el sticker).
- **R17.** Quitar fondo **no** aplica a video en el MVP (muy pesado para el celular).
- **R18.** Al guardar, la app genera automáticamente:
  1. El sticker animado (sin sonido, formato que acepta WhatsApp).
  2. El **audio del tramo**, guardado junto al sticker.
  3. Un **video corto con sonido** (mismo tramo y recorte) para crear el sticker con sonido dentro de WhatsApp.
- **R19.** En el detalle del sticker puedo escuchar su audio y guardar el video corto en la galería.

### 5.4 Sonido en WhatsApp
- **R20.** Hoy la herramienta oficial para que apps externas envíen paquetes solo documenta stickers estáticos y animados, sin audio. Por eso los paquetes se envían sin sonido.
- **R21.** Para un sticker con sonido, la app deja listo el video corto (R18.3) y el usuario lo convierte en sticker desde el editor de WhatsApp, que conserva el audio del video.
- **R22.** El audio queda guardado por sticker para activarlo directamente en el paquete si WhatsApp lo habilita para apps externas.

### 5.5 Formato exigido por WhatsApp (verificar en el repositorio oficial `WhatsApp/stickers` antes de programar)
- **R23.** Sticker: WebP de 512×512 px. Estático ≤ 100 KB; animado ≤ 500 KB.
- **R24.** Ícono de portada: PNG de 96×96 px, liviano (≤ 50 KB).
- **R25.** Si un sticker supera el tamaño, la app reduce calidad o cuadros por segundo automáticamente hasta cumplir, y avisa si no lo logra.
- **R26.** Al editar una carpeta ya enviada, la app actualiza la versión del paquete para que WhatsApp tome los cambios.

### 5.6 Datos
- **R27.** Todo se guarda solo en el celular.
- **R28.** No se guarda la foto o video original completo, solo el resultado (sticker, audio y video corto).

## 6. Alcance MVP
### Entra
- Carpetas (crear, editar, eliminar) con nombre, ícono y tipo.
- Stickers desde foto: recorte rectángulo/círculo, quitar fondo, texto, dibujo.
- Stickers desde video: tramo de hasta 5 s, recorte, texto, dibujo, audio guardado y video corto con sonido.
- Botón oficial "Agregar a WhatsApp".
- Mover y eliminar stickers.

### No entra
- Cámara (solo galería).
- Quitar fondo en video.
- Audio desde grabación de voz o archivo externo.
- Telegram u otras apps.
- Publicar en Play Store.
- Nube, cuentas, respaldo.

## 7. Criterios de éxito
- Crear un sticker desde una foto en menos de 1 minuto.
- Convertir un video en sticker animado con su audio guardado en menos de 1 minuto.
- Un paquete de 3 a 30 stickers se agrega a WhatsApp sin errores.
