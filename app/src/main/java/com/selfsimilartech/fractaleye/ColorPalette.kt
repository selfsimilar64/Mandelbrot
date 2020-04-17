package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import java.lang.Math.random
import kotlin.math.floor


fun FloatArray.mult(s: Float) : FloatArray {
    return FloatArray(this.size) {i: Int -> s*this[i]}
}
fun FloatArray.invert() : FloatArray {
    return FloatArray(this.size) {i: Int -> 1.0f - this[i]}
}
fun colorToRGB(c: Int) : FloatArray {
    return floatArrayOf(
            Color.red(c)   / 255f,
            Color.green(c) / 255f,
            Color.blue(c)  / 255f
    )
}
fun RGBToColor(r: Int, g: Int, b: Int) : Int {
    val hsv = FloatArray(3)
    Color.RGBToHSV(r, g, b, hsv)
    return Color.HSVToColor(hsv)
}
fun randomColor() : Int {
    return Color.HSVToColor(floatArrayOf(
            360*random().toFloat(),
            random().toFloat(),
            random().toFloat()
    ))
}

fun Int.toHSV() : FloatArray {
    val hsv = floatArrayOf(0f, 0f, 0f)
    Color.colorToHSV(this, hsv)
    return hsv
}
fun Int.hue() : Float {
    return toHSV()[0]
}
fun Int.sat() : Float {
    return toHSV()[1]
}
fun Int.value() : Float {
    return toHSV()[2]
}

class ColorPalette (
        val nameId: Int = -1,
        var name: String = "",
        private val ids: List<Int> = listOf(),
        var colors: ArrayList<Int> = arrayListOf(),
        oscillate: Boolean = true,
        val isCustom: Boolean = false
) {


    companion object {

        const val MAX_CUSTOM_PALETTE_COLORS = 6

        val yinyang = ColorPalette(
                nameId = R.string.yinyang,
                ids = listOf(
                R.color.black,
                R.color.white
        ))
        val peacock = ColorPalette(
                nameId = R.string.peacock,
                ids = listOf(
                R.color.peacock1,
                R.color.peacock2,
                R.color.peacock3,
                R.color.peacock4,
                R.color.peacock5,
                R.color.peacock6,
                R.color.peacock7,
                R.color.peacock8,
                R.color.peacock9,
                R.color.peacock10,
                R.color.peacock11,
                R.color.peacock12
        ))
        val p1 = ColorPalette(
                nameId = R.string.empty,
                ids = listOf(
                R.color.white,
                R.color.purple1,
                R.color.black,
                R.color.deepred,
                R.color.white
        ))
        val p3 = ColorPalette(
                nameId = R.string.empty,
                ids = listOf(
                R.color.q1,
                R.color.q2,
                R.color.white,
                R.color.q3
        ))
        val vascular = ColorPalette(
                nameId = R.string.vascular,
                ids = listOf(
                R.color.black,
                R.color.purple3,
                R.color.deepred,
                R.color.yellowish1,
                R.color.darkblue2
        ))
        val flora = ColorPalette(
                nameId = R.string.flora,
                ids = listOf(
                R.color.flora1,
                R.color.flora2,
                R.color.flora3,
                R.color.flora4,
                R.color.flora5,
                R.color.flora6
        ))
        val royal = ColorPalette(
                nameId = R.string.royal,
                ids = listOf(
                R.color.yellowish1,
                R.color.darkblue1,
                R.color.softgreen2,
                R.color.purple3,
                R.color.maroon
        ))
        val groovy = ColorPalette(
                nameId = R.string.groovy,
                ids = listOf(
                R.color.black,
                R.color.q4,
                R.color.q5,
                R.color.q6
        ))
        val canyon = ColorPalette(
                nameId = R.string.canyon,
                ids = listOf(
                R.color.deepred2,
                R.color.q6,
                R.color.q8,
                R.color.q9,
                R.color.purple3
        ))
        val anubis = ColorPalette(
                nameId = R.string.anubis,
                ids = listOf(
                R.color.black,
                R.color.purple2,
                R.color.mint,
                R.color.yellowish1,
                R.color.q10,
                R.color.tangerine
        ))
        val p9 = ColorPalette(
                nameId = R.string.p9,
                ids = listOf(
                R.color.q12,
                R.color.q13,
                R.color.q14,
                R.color.q15,
                R.color.q16,
                R.color.q17,
                R.color.q18
        ))
        val viridis = ColorPalette(
                nameId = R.string.viridis,
                ids = listOf(
                R.color.q19,
                R.color.q20,
                R.color.q21,
                R.color.q22,
                R.color.q23,
                R.color.q24
        ))
        val plasma = ColorPalette(
                nameId = R.string.plasma,
                ids = listOf(
                R.color.q25,
                R.color.q26,
                R.color.q27,
                R.color.q28,
                R.color.q29,
                R.color.q30,
                R.color.q31
        ))
        val inferno = ColorPalette(
                nameId = R.string.empty,
                ids = listOf(
                R.color.q32,
                R.color.q33,
                R.color.q34,
                R.color.q35,
                R.color.q36,
                R.color.q37
        ))
        val magma = ColorPalette(
                nameId = R.string.magma,
                ids = listOf(
                R.color.q38,
                R.color.q39,
                R.color.q40,
                R.color.q41,
                R.color.q42,
                R.color.q43
        ))
        val night = ColorPalette(
                nameId = R.string.night,
                ids = listOf(
                R.color.night1,
                R.color.night2,
                R.color.night3,
                R.color.night4,
                R.color.night5,
                R.color.night6,
                R.color.night7
        ))
        val cosmic = ColorPalette(
                nameId = R.string.cosmic,
                ids = listOf(
                R.color.cosmic1,
                R.color.cosmic2,
                R.color.cosmic3,
                R.color.cosmic4,
                R.color.cosmic5,
                R.color.cosmic6,
                R.color.cosmic7,
                R.color.cosmic8,
                R.color.cosmic9,
                R.color.cosmic10
        ))
        val oldskool = ColorPalette(
                nameId = R.string.oldskool,
                ids = listOf(
                R.color.oldskool2,
                R.color.oldskool3,
                R.color.oldskool4,
                R.color.oldskool5
        ))
        val elephant = ColorPalette(
                nameId = R.string.elephant,
                ids = listOf(
                R.color.elephant1,
                R.color.elephant2,
                R.color.elephant3,
                R.color.elephant4,
                R.color.elephant5,
                R.color.elephant6,
                R.color.elephant7
        ))
        val gold = ColorPalette(
                nameId = R.string.gold,
                ids = listOf(
                R.color.gold1,
                R.color.gold2,
                R.color.gold3,
                R.color.gold4,
                R.color.gold5,
                R.color.gold6
        ))
        val clover = ColorPalette(
                nameId = R.string.empty,
                ids = listOf(
                R.color.clover1,
                R.color.clover2,
                R.color.clover3,
                R.color.clover4,
                R.color.clover5
        ))
        val backwards = ColorPalette(
                nameId = R.string.backwards,
                ids = listOf(
                        R.color.backwards1,
                        R.color.backwards2,
                        R.color.backwards3,
                        R.color.backwards4,
                        R.color.backwards5,
                        R.color.backwards6,
                        R.color.backwards7
                )
        )
        val slow = ColorPalette(
                nameId = R.string.slow,
                ids = listOf(
                        R.color.slow1,
                        R.color.slow2,
                        R.color.slow3,
                        R.color.slow4,
                        R.color.slow5,
                        R.color.slow6,
                        R.color.slow7,
                        R.color.slow8,
                        R.color.slow9
                )
        )
        val all = arrayListOf(
                yinyang,
                night,
                cosmic,
                peacock,
                backwards,
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
                slow,
                anubis
        )


        fun generateColors(n: Int) : ArrayList<Int> {
            return ArrayList(List(n) { Color.HSVToColor(floatArrayOf(
                    360*random().toFloat(),
                    random().toFloat(),
                    random().toFloat()
            ))})
        }
        fun generateHighlightColors(n: Int) : ArrayList<Int> {
            return ArrayList(List(n) { i -> Color.HSVToColor(floatArrayOf(
                    360*random().toFloat(),
                    (i+2).toFloat()/(n+5).toFloat(),
                    i.toFloat()/n.toFloat()
            ))})
        }


    }


    val size : Int
        get() = if (oscillate) 2*colors.size - 1 else colors.size + 1
        // 2*ids.size - 1
        // ids.size + 1

    val oscillateInit = oscillate

    var oscillate = oscillateInit

    var thumbnail : Bitmap? = null

    val gradientDrawable : GradientDrawable
        get() = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors.toIntArray())
    var flatPalette = floatArrayOf()




    fun initialize(res: Resources, thumbRes: Point) {

        when {
            ids.isEmpty() && colors.isEmpty() -> {
                throw Error("neither ids nor colors was passed to the constructor")
            }
            colors.isEmpty() -> {
                colors = ArrayList(getColors(res, ids).toMutableList())
            }
        }

        when {
            nameId == -1 && name == "" -> {
                throw Error("neither nameId nor name was passed to the constructor")
            }
            name == "" -> {
                name = res.getString(nameId)
            }
        }

        thumbnail = Bitmap.createBitmap(thumbRes.x, thumbRes.x, Bitmap.Config.ARGB_8888)

        updateFlatPalette()

    }
    fun reset() {
        oscillate = oscillateInit
    }
    fun updateFlatPalette() {

        val palette = intArrayToList(
                if (oscillate) colors.minus(colors.last()).plus(colors.reversed())
                else colors.plus(colors.first())
        )

        flatPalette = FloatArray(palette.size * 3) { i: Int ->
            val a = floor(i / 3.0f).toInt()
            val b = i % 3
            palette[a][b]
        }

    }
    private fun intArrayToList(C: List<Int>) : List<FloatArray> {
        return List(C.size) { i: Int -> colorToRGB(C[i]) }
    }

    override fun equals(other: Any?): Boolean {
        return other is ColorPalette && other.name == name
    }
    override fun hashCode(): Int { return name.hashCode() }

}