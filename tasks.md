# tasks.md — Tareas priorizadas

Orden pensado para programar con IA: primero se prueban las partes riesgosas, después se construye. Marca `[x]` al terminar y actualiza `progress.md`.

## Fase 0 — Preparación y pruebas técnicas
- [x] T0.1 Crear proyecto Android Studio "Empty Activity (Compose)", Kotlin, min SDK 26.
- [x] T0.2 Leer el README del repositorio oficial `WhatsApp/stickers` y anotar en `progress.md` los límites vigentes (tamaños, emojis, ícono, mínimo/máximo, regla de no mezclar tipos).
- [x] T0.3 Agregar dependencias: Room, Navigation Compose, Material 3, Media3 Transformer, ML Kit Subject Segmentation.
- [x] T0.4 Estructura de paquetes `ui/`, `editor/`, `media/`, `domain/`, `data/`.
- [ ] T0.5 **Prueba A:** importar el sample oficial y enviar a WhatsApp un paquete de 3 stickers de prueba. _(requiere celular con WhatsApp: el ContentProvider ya está adaptado del sample; probar en el teléfono)_
- [ ] T0.6 **Prueba B (crítica):** convertir 3 s de un video en WebP animado 512×512 ≤ 500 KB que WhatsApp acepte. Elegir librería y anotarla en `plan.md`. _(código hecho: empaquetador WebP animado propio en Kotlin, ver plan.md; falta confirmar en WhatsApp real)_
- [ ] T0.7 **Prueba C:** generar un MP4 corto con sonido, abrirlo desde el creador de stickers de WhatsApp y confirmar que conserva el audio. _(requiere celular con WhatsApp)_
- [ ] T0.8 Diseñar pantallas en Stitch con `design.md`. _(tarea manual en Google Stitch; la UI se programó directamente desde design.md)_

## Fase 1 — Datos y reglas
- [x] T1.1 Entidades `Carpeta` y `Sticker`, DAOs con `Flow`, `AppDatabase`.
- [x] T1.2 `AppContainer` y repositorios.
- [x] T1.3 Reglas de dominio: máx. 30, mín. 3 para enviar, tipo fijo, mover solo a carpeta del mismo tipo con espacio.
- [x] T1.4 Tests JUnit de las reglas:
  - [x] Carpeta con 30 no acepta más.
  - [x] Carpeta con 2 no habilita "Agregar a WhatsApp".
  - [x] No se puede mover un sticker animado a carpeta de imagen.
  - [x] Editar carpeta incrementa `version`.
- [x] T1.5 Gestor de archivos internos (guardar/borrar por carpeta).

## Fase 2 — Pantallas base
- [x] T2.1 Tema verde (ver `design.md`), claro/oscuro.
- [x] T2.2 Inicio: lista de carpetas, estado vacío, FAB "Nueva carpeta".
- [x] T2.3 Formulario de carpeta: nombre, tipo, ícono.
- [x] T2.4 Carpeta: cuadrícula, contador "N/30", botón "Agregar a WhatsApp", FAB "Nuevo sticker".

## Fase 3 — Editor de foto
- [x] T3.1 Photo Picker de imágenes.
- [x] T3.2 Recorte rectángulo/círculo sin borde.
- [x] T3.3 Quitar fondo con ML Kit (botón, progreso, deshacer).
- [x] T3.4 Capa de texto (color, tamaño, mover).
- [x] T3.5 Capa de dibujo (color, grosor, deshacer).
- [x] T3.6 Exportar WebP 512×512 ≤ 100 KB y guardar.

## Fase 4 — Editor de video
- [x] T4.1 Photo Picker de videos + selector de tramo (≤ 5 s) con vista previa.
- [x] T4.2 Recorte rectángulo/círculo, texto y dibujo sobre el video.
- [x] T4.3 Generar WebP animado (librería de T0.6) con ajuste automático a ≤ 500 KB.
- [x] T4.4 Extraer audio del tramo (`.m4a`).
- [x] T4.5 Generar MP4 corto con sonido.
- [x] T4.6 Procesamiento en segundo plano con progreso y manejo de errores.

## Fase 5 — WhatsApp y detalle
- [x] T5.1 ContentProvider adaptado del sample oficial.
- [x] T5.2 Validador previo al envío con mensajes claros.
- [x] T5.3 Generar ícono 96×96 (elegido o primer sticker).
- [x] T5.4 Detalle de sticker: reproducir audio, "Guardar video con sonido", mover, eliminar.
- [x] T5.5 Guía de 3 pasos para crear el sticker con sonido en WhatsApp.

## Fase 6 — Cierre
- [ ] T6.1 Crear 1 carpeta de imagen y 1 animada con 3+ stickers y enviarlas a WhatsApp. _(requiere celular con WhatsApp)_
- [ ] T6.2 Probar actualizar una carpeta ya enviada. _(requiere celular con WhatsApp)_
- [x] T6.3 Generar APK firmado e instalar. _(APK firmado en `app/build/outputs/apk/release/app-release.apk`, probado en emulador; falta instalarlo en tu celular)_
