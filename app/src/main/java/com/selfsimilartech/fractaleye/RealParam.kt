package com.selfsimilartech.fractaleye

import android.renderscript.Double2
import android.util.Log
import android.util.Range
import kotlin.math.*

open class RealParam (

    val nameId                  : Int               = -1,
    val iconId                  : Int               = R.drawable.parameter,
    u                           : Double            = 0.0,
    uRange                      : Range<Double>     = Range(0.0, 1.0),
    uLocked                     : Boolean           = false,
    var isDiscrete                : Boolean           = false,
    val toRadians               : Boolean           = false,
    override val goldFeature    : Boolean           = false,
    val devFeature              : Boolean           = false,
    sensitivity                 : Double            = 50.0,
    val restrictValue           : Boolean           = false,
    val scale                   : Scale             = Scale.LINEAR,
    val displayLinear           : Boolean           = true,
    asClone                     : Boolean           = false

) : Goldable {

    constructor(data: Data) : this(u = data.u, sensitivity = if (data.sensitivity == 0.0) 50.0 else data.sensitivity)
    constructor(nameId: Int = -1, u: Double = 0.0) : this(nameId, R.drawable.parameter, u)
    constructor(u: Double) : this(-1, R.drawable.parameter, u)


    class Scale(val convert: (y: Double) -> Double, val convertBack: (x: Double) -> Double) {
        companion object {
            val LINEAR = Scale(
                { y -> y },
                { x -> x }
            )
            val EXP = Scale(
                { y -> if (y <= 0.0) 1.0 else log2(y) },
                { x -> 2.0.pow(x) }
            )
            val EXP_INV = Scale(
                { y -> if (y <= 0.0) 1.0 else log2(-y) },
                { x -> 2.0.pow(-x) }
            )
            val EXP_REFLECTED = Scale(
                { y -> if (y < 0.0) -log2(1.0 - y) else log2(1.0 + y) },
                { x -> if (x < 0.0) 1.0 - 2.0.pow(-x) else 2.0.pow(x) - 1.0 }
            )
            val EXP_SQRT = Scale(
                { y -> if (y <= 0.0) 1.0 else log2(y).pow(2.0) },
                { x -> if (x < 0.0) 1.0 else 2.0.pow(sqrt(x)) }
            )
            val POW =  { p: Double -> Scale(
                { y -> y.pow(1.0/p) },
                { x -> x.pow(p) }
            )}
        }
    }

    data class Data(val u: Double, val v: Double, val isComplex: Boolean, val sensitivity: Double)

    companion object {

        const val SENSITIVITY_EXP_LOW = -2.0
        const val SENSITIVITY_EXP_HIGH = 0.75
        const val ADJUST_DISCRETE = 7.5f
        const val ADJUST_CONTINUOUS = 30f

    }

    var name = ""

    val uInit = if (asClone) u else scale.convert(u)
    val uLockedInit = uLocked
    val toRadiansInit = toRadians

    var uLocked = uLockedInit
    open var u = if (asClone) u else scale.convert(u)
        set(value) {
            if (!uLocked) {
                field = when {
                    toRadians -> value.mod(360.0)
                    Settings.restrictParams || restrictValue -> clamp(value)
                    else -> value
                }
            }
        }

    open var uRange = if (asClone) uRange else Range(
        scale.convert(uRange.lower),
        scale.convert(uRange.upper)
    )

    open var sensitivity = sensitivity
        set(value) {
            field = value.clamp(1.0, 99.0)
            val t = 0.5 - (field - 1.0)/98.0
            sensitivityFactor = 10.0.pow(t*(SENSITIVITY_EXP_LOW - SENSITIVITY_EXP_HIGH)) * uRange.size()/3.5
        }

    open var sensitivityFactor = if (asClone) uRange.size()/5.0 else (scale.convert(uRange.upper) - scale.convert(uRange.lower))/3.5


    val interval = if (asClone) uRange.upper - uRange.lower else scale.convert(uRange.upper) - scale.convert(uRange.lower)

    val scaledValue : Double
        get() = scale.convertBack(u)

    open fun valueEquals(other: RealParam) : Boolean {
        return u == other.u
    }

    open fun randomize(mag: Double) {
        u = Math.random()*(uRange.upper - uRange.lower) + uRange.lower
    }

    open fun randomizePerturb(mag: Double) {
        u += Math.random()*(uRange.upper - uRange.lower)*mag
    }

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

        val newParam = RealParam(
            nameId,
            iconId,
            uInit,
            uRange,
            uLocked,
            isDiscrete,
            toRadians,
            goldFeature,
            devFeature,
            sensitivity,
            restrictValue,
            scale,
            asClone = true
        )
        newParam.u = u
        return newParam

    }

//    open fun setFrom(a: Double, b: Double) {
//        // assuming input values are in linear space
//        u = scale.convert(a)
//    }

    open fun setFrom(newParam: RealParam) {

        uLocked = false
        u = scale.convert(newParam.scale.convertBack(newParam.u))
        sensitivity = newParam.sensitivity

    }

    open fun toFloatArray() : FloatArray = floatArrayOf(scaledValue.toFloat(), 0f)

    open fun toDouble2() : Double2 = Double2(u, 0.0)

    open fun toData(index: Int) : Data {
        return Data(scaledValue, 0.0, false, sensitivity)
    }

    override fun toString(): String {
        val displayValue = if (displayLinear) scaledValue else u
        return when {
            isDiscrete -> "%d".format(floor(displayValue).toInt())
            toRadians -> "%.1f".format(displayValue)
            else -> "%.2f".format(displayValue)
        }
    }

    fun toString(U: Double) : String {
        return if (isDiscrete) "%d".format(U.roundToInt()) else U.format(REAL_PARAM_DIGITS)
    }

    open fun toConstructorString() : String {
        return "RealParam($u)"
    }

}