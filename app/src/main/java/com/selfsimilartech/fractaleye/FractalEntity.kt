package com.selfsimilartech.fractaleye

import android.graphics.Color
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fractal")
data class FractalEntity (

        @PrimaryKey(autoGenerate = true) val id: Int = 0,

        val name                : String        = "",
        val isFavorite          : Boolean       = false,
        val thumbnailPath       : String        = "",


        val shapeId             : Int           = -1,
        val customShape         : Boolean       = false,
        val juliaMode           : Boolean       = false,
        val maxIter             : Int           = 256,

        @Embedded(prefix = "p1_")       val p1          : RealParam.Data? = null,
        @Embedded(prefix = "p2_")       val p2          : RealParam.Data? = null,
        @Embedded(prefix = "p3_")       val p3          : RealParam.Data? = null,
        @Embedded(prefix = "p4_")       val p4          : RealParam.Data? = null,
        @Embedded(prefix = "julia_")    val julia       : RealParam.Data? = null,
        @Embedded(prefix = "seed_")     val seed        : RealParam.Data? = null,
        @Embedded(prefix = "pos_")      val position    : Position.Data?  = null,


        val textureId           : Int           = -1,
        val customTexture       : Boolean       = false,
        val textureMode         : Int           = TextureMode.OUT.ordinal,
        val radius              : Float         = 1e2f,
        val textureMin          : Float         = 0f,
        val textureMax          : Float         = 1f,
        val imagePath           : String        = "",
        val imageId             : Int           = -1,

        @Embedded(prefix = "q1_")       val q1          : RealParam.Data? = null,
        @Embedded(prefix = "q2_")       val q2          : RealParam.Data? = null,
        @Embedded(prefix = "q3_")       val q3          : RealParam.Data? = null,
        @Embedded(prefix = "q4_")       val q4          : RealParam.Data? = null,

        val paletteId           : Int           = -1,
        val customPalette       : Boolean       = false,
        val frequency           : Float         = 1f,
        val phase               : Float         = 0.65f,
        val density             : Float         = 0.0f,
        val solidFillColor      : Int           = Color.BLACK,
        val accent2             : Int           = Color.BLACK

)