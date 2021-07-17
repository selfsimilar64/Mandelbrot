package com.selfsimilartech.fractaleye

import android.graphics.Point
import android.util.Log
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

fun Double.roundEven() : Int {
    val f = floor(this).toInt()
    return if (f % 2 == 0) f else f + 1
}

class Resolution(width: Int, private val builders: List<Resolution>? = null, var videoCompanions: Pair<Resolution, Resolution>? = null) {

    constructor(width: Int, height: Int) : this(width, null) {
        h = height
        n = width*height
    }


    val w = width
    var h = 0
    var n = 0

    fun getBuilder() = builders?.last { it.w <= SCREEN.w } ?: this


    companion object {

        val R4     = Resolution(4)
        val R1187  = Resolution(1187)
        val R1781  = Resolution(1781)
        val R2374  = Resolution(2374)

        val R45    = Resolution(45)
        val R60    = Resolution(60)
        val R90    = Resolution(90)
        val R120   = Resolution(120)
        val R180   = Resolution(180)
        val R240   = Resolution(240)
        val R360   = Resolution(360)
        val R480   = Resolution(480)
        val R720   = Resolution(720, videoCompanions = Pair(R1187, R4))
        val R1080  = Resolution(1080, listOf(R360), videoCompanions = Pair(R1781, R4))
        val R1440  = Resolution(1440, listOf(R720), videoCompanions = Pair(R2374, R4))
        val R2160  = Resolution(2160, listOf(R720))
        val R2880  = Resolution(2880, listOf(R720))

        val R3600  = Resolution( 3600, listOf(R720)         )
        val R4320  = Resolution( 4320, listOf(R720, R1440)  )
        val R5040  = Resolution( 5040, listOf(R720)         )
        val R5760  = Resolution( 5760, listOf(R720, R1440)  )

        val all = ArrayList(arrayListOf(
                R4, R45, R60, R90, R120, R180, R240, R360, R480, R720, R1080, R1440, R2160, R2880,
                // R3600, R4320, R5040, R5760,
                R1187, R1781, R2374
        ).filter { BuildConfig.DEV_VERSION || it.w <= 2880 })
        val working = ArrayList(all.minus(listOf(R4, R45, R60, R90, R1187, R1781, R2374)))
        val continuous = arrayListOf<Resolution>()

        fun valueOf(width: Int) : Resolution? {
            return working.first { it.w == width }
        }

        fun initialize(screenRatio: Double) {

            all.forEach { res ->
                res.h = when (res) {
                    R1080 -> (R360.h*3.0).toInt()
                    R1440 -> (R720.h*2.0).toInt()
                    R2160 -> (R720.h*3.0).toInt()
                    R2880 -> (R720.h*4.0).toInt()
                    R3600 -> (R720.h*5.0).toInt()
                    R4320 -> (R720.h*6.0).toInt()
                    R5040 -> (R720.h*7.0).toInt()
                    R5760 -> (R720.h*8.0).toInt()
                    else -> (res.w.toDouble()*screenRatio).roundEven()
                }
            }

            all.forEach { res -> res.n = res.w*res.h }

            continuous.addAll(all.subList(1, all.indexOf(SCREEN) - 1))

            NUM_VALUES_GT_SCREEN_DIMS = all.count { it.w > SCREEN.w }
            NUM_VALUES_FREE = working.indexOf(R1080) + 1


            R1187.videoCompanions = Pair(R720, R4)
            R1781.videoCompanions = Pair(R1080, R4)
            R2374.videoCompanions = Pair(R1440, R4)


        }

        fun addResolution(width: Int) {
            val newRes = Resolution(width)
            all.add(all.indexOfFirst { newRes.w < it.w }, newRes)
            working.add(working.indexOfFirst { newRes.w < it.w }, newRes)
        }


        val BG = R180
        val THUMB = R180
        var SCREEN = R1080
        val MAX_FREE = R1080
        var NUM_VALUES_GT_SCREEN_DIMS = 0
        val NUM_VALUES_WORKING = { working.size }
        var NUM_VALUES_FREE = 0

    }
}