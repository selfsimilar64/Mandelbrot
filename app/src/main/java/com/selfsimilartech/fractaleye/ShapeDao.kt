
package com.selfsimilartech.fractaleye

import androidx.room.*

@Dao
interface ShapeDao {

    @Query("SELECT * FROM shape")
    fun getAll(): List<ShapeEntity>

    @Query("SELECT * FROM shape WHERE name IS :name")
    fun findByName(name: String): ShapeEntity

    @Query("SELECT * FROM shape WHERE id IS :id")
    fun findById(id: Int): ShapeEntity

    @Insert
    fun insertAll(vararg shapes: ShapeEntity)

    @Insert
    fun insert(shape: ShapeEntity) : Long

    @Update
    fun update(shape: ShapeEntity) : Int

    @Delete
    fun delete(shape: ShapeEntity)

    @Query("UPDATE shape SET name=:newName, latex=:newLatex, loopSF=:newLoopSF, loopDF=:newLoopDF WHERE id IS :id")
    fun update(id: Int, newName: String, newLatex: String, newLoopSF: String, newLoopDF: String)

}