package com.selfsimilartech.fractaleye

import kotlin.math.floor

fun Double.roundEven() : Int {
    val f = floor(this).toInt()
    return if (f % 2 == 0) f else f + 1
}

class Resolution(
    width: Int,
    private val builders: List<Resolution>? = null,
    var videoCompanion: Resolution? = null,
    var bitrateCoef : Double = 1.0
) {

    constructor(width: Int, height: Int) : this(width, null) {
        h = height
        n = width*height
    }


    val w = width
    var h = 0
    var n = 0

    val goldFeature = w > 1080

    fun getBuilder() = builders?.last { it.w <= SCREEN.w } ?: this

    override fun toString(): String {
        return "$w x $h"
    }

    fun toString(ratio: AspectRatio) : String {
        return "$w x ${(w*ratio.r).toInt()}"
    }

    companion object {

        val SAMPLE = Resolution(4)
        val R791   = Resolution(791)
        val R1187  = Resolution(1187)
        val R1781  = Resolution(1781)
        val R2374  = Resolution(2374)
        val R3561  = Resolution(3561)

        val R180   = Resolution(180)
        val R360   = Resolution(360)
        val R480   = Resolution(480,                videoCompanion = R791,  bitrateCoef = 0.11111111)
        val R720   = Resolution(720,                videoCompanion = R1187, bitrateCoef = 0.25)
        val R1080  = Resolution(1080, listOf(R360), videoCompanion = R1781, bitrateCoef = 0.5625)
        val R1440  = Resolution(1440, listOf(R720), videoCompanion = R2374, bitrateCoef = 1.0)
        val R2160  = Resolution(2160, listOf(R720), videoCompanion = R3561, bitrateCoef = 0.0)
        val R2880  = Resolution(2880, listOf(R720))

        val R3600  = Resolution(3600, listOf(R720),      )
        val R4320  = Resolution(4320, listOf(R720, R1440))
        val R5040  = Resolution(5040, listOf(R720),      )
        val R5760  = Resolution(5760, listOf(R720, R1440))

        val ultraHigh = listOf(R3600, R4320, R5040, R5760)
        val all = ArrayList(arrayListOf(
                SAMPLE, R180, R360, R480, R720, R1080, R1440, R2160, R2880, R3600, R4320, R5040, R5760,
                R791, R1187, R1781, R2374, R3561
        ))
        val foregrounds = arrayListOf(R360, R480, R720, R1080, R1440, R2160, R2880, R3600, R4320, R5040, R5760)

        fun valueOf(width: Int) : Resolution? {
            return foregrounds.firstOrNull { it.w == width }
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

            all.forEach { res ->
                res.n = res.w*res.h
                if (res.videoCompanion != null) res.videoCompanion?.videoCompanion = res
            }

        }

        fun addResolution(width: Int) {
            val newRes = Resolution(width)
            all.add(all.indexOfFirst { newRes.w < it.w }, newRes)
            foregrounds.add(foregrounds.indexOfFirst { newRes.w < it.w }, newRes)
        }


        val BG = R180
        val THUMB = R180
        var SCREEN = R1080
        val MAX_FREE = R1080
        val NUM_VALUES_WORKING = { foregrounds.size }
        var NUM_VALUES_FREE = 0

    }

}