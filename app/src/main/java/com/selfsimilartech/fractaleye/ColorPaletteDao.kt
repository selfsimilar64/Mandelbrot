
package com.selfsimilartech.fractaleye

import androidx.room.*

@Dao
interface ColorPaletteDao {

    @Query("SELECT * FROM palette")
    suspend fun getAll(): List<ColorPaletteEntity>

    @Query("SELECT * FROM palette WHERE name IS :name")
    suspend fun findByName(name: String): ColorPaletteEntity

    @Query("SELECT * FROM palette WHERE id IS :id")
    suspend fun findById(id: Int): ColorPaletteEntity

    @Insert
    suspend fun insertAll(vararg palettes: ColorPaletteEntity)

    @Insert
    suspend fun insert(palette: ColorPaletteEntity) : Long

    @Update
    suspend fun update(palette: ColorPaletteEntity) : Int

    @Query("UPDATE palette SET starred=:newStarred WHERE id IS :id")
    suspend fun updateIsFavorite(id: Int, newStarred: Boolean)

    @Delete
    suspend fun delete(palette: ColorPaletteEntity)

}