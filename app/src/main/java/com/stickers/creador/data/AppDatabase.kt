package com.stickers.creador.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Carpeta::class, Sticker::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carpetaDao(): CarpetaDao
    abstract fun stickerDao(): StickerDao

    companion object {
        fun crear(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "stickers.db").build()
    }
}
