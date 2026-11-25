# El ContentProvider lo instancia el sistema por nombre.
-keep class com.stickers.creador.media.whatsapp.StickerContentProvider { *; }
# ML Kit / Play Services
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
