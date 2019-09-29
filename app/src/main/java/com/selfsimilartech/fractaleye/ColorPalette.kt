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

        val all     = mapOf(
                "Yin Yang"      to  bw,
                "Beach"   to  beach,
                "Vascular"      to  p4,
                "Flora"      to  p5,
                "Royal"   to  royal,
                "Groovy"      to  p8,
                "Canyon"  to  canyon,
                "Anubis"  to  anubis
        )

    }

    private val colors = List(colorInts.size) { i: Int -> intToFloatArray(colorInts[i]) }
    private val palette = colors.plus(colors[0])
    val size = palette.size
    val flatPalette = FloatArray(palette.size * 3) {i: Int ->
        val a = floor(i / 3.0f).toInt()
        val b = i % 3
        palette[a][b]
    }
    var drawableOrientation = GradientDrawable.Orientation.LEFT_RIGHT
    val drawable = GradientDrawable(drawableOrientation, colorInts)

    override fun toString() : String { return name }
    override fun equals(other: Any?): Boolean {
        return other is ColorPalette && other.name == name
    }
    override fun hashCode(): Int { return name.hashCode() }

}