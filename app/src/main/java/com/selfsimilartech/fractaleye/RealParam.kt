package com.selfsimilartech.fractaleye

import android.renderscript.Double2
import android.util.Range
import kotlin.math.roundToInt

open class RealParam (

        val nameId        : Int             = -1,
        u                 : Double          = 0.0,
        var uRange        : Range<Double> = Range(0.0, 1.0),
        uLocked           : Boolean         = false,
        var discrete      : Boolean         = false,
        var toRadians     : Boolean         = false,
        val goldFeature   : Boolean         = false,
        val isPrimary     : Boolean         = true,
        val isRateParam   : Boolean         = false,
        val parent        : RealParam?          = null

) {

    var name = ""

    val uInit = u
    val uLockedInit = uLocked
    val toRadiansInit = toRadians

    var uLocked = uLockedInit
    open var u = u
        set (value) { if (!uLocked) field = value }
    val sensitivity =
            if (isRateParam) null
            else RealParam(
                    R.string.sensitivity,
                    if (toRadians) 45.0 else 1.0,
                    if (toRadians) Range(0.001, 100.0) else Range(0.001, 15.0),
                    isPrimary = false,
                    isRateParam = true,
                    parent = this
            )

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
    open fun reset() {
        uLocked = false
        u = uInit
        uLocked = uLockedInit
        sensitivity?.reset()
    }
    open fun clone() : RealParam {

        return RealParam(
                nameId,
                uInit,
                uRange,
                uLockedInit,
                toRadiansInit,
                goldFeature
        )

    }
    open fun setFrom(newParam: RealParam) {

        uLocked = newParam.uLockedInit
        u = newParam.uInit
        toRadians = newParam.toRadiansInit
        newParam.sensitivity?.let { sensitivity?.setFrom(it) }

    }
    open fun toFloatArray() : FloatArray = floatArrayOf(u.toFloat(), 0f)
    open fun toDouble2() : Double2 = Double2(u, 0.0)

    override fun toString(): String {
        return if (discrete) "%d".format(u.roundToInt()) else "%.3f".format(u)
    }

    fun toString(U: Double) : String {
        return if (discrete) "%d".format(U.roundToInt()) else "%.3f".format(U)
    }

}