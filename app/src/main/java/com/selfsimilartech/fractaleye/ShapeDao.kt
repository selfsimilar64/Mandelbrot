
package com.selfsimilartech.fractaleye

import androidx.room.*

@Dao
interface ShapeDao {

    @Query("SELECT * FROM shape")
    suspend fun getAll(): List<ShapeEntity>

    @Query("SELECT * FROM shape WHERE name IS :name")
    suspend fun findByName(name: String): ShapeEntity

    @Query("SELECT * FROM shape WHERE id IS :id")
    suspend fun findById(id: Int): ShapeEntity

    @Insert
    suspend fun insertAll(vararg shapes: ShapeEntity)

    @Insert
    suspend fun insert(shape: ShapeEntity) : Long

    @Update
    suspend fun update(shape: ShapeEntity) : Int

    @Delete
    suspend fun delete(shape: ShapeEntity)

    @Query("UPDATE shape SET name=:newName, latex=:newLatex, loopSF=:newLoopSF, loopDF=:newLoopDF WHERE id IS :id")
    suspend fun update(id: Int, newName: String, newLatex: String, newLoopSF: String, newLoopDF: String)

    @Query("UPDATE shape SET isFavorite=:newIsFavorite WHERE id IS :id")
    suspend fun updateIsFavorite(id: Int, newIsFavorite: Boolean)

}