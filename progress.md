# progress.md — Seguimiento

## Estado actual
- **Fase:** 6 — Cierre (código del MVP completo)
- **Última tarea completada:** T6.3 (APK firmado)
- **Siguiente tarea:** T0.5 / T0.6 / T0.7 / T6.1 / T6.2 en un celular con WhatsApp
- **Bloqueos:** las pruebas con WhatsApp real necesitan un teléfono (el emulador no tiene WhatsApp)

## Checkpoints
| # | Checkpoint | Criterio | Estado |
|---|---|---|---|
| C1 | WhatsApp acepta paquetes | Prueba A: paquete de prueba agregado | 🟡 Validador propio OK; falta probar con WhatsApp real |
| C2 | Video → animado funciona | Prueba B: WebP animado aceptado por WhatsApp | 🟡 WebP animado 512×512, 352 KB, 44 cuadros de 83 ms, decodificado por Android y Pillow; falta WhatsApp real |
| C3 | Sonido viable | Prueba C: el video corto conserva el audio en el editor de WhatsApp | 🟡 MP4 512×512 generado y guardado en galería; falta probar con un video con audio y WhatsApp |
| C4 | Carpetas funcionan | Crear, editar, borrar y persistir | ✅ |
| C5 | Stickers de foto | Recorte, quitar fondo, texto y dibujo | ✅ (quitar fondo: el modelo de ML Kit se estaba descargando en el emulador; se mostró el aviso correcto) |
| C6 | Stickers de video | Tramo, recorte, animado, audio y video corto | ✅ (audio solo probado con video sin audio) |
| C7 | MVP terminado | Dos carpetas enviadas a WhatsApp y APK instalado | 🟡 APK firmado listo; falta enviarlas desde el celular |

> Si C2 falla, no avanzar a la Fase 4: decidir otra librería o reducir alcance antes de seguir.

## Límites oficiales de WhatsApp (llenar en T0.2)
- Tamaño sticker estático: WebP 512×512, ≤ 100 KB, fondo transparente
- Tamaño sticker animado: WebP 512×512, ≤ 500 KB, más de 1 cuadro; el primer cuadro debe ser la imagen completa
- Duración máx. animado: ≤ 10 s; cada cuadro ≥ 8 ms
- Ícono de portada: PNG 96×96 (el validador acepta 24–512 px), ≤ 50 KB
- Emojis por sticker: 1 a 3 (obligatorio)
- Mín./máx. por paquete: 3 / 30 (de 1 a 10 paquetes por app)
- ¿Mezclar tipos?: No. Paquete todo estático o todo animado (`animated_sticker_pack`)
- ¿Soporta audio para apps externas?: No. El README, el validador y el ContentProvider oficiales no mencionan audio
- Nombre y autor: ≤ 128 caracteres. Identificador: ≤ 128, solo `a-z A-Z 0-9 _ - . espacio`, sin ".."
- Fuente: README y código de `WhatsApp/stickers` (rama main), leído el 2026-11-24

## Reglas para la IA (pegar al inicio de cada sesión)
1. Lee `spec.md`, `plan.md`, `tasks.md` y este archivo antes de escribir código.
2. Una tarea a la vez, en el orden de `tasks.md`.
3. Para el envío a WhatsApp, parte del sample oficial `WhatsApp/stickers`; no inventes el formato.
4. Usa Photo Picker, no permisos de almacenamiento.
5. Procesamiento de imagen y video siempre fuera del hilo principal.
6. No agregues internet, anuncios ni analítica.
7. Al terminar: marca `[x]` en `tasks.md`, actualiza "Estado actual" y agrega una entrada al registro.
8. Si algo de `spec.md` no está claro, pregunta antes de inventar.

## Registro de sesiones
<!-- La más reciente arriba -->

### 2026-11-24 — Sesión 1
- **Tareas:** T0.1–T0.4, T1.1–T1.5, T2.1–T2.4, T3.1–T3.6, T4.1–T4.6, T5.1–T5.5, T6.3.
- **Hecho:** proyecto Android completo (Kotlin, Compose, Room, Navigation, Media3, ML Kit). 6 pantallas de `plan.md` §5. ContentProvider con las columnas exactas del sample oficial. Validador previo al envío. 20 tests JUnit (reglas, contenedor WebP, validador): todos pasan. Lint: 0 errores. APK de debug y release firmado. Recorrido completo probado en emulador (Android 17): crear carpeta, 3 stickers de foto (rectángulo, círculo, texto, dibujo), vista previa, envío (validación OK; el emulador no tiene WhatsApp), carpeta animada, tramo de 5 s máx., WebP animado, MP4, guardar en galería, regla de mover.
- **Problemas / decisiones:**
  - T0.6: no se usa ninguna librería nativa. Se escribió un empaquetador WebP animado en Kotlin (ver `plan.md` §1 y §6).
  - Error corregido: el recorte en círculo no borraba las esquinas (se usaba `DST_IN` solo dentro del círculo). Ahora se borra el exterior con un `Path` de relleno inverso.
  - Error corregido (lint): `AnimatedImageDrawable` se usaba sin comprobar la versión y fallaría en Android 8. Ahora se usa la interfaz `Animatable`.
  - El MP4 corto no tiene transparencia: en modo círculo, fuera del círculo queda blanco.
  - Firma: `stickers-release.jks` + `keystore.properties` (están en `.gitignore`). **Haz copia de seguridad**: sin ese archivo no podrás actualizar la app instalada.
  - T0.8 (Stitch) no se hizo: la UI se programó directo desde `design.md`.
- **Siguiente:** instalar `app-release.apk` en el celular y hacer T0.5, T0.6, T0.7, T6.1 y T6.2 con WhatsApp real. Probar también "Quitar fondo" con el modelo ya descargado y un video con audio.

### AAAA-MM-DD — Sesión N
- **Tareas:**
- **Hecho:**
- **Problemas / decisiones:**
- **Siguiente:**
