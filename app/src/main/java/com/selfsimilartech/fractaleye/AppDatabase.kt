package com.selfsimilartech.fractaleye

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ColorPaletteEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorPaletteDao(): ColorPaletteDao
}