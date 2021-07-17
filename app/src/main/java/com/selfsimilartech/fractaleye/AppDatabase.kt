package com.selfsimilartech.fractaleye

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

@Database(

    entities = [ColorPaletteEntity::class, ShapeEntity::class, FractalEntity::class],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 11, to = 12)
                     ],
    version = 12

)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorPaletteDao() : ColorPaletteDao
    abstract fun shapeDao() : ShapeDao
    abstract fun fractalDao() : FractalDao
}