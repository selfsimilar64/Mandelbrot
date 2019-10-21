package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import kotlin.math.floor


class ColorPalette (
        val name: String,
        private val colorInts: IntArray = intArrayOf()
) {

    companion object {

        private fun getColor(res: Resources, id: Int) : Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) res.getColor(id, null)
            else res.getColor(id)
        }
        private fun getColorResources(res: Resources, ids: IntArray) : IntArray {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) IntArray(ids.size) { i: Int -> res.getColor(ids[i], null) }
            else IntArray(ids.size) { i: Int -> res.getColor(ids[i]) }
        }
        fun intToFloatArray(c: Int) : FloatArray {
            return floatArrayOf(Color.red(c)/255f, Color.green(c)/255f, Color.blue(c)/255f)
        }
        fun FloatArray.mult(s: Float) : FloatArray {
            return FloatArray(this.size) {i: Int -> s*this[i]}
        }
        fun FloatArray.invert() : FloatArray {
            return FloatArray(this.size) {i: Int -> 1.0f - this[i]}
        }

        val bw      = { res: Resources -> ColorPalette(
                "Yin Yang", getColorResources(res, intArrayOf(
                R.color.black,
                R.color.white
        )))}
        val beach   = { res: Resources -> ColorPalette(
                "Beach", getColorResources(res, intArrayOf(
                R.color.yellowish1,
                R.color.darkblue1,
                R.color.black,
                R.color.turquoise,
                R.color.tusk)
        )) }
        val p1      = { res: Resources -> ColorPalette(
                "P1", getColorResources(res, intArrayOf(
                R.color.white,
                R.color.purple1,
                R.color.black,
                R.color.deepred,
                R.color.white
        )))}
        val p3      = { res: Resources -> ColorPalette(
                "P3", getColorResources(res, intArrayOf(
                R.color.q1,
                R.color.darkblue1,
                R.color.white,
                R.color.q2,
                R.color.q3
        )))}
        val p4      = { res: Resources -> ColorPalette(
                "Vascular", getColorResources(res, intArrayOf(
                R.color.yellowish1,
                R.color.darkblue2,
                R.color.black,
                R.color.purple3,
                R.color.deepred
        )
        )) }
        val p5      = { res: Resources -> ColorPalette(
                "Flora", getColorResources(res, intArrayOf(
                R.color.yellowish2,
                R.color.magenta2,
                R.color.white,
                R.color.darkblue1,
                R.color.black,
                R.color.darkblue1
        )))}
        val royal   = { res: Resources -> ColorPalette(
                "Royal", getColorResources(res, intArrayOf(
                R.color.yellowish1,
                R.color.darkblue1,
                R.color.softgreen2,
                R.color.purple3,
                R.color.maroon
        )))}
        val p8      = { res: Resources -> ColorPalette(
                "Groovy", getColorResources(res, intArrayOf(
                R.color.q4,
                R.color.q5,
                R.color.q6,
                R.color.black
        )))}
        val canyon  = { res: Resources -> ColorPalette(
                "Canyon", getColorResources(res, intArrayOf(
                R.color.deepred2,
                R.color.q6,
                R.color.q8,
                R.color.q9,
                R.color.purple3
        )))}
        val anubis  = { res: Resources -> ColorPalette(
                "Anubis", getColorResources(res, intArrayOf(
                R.color.black,
                R.color.purple2,
                R.color.mint,
                R.color.yellowish1,
                R.color.q10,
                R.color.tangerine
        )))}
        val p9      = { res: Resources -> ColorPalette(
                "P9", getColorResources(res, intArrayOf(
                R.color.q12,
                R.color.q13,
                R.color.q14,
                R.color.q15,
                R.color.q16,
                R.color.q17,
                R.color.q18
        )))}
        val viridis      = { res: Resources -> ColorPalette(
                "Viridis", getColorResources(res, intArrayOf(
                R.color.q19,
                R.color.q20,
                R.color.q21,
                R.color.q22,
                R.color.q23,
                R.color.q24
        )))}
        val plasma      = { res: Resources -> ColorPalette(
                "Plasma", getColorResources(res, intArrayOf(
                R.color.q25,
                R.color.q26,
                R.color.q27,
                R.color.q28,
                R.color.q29,
                R.color.q30,
                R.color.q31
        )))}
        val inferno      = { res: Resources -> ColorPalette(
                "Inferno", getColorResources(res, intArrayOf(
                R.color.q32,
                R.color.q33,
                R.color.q34,
                R.color.q35,
                R.color.q36,
                R.color.q37
        )))}
        val magma = { res: Resources -> ColorPalette(
                "Magma", getColorResources(res, intArrayOf(
                R.color.q38,
                R.color.q39,
                R.color.q40,
                R.color.q41,
                R.color.q42,
                R.color.q43
        )))}
        val all     = mapOf(
                "Yin Yang"      to  bw,
                "Beach"         to  beach,
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

    private val colors = List(colorInts.size) { i: Int -> intToFloatArray(colorInts[i]) }

    var oscillate = true

    private var palette = listOf(floatArrayOf())
    var size = 0

    val flatPalette = {
        FloatArray(palette.size * 3) { i: Int ->
            val a = floor(i / 3.0f).toInt()
            val b = i % 3
            palette[a][b]
        }
    }

    private val drawableOrientation = GradientDrawable.Orientation.LEFT_RIGHT
    val drawable = GradientDrawable(drawableOrientation, colorInts)


    init {

        updatePalette()

    }

    fun updatePalette() {

        palette =
            if (oscillate) {  colors.minus(colors.last()).plus(colors.reversed())  }
            else {            colors.plus(colors.first())                          }
        size = palette.size


    }

    override fun toString() : String { return name }
    override fun equals(other: Any?): Boolean {
        return other is ColorPalette && other.name == name
    }
    override fun hashCode(): Int { return name.hashCode() }

}