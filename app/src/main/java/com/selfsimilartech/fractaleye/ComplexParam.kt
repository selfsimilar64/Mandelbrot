package com.selfsimilartech.fractaleye

import android.renderscript.Double2
import android.util.Range
import kotlin.math.pow

class ComplexParam (

        nameId      : Int = -1,
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

) : RealParam(nameId, u, uRange, uLocked, false, toRadians, goldFeature, sensitivity = sensitivity) {

    constructor(u: Double = 0.0, v: Double = 0.0) : this(-1, u, v)
    constructor(data: Data) : this(u = data.u, v = data.v, sensitivity = data.sensitivity)

    companion object {
        const val SENSITIVITY_EXP_LOW = -5.0
        const val SENSITIVITY_EXP_HIGH = 2.0
    }

    override var sensitivity = sensitivity
        set(value) {
            field = clamp(value, 1.0, 99.0)
            val t = 0.5 - (field - 1.0)/98.0
            sensitivityFactor = 0.5*10.0.pow(t*(SENSITIVITY_EXP_LOW - SENSITIVITY_EXP_HIGH)) * 1.75
        }

    override var sensitivityFactor = 1.75

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

    override fun reset() {
        super.reset()
        vLocked = false
        v = vInit
        uLocked = uLockedInit
        linked = linkedInit
    }
    override fun clone() : ComplexParam {

        return ComplexParam(u = u, v = v, sensitivity = sensitivity)

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

}