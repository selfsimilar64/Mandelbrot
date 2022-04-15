package com.selfsimilartech.fractaleye

class TextureSpan(delta : Float = MotionValue.DELTA_CONTINUOUS) {

    var min : MotionValue = MotionValue( 0f, delta = delta )
    var max : MotionValue = MotionValue( 1f, delta = delta )

    override fun toString(): String {
        return "(min: $min, max: $max)"
    }

}