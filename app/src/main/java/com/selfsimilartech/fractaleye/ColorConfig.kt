package com.selfsimilartech.fractaleye

import android.graphics.Color

class ColorConfig(

    var frequency   : Double = 1.0,
    phase           : Double = 0.0,
    var density     : Double = 0.0,
    var fillColor     : Int    = Color.WHITE,
    var outlineColor     : Int    = Color.BLACK

) {

    val frequencyInit = frequency
    val phaseInit = phase
    val densityInit = density
    val fillColorInit = fillColor
    val outlineColorInit = outlineColor

    var phase = phase
        set(value) {
            var rem = value.rem(1f)
            if (rem < 0.0) rem += 1f
            field = rem
        }

    fun setFrom(newConfig: ColorConfig) {

        frequency   = newConfig.frequency
        phase       = newConfig.phase
        density     = newConfig.density
        fillColor     = newConfig.fillColor
        outlineColor     = newConfig.outlineColor

    }

    fun clone() : ColorConfig {
        val newConfig = ColorConfig(
            frequencyInit,
            phaseInit,
            densityInit,
            fillColorInit,
            outlineColorInit
        )
        newConfig.let{
            it.frequency = frequency
            it.phase = phase
            it.density = density
            it.fillColor = fillColor
            it.outlineColor = outlineColor
        }
        return newConfig
    }

    fun reset() {



    }

}