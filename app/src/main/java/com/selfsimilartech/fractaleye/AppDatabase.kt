package com.selfsimilartech.fractaleye

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ColorPaletteEntity::class, ShapeEntity::class, FractalEntity::class], version = 11)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorPaletteDao() : ColorPaletteDao
    abstract fun shapeDao() : ShapeDao
    abstract fun fractalDao() : FractalDao
}