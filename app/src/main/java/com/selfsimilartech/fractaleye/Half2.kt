package com.selfsimilartech.fractaleye

import kotlin.experimental.and

val SIZE = 16

/**
 * Epsilon is the difference between 1.0 and the next value representable
 * by a half-precision floating-point.
 */
val EPSILON = 0x1400.toShort()

/**
 * Maximum exponent a finite half-precision float may have.
 */
val MAX_EXPONENT = 15

/**
 * Minimum exponent a normalized half-precision float may have.
 */
val MIN_EXPONENT = -14

/**
 * Smallest negative value a half-precision float may have.
 */
val LOWEST_VALUE = 0xfbff.toShort()

/**
 * Maximum positive finite value a half-precision float may have.
 */
val MAX_VALUE = 0x7bff.toShort()

/**
 * Smallest positive normal value a half-precision float may have.
 */
val MIN_NORMAL = 0x0400.toShort()

/**
 * Smallest positive non-zero value a half-precision float may have.
 */
val MIN_VALUE = 0x0001.toShort()

/**
 * A Not-a-Number representation of a half-precision float.
 */
val NaN = 0x7e00.toShort()

/**
 * Negative infinity of type half-precision float.
 */
val NEGATIVE_INFINITY = 0xfc00.toShort()

/**
 * Negative 0 of type half-precision float.
 */
val NEGATIVE_ZERO = 0x8000.toShort()

/**
 * Positive infinity of type half-precision float.
 */
val POSITIVE_INFINITY = 0x7c00.toShort()

/**
 * Positive 0 of type half-precision float.
 */
val POSITIVE_ZERO = 0x0000.toShort()

val SIGN_SHIFT = 15

val EXPONENT_SHIFT = 10

val SIGN_MASK = 0x8000

val SHIFTED_EXPONENT_MASK = 0x1f

val SIGNIFICAND_MASK = 0x3ff

val EXPONENT_SIGNIFICAND_MASK = 0x7fff

val EXPONENT_BIAS = 15
private const val FP32_SIGN_SHIFT = 31
private const val FP32_EXPONENT_SHIFT = 23
private const val FP32_SHIFTED_EXPONENT_MASK = 0xff
private const val FP32_SIGNIFICAND_MASK = 0x7fffff
private const val FP32_EXPONENT_BIAS = 127
private const val FP32_QNAN_MASK = 0x400000
private const val FP32_DENORMAL_MAGIC = 126 shl 23
private val FP32_DENORMAL_FLOAT = java.lang.Float.intBitsToFloat(FP32_DENORMAL_MAGIC)

class Half2 {


    companion object {

        fun intBitsToHalf(bits: Int): Short {
            return (bits and 0xffff).toShort()
        }

        fun toFloat(h: Short): Float {
            val bits: Int = h.toInt() and 0xffff  // maybe revert?
            val s = bits and SIGN_MASK
            val e = bits ushr EXPONENT_SHIFT and SHIFTED_EXPONENT_MASK
            val m = bits and SIGNIFICAND_MASK
            var outE = 0
            var outM = 0
            if (e == 0) { // Denormal or 0
                if (m != 0) {
                    // Convert denorm fp16 into normalized fp32
                    var o = java.lang.Float.intBitsToFloat(FP32_DENORMAL_MAGIC + m)
                    o -= FP32_DENORMAL_FLOAT
                    return if (s == 0) o else -o
                }
            } else {
                outM = m shl 13
                if (e == 0x1f) { // Infinite or NaN
                    outE = 0xff
                    if (outM != 0) { // SNaNs are quieted
                        outM = outM or FP32_QNAN_MASK
                    }
                } else {
                    outE = e - EXPONENT_BIAS + FP32_EXPONENT_BIAS
                }
            }
            val out = s shl 16 or (outE shl FP32_EXPONENT_SHIFT) or outM
            return java.lang.Float.intBitsToFloat(out)
        }

    }

}