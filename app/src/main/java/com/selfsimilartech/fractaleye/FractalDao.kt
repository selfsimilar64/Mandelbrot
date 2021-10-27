
package com.selfsimilartech.fractaleye

import androidx.room.*

@Dao
interface FractalDao {

    @Query("SELECT * FROM fractal")
    suspend fun getAll(): List<FractalEntity>

    @Query("SELECT * FROM fractal WHERE name IS :name")
    suspend fun findByName(name: String): FractalEntity

    @Query("SELECT * FROM fractal WHERE id IS :id")
    suspend fun findById(id: Int): FractalEntity

    @Insert
    suspend fun insertAll(vararg fractals: FractalEntity)

    @Insert
    suspend fun insert(fractal: FractalEntity) : Long

    @Update
    suspend fun update(fractal: FractalEntity) : Int

    @Delete
    suspend fun delete(fractal: FractalEntity)

    @Query("UPDATE fractal SET paletteId=:newPalette, customPalette=:newPaletteIsCustom WHERE id IS :id")
    suspend fun update(id: Int, newPalette: Int, newPaletteIsCustom: Int)

    @Query("UPDATE fractal SET name=:newName WHERE id IS :id")
    suspend fun updateName(id: Int, newName: String)

    @Query("UPDATE fractal SET isFavorite=:newIsFavorite WHERE id IS :id")
    suspend fun updateIsFavorite(id: Int, newIsFavorite: Boolean)

    @Query("UPDATE fractal SET imagePath=:newPath, imageId=:newId WHERE id IS :id")
    suspend fun updateImage(id: Int, newPath: String, newId: Int)

}