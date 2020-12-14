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
        val nameId       : Int = -1,
        var name         : String = "",
        private val ids  : List<Int> = listOf(),
        var colors       : ArrayList<Int> = arrayListOf(),
        oscillate        : Boolean = true,
        var customId     : Int = -1,
        var isFavorite   : Boolean = false
) {


    companion object {

        const val MAX_CUSTOM_COLORS_GOLD = 12
        const val MAX_CUSTOM_COLORS_FREE = 3
        const val MAX_CUSTOM_PALETTES_FREE = 2

        val emptyFavorite = ColorPalette(name = "Empty Favorite", ids = listOf(R.color.black))
        val emptyCustom = ColorPalette(name = "Empty Custom", ids = listOf(R.color.black))
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
        val peach = ColorPalette(
                nameId = R.string.peach,
                ids = listOf(
                R.color.peach1,
                R.color.peach2,
                R.color.peach3,
                R.color.peach4,
                R.color.peach5
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
//        val anubis = ColorPalette(
//                nameId = R.string.anubis,
//                ids = listOf(
//                R.color.black,
//                R.color.purple2,
//                R.color.mint,
//                R.color.yellowish1,
//                R.color.q10,
//                R.color.tangerine
//        ))

        val anubis = ColorPalette(
                nameId = R.string.anubis,
                ids = listOf(
                        R.color.anubis1,
                        R.color.anubis2,
                        R.color.anubis3,
                        R.color.anubis4,
                        R.color.anubis5,
                        R.color.anubis6,
                        R.color.anubis7,
                        R.color.anubis8,
                        R.color.anubis9,
                        R.color.anubis10,
                        R.color.anubis11,
                        R.color.anubis12,
                        R.color.anubis13,
                        R.color.anubis14
                )
        )

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
        val bronze = ColorPalette(
                nameId = R.string.bronze,
                ids = listOf(
                        R.color.bronze1,
                        R.color.bronze2,
                        R.color.bronze3,
                        R.color.bronze4,
                        R.color.bronze5,
                        R.color.bronze6,
                        R.color.bronze7
                )
        )
        val gold = ColorPalette(
                nameId = R.string.gold,
                ids = listOf(
                R.color.gold8,
                R.color.gold7,
                R.color.gold6,
                R.color.gold5,
                R.color.gold4,
                R.color.gold3,
                R.color.gold2,
                R.color.gold1
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
        val alpha = ColorPalette(
                nameId = R.string.alpha,
                ids = listOf(
                        R.color.alpha1,
                        R.color.alpha2,
                        R.color.alpha3,
                        R.color.alpha4,
                        R.color.alpha5,
                        R.color.alpha6,
                        R.color.alpha7
                )
        )
        val jazz = ColorPalette(
                nameId = R.string.jazz,
                ids = listOf(
                        R.color.jazz1,
                        R.color.jazz2,
                        R.color.jazz3,
                        R.color.jazz4,
                        R.color.jazz5,
                        R.color.jazz6
                )
        )
        val chroma = ColorPalette(
                nameId = R.string.chroma,
                ids = listOf(
                        R.color.chroma1,
                        R.color.chroma2,
                        R.color.chroma3,
                        R.color.chroma4,
                        R.color.chroma5,
                        R.color.chroma6,
                        R.color.chroma7,
                        R.color.chroma8,
                        R.color.chroma9,
                        R.color.chroma10,
                        R.color.chroma11,
                        R.color.chroma12
                )
        )
        val island = ColorPalette(
                nameId = R.string.island,
                ids = listOf(
                        R.color.island1,
                        R.color.island2,
                        R.color.island3,
                        R.color.island4,
                        R.color.island5,
                        R.color.island6,
                        R.color.island7,
                        R.color.island8,
                        R.color.island9
                )
        )
        val bioluminescent = ColorPalette(
                nameId = R.string.bioluminescent,
                ids = listOf(
                        R.color.bioluminescent1,
                        R.color.bioluminescent2,
                        R.color.bioluminescent3,
                        R.color.bioluminescent4,
                        R.color.bioluminescent5,
                        R.color.bioluminescent6
                )
        )
        val kingfisher = ColorPalette(
                nameId = R.string.kingfisher,
                ids = listOf(
                        R.color.kingfisher1,
                        R.color.kingfisher2,
                        R.color.kingfisher3,
                        R.color.kingfisher4,
                        R.color.kingfisher5,
                        R.color.kingfisher6,
                        R.color.kingfisher7,
                        R.color.kingfisher8
                )
        )
        val polygon = ColorPalette(
                nameId = R.string.polygon,
                ids = listOf(
                        R.color.polygon1,
                        R.color.polygon2,
                        R.color.polygon3,
                        R.color.polygon4
                )
        )
        val rose = ColorPalette(
                nameId = R.string.rose,
                ids = listOf(
                        R.color.rose1,
                        R.color.rose2,
                        R.color.rose3,
                        R.color.rose4,
                        R.color.rose5,
                        R.color.rose6
                )
        )
        val fossil = ColorPalette(
                nameId = R.string.fossil,
                ids = listOf(
                        R.color.fossil1,
                        R.color.fossil2,
                        R.color.fossil3,
                        R.color.fossil4,
                        R.color.fossil5,
                        R.color.fossil6
                )
        )
        val atlas = ColorPalette(
                nameId = R.string.atlas,
                ids = listOf(
                        R.color.atlas3,
                        R.color.atlas4,
                        R.color.atlas5,
                        R.color.atlas6,
                        R.color.atlas7,
                        R.color.atlas8,
                        R.color.atlas9,
                        R.color.atlas10,
                        R.color.atlas11,
                        R.color.atlas12
                )
        )
        val time = ColorPalette(
                nameId = R.string.time,
                ids = listOf(
                        R.color.time1,
                        R.color.time2,
                        R.color.time3,
                        R.color.time4,
                        R.color.time5,
                        R.color.time6,
                        R.color.time7,
                        R.color.time8,
                        R.color.time9,
                        R.color.time10,
                        R.color.time11,
                        R.color.time12
                )
        )
        val overgrown = ColorPalette(
                nameId = R.string.overgrown,
                ids = listOf(
                        R.color.overgrown1,
                        R.color.overgrown2,
                        R.color.overgrown3,
                        R.color.overgrown4,
                        R.color.overgrown5,
                        R.color.overgrown6,
                        R.color.overgrown7
                )
        )
        val starling = ColorPalette(
                nameId = R.string.starling,
                ids = listOf(
                        R.color.starling1,
                        R.color.starling2,
                        R.color.starling3,
                        R.color.starling4,
                        R.color.starling5,
                        R.color.starling6,
                        R.color.starling7,
                        R.color.starling8,
                        R.color.starling9
                )
        )
        val pagoda = ColorPalette(
                nameId = R.string.pagoda,
                ids = listOf(
                        R.color.pagoda1,
                        R.color.pagoda2,
                        R.color.pagoda3,
                        R.color.pagoda4,
                        R.color.pagoda5
                )
        )
        val melted = ColorPalette(
                nameId = R.string.melted,
                ids = listOf(
                        R.color.melted1,
                        R.color.melted2,
                        R.color.melted3,
                        R.color.melted4,
                        R.color.melted5,
                        R.color.melted6,
                        R.color.melted7,
                        R.color.melted8,
                        R.color.melted9
                )
        )
        val torus = ColorPalette(
                nameId = R.string.torus,
                oscillate = false,
                ids = listOf(
                        R.color.torus1,
                        R.color.torus2,
                        R.color.torus3,
                        R.color.torus4,
                        R.color.torus5,
                        R.color.torus6,
                        R.color.torus7,
                        R.color.torus8,
                        R.color.torus9,
                        R.color.torus10,
                        R.color.torus11,
                        R.color.torus12
                )
        )
        val ruby = ColorPalette(
                nameId = R.string.ruby,
                ids = listOf(
                        R.color.ruby1,
                        R.color.ruby2,
                        R.color.ruby3,
                        R.color.ruby4,
                        R.color.ruby6,
                        R.color.ruby7,
                        R.color.ruby8
                )
        )
        val sapphire = ColorPalette(
                nameId = R.string.sapphire,
                ids = listOf(
                        R.color.sapphire1,
                        R.color.sapphire2,
                        R.color.sapphire3,
                        R.color.sapphire4,
                        R.color.sapphire5,
                        R.color.sapphire6,
                        R.color.sapphire7
                )
        )
        val amethyst = ColorPalette(
                nameId = R.string.amethyst,
                ids = listOf(
                        R.color.amethyst1,
                        R.color.amethyst2,
                        R.color.amethyst3,
                        R.color.amethyst4,
                        R.color.amethyst5,
                        R.color.amethyst6,
                        R.color.amethyst7
                )
        )
        val diamond = ColorPalette(
                nameId = R.string.diamond,
                ids = listOf(
                        R.color.diamond1,
                        R.color.diamond3,
                        R.color.diamond4,
                        R.color.diamond5,
                        R.color.diamond6,
                        R.color.diamond7,
                        R.color.diamond8
                )
        )
        val aztec = ColorPalette(
                nameId = R.string.aztec,
                ids = listOf(
                        R.color.aztec1,
                        R.color.aztec2,
                        R.color.aztec3,
                        R.color.aztec4,
                        R.color.aztec5,
                        R.color.aztec6,
                        R.color.aztec7,
                        R.color.aztec8,
                        R.color.aztec9,
                        R.color.aztec10,
                        R.color.aztec11,
                        R.color.aztec12,
                        R.color.aztec13
                )
        )
        val neon = ColorPalette(
                nameId = R.string.neon,
                ids = listOf(
                        R.color.neon1,
                        R.color.neon2,
                        R.color.neon3,
                        R.color.neon4,
                        R.color.neon5,
                        R.color.neon6
                )
        )
        val dragon = ColorPalette(
                nameId = R.string.dragon,
                ids = listOf(
                        R.color.dragon1,
                        R.color.dragon2,
                        R.color.dragon3,
                        R.color.dragon4,
                        R.color.dragon5,
                        R.color.dragon6,
                        R.color.dragon7,
                        R.color.dragon8,
                        R.color.dragon9,
                        R.color.dragon10,
                        R.color.dragon11
                )
        )
        val coral = ColorPalette(
                nameId = R.string.coral,
                ids = listOf(
                        R.color.ccoral1,
                        R.color.ccoral2,
                        R.color.ccoral3,
                        R.color.ccoral4,
                        R.color.ccoral5,
                        R.color.ccoral6,
                        R.color.ccoral7,
                        R.color.ccoral8,
                        R.color.ccoral9,
                        R.color.ccoral10,
                        R.color.ccoral11,
                        R.color.ccoral12,
                        R.color.ccoral13
                )
        )
        val cactus = ColorPalette(
                nameId = R.string.cactus,
                ids = listOf(
                        R.color.cactus1,
                        R.color.cactus2,
                        R.color.cactus3,
                        R.color.cactus4,
                        R.color.cactus5,
                        R.color.cactus6,
                        R.color.cactus7,
                        R.color.cactus8,
                        R.color.cactus9,
                        R.color.cactus10,
                        R.color.cactus11,
                        R.color.cactus12,
                        R.color.cactus13,
                        R.color.cactus14,
                        R.color.cactus15
                )
        )
        val aquamarine = ColorPalette(
                nameId = R.string.aquamarine,
                ids = listOf(
                        R.color.aquamarine1,
                        R.color.aquamarine2,
                        R.color.aquamarine3,
                        R.color.aquamarine4,
                        R.color.aquamarine5,
                        R.color.aquamarine6,
                        R.color.aquamarine7,
                        R.color.aquamarine8,
                        R.color.aquamarine9,
                        R.color.aquamarine10,
                        R.color.aquamarine11,
                        R.color.aquamarine12,
                        R.color.aquamarine13
                )
        )
        val parachute = ColorPalette(
                nameId = R.string.parachute,
                ids = listOf(
                        R.color.parachute4,
                        R.color.parachute5,
                        R.color.parachute6,
                        R.color.parachute7,
                        R.color.parachute8,
                        R.color.parachute9,
                        R.color.parachute10,
                        R.color.parachute11,
                        R.color.parachute12,
                        R.color.parachute13,
                        R.color.parachute14
                )
        )
        val polyphonic = ColorPalette(
                nameId = R.string.polyphonic,
                ids = listOf(
                        R.color.polyphonic1,
                        R.color.polyphonic2,
                        R.color.polyphonic3,
                        R.color.polyphonic4,
                        R.color.polyphonic5,
                        R.color.polyphonic6,
                        R.color.polyphonic7,
                        R.color.polyphonic8,
                        R.color.polyphonic9,
                        R.color.polyphonic10,
                        R.color.polyphonic11,
                        R.color.polyphonic12
                ),
                oscillate = false
        )
        val anaglyph = ColorPalette(
                nameId = R.string.anaglyph,
                ids = listOf(
                        R.color.anaglyph1,
                        R.color.anaglyph2,
                        R.color.anaglyph3,
                        R.color.anaglyph4,
                        R.color.anaglyph5
                )
        )
        val catalyst = ColorPalette(
                nameId = R.string.catalyst,
                ids = listOf(
                        R.color.catalyst1,
                        R.color.catalyst2,
                        R.color.catalyst3,
                        R.color.catalyst4,
                        R.color.catalyst5,
                        R.color.catalyst6,
                        R.color.catalyst7,
                        R.color.catalyst8,
                        R.color.catalyst9
                )
        )

        var nextCustomPaletteNum = 0
        val custom = arrayListOf<ColorPalette>()
        val default = arrayListOf(
                yinyang,
                parachute,
                night,
                torus,
                time,
                polyphonic,
                coral,
                overgrown,
                catalyst,
                starling,
                pagoda,
                island,
                aquamarine,
                cactus,
                chroma,
                rose,
                kingfisher,
                anaglyph,
                melted,
                dragon,
                bioluminescent,
                ruby,
                sapphire,
                amethyst,
                aztec,
                atlas,
                fossil,
                cosmic,
                peacock,
                backwards,
                alpha,
                peach,
                jazz,
                polygon,
                elephant,
                oldskool,
                bronze,
                gold,
                neon,
                viridis,
                plasma,
                magma,
                vascular,
                flora,
                royal,
                canyon,
                slow,
                anubis
        )
        val all = ArrayList(default)


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


    val hasCustomId : Boolean
        get() = customId != -1

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
    fun toDatabaseEntity() : ColorPaletteEntity {
        return ColorPaletteEntity(
                id = if (hasCustomId) customId else 0,
                name = name,
                size = colors.size,
                c1 = colors.getOrNull(0) ?: 0,
                c2 = colors.getOrNull(1) ?: 0,
                c3 = colors.getOrNull(2) ?: 0,
                c4 = colors.getOrNull(3) ?: 0,
                c5 = colors.getOrNull(4) ?: 0,
                c6 = colors.getOrNull(5) ?: 0,
                c7 = colors.getOrNull(6) ?: 0,
                c8 = colors.getOrNull(7) ?: 0,
                c9 = colors.getOrNull(7) ?: 0,
                c10 = colors.getOrNull(7) ?: 0,
                c11 = colors.getOrNull(7) ?: 0,
                c12 = colors.getOrNull(7) ?: 0,
                starred = isFavorite
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is ColorPalette && other.name == name
    }
    override fun hashCode(): Int { return name.hashCode() }

}