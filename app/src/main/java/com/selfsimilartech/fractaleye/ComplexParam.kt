package com.selfsimilartech.fractaleye

import android.renderscript.Double2
import android.util.Range

class ComplexParam (

        nameId        : Int = -1,
        u             : Double = 0.0,
        v             : Double = 0.0,
        uLocked       : Boolean = false,
        vLocked       : Boolean = false,
        uRange        : Range<Double> = Range(0.0, 1.0),
        var vRange    : Range<Double> = Range(0.0, 1.0),
        linked        : Boolean = false,
        toRadians     : Boolean = false,
        goldFeature   : Boolean = false

) : RealParam(nameId, u, uRange, uLocked, false, toRadians, goldFeature) {

    constructor(u: Double = 0.0, v: Double = 0.0) : this(-1, u, v)
    constructor(data: Data) : this(u = data.u, v = data.v)

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

        return ComplexParam(u = u, v = v)

    }
    override fun setFrom(newParam: RealParam) {

        super.setFrom(newParam)

        newParam as ComplexParam

        uLocked = false
        vLocked = false
        u = newParam.u
        v = newParam.v

    }
    override fun toFloatArray() : FloatArray = floatArrayOf(u.toFloat(), v.toFloat())
    override fun toDouble2() : Double2 = Double2(u, v)
    override fun toData(index: Int) : Data = Data(u, v, true)

}