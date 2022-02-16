package com.selfsimilartech.fractaleye

import android.renderscript.Double2
import android.util.Range
import kotlin.math.pow

class ComplexParam (

        nameId      : Int = -1,
        iconId      : Int = R.drawable.parameter,
        u           : Double = 0.0,
        v           : Double = 0.0,
        uLocked     : Boolean = false,
        vLocked     : Boolean = false,
        uRange      : Range<Double> = Range(0.0, 1.0),
        var vRange  : Range<Double> = Range(0.0, 1.0),
        linked      : Boolean = false,
        toRadians   : Boolean = false,
        sensitivity : Double = 50.0,
        goldFeature : Boolean = false

) : RealParam(nameId, iconId, u, uRange, uLocked, false, toRadians, goldFeature, sensitivity = sensitivity) {

    constructor(u: Double = 0.0, v: Double = 0.0) : this(-1, R.drawable.parameter, u, v)
    constructor(nameId: Int = -1, u: Double = 0.0, v: Double = 0.0) : this(nameId, R.drawable.parameter, u, v)
    constructor(data: Data) : this(u = data.u, v = data.v, sensitivity = data.sensitivity)

    companion object {
        const val SENSITIVITY_EXP_LOW = -6.0
        const val SENSITIVITY_EXP_HIGH = 2.0
    }

    override var sensitivity = sensitivity
        set(value) {
            field = value.clamp(1.0, 99.0)
            val t = 0.5 - (field - 1.0)/98.0
            sensitivityFactor = 10.0.pow(t*(SENSITIVITY_EXP_LOW - SENSITIVITY_EXP_HIGH)) * 2.75
        }

    override var sensitivityFactor = 3.5

    private val vInit = v
    private val vLockedInit = vLocked
    private val linkedInit = linked

    var vLocked = vLockedInit
    override var u = u
        set (value) {
            if (!uLocked) {
                field = value
                if (linked) v = u
            }
        }
    var v = v
        set (value) {
            if (!vLocked) {
                field = if (linked) u else value
            }
        }
    var linked = linkedInit
        set (value) {
            field = value
            if (value) v = u
        }


    override fun valueEquals(other: RealParam): Boolean {
        return other is ComplexParam && u == other.u && v == other.v
    }

    override fun randomize(mag: Double) {
        u = Math.random()*mag - 0.5*mag
        v = Math.random()*mag - 0.5*mag
    }

    override fun randomizePerturb(mag: Double) {
        u += Math.random()*10.0*mag
        v += Math.random()*10.0*mag
    }

    override fun reset() {
        super.reset()
        vLocked = false
        v = vInit
        uLocked = uLockedInit
        linked = linkedInit
    }

    override fun clone() : ComplexParam {

        val newParam = ComplexParam(
                nameId,
                iconId,
                uInit,
                vInit,
                uLocked,
                vLocked,
                uRange,
                vRange,
                linked,
                toRadians,
                sensitivity,
                goldFeature
        )
        newParam.u = u
        newParam.v = v
        return newParam

    }

    override fun setFrom(newParam: RealParam) {

        super.setFrom(newParam)

        newParam as ComplexParam

        uLocked = false
        vLocked = false
        u = newParam.u
        v = newParam.v
        sensitivity = newParam.sensitivity

    }

    override fun toFloatArray() : FloatArray = floatArrayOf(u.toFloat(), v.toFloat())

    override fun toDouble2() : Double2 = Double2(u, v)

    override fun toData(index: Int) : Data {
        return Data(u, v, true, sensitivity)
    }

    override fun toConstructorString(): String {
        return "ComplexParam($u, $v)"
    }

}