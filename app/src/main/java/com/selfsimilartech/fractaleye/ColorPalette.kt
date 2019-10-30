package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import kotlin.math.floor


fun FloatArray.mult(s: Float) : FloatArray {
    return FloatArray(this.size) {i: Int -> s*this[i]}
}
fun FloatArray.invert() : FloatArray {
    return FloatArray(this.size) {i: Int -> 1.0f - this[i]}
}

class ColorPalette (
        val name: String,
        val colors: List<Int> = listOf(),
        oscillate: Boolean = false
) {

    companion object {

        val bw = ColorPalette(
                "Yin Yang", listOf(
                R.color.black,
                R.color.white
        ))
        val beach = ColorPalette(
                "Beach", listOf(
                R.color.yellowish1,
                R.color.darkblue1,
                R.color.black,
                R.color.turquoise,
                R.color.tusk
        ))
        val p1 = ColorPalette(
                "P1", listOf(
                R.color.white,
                R.color.purple1,
                R.color.black,
                R.color.deepred,
                R.color.white
        ))
        val p3 = ColorPalette(
                "P3", listOf(
                R.color.q1,
                R.color.darkblue1,
                R.color.white,
                R.color.q2,
                R.color.q3
        ))
        val p4 = ColorPalette(
                "Vascular", listOf(
                R.color.yellowish1,
                R.color.darkblue2,
                R.color.black,
                R.color.purple3,
                R.color.deepred
        ))
        val p5 = ColorPalette(
                "Flora", listOf(
                R.color.yellowish2,
                R.color.magenta2,
                R.color.white,
                R.color.darkblue1,
                R.color.black,
                R.color.darkblue1
        ))
        val royal = ColorPalette(
                "Royal", listOf(
                R.color.yellowish1,
                R.color.darkblue1,
                R.color.softgreen2,
                R.color.purple3,
                R.color.maroon
        ))
        val p8 = ColorPalette(
                "Groovy", listOf(
                R.color.q4,
                R.color.q5,
                R.color.q6,
                R.color.black
        ))
        val canyon = ColorPalette(
                "Canyon", listOf(
                R.color.deepred2,
                R.color.q6,
                R.color.q8,
                R.color.q9,
                R.color.purple3
        ))
        val anubis = ColorPalette(
                "Anubis", listOf(
                R.color.black,
                R.color.purple2,
                R.color.mint,
                R.color.yellowish1,
                R.color.q10,
                R.color.tangerine
        ))
        val p9 = ColorPalette(
                "P9", listOf(
                R.color.q12,
                R.color.q13,
                R.color.q14,
                R.color.q15,
                R.color.q16,
                R.color.q17,
                R.color.q18
        ))
        val viridis = ColorPalette(
                "Viridis", listOf(
                R.color.q19,
                R.color.q20,
                R.color.q21,
                R.color.q22,
                R.color.q23,
                R.color.q24
        ))
        val plasma = ColorPalette(
                "Plasma", listOf(
                R.color.q25,
                R.color.q26,
                R.color.q27,
                R.color.q28,
                R.color.q29,
                R.color.q30,
                R.color.q31
        ))
        val inferno = ColorPalette(
                "Inferno", listOf(
                R.color.q32,
                R.color.q33,
                R.color.q34,
                R.color.q35,
                R.color.q36,
                R.color.q37
        ))
        val magma = ColorPalette(
                "Magma", listOf(
                R.color.q38,
                R.color.q39,
                R.color.q40,
                R.color.q41,
                R.color.q42,
                R.color.q43
        ))
        val all = mapOf(
                "Yin Yang"      to  bw,
                "Vascular"      to  p4,
                "Flora"         to  p5,
                "Royal"         to  royal,
                "Groovy"        to  p8,
                "Canyon"        to  canyon,
                "Anubis"        to  anubis,
                "P9"            to  p9,
                "Virids"        to  viridis,
                "Plasma"        to  plasma,
                "Inferno"       to  inferno,
                "Magma"         to  magma
        )

    }


    var size = if (oscillate) 2*colors.size - 1 else colors.size + 1

    val oscillateInit = oscillate

    var oscillate = oscillate
        set (value) {
            field = value
            size = if (oscillate) 2*colors.size - 1 else colors.size + 1
        }


    fun reset() {
        oscillate = oscillateInit
    }
    fun getFlatPalette(res: Resources) : FloatArray {

        val palette = intArrayToList(getColors(res,
                if (oscillate) colors.minus(colors.last()).plus(colors.reversed())
                else colors.plus(colors.first())
        ))

        return FloatArray(palette.size * 3) { i: Int ->
            val a = floor(i / 3.0f).toInt()
            val b = i % 3
            palette[a][b]
        }

    }
    fun getColors(res: Resources, ids: List<Int>) : IntArray {
        // takes color resource ids as input and returns color ints
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) IntArray(ids.size) { i: Int -> res.getColor(ids[i], null) }
        else IntArray(ids.size) { i: Int -> res.getColor(ids[i]) }
    }
    private fun intToFloatArray(c: Int) : FloatArray {
        return floatArrayOf(Color.red(c)/255f, Color.green(c)/255f, Color.blue(c)/255f)
    }
    private fun intArrayToList(C: IntArray) : List<FloatArray> {
        return List(C.size) { i: Int -> intToFloatArray(C[i]) }
    }

    override fun toString() : String { return name }
    override fun equals(other: Any?): Boolean {
        return other is ColorPalette && other.name == name
    }
    override fun hashCode(): Int { return name.hashCode() }

}