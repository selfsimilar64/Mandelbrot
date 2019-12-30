package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Bitmap
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
        val ids: List<Int> = listOf(),
        oscillate: Boolean = true
) {

    companion object {

        val yinyang = ColorPalette(
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
        val vascular = ColorPalette(
                "Vascular", listOf(
                R.color.black,
                R.color.purple3,
                R.color.deepred,
                R.color.yellowish1,
                R.color.darkblue2
        ))
        val flora = ColorPalette(
                "Flora", listOf(
                R.color.flora1,
                R.color.flora2,
                R.color.flora3,
                R.color.flora4,
                R.color.flora5,
                R.color.flora6
        ))
        val royal = ColorPalette(
                "Royal", listOf(
                R.color.yellowish1,
                R.color.darkblue1,
                R.color.softgreen2,
                R.color.purple3,
                R.color.maroon
        ))
        val groovy = ColorPalette(
                "Groovy", listOf(
                R.color.black,
                R.color.q4,
                R.color.q5,
                R.color.q6
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
        val night = ColorPalette(
                "Night", listOf(
                R.color.night1,
                R.color.night2,
                R.color.night3,
                R.color.night4,
                R.color.night5,
                R.color.night6,
                R.color.night7
        ))
        val cosmic = ColorPalette(
                "Cosmic", listOf(
                R.color.cosmic1,
                R.color.cosmic2,
                R.color.cosmic3,
                R.color.cosmic4,
                R.color.cosmic5,
                R.color.cosmic6,
                R.color.cosmic7,
                R.color.cosmic8
        ))
        val oldskool = ColorPalette(
                "Old Sk00l", listOf(
                R.color.oldskool1,
                R.color.oldskool2,
                R.color.oldskool3,
                R.color.oldskool4,
                R.color.oldskool5,
                R.color.oldskool6,
                R.color.oldskool7
        ))
        val elephant = ColorPalette(
                "Elephant", listOf(
                R.color.elephant1,
                R.color.elephant2,
                R.color.elephant3,
                R.color.elephant4,
                R.color.elephant5,
                R.color.elephant6,
                R.color.elephant7
        ))
        val gold = ColorPalette(
                "Gold", listOf(
                R.color.gold1,
                R.color.gold2,
                R.color.gold3,
                R.color.gold4,
                R.color.gold5,
                R.color.gold6
        ))
        val all = arrayListOf(
                yinyang,
                night,
                cosmic,
                elephant,
                oldskool,
                gold,
                p9,
                viridis,
                plasma,
                magma,
                vascular,
                flora,
                royal,
                groovy,
                canyon,
                anubis
        )

        fun getColors(res: Resources, ids: List<Int>) : IntArray {
            // takes color resource ids as input and returns color ints
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) IntArray(ids.size) { i: Int -> res.getColor(ids[i], null) }
            else IntArray(ids.size) { i: Int -> res.getColor(ids[i]) }
        }
        fun intToFloatArray(c: Int) : FloatArray {
            return floatArrayOf(Color.red(c)/255f, Color.green(c)/255f, Color.blue(c)/255f)
        }

    }


    var size = if (oscillate) 2*ids.size - 1 else ids.size + 1

    val oscillateInit = oscillate

    var oscillate = oscillate
        set (value) {
            field = value
            size = if (oscillate) 2*ids.size - 1 else ids.size + 1
        }

    var thumbnail : Bitmap? = null


    fun reset() {
        oscillate = oscillateInit
    }
    fun getFlatPalette(res: Resources) : FloatArray {

        val palette = intArrayToList(getColors(res,
                if (oscillate) ids.minus(ids.last()).plus(ids.reversed())
                else ids.plus(ids.first())
        ))

        return FloatArray(palette.size * 3) { i: Int ->
            val a = floor(i / 3.0f).toInt()
            val b = i % 3
            palette[a][b]
        }

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