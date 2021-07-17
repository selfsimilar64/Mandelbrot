package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
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
fun RGBToColor(r: Float, g: Float, b: Float) : Int {
    return RGBToColor((r*255).toInt(), (g*255).toInt(), (b*255).toInt())
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

class Palette (

        var id                  : Int = -1,
        var hasCustomId         : Boolean = false,
        val nameId              : Int = -1,
        override var name       : String = "",
        private val arrayId     : Int = -1,
        var colors              : ArrayList<Int> = arrayListOf(),
        oscillate               : Boolean = true,
        override var isFavorite : Boolean = false

) : Customizable {


    companion object {
        
        const val MAX_CUSTOM_COLORS_GOLD = 12
        const val MAX_CUSTOM_COLORS_FREE = 3
        const val MAX_CUSTOM_PALETTES_FREE = 2

        val emptyFavorite = Palette(name = "Empty Favorite", arrayId = R.array.empty_array)
        val emptyCustom = Palette(name = "Empty Custom", arrayId = R.array.empty_array)

        val yinyang         = Palette( id = 0, nameId = R.string.yinyang,         arrayId = R.array.yinyang                               )
        val peacock         = Palette( id = 1, nameId = R.string.peacock,          arrayId = R.array.peacock                               )
        val vascular        = Palette( id = 2, nameId = R.string.vascular,         arrayId = R.array.vascular                              )
        val flora           = Palette( id = 3, nameId = R.string.flora,            arrayId = R.array.flora                                 )
        val royal           = Palette( id = 4, nameId = R.string.royal,            arrayId = R.array.royal                                 )
        val peach           = Palette( id = 5, nameId = R.string.peach,            arrayId = R.array.peach                                 )
        val canyon          = Palette( id = 6, nameId = R.string.canyon,           arrayId = R.array.canyon                                )
        val anubis          = Palette( id = 7, nameId = R.string.anubis,           arrayId = R.array.anubis                                )
        val viridis         = Palette( id = 8, nameId = R.string.viridis,          arrayId = R.array.viridis                               )
        val plasma          = Palette( id = 9, nameId = R.string.plasma,           arrayId = R.array.plasma                                )
        val magma           = Palette( id = 10, nameId = R.string.magma,            arrayId = R.array.magma                                 )
        val night           = Palette( id = 11, nameId = R.string.night,            arrayId = R.array.night                                 )
        val cosmic          = Palette( id = 12, nameId = R.string.cosmic,           arrayId = R.array.cosmic                                )
        val oldskool        = Palette( id = 13, nameId = R.string.oldskool,        arrayId = R.array.oldskool                              )
        val elephant        = Palette( id = 14, nameId = R.string.elephant,         arrayId = R.array.elephant                              )
        val bronze          = Palette( id = 15, nameId = R.string.bronze,           arrayId = R.array.bronze                                )
        val gold            = Palette( id = 16, nameId = R.string.gold,             arrayId = R.array.gold                                  )
        val backwards       = Palette( id = 17, nameId = R.string.backwards,        arrayId = R.array.backwards                             )
        val slow            = Palette( id = 18, nameId = R.string.slow,             arrayId = R.array.slow                                  )
        val alpha           = Palette( id = 19, nameId = R.string.alpha,            arrayId = R.array.alpha                                 )
        val jazz            = Palette( id = 20, nameId = R.string.jazz,             arrayId = R.array.jazz                                  )
        val chroma          = Palette( id = 21, nameId = R.string.chroma,           arrayId = R.array.chroma                                )
        val island          = Palette( id = 22, nameId = R.string.island,           arrayId = R.array.island                                )
        val bioluminescent  = Palette( id = 23, nameId = R.string.bioluminescent,   arrayId = R.array.bioluminescent                        )
        val kingfisher      = Palette( id = 24, nameId = R.string.kingfisher,       arrayId = R.array.kingfisher                            )
        val polygon         = Palette( id = 25, nameId = R.string.polygon,          arrayId = R.array.polygon                               )
        val rose            = Palette( id = 26, nameId = R.string.rose,             arrayId = R.array.rose                                  )
        val fossil          = Palette( id = 27, nameId = R.string.fossil,           arrayId = R.array.fossil                                )
        val atlas           = Palette( id = 28, nameId = R.string.atlas,            arrayId = R.array.atlas                                 )
        val time            = Palette( id = 29, nameId = R.string.time,             arrayId = R.array.time                                  )
        val overgrown       = Palette( id = 30, nameId = R.string.overgrown,        arrayId = R.array.overgrown                             )
        val starling        = Palette( id = 31, nameId = R.string.starling,         arrayId = R.array.starling                              )
        val pagoda          = Palette( id = 32, nameId = R.string.pagoda,           arrayId = R.array.pagoda                                )
        val melted          = Palette( id = 33, nameId = R.string.melted,           arrayId = R.array.melted                                )
        val torus           = Palette( id = 34, nameId = R.string.torus,            arrayId = R.array.torus,            oscillate = false   )
        val ruby            = Palette( id = 35, nameId = R.string.ruby,             arrayId = R.array.ruby                                  )
        val sapphire        = Palette( id = 36, nameId = R.string.sapphire,         arrayId = R.array.sapphire                              )
        val amethyst        = Palette( id = 37, nameId = R.string.amethyst,         arrayId = R.array.amethyst                              )
        val diamond         = Palette( id = 38, nameId = R.string.diamond,          arrayId = R.array.diamond                               )
        val aztec           = Palette( id = 39, nameId = R.string.aztec,            arrayId = R.array.aztec                                 )
        val neon            = Palette( id = 40, nameId = R.string.neon,             arrayId = R.array.neon                                  )
        val dragon          = Palette( id = 41, nameId = R.string.dragon,           arrayId = R.array.dragon                                )
        val coral           = Palette( id = 42, nameId = R.string.coral,            arrayId = R.array.coral                                 )
        val cactus          = Palette( id = 43, nameId = R.string.cactus,           arrayId = R.array.cactus                                )
        val aquamarine      = Palette( id = 44, nameId = R.string.aquamarine,       arrayId = R.array.aquamarine                            )
        val parachute       = Palette( id = 45, nameId = R.string.parachute,        arrayId = R.array.parachute                             )
        val polyphonic      = Palette( id = 46, nameId = R.string.polyphonic,       arrayId = R.array.polyphonic2,      oscillate = false   )
        val anaglyph        = Palette( id = 47, nameId = R.string.anaglyph,         arrayId = R.array.anaglyph                              )
        val catalyst        = Palette( id = 48, nameId = R.string.catalyst,         arrayId = R.array.catalyst                              )
        val monster         = Palette( id = 49, nameId = R.string.monster,          arrayId = R.array.monster                               )
        val carousel        = Palette( id = 50, nameId = R.string.carousel,         arrayId = R.array.carousel,         oscillate = false   )
        val refraction      = Palette( id = 51, nameId = R.string.refraction,       arrayId = R.array.refraction2,      oscillate = false   )
        val fusion          = Palette( id = 52, nameId = R.string.fusion,           arrayId = R.array.fusion,           oscillate = false   )
        val honor           = Palette( id = 53, nameId = R.string.honor,            arrayId = R.array.honor                                 )
        val p9              = Palette( id = 54, nameId = R.string.p9,               arrayId = R.array.p9,               oscillate = false   )
        val sphere          = Palette( id = 56, nameId = R.string.sphere,           arrayId = R.array.sphere                                )
        val who             = Palette( id = 57, nameId = R.string.who,              arrayId = R.array.who,              oscillate = false   )
        val void            = Palette( id = 58, nameId = R.string.string_void,             arrayId = R.array.array_void                            )
        val eye             = Palette( id = 59, nameId = R.string.eye,             arrayId = R.array.eye                            )



        var nextCustomPaletteNum = 0
        val custom = arrayListOf<Palette>()
        val default = arrayListOf(
                eye,
                yinyang,
                who,
                parachute,
                night,
                torus,
                sphere,
                time,
                polyphonic,
                carousel,
                coral,
                honor,
                overgrown,
                catalyst,
                refraction,
                starling,
                pagoda,
                island,
                aquamarine,
                fusion,
                cactus,
                p9,
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

    init {
        if (!hasCustomId && id != -1) id = Integer.MAX_VALUE - id
    }


    override val goldFeature: Boolean = false

    val size : Int
        get() = if (oscillate) 2*colors.size - 1 else colors.size + 1
        // 2*ids.size - 1
        // ids.size + 1

    val oscillateInit = oscillate

    var oscillate = oscillateInit

    override var thumbnail : Bitmap? = null

    val gradientDrawable : GradientDrawable
        get() = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors.toIntArray())
    var flatPalette = floatArrayOf()


    fun getColor(t: Float) : Int {
        return when (t) {
            0f -> colors.first()
            1f -> colors.last()
            else -> {
                val n = floor(t * (colors.size - 1)).toInt()
                Log.d("PALETTE", "getting color at t= $t, n= $n, size= ${colors.size}")
                val m = t * (colors.size - 1) % 1f
                val c1 = colorToRGB(colors[n])
                val c2 = colorToRGB(colors[n + 1])
                RGBToColor(
                        m*c1[0] + (1f - m)*c2[0],
                        m*c1[1] + (1f - m)*c2[1],
                        m*c1[2] + (1f - m)*c2[2]
                )
            }
        }
    }
    fun getColors(n: Int) : ArrayList<Int> {
        val list = arrayListOf<Int>()
        for (i in 0 until n) list.add(getColor(i.toFloat()/(n - 1)))
        return list
    }

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
    fun release() {
        thumbnail?.recycle()
        thumbnail = null
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
                id = if (hasCustomId) id else 0,
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
        return other is Palette && other.id == id
    }
    override fun hashCode(): Int { return name.hashCode() }

    override fun isCustom() : Boolean = hasCustomId

    fun clone(res: Resources) : Palette {
        return Palette(
                name = name + " " + res.getString(R.string.copy),
                arrayId = arrayId,
                colors = if (hasCustomId || colors.size <= MAX_CUSTOM_COLORS_GOLD) ArrayList(colors) else getColors(MAX_CUSTOM_COLORS_GOLD)
        ).also { it.initialize(res) }
    }

}