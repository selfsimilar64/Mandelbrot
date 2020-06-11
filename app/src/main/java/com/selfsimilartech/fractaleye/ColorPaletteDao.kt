
package com.selfsimilartech.fractaleye

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ColorPaletteDao {

    @Query("SELECT * FROM palette")
    fun getAll(): List<ColorPaletteEntity>

    @Query("SELECT * FROM palette WHERE name IS :name")
    fun findByName(name: String): ColorPaletteEntity

    @Query("SELECT * FROM palette WHERE id IS :id")
    fun findById(id: Int): ColorPaletteEntity

    @Insert
    fun insertAll(vararg palettes: ColorPaletteEntity)

    @Insert
    fun insert(palette: ColorPaletteEntity) : Long

    @Delete
    fun delete(palette: ColorPaletteEntity)

}