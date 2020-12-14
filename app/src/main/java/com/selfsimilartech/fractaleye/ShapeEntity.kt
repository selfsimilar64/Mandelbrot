package com.selfsimilartech.fractaleye

import androidx.annotation.ColorInt
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shape")
data class ShapeEntity (
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val name            : String = "",
        val latex           : String = "",
        val loopSF          : String = "",
        val loopDF          : String = "",
        val conditional     : String = "",
        val xPosDefault     : Double = 0.0,
        val yPosDefault     : Double = 0.0,
        val zoomDefault     : Double = 1.0,
        val rotationDefault : Double = 0.0,
        val xPosJulia       : Double = 0.0,
        val yPosJulia       : Double = 0.0,
        val zoomJulia       : Double = 1.0,
        val rotationJulia   : Double = 0.0,
        val juliaMode       : Boolean = false,
        val juliaSeed       : Boolean = false,
        val xSeed           : Double = 0.0,
        val ySeed           : Double = 0.0,
        val maxIter         : Int = 256,
        val bailoutRadius   : Float = 1e6f,
        val isConvergent    : Boolean = false,
        val hasDualFloat    : Boolean = true,
        val isFavorite      : Boolean = false
)