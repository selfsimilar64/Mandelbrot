package com.selfsimilartech.fractaleye

class TextureSpan(delta : Float) {

    var center : MotionValue = MotionValue( 0.5f, delta = delta )
    var size   : MotionValue = MotionValue(   1f, delta = delta )

    fun min() : Float = center.position - 0.5f*size.position

    fun max() : Float = center.position + 0.5f*size.position

    override fun toString(): String {
        return "[center] -- $center, [size] -- $size"
    }

}