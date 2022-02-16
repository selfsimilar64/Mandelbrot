package com.selfsimilartech.fractaleye

class MotionValue (

        var position     : Float = 0f,
        var velocity     : Float = 0f,
        // var acceleration : Float = 0f,
        var delta        : Float

) {

    companion object {
        const val DELTA_CONTINUOUS = 0.05f
        const val DELTA_DISCRETE = 0.25f
    }

    fun impulse(s: Float, weight: Float = 1f) {
        // val jerk = s - position - 8f*velocity - 12f*acceleration
        // acceleration += delta*jerk
        val acceleration = s - position - 2f*velocity
        velocity += weight*delta*acceleration
        position += weight*delta*velocity
    }

    fun step() { impulse(0f) }

    fun set(
            newPosition: Float = 0f,
            newVelocity: Float = 0f
            // newAcceleration: Float = 0f
    ) {
        // acceleration = newAcceleration
        velocity = newVelocity
        position = newPosition
    }

    override fun toString(): String {
        // return "x: $position, v: $velocity, a: $acceleration"
        return "$position"
    }

}