
package com.selfsimilartech.fractaleye

import androidx.room.*

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

    @Update
    fun update(palette: ColorPaletteEntity) : Int

    @Delete
    fun delete(palette: ColorPaletteEntity)

}