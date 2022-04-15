package com.selfsimilartech.fractaleye

import android.graphics.Color
import android.util.Log

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


    fun fitToSpan(span: TextureSpan) {

        val max = span.max.x
        val min = span.min.x
        val length = max - min
        val prevFreq = frequency
        val prevPhase = phase

        frequency = prevFreq * length
        phase = prevPhase + prevFreq * min
        Log.v("COLOR CONFIG", "frequency set $frequency")

    }

    fun unfitFromSpan(span: TextureSpan) {

        val max = span.max.x
        val min = span.min.x
        val length = max - min
        val prevFreq = frequency
        val prevPhase = phase

        frequency = prevFreq / length
        phase = prevPhase - prevFreq * min / length
        Log.v("COLOR CONFIG", "frequency set to $frequency")

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