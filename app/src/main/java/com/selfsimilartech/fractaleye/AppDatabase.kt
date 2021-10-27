package com.selfsimilartech.fractaleye

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

const val DATABASE_NAME = "custom"

@Database(

    entities = [ColorPaletteEntity::class, ShapeEntity::class, FractalEntity::class],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 11, to = 12)
                     ],
    version = 12

)
abstract class AppDatabase : RoomDatabase() {

    companion object {

        private var instance : AppDatabase? = null

        fun getInstance(ctx: Context) : AppDatabase {

            if (instance == null) {

                val migrate2to3 = object : Migration(2, 3) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE palette ADD COLUMN starred INTEGER DEFAULT 0 not null")
                    }
                }
                val migrate3to4 = object : Migration(3, 4) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE palette ADD COLUMN c9 INTEGER DEFAULT 0 not null")
                        database.execSQL("ALTER TABLE palette ADD COLUMN c10 INTEGER DEFAULT 0 not null")
                        database.execSQL("ALTER TABLE palette ADD COLUMN c11 INTEGER DEFAULT 0 not null")
                        database.execSQL("ALTER TABLE palette ADD COLUMN c12 INTEGER DEFAULT 0 not null")
                    }
                }
                val migrate4to5 = object : Migration(4, 5) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL(
                            """
                CREATE TABLE fractal (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                isFavorite INTEGER NOT NULL,
                thumbnailPath TEXT NOT NULL,
                shapeId INTEGER NOT NULL,
                customShape INTEGER NOT NULL,
                juliaMode INTEGER NOT NULL,
                maxIter INTEGER NOT NULL,
                p1_id INTEGER,
                p1_u REAL,
                p1_v REAL,
                p1_isComplex INTEGER,
                p2_id INTEGER,
                p2_u REAL,
                p2_v REAL,
                p2_isComplex INTEGER,
                p3_id INTEGER,
                p3_u REAL,
                p3_v REAL,
                p3_isComplex INTEGER,
                p4_id INTEGER,
                p4_u REAL,
                p4_v REAL,
                p4_isComplex INTEGER,
                julia_id INTEGER,
                julia_u REAL,
                julia_v REAL,
                julia_isComplex INTEGER,
                seed_id INTEGER,
                seed_u REAL,
                seed_v REAL,
                seed_isComplex INTEGER,
                pos_x REAL,
                pos_y REAL,
                pos_zoom REAL,
                pos_rotation REAL,
                textureId INTEGER NOT NULL,
                customTexture INTEGER NOT NULL,
                textureMode INTEGER NOT NULL,
                radius REAL NOT NULL,
                q1_id INTEGER,
                q1_u REAL,
                q1_v REAL,
                q1_isComplex INTEGER,
                q2_id INTEGER,
                q2_u REAL,
                q2_v REAL,
                q2_isComplex INTEGER,
                q3_id INTEGER,
                q3_u REAL,
                q3_v REAL,
                q3_isComplex INTEGER,
                q4_id INTEGER,
                q4_u REAL,
                q4_v REAL,
                q4_isComplex INTEGER,
                paletteId INTEGER NOT NULL,
                customPalette INTEGER NOT NULL,
                frequency REAL NOT NULL,
                phase REAL NOT NULL,
                solidFillColor INTEGER NOT NULL
                )
            """.trimIndent()
                        )
                    }
                }
                val migrate5to6 = object : Migration(5, 6) {
                    override fun migrate(database: SupportSQLiteDatabase) {

                        // create new table (remove parameter ids)
                        database.execSQL(
                            """
                CREATE TABLE fractal_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                isFavorite INTEGER NOT NULL,
                thumbnailPath TEXT NOT NULL,
                shapeId INTEGER NOT NULL,
                customShape INTEGER NOT NULL,
                juliaMode INTEGER NOT NULL,
                maxIter INTEGER NOT NULL,
                p1_u REAL,
                p1_v REAL,
                p1_isComplex INTEGER,
                p2_u REAL,
                p2_v REAL,
                p2_isComplex INTEGER,
                p3_u REAL,
                p3_v REAL,
                p3_isComplex INTEGER,
                p4_u REAL,
                p4_v REAL,
                p4_isComplex INTEGER,
                julia_u REAL,
                julia_v REAL,
                julia_isComplex INTEGER,
                seed_u REAL,
                seed_v REAL,
                seed_isComplex INTEGER,
                pos_x REAL,
                pos_y REAL,
                pos_zoom REAL,
                pos_rotation REAL,
                textureId INTEGER NOT NULL,
                customTexture INTEGER NOT NULL,
                textureMode INTEGER NOT NULL,
                radius REAL NOT NULL,
                q1_u REAL,
                q1_v REAL,
                q1_isComplex INTEGER,
                q2_u REAL,
                q2_v REAL,
                q2_isComplex INTEGER,
                q3_u REAL,
                q3_v REAL,
                q3_isComplex INTEGER,
                q4_u REAL,
                q4_v REAL,
                q4_isComplex INTEGER,
                paletteId INTEGER NOT NULL,
                customPalette INTEGER NOT NULL,
                frequency REAL NOT NULL,
                phase REAL NOT NULL,
                solidFillColor INTEGER NOT NULL
                )
            """.trimIndent()
                        )

                        // copy data into new table
                        database.execSQL(
                            """
                INSERT INTO fractal_new (
                id,
                name,
                isFavorite,
                thumbnailPath,
                shapeId,
                customShape,
                juliaMode,
                maxIter,
                p1_u,
                p1_v,
                p1_isComplex,
                p2_u,
                p2_v,
                p2_isComplex,
                p3_u,
                p3_v,
                p3_isComplex,
                p4_u,
                p4_v,
                p4_isComplex,
                julia_u,
                julia_v,
                julia_isComplex,
                seed_u,
                seed_v,
                seed_isComplex,
                pos_x,
                pos_y,
                pos_zoom,
                pos_rotation,
                textureId,
                customTexture,
                textureMode,
                radius,
                q1_u,
                q1_v,
                q1_isComplex,
                q2_u,
                q2_v,
                q2_isComplex,
                q3_u,
                q3_v,
                q3_isComplex,
                q4_u,
                q4_v,
                q4_isComplex,
                paletteId,
                customPalette,
                frequency,
                phase,
                solidFillColor
                ) SELECT id,
                name,
                isFavorite,
                thumbnailPath,
                shapeId,
                customShape,
                juliaMode,
                maxIter,
                p1_u,
                p1_v,
                p1_isComplex,
                p2_u,
                p2_v,
                p2_isComplex,
                p3_u,
                p3_v,
                p3_isComplex,
                p4_u,
                p4_v,
                p4_isComplex,
                julia_u,
                julia_v,
                julia_isComplex,
                seed_u,
                seed_v,
                seed_isComplex,
                pos_x,
                pos_y,
                pos_zoom,
                pos_rotation,
                textureId,
                customTexture,
                textureMode,
                radius,
                q1_u,
                q1_v,
                q1_isComplex,
                q2_u,
                q2_v,
                q2_isComplex,
                q3_u,
                q3_v,
                q3_isComplex,
                q4_u,
                q4_v,
                q4_isComplex,
                paletteId,
                customPalette,
                frequency,
                phase,
                solidFillColor FROM fractal
            """.trimIndent()
                        )

                        // remove old table
                        database.execSQL("DROP TABLE fractal")

                        // Change table name
                        database.execSQL("ALTER TABLE fractal_new RENAME TO fractal")

                    }
                }
                val migrate6to7 = object : Migration(6, 7) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE fractal ADD COLUMN accent2 INTEGER DEFAULT 0 NOT NULL")
                    }
                }
                val migrate7to8 = object : Migration(7, 8) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE fractal ADD COLUMN density REAL DEFAULT 0.0 NOT NULL")
                    }
                }
                val migrate8to9 = object : Migration(8, 9) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE fractal ADD COLUMN textureMin REAL DEFAULT 0.0 NOT NULL")
                        database.execSQL("ALTER TABLE fractal ADD COLUMN textureMax REAL DEFAULT 1.0 NOT NULL")
                    }
                }
                val migrate9to10 = object : Migration(9, 10) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE fractal ADD COLUMN imagePath TEXT DEFAULT '' NOT NULL")
                    }
                }
                val migrate10to11 = object : Migration(10, 11) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE fractal ADD COLUMN imageId INTEGER DEFAULT -1 NOT NULL")
                    }
                }

                instance = Room.databaseBuilder(
                    ctx,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigrationFrom(1).addMigrations(
                    migrate2to3,
                    migrate3to4,
                    migrate4to5,
                    migrate5to6,
                    migrate6to7,
                    migrate7to8,
                    migrate8to9,
                    migrate9to10,
                    migrate10to11
                ).build()

            }

            return instance!!

        }

    }

    abstract fun colorPaletteDao() : ColorPaletteDao
    abstract fun shapeDao() : ShapeDao
    abstract fun fractalDao() : FractalDao

}