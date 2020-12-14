package com.selfsimilartech.fractaleye

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ColorPaletteEntity::class, ShapeEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorPaletteDao() : ColorPaletteDao
    abstract fun shapeDao() : ShapeDao
}