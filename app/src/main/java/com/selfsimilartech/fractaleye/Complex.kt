package com.selfsimilartech.fractaleye
import kotlin.math.sqrt

data class Complex(
        val x: Double,
        val y: Double
) {

    companion object {

        val ZERO = Complex(0.0, 0.0)
        val ONE = Complex(1.0, 0.0)
        val I = Complex(0.0, 1.0)
        val PI = Complex(Math.PI, 0.0)

    }


    override fun toString(): String {
        return "($x, $y)"
    }

    fun conj() : Complex{
        return Complex(x, -y)
    }

    fun mod() : Double {
        return sqrt(x * x + y * y)
    }

    operator fun unaryMinus() : Complex {
        return Complex(-x, -y)
    }

    operator fun plus(w: Complex) : Complex {
        return Complex(x + w.x, y + w.y)
    }

    operator fun minus(w: Complex) : Complex {
        return Complex(x - w.x, y - w.y)
    }

    operator fun times(s: Double) : Complex {
        return Complex(s * x, s * y)
    }

    operator fun times(w: Complex) : Complex {
        return Complex(x * w.x - y * w.y, x * w.y + y * w.x)
    }

    operator fun div(s: Double) : Complex {
        return Complex(x / s, y / s)
    }

    operator fun div(w: Complex) : Complex {
        return (this*w.conj())/mod()
    }

}