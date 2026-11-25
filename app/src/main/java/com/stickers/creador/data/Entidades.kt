package com.stickers.creador.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stickers.creador.domain.TipoCarpeta

@Entity(
    tableName = "carpetas",
    indices = [Index(value = ["identificador"], unique = true)],
)
data class Carpeta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ID del paquete para WhatsApp. */
    val identificador: String,
    val nombre: String,
    val tipo: TipoCarpeta,
    /** null = usar el primer sticker como ícono (R6). */
    val iconoRuta: String?,
    /** Se incrementa con cada cambio (R26). */
    val version: Int = 1,
    val creadoEn: Long,
)

@Entity(
    tableName = "stickers",
    foreignKeys = [
        ForeignKey(
            entity = Carpeta::class,
            parentColumns = ["id"],
            childColumns = ["carpetaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("carpetaId")],
)
data class Sticker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carpetaId: Long,
    /** WebP 512x512. */
    val webpRuta: String,
    /** Solo animados (.m4a, <= 5 s). */
    val audioRuta: String? = null,
    /** Solo animados (.mp4 con sonido). */
    val videoCortoRuta: String? = null,
    /** WhatsApp exige de 1 a 3 emojis por sticker (separados por coma). */
    val emojis: String,
    val orden: Int,
    val creadoEn: Long,
)

/** Carpeta con su cantidad de stickers y el primero (para el ícono por defecto). */
data class CarpetaResumen(
    @Embedded val carpeta: Carpeta,
    val cantidad: Int,
    val primerSticker: String?,
)
