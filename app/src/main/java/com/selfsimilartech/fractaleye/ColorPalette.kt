package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import java.lang.Math.random


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
        val nameId              : Int = -1,
        var name                : String = "",
        private val arrayId     : Int = -1,
        var colors              : ArrayList<Int> = arrayListOf(),
        oscillate               : Boolean = true,
        var customId            : Int = -1,
        var isFavorite          : Boolean = false
) {


    companion object {

        const val MAX_CUSTOM_COLORS_GOLD = 12
        const val MAX_CUSTOM_COLORS_FREE = 3
        const val MAX_CUSTOM_PALETTES_FREE = 2

        val emptyFavorite = ColorPalette(name = "Empty Favorite", arrayId = R.array.empty_array)
        val emptyCustom = ColorPalette(name = "Empty Custom", arrayId = R.array.empty_array)

        val yinyang         = ColorPalette( nameId = R.string.yinyang,          arrayId = R.array.yinyang                               )
        val peacock         = ColorPalette( nameId = R.string.peacock,          arrayId = R.array.peacock                               )
        val vascular        = ColorPalette( nameId = R.string.vascular,         arrayId = R.array.vascular                              )
        val flora           = ColorPalette( nameId = R.string.flora,            arrayId = R.array.flora                                 )
        val royal           = ColorPalette( nameId = R.string.royal,            arrayId = R.array.royal                                 )
        val peach           = ColorPalette( nameId = R.string.peach,            arrayId = R.array.peach                                 )
        val canyon          = ColorPalette( nameId = R.string.canyon,           arrayId = R.array.canyon                                )
        val anubis          = ColorPalette( nameId = R.string.anubis,           arrayId = R.array.anubis                                )
        val viridis         = ColorPalette( nameId = R.string.viridis,          arrayId = R.array.viridis                               )
        val plasma          = ColorPalette( nameId = R.string.plasma,           arrayId = R.array.plasma                                )
        val magma           = ColorPalette( nameId = R.string.magma,            arrayId = R.array.magma                                 )
        val night           = ColorPalette( nameId = R.string.night,            arrayId = R.array.night                                 )
        val cosmic          = ColorPalette( nameId = R.string.cosmic,           arrayId = R.array.cosmic                                )
        val oldskool        = ColorPalette( nameId = R.string.oldskool,         arrayId = R.array.oldskool                              )
        val elephant        = ColorPalette( nameId = R.string.elephant,         arrayId = R.array.elephant                              )
        val bronze          = ColorPalette( nameId = R.string.bronze,           arrayId = R.array.bronze                                )
        val gold            = ColorPalette( nameId = R.string.gold,             arrayId = R.array.gold                                  )
        val backwards       = ColorPalette( nameId = R.string.backwards,        arrayId = R.array.backwards                             )
        val slow            = ColorPalette( nameId = R.string.slow,             arrayId = R.array.slow                                  )
        val alpha           = ColorPalette( nameId = R.string.alpha,            arrayId = R.array.alpha                                 )
        val jazz            = ColorPalette( nameId = R.string.jazz,             arrayId = R.array.jazz                                  )
        val chroma          = ColorPalette( nameId = R.string.chroma,           arrayId = R.array.chroma                                )
        val island          = ColorPalette( nameId = R.string.island,           arrayId = R.array.island                                )
        val bioluminescent  = ColorPalette( nameId = R.string.bioluminescent,   arrayId = R.array.bioluminescent                        )
        val kingfisher      = ColorPalette( nameId = R.string.kingfisher,       arrayId = R.array.kingfisher                            )
        val polygon         = ColorPalette( nameId = R.string.polygon,          arrayId = R.array.polygon                               )
        val rose            = ColorPalette( nameId = R.string.rose,             arrayId = R.array.rose                                  )
        val fossil          = ColorPalette( nameId = R.string.fossil,           arrayId = R.array.fossil                                )
        val atlas           = ColorPalette( nameId = R.string.atlas,            arrayId = R.array.atlas                                 )
        val time            = ColorPalette( nameId = R.string.time,             arrayId = R.array.time                                  )
        val overgrown       = ColorPalette( nameId = R.string.overgrown,        arrayId = R.array.overgrown                             )
        val starling        = ColorPalette( nameId = R.string.starling,         arrayId = R.array.starling                              )
        val pagoda          = ColorPalette( nameId = R.string.pagoda,           arrayId = R.array.pagoda                                )
        val melted          = ColorPalette( nameId = R.string.melted,           arrayId = R.array.melted                                )
        val torus           = ColorPalette( nameId = R.string.torus,            arrayId = R.array.torus,            oscillate = false   )
        val ruby            = ColorPalette( nameId = R.string.ruby,             arrayId = R.array.ruby                                  )
        val sapphire        = ColorPalette( nameId = R.string.sapphire,         arrayId = R.array.sapphire                              )
        val amethyst        = ColorPalette( nameId = R.string.amethyst,         arrayId = R.array.amethyst                              )
        val diamond         = ColorPalette( nameId = R.string.diamond,          arrayId = R.array.diamond                               )
        val aztec           = ColorPalette( nameId = R.string.aztec,            arrayId = R.array.aztec                                 )
        val neon            = ColorPalette( nameId = R.string.neon,             arrayId = R.array.neon                                  )
        val dragon          = ColorPalette( nameId = R.string.dragon,           arrayId = R.array.dragon                                )
        val coral           = ColorPalette( nameId = R.string.coral,            arrayId = R.array.coral                                 )
        val cactus          = ColorPalette( nameId = R.string.cactus,           arrayId = R.array.cactus                                )
        val aquamarine      = ColorPalette( nameId = R.string.aquamarine,       arrayId = R.array.aquamarine                            )
        val parachute       = ColorPalette( nameId = R.string.parachute,        arrayId = R.array.parachute                             )
        val polyphonic      = ColorPalette( nameId = R.string.polyphonic,       arrayId = R.array.polyphonic,       oscillate = false   )
        val anaglyph        = ColorPalette( nameId = R.string.anaglyph,         arrayId = R.array.anaglyph                              )
        val catalyst        = ColorPalette( nameId = R.string.catalyst,         arrayId = R.array.catalyst                              )
        val monster         = ColorPalette( nameId = R.string.monster,        arrayId = R.array.monster                          )
        val carousel        = ColorPalette( nameId = R.string.carousel,         arrayId = R.array.carousel,         oscillate = false   )



        var nextCustomPaletteNum = 0
        val custom = arrayListOf<ColorPalette>()
        val default = arrayListOf(
                yinyang,
                parachute,
                night,
                torus,
                time,
                polyphonic,
                carousel,
                coral,
                overgrown,
                catalyst,
                starling,
                pagoda,
                island,
                aquamarine,
                cactus,
                chroma,
                monster,
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




    fun initialize(res: Resources) {

        when {
            arrayId == -1 && colors.isEmpty() -> {
                throw Error("neither ids nor colors was passed to the constructor")
            }
            colors.isEmpty() -> {
                colors = ArrayList(res.getIntArray(arrayId).asList())
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

        thumbnail = Bitmap.createBitmap(
                Resolution.THUMB.w,
                Resolution.THUMB.w,
                Bitmap.Config.RGB_565
        )

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

        flatPalette = palette.flatMap { a -> a.asIterable() }.toFloatArray()

    }
    private fun intArrayToList(C: List<Int>) : List<FloatArray> {
        return List(C.size) { i: Int -> colorToRGB(C[i]) }
    }
    fun toDatabaseEntity() : ColorPaletteEntity {
        return ColorPaletteEntity(
                id = if (hasCustomId) customId else 0,
                name = name,
                size = colors.size,
                c1 =  colors.getOrNull(0)  ?: 0,
                c2 =  colors.getOrNull(1)  ?: 0,
                c3 =  colors.getOrNull(2)  ?: 0,
                c4 =  colors.getOrNull(3)  ?: 0,
                c5 =  colors.getOrNull(4)  ?: 0,
                c6 =  colors.getOrNull(5)  ?: 0,
                c7 =  colors.getOrNull(6)  ?: 0,
                c8 =  colors.getOrNull(7)  ?: 0,
                c9 =  colors.getOrNull(8)  ?: 0,
                c10 = colors.getOrNull(9)  ?: 0,
                c11 = colors.getOrNull(10) ?: 0,
                c12 = colors.getOrNull(11) ?: 0,
                starred = isFavorite
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is ColorPalette && other.name == name
    }
    override fun hashCode(): Int { return name.hashCode() }

}