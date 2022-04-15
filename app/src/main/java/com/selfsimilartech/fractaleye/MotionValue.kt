package com.selfsimilartech.fractaleye

class MotionValue (

    var x : Float = 0f,  // position
    var v : Float = 0f,  // velocity
    var delta : Float

) {

    companion object {
        const val DELTA_CONTINUOUS = 0.05f
        const val DELTA_DISCRETE = 0.25f
    }

    fun impulse(s: Float, weight: Float = 1f) {
        // val jerk = s - position - 8f*velocity - 12f*acceleration
        // acceleration += delta*jerk
        val acceleration = s - x - 2f*v
        v += weight*delta*acceleration
        x += weight*delta*v
    }

    fun step() { impulse(0f) }

    fun set(
            newPosition: Float = 0f,
            newVelocity: Float = 0f
            // newAcceleration: Float = 0f
    ) {
        // acceleration = newAcceleration
        v = newVelocity
        x = newPosition
    }

    override fun toString(): String {
        // return "x: $position, v: $velocity, a: $acceleration"
        return "$x"
    }

}