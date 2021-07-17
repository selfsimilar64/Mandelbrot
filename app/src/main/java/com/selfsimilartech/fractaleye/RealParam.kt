package com.selfsimilartech.fractaleye

import android.renderscript.Double2
import android.util.Range
import kotlin.math.pow
import kotlin.math.roundToInt

open class RealParam (

        val nameId      : Int             = -1,
        u               : Double          = 0.0,
        var uRange      : Range<Double>   = Range(0.0, 1.0),
        uLocked         : Boolean         = false,
        var discrete    : Boolean         = false,
        var toRadians   : Boolean         = false,
        val goldFeature : Boolean         = false,
        val devFeature  : Boolean         = false,
        sensitivity     : Double          = 50.0

) {

    constructor(data: Data) : this(u = data.u, sensitivity = if (data.sensitivity == 0.0) 50.0 else data.sensitivity)

    data class Data(val u: Double, val v: Double, val isComplex: Boolean, val sensitivity: Double)

    companion object {

        const val SENSITIVITY_EXP_LOW = -2.0
        const val SENSITIVITY_EXP_HIGH = 0.75
        const val ADJUST_DISCRETE = 7.5f
        const val ADJUST_CONTINUOUS = 30f

    }

    var name = ""

    val uInit = u
    val uLockedInit = uLocked
    val toRadiansInit = toRadians

    var uLocked = uLockedInit
    open var u = u
        set (value) {
            if (!uLocked) {
                field = if (SettingsConfig.restrictParams) clamp(value) else value
            }
        }

    open var sensitivity = sensitivity
        set(value) {
            field = clamp(value, 1.0, 99.0)
            val t = 0.5 - (field - 1.0)/98.0
            sensitivityFactor = 0.5*10.0.pow(t*(SENSITIVITY_EXP_LOW - SENSITIVITY_EXP_HIGH)) * uRange.size()/3.5
        }

    open var sensitivityFactor = uRange.size()/3.5


    val interval = uRange.upper - uRange.lower

    open fun getProgress() : Double {
        return (u - uRange.lower)/interval
    }
    open fun getValueFromProgress(p: Double) : Double {
        return uRange.lower + p*interval
    }
    open fun setValueFromProgress(p: Double) {
        u = getValueFromProgress(p)
    }
    open fun clamp(u: Double) : Double {
        return when {
            u < uRange.lower -> uRange.lower
            u > uRange.upper -> uRange.upper
            else -> u
        }
    }
    open fun reset() {
        uLocked = false
        u = uInit
        uLocked = uLockedInit
        sensitivity = 50.0
    }
    open fun clone() : RealParam {

        return RealParam(u = u)

    }
    open fun setFrom(newParam: RealParam) {

        uLocked = false
        u = newParam.u
        sensitivity = newParam.sensitivity

    }
    open fun toFloatArray() : FloatArray = floatArrayOf(u.toFloat(), 0f)
    open fun toDouble2() : Double2 = Double2(u, 0.0)
    open fun toData(index: Int) : Data {
        return Data(u, 0.0, false, sensitivity)
    }

    override fun toString(): String {
        return if (discrete) "%d".format(u.roundToInt()) else u.format(REAL_PARAM_DIGITS)
    }

    fun toString(U: Double) : String {
        return if (discrete) "%d".format(U.roundToInt()) else U.format(REAL_PARAM_DIGITS)
    }

}