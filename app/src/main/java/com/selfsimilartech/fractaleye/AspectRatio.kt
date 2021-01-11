package com.selfsimilartech.fractaleye

import android.graphics.Point

class AspectRatio {

    companion object {

        val RATIO_1_1   = AspectRatio( 1.0,  1.0                        )
        val RATIO_4_5   = AspectRatio( 4.0,  5.0,   goldFeature = true  )
        val RATIO_5_7   = AspectRatio( 5.0,  7.0,   goldFeature = true  )
        val RATIO_2_3   = AspectRatio( 2.0,  3.0,   goldFeature = true  )
        val RATIO_16_9  = AspectRatio( 16.0, 9.0,   goldFeature = true  )
        val RATIO_9_16  = AspectRatio( 9.0,  16.0,  goldFeature = true  )
        val RATIO_2_1   = AspectRatio( 2.0,  1.0,   goldFeature = true  )
        val RATIO_1_2   = AspectRatio( 1.0,  2.0,   goldFeature = true  )
        val RATIO_1_3   = AspectRatio( 1.0,  3.0,   goldFeature = true  )

        val RATIO_SCREEN = AspectRatio(1.0, 1.0)

        val all = listOf(RATIO_SCREEN, RATIO_1_1, RATIO_4_5, RATIO_5_7, RATIO_2_3, RATIO_9_16, RATIO_1_2)

        fun initialize() {
            RATIO_SCREEN.w = Resolution.SCREEN.w.toDouble()
            RATIO_SCREEN.h = Resolution.SCREEN.h.toDouble()
            RATIO_SCREEN.r = RATIO_SCREEN.h/RATIO_SCREEN.w
        }

    }

    constructor(w: Double, h: Double, goldFeature: Boolean = false) {
        this.w = w
        this.h = h
        r = h/w
        this.goldFeature = goldFeature
    }

    constructor(r: Double, goldFeature: Boolean = false) {
        w = 1.0
        h = r
        this. r = r
        this.goldFeature = goldFeature
    }

    var w: Double
    var h: Double
    var r: Double
    val goldFeature : Boolean

    fun getDimensions(res: Resolution) : Point {
        var width = res.w
        var height = res.h
        if (r > RATIO_SCREEN.r) width = (height / r).toInt()
        if (r < RATIO_SCREEN.r) height = (width * r).toInt()
        return Point(width, height)
    }

}