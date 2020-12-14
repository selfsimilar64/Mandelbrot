package com.selfsimilartech.fractaleye

import androidx.annotation.ColorInt
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "palette")
data class ColorPaletteEntity (
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val name: String = "",
        val size: Int = 0,
        @ColorInt val c1: Int = 0,
        @ColorInt val c2: Int = 0,
        @ColorInt val c3: Int = 0,
        @ColorInt val c4: Int = 0,
        @ColorInt val c5: Int = 0,
        @ColorInt val c6: Int = 0,
        @ColorInt val c7: Int = 0,
        @ColorInt val c8: Int = 0,
        @ColorInt val c9: Int = 0,
        @ColorInt val c10: Int = 0,
        @ColorInt val c11: Int = 0,
        @ColorInt val c12: Int = 0,
        val starred: Boolean = false
)