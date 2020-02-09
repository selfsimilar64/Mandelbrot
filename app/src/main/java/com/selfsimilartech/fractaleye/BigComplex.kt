package com.selfsimilartech.fractaleye

import java.math.BigDecimal
import java.math.BigDecimal.ROUND_HALF_UP


val TWO = BigDecimal("2")

fun BigDecimal.sqrt(): BigDecimal {
    var x0 = BigDecimal("0")
    var x1 = BigDecimal(Math.sqrt(this.toDouble()))
    while (x0 != x1) {
        x0 = x1
        x1 = this.divide(x0, this.scale(), ROUND_HALF_UP)
        x1 = x1.add(x0)
        x1 = x1.divide(TWO, this.scale(), ROUND_HALF_UP)
    }
    return x1
}

data class BigComplex(val real : BigDecimal, val imag : BigDecimal) {

    companion object {

        val ZERO = BigComplex(BigDecimal.ZERO, BigDecimal.ZERO)
        val ONE = BigComplex(BigDecimal.ONE, BigDecimal.ZERO)
        val TWO = BigComplex(BigDecimal("2"), BigDecimal.ZERO)
        val I = BigComplex(BigDecimal.ZERO, BigDecimal.ONE)

    }

    operator fun plus(w: BigComplex) : BigComplex {
        return BigComplex(
                this.real.plus(w.real),
                this.imag.plus(w.imag)
        )
    }

    operator fun plus(u: BigDecimal) : BigComplex {
        return BigComplex(
                this.real.plus(u),
                this.imag
        )
    }

    operator fun minus(w: BigComplex) : BigComplex {
        return BigComplex(
                this.real.minus(w.real),
                this.imag.minus(w.imag)
        )
    }

    operator fun minus(u: BigDecimal) : BigComplex {
        return BigComplex(
                this.real.minus(u),
                this.imag
        )
    }

    operator fun times(w: BigComplex) : BigComplex {
        return BigComplex(
                this.real.times(w.real).minus(this.imag*w.imag),
                this.real.times(w.imag).plus(this.imag*w.real)
        )
    }

    operator fun times(u: BigDecimal) : BigComplex {
        return BigComplex(
                this.real.times(u),
                this.imag.times(u)
        )
    }

    fun sqr() : BigComplex {
        return this*this
    }

    fun mod() : BigDecimal {
        return this.real.times(this.real).plus(this.imag.times(this.imag)).sqrt()
    }

    fun conj() : BigComplex {
        return BigComplex(
                this.real,
                this.imag.negate()
        )
    }

    operator fun div(w: BigComplex) : BigComplex {
        val numer = this*w.conj()
        val denom = w.mod()
        return BigComplex(
                numer.real.div(denom),
                numer.imag.div(denom)
        )
    }

    operator fun div(u: BigDecimal) : BigComplex {
        return BigComplex(
                this.real.div(u),
                this.imag.div(u)
        )
    }


}