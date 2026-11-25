package com.stickers.creador.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CarpetaDao {
    @Query(
        """
        SELECT c.*,
            (SELECT COUNT(*) FROM stickers s WHERE s.carpetaId = c.id) AS cantidad,
            (SELECT s.webpRuta FROM stickers s WHERE s.carpetaId = c.id ORDER BY s.orden, s.id LIMIT 1) AS primerSticker
        FROM carpetas c ORDER BY c.creadoEn DESC
        """,
    )
    fun observarResumenes(): Flow<List<CarpetaResumen>>

    @Query("SELECT * FROM carpetas WHERE id = :id")
    fun observar(id: Long): Flow<Carpeta?>

    @Query("SELECT * FROM carpetas WHERE id = :id")
    suspend fun obtener(id: Long): Carpeta?

    @Query("SELECT * FROM carpetas ORDER BY creadoEn DESC")
    suspend fun todas(): List<Carpeta>

    // Consultas síncronas para el ContentProvider (se llaman en hilos de Binder).
    @Query("SELECT * FROM carpetas ORDER BY creadoEn DESC")
    fun todasSync(): List<Carpeta>

    @Query("SELECT * FROM carpetas WHERE identificador = :identificador LIMIT 1")
    fun porIdentificadorSync(identificador: String): Carpeta?

    @Insert
    suspend fun insertar(carpeta: Carpeta): Long

    @Update
    suspend fun actualizar(carpeta: Carpeta)

    @Delete
    suspend fun eliminar(carpeta: Carpeta)
}

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers WHERE carpetaId = :carpetaId ORDER BY orden, id")
    fun observarDeCarpeta(carpetaId: Long): Flow<List<Sticker>>

    @Query("SELECT * FROM stickers WHERE carpetaId = :carpetaId ORDER BY orden, id")
    suspend fun deCarpeta(carpetaId: Long): List<Sticker>

    @Query("SELECT * FROM stickers WHERE carpetaId = :carpetaId ORDER BY orden, id")
    fun deCarpetaSync(carpetaId: Long): List<Sticker>

    @Query("SELECT * FROM stickers WHERE id = :id")
    fun observar(id: Long): Flow<Sticker?>

    @Query("SELECT * FROM stickers WHERE id = :id")
    suspend fun obtener(id: Long): Sticker?

    @Query("SELECT COUNT(*) FROM stickers WHERE carpetaId = :carpetaId")
    suspend fun contar(carpetaId: Long): Int

    @Query("SELECT COALESCE(MAX(orden), -1) FROM stickers WHERE carpetaId = :carpetaId")
    suspend fun maxOrden(carpetaId: Long): Int

    @Insert
    suspend fun insertar(sticker: Sticker): Long

    @Update
    suspend fun actualizar(sticker: Sticker)

    @Delete
    suspend fun eliminar(sticker: Sticker)
}
