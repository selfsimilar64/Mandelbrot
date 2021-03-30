
package com.selfsimilartech.fractaleye

import androidx.room.*

@Dao
interface FractalDao {

    @Query("SELECT * FROM fractal")
    fun getAll(): List<FractalEntity>

    @Query("SELECT * FROM fractal WHERE name IS :name")
    fun findByName(name: String): FractalEntity

    @Query("SELECT * FROM fractal WHERE id IS :id")
    fun findById(id: Int): FractalEntity

    @Insert
    fun insertAll(vararg fractals: FractalEntity)

    @Insert
    fun insert(fractal: FractalEntity) : Long

    @Update
    fun update(fractal: FractalEntity) : Int

    @Delete
    fun delete(fractal: FractalEntity)

    @Query("UPDATE fractal SET paletteId=:newPalette, customPalette=:newPaletteIsCustom WHERE id IS :id")
    fun update(id: Int, newPalette: Int, newPaletteIsCustom: Int)

    @Query("UPDATE fractal SET name=:newName WHERE id IS :id")
    fun update(id: Int, newName: String)

    @Query("UPDATE fractal SET imagePath=:newPath, imageId=:newId WHERE id IS :id")
    fun updateImage(id: Int, newPath: String, newId: Int)

}