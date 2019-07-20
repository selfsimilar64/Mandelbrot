package com.example.matt.gputest

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.content.res.Configuration
import android.content.res.Resources
import android.database.DataSetObserver
import android.graphics.drawable.TransitionDrawable
import android.os.*
import javax.microedition.khronos.egl.EGLConfig
import android.support.v7.app.AppCompatActivity
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.opengl.GLES32 as GL
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteOrder
import java.nio.ByteBuffer.allocateDirect
import java.util.*
import javax.microedition.khronos.opengles.GL10
import java.nio.IntBuffer
import kotlin.math.*
import java.nio.ByteBuffer


const val SPLIT = 8193.0




data class DualDouble (
        var hi : Double,
        var lo : Double
) {

    override fun toString() : String {
        return "{$hi + $lo}"
    }

    private fun quickTwoSum(a: Double, b: Double) : DualDouble {
        val s = a + b
        val e = b - (s - a)
        return DualDouble(s, e)
    }

    private fun twoSum(a: Double, b: Double) : DualDouble {
        val s = a + b
        val v = s - a
        val e = a - (s - v) + (b - v)
        return DualDouble(s, e)
    }

    private fun split(a: Double): DualDouble {
        val t = a * SPLIT
        val aHi = t - (t - a)
        val aLo = a - aHi
        return DualDouble(aHi, aLo)
    }

    private fun twoProd(a: Double, b: Double) : DualDouble {
        val p = a * b
        val aS = split(a)
        val bS = split(b)
        val err = aS.hi * bS.hi - p + aS.hi * bS.lo + aS.lo * bS.hi + aS.lo * bS.lo
        return DualDouble(p, err)
    }

    operator fun unaryMinus() : DualDouble {
        return DualDouble(-hi, -lo)
    }

    operator fun plus(b: DualDouble) : DualDouble {
        var s = twoSum(hi, b.hi)
        val t = twoSum(lo, b.lo)
        s.lo += t.hi
        s = quickTwoSum(s.hi, s.lo)
        s.lo += t.lo
        s = quickTwoSum(s.hi, s.lo)
        return s
    }

    operator fun minus(b: DualDouble) : DualDouble {
        return plus(b.unaryMinus())
    }

    operator fun times(b: DualDouble) : DualDouble {
        var p = twoProd(hi, b.hi)
        p.lo += hi * b.lo
        p.lo += lo * b.hi
        p = quickTwoSum(p.hi, p.lo)
        return p
    }

    operator fun div(b: DualDouble) : DualDouble {

        val xn = 1.0 / b.hi
        val yn = hi * xn
        val diff = minus(b*DualDouble(yn, 0.0))
        val prod = twoProd(xn, diff.hi)
        return DualDouble(yn, 0.0) + prod

    }

}

class Texture (
        val res         : IntArray,
        interpolation   : Int,
        format          : Int,
        index           : Int
) {

    val id : Int
    private val buffer : ByteBuffer

    init {
        // create texture id
        val b = IntBuffer.allocate(1)
        GL.glGenTextures(1, b)
        id = b[0]

        // allocate texture memory
        buffer = when(format) {
            GL.GL_RGBA8 -> allocateDirect(res[0] * res[1] * 4).order(ByteOrder.nativeOrder())
            GL.GL_RGBA16F -> allocateDirect(res[0] * res[1] * 8).order(ByteOrder.nativeOrder())
            GL.GL_RGBA32F -> allocateDirect(res[0] * res[1] * 16).order(ByteOrder.nativeOrder())
            else -> allocateDirect(0)
        }

        // bind and set texture parameters
        GL.glActiveTexture(GL.GL_TEXTURE0 + index)
        GL.glBindTexture(GL.GL_TEXTURE_2D, id)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, interpolation)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, interpolation)

        val type = when(format) {
            GL.GL_RGBA8 -> GL.GL_UNSIGNED_BYTE
            GL.GL_RGBA16F -> GL.GL_HALF_FLOAT
            GL.GL_RGBA32F -> GL.GL_FLOAT
            else -> 0
        }

        // define texture specs
        GL.glTexImage2D(
                GL.GL_TEXTURE_2D,           // target
                0,                          // mipmap level
                format,                     // internal format
                res[0], res[1],              // texture resolution
                0,                          // border
                GL.GL_RGBA,                 // format
                type,                       // type
                buffer                      // memory pointer
        )
    }

    fun delete() { GL.glDeleteTextures(1, intArrayOf(id), 0) }

}



class ColorPalette (
        val name: String,
        private val colors: List<FloatArray>
) {

    companion object {

        val RED         = floatArrayOf( 1.0f,  0.0f,  0.0f )
        val GREEN       = floatArrayOf( 0.0f,  1.0f,  0.0f )
        val BLUE        = floatArrayOf( 0.0f,  0.0f,  1.0f )
        val YELLOW      = floatArrayOf( 1.0f,  1.0f,  0.0f )
        val MAGENTA     = floatArrayOf( 1.0f,  0.0f,  1.0f )
        val CYAN        = floatArrayOf( 0.0f,  1.0f,  1.0f )
        val BLACK       = floatArrayOf( 0.0f,  0.0f,  0.0f )
        val WHITE       = floatArrayOf( 1.0f,  1.0f,  1.0f )

        val PURPLE      = floatArrayOf( 0.3f,   0.0f,   0.5f  )
        val PINK        = floatArrayOf( 1.0f,   0.3f,   0.4f  )
        val DARKBLUE1   = floatArrayOf( 0.0f,   0.15f,  0.25f )
        val DARKBLUE2   = floatArrayOf( 0.11f,  0.188f, 0.35f )
        val ORANGE1     = floatArrayOf( 1.0f,   0.6f,   0.0f  )
        val ORAGNE2     = floatArrayOf( 0.9f,   0.4f,   0.2f  )
        val TURQUOISE   = floatArrayOf( 0.25f,  0.87f,  0.82f )
        val TUSK        = floatArrayOf( 0.93f,  0.8f,   0.73f )
        val YELLOWISH   = floatArrayOf( 1.0f,   0.95f,  0.75f )
        val GRASS       = floatArrayOf( 0.31f,  0.53f,  0.45f )
        val SOFTGREEN   = floatArrayOf( 0.6f,   0.82f,  0.9f  )
        val DEEPRED     = floatArrayOf( 0.8f,   0.0f,   0.3f  )
        val MAROON      = floatArrayOf( 0.4f,   0.1f,   0.2f  )


        val beach   = ColorPalette("Beach", listOf(
                YELLOWISH,
                DARKBLUE1,
                BLACK,
                TURQUOISE,
                TUSK
        ))
        val p1      = ColorPalette("P1", listOf(
                WHITE,
                PURPLE,
                BLACK,
                DEEPRED,
                WHITE
        ))
        val p2      = ColorPalette("P2", listOf(
                TURQUOISE, // 0.6
                PURPLE,
                BLACK,
                DEEPRED,
                WHITE
        ))
        val p3      = ColorPalette("P3", listOf(
                floatArrayOf(0.0f, 0.1f, 0.2f),
                DARKBLUE1,
                WHITE,
                ORAGNE2,
                PURPLE
        ))
        val p4      = ColorPalette("P4", listOf(
                YELLOWISH,
                DARKBLUE2,
                BLACK,
                PURPLE.mult(0.7f),
                DEEPRED
        ))
        val p5      = ColorPalette("P5", listOf(
                YELLOWISH.mult(1.1f),
                MAGENTA.mult(0.4f),
                WHITE,
                DARKBLUE1,
                BLACK,
                DARKBLUE1
        ))
        val p6      = ColorPalette("P6", listOf(
                YELLOWISH.mult(1.2f),
                GRASS.mult(0.35f),
                DARKBLUE1.mult(1.2f)
        ))
        val royal   = ColorPalette("Royal", listOf(
                YELLOWISH,
                DARKBLUE1,
                SOFTGREEN.mult(0.5f),
                PURPLE.mult(0.35f),
                MAROON
        ))
        val p7      = ColorPalette("P7", listOf(
                floatArrayOf(75.0f, 115.0f, 115.0f).mult(1.0f/255.0f),
                floatArrayOf(255.0f, 200.0f, 75.0f).mult(1.0f/255.0f),
                WHITE,
                floatArrayOf(255.0f, 118.0f, 111.0f).mult(1.0f/255.0f),
                BLACK
        ))
        val p8      = ColorPalette("P8", listOf(
                floatArrayOf(75.0f, 115.0f, 115.0f).mult(1.0f/255.0f),
                floatArrayOf(255.0f, 170.0f, 28.0f).mult(1.0f/255.0f),
                floatArrayOf(255.0f, 118.0f, 111.0f).mult(1.0f/255.0f),
                BLACK
        ))
        val canyon  = ColorPalette("Canyon", listOf(
                DEEPRED.mult(0.4f),
                floatArrayOf(255.0f, 115.0f, 110.0f).mult(1.0f/255.0f),
                floatArrayOf(1.0f, 0.8f, 0.6f),
                floatArrayOf(75.0f, 130.0f, 115.0f).mult(1.0f/255.0f),
                PURPLE.mult(0.3f)
        ))
        val all     = mapOf(
                beach.name   to  beach,
                p1.name      to  p1,
                p2.name      to  p2,
                p3.name      to  p3,
                p4.name      to  p4,
                p5.name      to  p5,
                p6.name      to  p6,
                royal.name   to  royal,
                p7.name      to  p7,
                p8.name      to  p8,
                canyon.name  to  canyon
        )

    }

    private val palette = colors.plus(colors[0])
    val size = palette.size
    val flatPalette = FloatArray(palette.size * 3) {i: Int ->
        val a = floor(i / 3.0f).toInt()
        val b = i % 3
        palette[a][b]
    }

    fun invert() : ColorPalette {
        return ColorPalette(name, List(size) { i: Int -> colors[i].invert() })
    }

    override fun toString() : String { return name }
    override fun equals(other: Any?): Boolean {
        return other is ColorPalette && other.name == name
    }
    override fun hashCode(): Int { return name.hashCode() }

}

class ComplexMap (
        val name            : String,
        val z0              : DoubleArray,
        val initJuliaMode   : Boolean,
        val initCoords      : DoubleArray,
        val initScale       : Double,
        val initMapParams   : List<DoubleArray>,
        val conditionalSF   : String?,
        val initSF          : String?,
        val loopSF          : String?,
        val finalSF         : String?,
        val conditionalDF   : String?,
        val initDF          : String?,
        val loopDF          : String?,
        val finalDF         : String?
) {
    
    companion object {
        
        val empty       = { ComplexMap(
                "Empty",
                doubleArrayOf(0.0, 0.0),
                false,
                doubleArrayOf(0.0, 0.0),
                1.0,
                listOf(),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        )}
        val mandelbrot  = { res: Resources -> ComplexMap(
                "Mandelbrot",
                doubleArrayOf(0.0, 0.0),
                false,
                doubleArrayOf(-0.75, 0.0),
                3.5,
                listOf(),
                res.getString(R.string.escape_sf),
                "",
                res.getString(R.string.mandelbrot_loop_sf),
                "",
                res.getString(R.string.escape_df),
                "",
                res.getString(R.string.mandelbrot_loop_df),
                ""
        )}
        val mandelbrotCpow  = { res: Resources -> ComplexMap(
                "Mandelbrot Cpow",
                doubleArrayOf(0.0, 0.0),
                false,
                doubleArrayOf(-0.75, 0.0),
                3.5,
                listOf(doubleArrayOf(2.0, 2.0)),
                res.getString(R.string.escape_sf),
                "",
                res.getString(R.string.mandelbrotcpow_loop_sf),
                "",
                "",
                "",
                "",
                ""
        )}
        val dualpow     = { res: Resources -> ComplexMap(
                "Dual Power",
                doubleArrayOf(1.0, 0.0),
                false,
                doubleArrayOf(0.0, 0.0),
                3.0,
                listOf(doubleArrayOf(0.0, 0.0)),
                res.getString(R.string.escape_sf),
                res.getString(R.string.dualpow_init_sf),
                res.getString(R.string.dualpow_loop_sf),
                "",
                res.getString(R.string.escape_df),
                "",
                "",
                ""
        )}
        val sine1       = { res: Resources -> ComplexMap(
                "Sine 1",
                doubleArrayOf(1.0, 0.0),
                false,
                doubleArrayOf(0.0, 0.0),
                3.5,
                listOf(doubleArrayOf(0.31960705187983646, 0.0)),
                res.getString(R.string.escape_sf),
                "",
                res.getString(R.string.sine1_loop_sf),
                "",
                res.getString(R.string.escape_df),
                "",
                "",
                ""
        ) }
        val sine2       = { res: Resources -> ComplexMap(
                "Sine 2",
                doubleArrayOf(1.0, 0.0),
                false,
                doubleArrayOf(0.0, 0.0),
                3.5,
                listOf(doubleArrayOf(-0.26282883851642613, 2.042520182493586E-6)),
                res.getString(R.string.escape_sf),
                "",
                res.getString(R.string.sine2_loop_sf),
                "",
                res.getString(R.string.escape_df),
                "",
                "",
                ""
        )}
        val sine4       = { res: Resources -> ComplexMap(
                "Sine 4",
                doubleArrayOf(1.0, 0.0),
                false,
                doubleArrayOf(0.0, 0.0),
                3.5,
                listOf(doubleArrayOf(1.0, 1.0)),
                res.getString(R.string.escape_sf),
                "",
                res.getString(R.string.sine4_loop_sf),
                "",
                res.getString(R.string.escape_df),
                "",
                res.getString(R.string.sine4_loop_df),
                ""
        )}
        val newton2     = { res: Resources -> ComplexMap(
                "Newton 2",
                doubleArrayOf(0.0, 0.0),
                true,
                doubleArrayOf(0.0, 0.0),
                3.5,
                listOf(
                    doubleArrayOf(1.0, 1.0),
                    doubleArrayOf(-1.0, -1.0),
                    doubleArrayOf(2.0, -0.5)
                ),
                res.getString(R.string.converge_sf),
                "",
                res.getString(R.string.newton2_loop_sf),
                "",
                res.getString(R.string.converge_df),
                "",
                "",
                ""
        ) }
        val all         = mapOf(
            "Mandelbrot"  to  mandelbrot,
            "Mandelbrot Cpow"  to  mandelbrotCpow,
            "Dual Power"  to  dualpow,
            "Sine 1"      to  sine1,
            "Sine 2"      to  sine2,
            "Sine 4"      to  sine4,
            "Newton 2"    to  newton2
        )
        
    }

    override fun toString() : String { return name }
    override fun equals(other: Any?): Boolean {
        return other is ComplexMap && name == other.name
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }

}

class TextureAlgorithm (
        val name     : String,
        val initSF   : String?,
        val loopSF   : String?,
        val finalSF  : String?,
        val initDF   : String?,
        val loopDF   : String?,
        val finalDF  : String?
) {

    companion object {

        val empty                       = {
            TextureAlgorithm("Empty", "", "", "", "", "", "")
        }
        val escape              = { res: Resources -> TextureAlgorithm(
                "Escape Time",
                "",
                "",
                res.getString(R.string.escape_final),
                "",
                "",
                res.getString(R.string.escape_final)
        )}
        val escapeSmooth        = { res: Resources -> TextureAlgorithm(
                "Escape Time Smooth",
                "",
                "",
                res.getString(R.string.mandelbrot_smooth_final_sf),
                "",
                "",
                res.getString(R.string.mandelbrot_smooth_final_df)
        )}
        val lighting            = { res: Resources -> TextureAlgorithm(
                "Lighting",
                res.getString(R.string.mandelbrot_light_init_sf),
                res.getString(R.string.mandelbrot_light_loop_sf),
                res.getString(R.string.mandelbrot_light_final),
                res.getString(R.string.mandelbrot_light_init_df),
                res.getString(R.string.mandelbrot_light_loop_df),
                res.getString(R.string.mandelbrot_light_final)
        )}
        val triangleIneqAvg     = { res: Resources -> TextureAlgorithm(
                "Triangle Inequality Average",
                res.getString(R.string.mandelbrot_triangle_init_sf),
                res.getString(R.string.mandelbrot_triangle_loop_sf),
                res.getString(R.string.mandelbrot_triangle_final_sf),
                res.getString(R.string.mandelbrot_triangle_init_df),
                res.getString(R.string.mandelbrot_triangle_loop_df),
                res.getString(R.string.mandelbrot_triangle_final_df)
        )}
        val curvatureAvg        = { res: Resources -> TextureAlgorithm(
                "Curvature Average",
                res.getString(R.string.curvature_init),
                res.getString(R.string.curvature_loop_sf),
                res.getString(R.string.curvature_final_sf),
                res.getString(R.string.curvature_init),
                res.getString(R.string.curvature_loop_df),
                res.getString(R.string.curvature_final_df)
        )}
        val stripeAvg           = { res: Resources -> TextureAlgorithm(
                "Stripe Average",
                res.getString(R.string.stripe_init),
                res.getString(R.string.stripe_loop_sf).format(5.0f),
                res.getString(R.string.stripe_final_sf),
                res.getString(R.string.stripe_init),
                res.getString(R.string.stripe_loop_df).format(5.0f),
                res.getString(R.string.stripe_final_df)
        )}
        val orbitTrap           = { res: Resources -> TextureAlgorithm(
                "Orbit Trap",
                res.getString(R.string.minmod_init),
                res.getString(R.string.minmod_loop_sf),
                res.getString(R.string.minmod_final),
                res.getString(R.string.minmod_init),
                res.getString(R.string.minmod_loop_df),
                res.getString(R.string.minmod_final)
        )}
        val overlayAvg          = { res: Resources -> TextureAlgorithm(
                "Overlay Average",
                res.getString(R.string.overlay_init),
                res.getString(R.string.overlay_loop_sf),
                res.getString(R.string.overlay_final_sf),
                res.getString(R.string.overlay_init),
                res.getString(R.string.overlay_loop_df),
                res.getString(R.string.overlay_final_df)
        )}
        val all = mapOf(
            "Escape Time"                  to  escape               ,
            "Escape Time Smooth"           to  escapeSmooth         ,
            "Triangle Inequality Average"  to  triangleIneqAvg      ,
            "Curvature Average"            to  curvatureAvg         ,
            "Stripe Average"               to  stripeAvg            ,
            "Orbit Trap"                   to  orbitTrap            ,
            "Overlay Average"              to  overlayAvg
        )

    }

    fun add(alg : TextureAlgorithm) : TextureAlgorithm {
        return TextureAlgorithm(
                "$name with ${alg.name}",
                initSF + alg.initSF,
                loopSF + alg.loopSF,
                finalSF + alg.finalSF,
                initDF + alg.initDF,
                loopDF + alg.loopDF,
                finalDF + alg.finalDF
        )
    }

    override fun toString() : String { return name }

    override fun equals(other: Any?): Boolean {
        return other is TextureAlgorithm && name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}



enum class Precision { SINGLE, DUAL, QUAD, AUTO }

enum class Reaction { TRANSFORM, COLOR, P1, P2, P3, P4 }

enum class Resolution { LOW, MED, HIGH, ULTRA }




class FractalConfig (val params : MutableMap<String, Any>) {

    val map                 = { params["map"]              as ComplexMap        }
    val p1                  = { params["p1"]               as DoubleArray       }
    val p2                  = { params["p2"]               as DoubleArray       }
    val p3                  = { params["p3"]               as DoubleArray       }
    val p4                  = { params["p4"]               as DoubleArray       }
    val texture             = { params["texture"]          as TextureAlgorithm  }
    val juliaMode           = { params["juliaMode"]        as Boolean           }
    val paramSensitivity    = { params["paramSensitivity"] as Double            }
    val coords              = { params["coords"]           as DoubleArray       }
    val scale               = { params["scale"]            as DoubleArray       }
    val maxIter             = { params["maxIter"]          as Int               }
    val bailoutRadius       = { params["bailoutRadius"]    as Float             }

}

class ColorConfig (val params : MutableMap<String, Any>) {

    val palette            = { params["palette"]    as  ColorPalette   }
    val frequency          = { params["frequency"]  as  Float          }
    val phase              = { params["phase"]      as  Float          }
}

class SettingsConfig (val params: MutableMap<String, Any>) {

    val resolution         = { params["resolution"]        as Resolution }
    val precision          = { params["precision"]         as Precision  }
    val continuousRender   = { params["continuousRender"]  as Boolean    }
    val displayParams      = { params["displayParams"]     as Boolean    }

}




fun MotionEvent.focalLength() : Float {
    val f = focus()
    val pos = floatArrayOf(x, y)
    val dist = floatArrayOf(pos[0] - f[0], pos[1] - f[1])
    return sqrt(dist[0].toDouble().pow(2.0) +
            dist[1].toDouble().pow(2.0)).toFloat()
}

fun MotionEvent.focus() : FloatArray {
    return if (pointerCount == 1) floatArrayOf(x, y)
    else { floatArrayOf((getX(0) + getX(1))/2.0f, (getY(0) + getY(1))/2.0f) }
}

fun FloatArray.mult(s: Float) : FloatArray {
    return FloatArray(this.size) {i: Int -> s*this[i]}
}

fun FloatArray.invert() : FloatArray {
    return FloatArray(this.size) {i: Int -> 1.0f - this[i]}
}

fun DoubleArray.mult(s: Double) : DoubleArray {
    return DoubleArray(this.size) {i: Int -> s*this[i]}
}

fun DoubleArray.negative() : DoubleArray {
    return doubleArrayOf(-this[0], -this[1])
}

fun splitSD(a: Double) : FloatArray {

    val b = FloatArray(2)
    b[0] = a.toFloat()
    b[1] = (a - b[0].toDouble()).toFloat()
    return b

}

fun splitDD(a: DualDouble) : FloatArray {

    val b = FloatArray(4)
    b[0] = a.hi.toFloat()
    b[1] = (a.hi - b[0].toDouble()).toFloat()
    b[2] = a.lo.toFloat()
    b[3] = (a.lo - b[2].toDouble()).toFloat()
    return b

}



class Fractal(
        private val context     : Activity,
        val fractalConfig      : FractalConfig,
        val colorConfig         : ColorConfig,
        val settingsConfig      : SettingsConfig,
        val screenRes           : IntArray
) {


    private var header       : String = ""
    private var arithmetic   : String = ""
    private var init         : String = ""
    private var loop         : String = ""
    private var conditional  : String = ""
    private var algInit      : String = ""
    private var algLoop      : String = ""
    private var algFinal     : String = ""
    private var mapInit      : String = ""
    private var mapLoop      : String = ""
    private var mapFinal     : String = ""

    private val res = context.resources
    private val colorHeader = res.getString(R.string.color_header)
    private val colorIndex = res.getString(R.string.color_index)
    private var colorPostIndex  : String = ""


    private val precisionThreshold = 6e-5
    private val aspectRatio = screenRes[1].toDouble()/screenRes[0]
    var renderShaderChanged = false
    var colorShaderChanged = false
    var resolutionChanged = false
    var renderProfileChanged = false
    var innerColor = "1.0"

    val autoPrecision = {
        if (fractalConfig.scale()[0] > precisionThreshold) Precision.SINGLE else Precision.DUAL
    }
    val precision = {
        if (settingsConfig.precision() == Precision.AUTO) autoPrecision() else settingsConfig.precision()
    }
    var texRes = {
        when (settingsConfig.resolution()) {
            Resolution.LOW -> intArrayOf(screenRes[0]/8, screenRes[1]/8)
            Resolution.MED -> intArrayOf(screenRes[0]/3, screenRes[1]/3)
            Resolution.HIGH -> screenRes
            Resolution.ULTRA -> intArrayOf((7*screenRes[0])/4, (7*screenRes[1])/4)
        }
    }


    val renderShader = {

        loadRenderResources()

        """
        $header
        $arithmetic
        void main() {
            $init
            $mapInit
            $algInit
            for (int n = 0; n < maxIter; n++) {
                if (n == maxIter - 1) {
                    $algFinal
                    colorParams.w = -2.0;
                    break;
                }
                $loop
                $mapLoop
                $conditional {
                    $mapFinal
                    $algFinal
                    break;
                }
                $algLoop
            }
            fragmentColor = colorParams;
        }
        """

    }

    val colorShader = {

        loadColorResources()

        """
        $colorHeader
        void main() {

            vec3 color = vec3(1.0);
            vec4 s = texture(tex, texCoord);

            if (s.w != -1.0) {
                $colorIndex
                $colorPostIndex
            }

            fragmentColor = vec4(color, 1.0);

        }
        """

    }


    init {
        reset()
    }


    private fun loadRenderResources() {

        when(precision()) {
            Precision.SINGLE -> {
                header      = res.getString(R.string.header_sf)
                arithmetic  = res.getString(R.string.arithmetic_sf)
                init        = res.getString(R.string.general_init_sf)
                if (fractalConfig.juliaMode()) { init += res.getString(R.string.julia_sf) }
                else { init += res.getString(R.string.constant_sf) }
                loop        = res.getString(R.string.general_loop_sf)
                conditional = fractalConfig.map().conditionalSF    ?: ""
                mapInit     = fractalConfig.map().initSF           ?: ""
                algInit     = fractalConfig.texture().initSF        ?: ""
                mapLoop     = fractalConfig.map().loopSF           ?: ""
                if (fractalConfig.juliaMode()) {
                    mapLoop = mapLoop.replace("C", "P${fractalConfig.map().initMapParams.size + 1}", false)
                }
                algLoop     = fractalConfig.texture().loopSF        ?: ""
                mapFinal    = fractalConfig.map().finalSF          ?: ""
                algFinal    = fractalConfig.texture().finalSF       ?: ""
            }
            Precision.DUAL -> {

                header      = res.getString(R.string.header_df)
                arithmetic  = res.getString(R.string.arithmetic_util)
                arithmetic += res.getString(R.string.arithmetic_sf)
                arithmetic += res.getString(R.string.arithmetic_df)
                init        = res.getString(R.string.general_init_df)
                if (fractalConfig.juliaMode()) { init += res.getString(R.string.julia_df) }
                else { init += res.getString(R.string.constant_df) }
                loop        = res.getString(R.string.general_loop_df)
                conditional = fractalConfig.map().conditionalDF    ?: ""
                mapInit     = fractalConfig.map().initDF           ?: ""
                algInit     = fractalConfig.texture().initDF        ?: ""
                mapLoop     = fractalConfig.map().loopDF           ?: ""
                if (fractalConfig.juliaMode()) {
                    mapLoop = mapLoop.replace("A", "vec2(P${fractalConfig.map().initMapParams.size + 1}.x, 0.0)", false)
                    mapLoop = mapLoop.replace("B", "vec2(P${fractalConfig.map().initMapParams.size + 1}.y, 0.0)", false)
                }
                algLoop     = fractalConfig.texture().loopDF        ?: ""
                mapFinal    = fractalConfig.map().finalDF          ?: ""
                algFinal    = fractalConfig.texture().finalDF       ?: ""

            }
            else -> {}
        }

    }
    private fun loadColorResources() {

        if (fractalConfig.texture().name == "Escape Time Smooth with Lighting") {
            colorPostIndex = context.resources.getString(R.string.color_lighting)
            innerColor = "1.0"
        }
        else {
            colorPostIndex = ""
            innerColor = "0.0"
        }

    }

    fun resetPosition() {
        fractalConfig.coords()[0] = fractalConfig.map().initCoords[0]
        fractalConfig.coords()[1] = fractalConfig.map().initCoords[1]
        fractalConfig.scale()[0] = fractalConfig.map().initScale
        fractalConfig.scale()[1] = fractalConfig.map().initScale * aspectRatio
        updatePositionEditTexts()
    }
    fun resetMapParams() {
        for (i in 0 until fractalConfig.map().initMapParams.size) {
            (fractalConfig.params["p${i + 1}"] as DoubleArray)[0] = fractalConfig.map().initMapParams[i][0]
            (fractalConfig.params["p${i + 1}"] as DoubleArray)[1] = fractalConfig.map().initMapParams[i][1]
        }
        updateMapParamEditTexts()
    }
    fun reset() {
        resetPosition()
        resetMapParams()
        fractalConfig.params["juliaMode"] = fractalConfig.map().initJuliaMode
    }
    fun updatePositionEditTexts() {

        val xCoordEdit = context.findViewById<EditText>(R.id.xCoordEdit)
        val yCoordEdit = context.findViewById<EditText>(R.id.yCoordEdit)
        val scaleSignificandEdit = context.findViewById<EditText>(R.id.scaleSignificandEdit)
        val scaleExponentEdit = context.findViewById<EditText>(R.id.scaleExponentEdit)
        val scaleStrings = "%e".format(fractalConfig.scale()[0]).split("e")
        val bailoutSignificandEdit = context.findViewById<EditText>(R.id.bailoutSignificandEdit)
        val bailoutExponentEdit = context.findViewById<EditText>(R.id.bailoutExponentEdit)
        val bailoutStrings = "%e".format(fractalConfig.bailoutRadius()).split("e")

        xCoordEdit?.setText("%.17f".format(fractalConfig.coords()[0]))
        yCoordEdit?.setText("%.17f".format(fractalConfig.coords()[1]))
        scaleSignificandEdit?.setText(scaleStrings[0])
        scaleExponentEdit?.setText(scaleStrings[1])
        bailoutSignificandEdit?.setText(bailoutStrings[0])
        bailoutExponentEdit?.setText(bailoutStrings[1])

    }
    fun updateMapParamEditText(i: Int) {
        // Log.d("FRACTAL", "updating map param EditText $i")

        val xEdit : EditText?
        val yEdit : EditText?

        when (i) {
            1 -> {
                xEdit = context.findViewById(R.id.p1xEdit)
                yEdit = context.findViewById(R.id.p1yEdit)
            }
            2 -> {
                xEdit = context.findViewById(R.id.p2xEdit)
                yEdit = context.findViewById(R.id.p2yEdit)
            }
            3 -> {
                xEdit = context.findViewById(R.id.p3xEdit)
                yEdit = context.findViewById(R.id.p3yEdit)
            }
            4 -> {
                xEdit = context.findViewById(R.id.p4xEdit)
                yEdit = context.findViewById(R.id.p4yEdit)
            }
            else -> {
                xEdit = null
                yEdit = null
            }
        }

        xEdit?.setText("%.8f".format((fractalConfig.params["p$i"] as DoubleArray)[0]))
        yEdit?.setText("%.8f".format((fractalConfig.params["p$i"] as DoubleArray)[1]))

    }
    fun updateMapParamEditTexts() {
        for (i in 1..4) {
            updateMapParamEditText(i)
        }
    }
    fun updateDisplayParams(reaction: Reaction, reactionChanged: Boolean) {
        val displayParams = context.findViewById<LinearLayout>(R.id.displayParams)
        if (settingsConfig.displayParams()) {
            when (reaction) {
                Reaction.TRANSFORM -> {
                    (displayParams.getChildAt(1) as TextView).text = "x: %.17f".format(fractalConfig.coords()[0])
                    (displayParams.getChildAt(2) as TextView).text = "y: %.17f".format(fractalConfig.coords()[1])
                    (displayParams.getChildAt(3) as TextView).text = "scale: %e".format(fractalConfig.scale()[0])
                }
                Reaction.COLOR -> {
                    (displayParams.getChildAt(1) as TextView).text = "frequency: %.4f".format(colorConfig.frequency())
                    (displayParams.getChildAt(2) as TextView).text = "phase: %.4f".format(colorConfig.phase())
                }
                else -> {
                    val i = reaction.ordinal - 2
                    (displayParams.getChildAt(1) as TextView).text = "x: %.8f".format((fractalConfig.params["p${i + 1}"] as DoubleArray)[0])
                    (displayParams.getChildAt(2) as TextView).text = "y: %.8f".format((fractalConfig.params["p${i + 1}"] as DoubleArray)[1])
                    (displayParams.getChildAt(3) as TextView).text = "sensitivity: %.4f".format(fractalConfig.paramSensitivity())
                }
            }
        }
        if (settingsConfig.displayParams() || reactionChanged) {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 1000L
            fadeOut.startOffset = 2500L
            fadeOut.fillAfter = true
            displayParams.animation = fadeOut
            displayParams.animation.start()
            displayParams.requestLayout()
        }
    }
    fun switchOrientation() {

//        val fractalConfig.coords() = doubleArrayOf((xCoords[0] + xCoords[1]) / 2.0, -(yCoords[0] + yCoords[1]) / 2.0)
//        translate(fractalConfig.coords())
//
//        // rotation by 90 degrees counter-clockwise
//        val xCoordsNew = doubleArrayOf(-yCoords[1], -yCoords[0])
//        val yCoordsNew = doubleArrayOf(xCoords[0], xCoords[1])
//        xCoords[0] = xCoordsNew[0]
//        xCoords[1] = xCoordsNew[1]
//        yCoords[0] = yCoordsNew[0]
//        yCoords[1] = yCoordsNew[1]
//
//        translate(fractalConfig.coords().negative())

//        Log.d("FRACTAL", "xCoordsNew:  (${xCoordsNew[0]}, ${xCoordsNew[1]})")
//        Log.d("FRACTAL", "yCoordsNew:  (${yCoordsNew[0]}, ${yCoordsNew[1]})")


    }
    fun setMapParam(i: Int, dPos: FloatArray) {
        // dx -- [0, screenWidth]
        val sensitivity =
            if (fractalConfig.paramSensitivity() == -1.0) fractalConfig.scale()[0]
            else fractalConfig.paramSensitivity()
        (fractalConfig.params["p$i"] as DoubleArray)[0] += sensitivity*dPos[0]/screenRes[0]
        (fractalConfig.params["p$i"] as DoubleArray)[1] += sensitivity*dPos[1]/screenRes[1]

        // Log.d("FRACTAL", "setting map param ${p + 1} to (${fractalConfig.map().params[p - 1][0]}, ${fractalConfig.map().params[p - 1][1]})")

        // SINE2 :: (-0.26282883851642613, 2.042520182493586E-6)
        // SINE2 :: (-0.999996934286532, 9.232660318047263E-5)
        // SINE2 :: (-0.2287186333845716, 0.1340647963904784)

        // SINE1 :: -0.578539160583084
        // SINE1 :: -0.8717463705274795
        // SINE1 :: 0.2948570315666499
        // SINE1 :: 0.31960705187983646
        // SINE1 :: -0.76977662
        //      JULIA :: (-0.85828304, -0.020673078)
        //      JULIA :: (-0.86083659, 0.0)
        // SINE1 :: -1.0
        //      JULIA :: (0.53298706, 0.00747937)

        // JULIA :: (0.38168508, -0.20594095) + TRIANGLE INEQ !!!!!

        // MANDELBROT CPOW :: (1.31423213, 2.86942864)
        //      JULIA :: (-0.84765975, -0.02321229)

        updateDisplayParams(Reaction.valueOf("P$i"), false)
        updateMapParamEditText(i)

    }
    fun setMapParamSensetivity(i: Int, dScale: Float) {
        fractalConfig.params["paramSensitivity"] = fractalConfig.paramSensitivity() * dScale
        updateMapParamEditText(i)
        updateDisplayParams(Reaction.valueOf("P$i"), false)
    }
    fun translate(dScreenPos: FloatArray) {

        // update complex coordinates
        when (settingsConfig.precision()) {
            Precision.QUAD -> {
//                        val dPosDD = arrayOf(
//                                DualDouble((dScreenPos[0].toDouble() / screenRes[0]), 0.0) * (xCoordsDD[1] - xCoordsDD[0]),
//                                DualDouble((dScreenPos[1].toDouble() / screenRes[1]), 0.0) * (yCoordsDD[1] - yCoordsDD[0])
//                        )
//                        xCoordsDD[0] -= dPosDD[0]
//                        xCoordsDD[1] -= dPosDD[0]
//                        yCoordsDD[0] += dPosDD[1]
//                        yCoordsDD[1] += dPosDD[1]
            }
            else -> {
                fractalConfig.coords()[0] -= (dScreenPos[0] / screenRes[0])*fractalConfig.scale()[0]
                fractalConfig.coords()[1] += (dScreenPos[1] / screenRes[1])*fractalConfig.scale()[1]
            }
        }

        updatePositionEditTexts()
        updateDisplayParams(Reaction.TRANSFORM, false)
//        Log.d("FRACTAL", "translation (pixels) -- dx: ${dScreenPos[0]}, dy: ${dScreenPos[1]}")

    }
    fun translate(dPos: DoubleArray) {

        // update complex coordinates
        when (settingsConfig.precision()) {
            Precision.QUAD -> {
//                        val dPosDD = arrayOf(
//                                DualDouble((dScreenPos[0].toDouble() / screenRes[0]), 0.0) * (xCoordsDD[1] - xCoordsDD[0]),
//                                DualDouble((dScreenPos[1].toDouble() / screenRes[1]), 0.0) * (yCoordsDD[1] - yCoordsDD[0])
//                        )
//                        xCoordsDD[0] -= dPosDD[0]
//                        xCoordsDD[1] -= dPosDD[0]
//                        yCoordsDD[0] += dPosDD[1]
//                        yCoordsDD[1] += dPosDD[1]
            }
            else -> {
                fractalConfig.coords()[0] += dPos[0]
                fractalConfig.coords()[1] += dPos[1]
            }
        }

//        Log.d("FRACTAL", "translation (coordinates) -- dx: ${dPos[0]}, dy: ${dPos[1]}")

    }
    fun scale(dScale: Float, screenFocus: FloatArray) {

        // update complex coordinates
        // convert focus coordinates from screen space to complex space
        val prop = doubleArrayOf(
                screenFocus[0].toDouble() / screenRes[0].toDouble(),
                screenFocus[1].toDouble() / screenRes[1].toDouble()
        )

        val precisionPreScale = precision()
        when (precisionPreScale) {
            Precision.QUAD -> {
//                        val focusDD = arrayOf(
//                                DualDouble(prop[0], 0.0) * (xCoordsDD[1] - xCoordsDD[0]) + xCoordsDD[0],
//                                DualDouble(prop[1], 0.0) * (yCoordsDD[0] - yCoordsDD[1]) + yCoordsDD[1]
//                        )
//                        val dScaleDD = DualDouble(1.0 / dScale.toDouble(), 0.0)
//
//                        // translate focus to origin in complex coordinates
//                        xCoordsDD[0] -= focusDD[0]
//                        xCoordsDD[1] -= focusDD[0]
//                        yCoordsDD[0] -= focusDD[1]
//                        yCoordsDD[1] -= focusDD[1]
//
//                        // scale complex coordinates
//                        xCoordsDD[0] *= dScaleDD
//                        xCoordsDD[1] *= dScaleDD
//                        yCoordsDD[0] *= dScaleDD
//                        yCoordsDD[1] *= dScaleDD
//
//                        // translate origin back to focusDD in complex coordinates
//                        xCoordsDD[0] += focusDD[0]
//                        xCoordsDD[1] += focusDD[0]
//                        yCoordsDD[0] += focusDD[1]
//                        yCoordsDD[1] += focusDD[1]
            }
            else -> {
                val focus = doubleArrayOf(
//                    xCoords[0] * (1.0 - prop[0]) + prop[0] * xCoords[1],
//                    yCoords[1] * (1.0 - prop[1]) + prop[1] * yCoords[0]
                    fractalConfig.coords()[0] + (prop[0] - 0.5)*fractalConfig.scale()[0],
                    fractalConfig.coords()[1] - (prop[1] - 0.5)*fractalConfig.scale()[1]
                )
                // Log.d("FRACTAL", "focus (coordinates) -- x: ${focus[0]}, y: ${focus[1]}")

                translate(focus.negative())
                fractalConfig.coords()[0] = fractalConfig.coords()[0] / dScale
                fractalConfig.coords()[1] = fractalConfig.coords()[1] / dScale
                translate(focus)
                fractalConfig.scale()[0] = fractalConfig.scale()[0] / dScale
                fractalConfig.scale()[1] = fractalConfig.scale()[1] / dScale
            }
        }

//        Log.d("FRACTAL", "length of x-interval: ${abs(xCoords[1] - xCoords[0])}")

        val precisionPostScale = precision()
        if (precisionPostScale != precisionPreScale) {
            renderShaderChanged = true
            Log.d("FRACTAL", "precision changed")
        }

        updatePositionEditTexts()
        updateDisplayParams(Reaction.TRANSFORM, false)
//        Log.d("FRACTAL", "scale -- dscale: $dScale")

    }
    fun setFrequency(dScale: Float) {
        colorConfig.params["frequency"] = colorConfig.frequency() * dScale
        updateDisplayParams(Reaction.COLOR, false)
    }
    fun setPhase(dx: Float) {
        colorConfig.params["phase"] = (colorConfig.phase() + dx/screenRes[0])
        updateDisplayParams(Reaction.COLOR, false)
    }

}



@SuppressLint("ViewConstructor")
class FractalSurfaceView(
        var f                 : Fractal,
        private val context   : Activity
) : GLSurfaceView(context) {

    inner class FractalRenderer(var f: Fractal, val context: Activity) : Renderer {

        inner class RenderRoutine {

            private val maxPixelsPerChunk = f.screenRes[0]*f.screenRes[1]/8

            // coordinates of default view boundaries
            private val viewCoords = floatArrayOf(
                    -1.0f,   1.0f,   0.0f,     // top left
                    -1.0f,  -1.0f,   0.0f,     // bottom left
                    1.0f,  -1.0f,   0.0f,     // bottom right
                    1.0f,   1.0f,   0.0f )    // top right
            private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

            private val renderProgram = GL.glCreateProgram()
            private val viewCoordsHandle : Int
            private val iterHandle       : Int
            private val rHandle          : Int
            private val xInitHandle      : Int
            private val yInitHandle      : Int
            private val xScaleHandle     : Int
            private val yScaleHandle     : Int
            private val xOffsetHandle    : Int
            private val yOffsetHandle    : Int
            private val bgScaleHandle    : Int
            private val paramHandles     : IntArray

            private val sampleProgram = GL.glCreateProgram()
            private val viewCoordsSampleHandle : Int
            private val quadCoordsSampleHandle : Int
            private val textureSampleHandle    : Int

            private val colorProgram = GL.glCreateProgram()
            private val viewCoordsColorHandle : Int
            private val quadCoordsColorHandle : Int
            private val numColorsHandle       : Int
            private val textureColorHandle    : Int
            private val paletteHandle         : Int
            private val frequencyHandle       : Int
            private val phaseHandle           : Int


            private val vRenderShader : Int
            private val vSampleShader : Int

            private var fRenderShader : Int
            private var fColorShader  : Int
            private val fSampleShader : Int

            // define texture resolutions
            private val bgTexWidth = { if (f.settingsConfig.continuousRender()) 1 else f.screenRes[0]/8 }
            private val bgTexHeight = { if (f.settingsConfig.continuousRender()) 1 else f.screenRes[1]/8 }

            private val interpolation = {
                if (f.settingsConfig.resolution() == Resolution.ULTRA) GL.GL_LINEAR
                else GL.GL_NEAREST
            }

            private val textures = arrayOf(
                    Texture(intArrayOf(bgTexWidth(), bgTexHeight()), GL.GL_NEAREST, GL.GL_RGBA16F, 0),
                    Texture(f.texRes(), interpolation(), GL.GL_RGBA16F, 1),
                    Texture(f.texRes(), interpolation(), GL.GL_RGBA16F, 2)
            )

            // allocate memory for textures
            private val quadBuffer =
                    allocateDirect(viewCoords.size * 4)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer()
            private val bgQuadBuffer =
                    allocateDirect(viewCoords.size * 4)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer()

            // create variables to store texture and fbo IDs
            private val fboIDs : IntBuffer = IntBuffer.allocate(1)
            private var currIndex = 1      // current high-res texture ID index
            private var intIndex = 2       // intermediate high-res texture ID index

            // initialize byte buffer for the draw list
            // num coord values * 2 bytes/short
            private val drawListBuffer =
                    allocateDirect(drawOrder.size * 2)
                            .order(ByteOrder.nativeOrder())
                            .asShortBuffer()
                            .put(drawOrder)
                            .position(0)

            // initialize byte buffer for view coordinates
            // num coord values * 4 bytes/float
            private val viewBuffer =
                    allocateDirect(viewCoords.size * 4)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer()
                            .put(viewCoords)
                            .position(0)
            private val viewChunkBuffer =
                    allocateDirect(viewCoords.size * 4)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer()


            init {

                // load all vertex and fragment shader code
                var s = context.resources.openRawResource(R.raw.vert_render)
                val vRenderCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()

                s = context.resources.openRawResource(R.raw.vert_sample)
                val vSampleCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()

//            s = context.resources.openRawResource(R.raw.render_qf)
//            val fRenderCodeQF = Scanner(s).useDelimiter("\\Z").next()
//            s.close()

                s = context.resources.openRawResource(R.raw.sample)
                val fSampleCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()

//            s = context.resources.openRawResource(R.raw.color)
//            val fColorCode = Scanner(s).useDelimiter("\\Z").next()
//            s.close()


                // create and compile shaders
                vRenderShader = loadShader(GL.GL_VERTEX_SHADER, vRenderCode)
                vSampleShader = loadShader(GL.GL_VERTEX_SHADER, vSampleCode)

                fRenderShader = loadShader(GL.GL_FRAGMENT_SHADER, f.renderShader())
                fSampleShader = loadShader(GL.GL_FRAGMENT_SHADER, fSampleCode)
                fColorShader  = loadShader(GL.GL_FRAGMENT_SHADER, f.colorShader())


                GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

                // generate texture and framebuffer objects
                GL.glGenFramebuffers(1, fboIDs)



                // attach shaders and create renderProgram executables
                GL.glAttachShader(renderProgram, vRenderShader)
                GL.glAttachShader(renderProgram, fRenderShader)
                GL.glLinkProgram(renderProgram)

                viewCoordsHandle =  GL.glGetAttribLocation(   renderProgram, "viewCoords"  )
                iterHandle       =  GL.glGetUniformLocation(  renderProgram, "maxIter"     )
                rHandle          =  GL.glGetUniformLocation(  renderProgram, "R"           )
                xInitHandle      =  GL.glGetUniformLocation(  renderProgram, "x0"          )
                yInitHandle      =  GL.glGetUniformLocation(  renderProgram, "y0"          )
                xScaleHandle     =  GL.glGetUniformLocation(  renderProgram, "xScale"      )
                yScaleHandle     =  GL.glGetUniformLocation(  renderProgram, "yScale"      )
                xOffsetHandle    =  GL.glGetUniformLocation(  renderProgram, "xOffset"     )
                yOffsetHandle    =  GL.glGetUniformLocation(  renderProgram, "yOffset"     )
                bgScaleHandle    =  GL.glGetUniformLocation(  renderProgram, "bgScale"     )
                paramHandles     =  IntArray(4) { i: Int ->
                    GL.glGetUniformLocation(renderProgram, "P${i+1}")
                }
//            p1Handle         =  GL.glGetUniformLocation(  renderProgram, "P1"          )
//            p2Handle         =  GL.glGetUniformLocation(  renderProgram, "P2"          )
//            p3Handle         =  GL.glGetUniformLocation(  renderProgram, "P3"          )
//            p4Handle         =  GL.glGetUniformLocation(  renderProgram, "P4"          )


                GL.glAttachShader(sampleProgram, vSampleShader)
                GL.glAttachShader(sampleProgram, fSampleShader)
                GL.glLinkProgram(sampleProgram)

                viewCoordsSampleHandle = GL.glGetAttribLocation(  sampleProgram, "viewCoords"  )
                quadCoordsSampleHandle = GL.glGetAttribLocation(  sampleProgram, "quadCoords"  )
                textureSampleHandle    = GL.glGetUniformLocation( sampleProgram, "tex"         )

                GL.glAttachShader(colorProgram, vSampleShader)
                GL.glAttachShader(colorProgram, fColorShader)
                GL.glLinkProgram(colorProgram)

                viewCoordsColorHandle = GL.glGetAttribLocation(   colorProgram, "viewCoords"   )
                quadCoordsColorHandle = GL.glGetAttribLocation(   colorProgram, "quadCoords"   )
                textureColorHandle    = GL.glGetUniformLocation(  colorProgram, "tex"          )
                numColorsHandle       = GL.glGetUniformLocation(  colorProgram, "numColors"    )
                paletteHandle         = GL.glGetUniformLocation(  colorProgram, "palette"      )
                frequencyHandle       = GL.glGetUniformLocation(  colorProgram, "frequency"    )
                phaseHandle           = GL.glGetUniformLocation(  colorProgram, "phase"        )

            }

            private fun loadShader(type: Int, shaderCode: String): Int {

                // create a vertex shader type (GL.GL_VERTEX_SHADER)
                // or a fragment shader type (GL.GL_FRAGMENT_SHADER)
                val shader = GL.glCreateShader(type)

                // add the source code to the shader and compile it
                GL.glShaderSource(shader, shaderCode)
                GL.glCompileShader(shader)

//            val a = IntBuffer.allocate(1)
//            GL.glGetShaderiv(shader, GL.GL_COMPILE_STATUS, a)
//            if (a[0] == GL.GL_FALSE) {
//                Log.d("RENDER ROUTINE", "shader compile failed")
//            }
//            else if (a[0] == GL.GL_TRUE) {
//                Log.d("RENDER ROUTINE", "shader compile succeeded")
//            }

                return shader

            }
            private fun splitCoords(xCoords: FloatArray, yCoords: FloatArray) : List<FloatArray> {

                val xLength = xCoords[1] - xCoords[0]
                val yLength = yCoords[1] - yCoords[0]
                val texRes = f.texRes()
                val xPixels = xLength / 2.0f * texRes[0]
                val yPixels = yLength / 2.0f * texRes[1]
                val numChunks = ceil((xPixels*yPixels) / maxPixelsPerChunk).toInt()
                val chunkInc = if (xLength >= yLength) xLength/numChunks else yLength/numChunks

                return if (xPixels >= yPixels) {
                    List(numChunks) { i: Int ->
                        floatArrayOf(
                                xCoords[0] + i*chunkInc,       yCoords[1], 0.0f,    // top left
                                xCoords[0] + i*chunkInc,       yCoords[0], 0.0f,    // bottom left
                                xCoords[0] + (i + 1)*chunkInc, yCoords[0], 0.0f,    // bottom right
                                xCoords[0] + (i + 1)*chunkInc, yCoords[1], 0.0f     // top right
                        )
                    }
                }
                else {
                    List(numChunks) { i: Int ->
                        floatArrayOf(
                                xCoords[0], yCoords[0] + (i + 1)*chunkInc, 0.0f,    // top left
                                xCoords[0], yCoords[0] + i*chunkInc,       0.0f,    // bottom left
                                xCoords[1], yCoords[0] + i*chunkInc,       0.0f,    // bottom right
                                xCoords[1], yCoords[0] + (i + 1)*chunkInc, 0.0f     // top right
                        )
                    }
                }

            }
            fun renderToTexture() {

                if (f.renderShaderChanged) {
                    Log.d("RENDER ROUTINE", "render shader changed")
                    GL.glDetachShader(renderProgram, fRenderShader)
                    fRenderShader = loadShader(GL.GL_FRAGMENT_SHADER, f.renderShader())
                    GL.glAttachShader(renderProgram, fRenderShader)
                    GL.glLinkProgram(renderProgram)
                    f.renderShaderChanged = false
                }
                if (f.resolutionChanged) {
                    val texRes = f.texRes()
                    textures[1].delete()
                    textures[1] = Texture(texRes, interpolation(), GL.GL_RGBA16F, 1)
                    textures[2].delete()
                    textures[2] = Texture(texRes, interpolation(), GL.GL_RGBA16F, 2)
                    f.resolutionChanged = false
                }
                if (f.renderProfileChanged) {
                    textures[0].delete()
                    textures[0] = Texture(intArrayOf(bgTexWidth(), bgTexHeight()), GL.GL_NEAREST, GL.GL_RGBA16F, 0)
                    f.renderProfileChanged = false
                }

                context.findViewById<ProgressBar>(R.id.progressBar).progress = 0

                GL.glUseProgram(renderProgram)
                GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer

                for (i in 0 until 4) {
                    val p = floatArrayOf(
                        (f.fractalConfig.params["p${i + 1}"] as DoubleArray)[0].toFloat(),
                        (f.fractalConfig.params["p${i + 1}"] as DoubleArray)[1].toFloat())
                    // Log.d("RENDER ROUTINE", "passing p${i + 1} in as (${p[0]}, ${p[1]})")
                    GL.glUniform2fv(paramHandles[i], 1, p, 0)
                }

                val x0 = floatArrayOf(f.fractalConfig.map().z0[0].toFloat())
                val y0 = floatArrayOf(f.fractalConfig.map().z0[1].toFloat())


                val xScaleSD = f.fractalConfig.scale()[0] / 2.0
                val yScaleSD = f.fractalConfig.scale()[1] / 2.0
                val xOffsetSD = f.fractalConfig.coords()[0]
                val yOffsetSD = f.fractalConfig.coords()[1]

                // calculate scale/offset parameters and pass to fragment shader
                when (f.precision()) {
                    Precision.SINGLE -> {

                        val xScaleSF = xScaleSD.toFloat()
                        val yScaleSF = yScaleSD.toFloat()
                        val xOffsetSF = xOffsetSD.toFloat()
                        val yOffsetSF = yOffsetSD.toFloat()

                        GL.glUniform2fv(xScaleHandle,  1,  floatArrayOf(xScaleSF, 0.0f),   0)
                        GL.glUniform2fv(yScaleHandle,  1,  floatArrayOf(yScaleSF, 0.0f),   0)
                        GL.glUniform2fv(xOffsetHandle, 1,  floatArrayOf(xOffsetSF, 0.0f),  0)
                        GL.glUniform2fv(yOffsetHandle, 1,  floatArrayOf(yOffsetSF, 0.0f),  0)

                    }
                    Precision.DUAL -> {

                        val xScaleDF = splitSD(xScaleSD)
                        val yScaleDF = splitSD(yScaleSD)
                        val xOffsetDF = splitSD(xOffsetSD)
                        val yOffsetDF = splitSD(yOffsetSD)

                        GL.glUniform2fv(xScaleHandle,  1,  xScaleDF,   0)
                        GL.glUniform2fv(yScaleHandle,  1,  yScaleDF,   0)
                        GL.glUniform2fv(xOffsetHandle, 1,  xOffsetDF,  0)
                        GL.glUniform2fv(yOffsetHandle, 1,  yOffsetDF,  0)

                    }
                    Precision.QUAD -> {

//                val xScaleQF = splitDD(xScaleDD)
//                val yScaleQF = splitDD(yScaleDD)
//                val xOffsetQF = splitDD(xOffsetDD)
//                val yOffsetQF = splitDD(yOffsetDD)
//
//                GL.glUniform4fv(xScaleHandle,  1,  xScaleQF,   0)
//                GL.glUniform4fv(yScaleHandle,  1,  yScaleQF,   0)
//                GL.glUniform4fv(xOffsetHandle, 1,  xOffsetQF,  0)
//                GL.glUniform4fv(yOffsetHandle, 1,  yOffsetQF,  0)

                    }
                    else -> {}
                }

                GL.glEnableVertexAttribArray(viewCoordsHandle)
                GL.glUniform1i(iterHandle, f.fractalConfig.maxIter())
                GL.glUniform1fv(rHandle, 1, floatArrayOf(f.fractalConfig.bailoutRadius()), 0)
                GL.glUniform1fv(xInitHandle, 1, x0, 0)
                GL.glUniform1fv(yInitHandle, 1, y0, 0)




                //======================================================================================
                // RENDER LOW-RES
                //======================================================================================

                GL.glViewport(0, 0, textures[0].res[0], textures[0].res[1])
                GL.glUniform1fv(bgScaleHandle, 1, bgScaleFloat, 0)
                GL.glVertexAttribPointer(
                        viewCoordsHandle,       // index
                        3,                      // coordinates per vertex
                        GL.GL_FLOAT,            // type
                        false,                  // normalized
                        12,                     // coordinates per vertex * bytes per float
                        viewBuffer              // coordinates
                )
                GL.glFramebufferTexture2D(
                        GL.GL_FRAMEBUFFER,              // target
                        GL.GL_COLOR_ATTACHMENT0,        // attachment
                        GL.GL_TEXTURE_2D,               // texture target
                        textures[0].id,                 // texture
                        0                               // level
                )

                GL.glClear(GL.GL_COLOR_BUFFER_BIT)
                GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)




                //======================================================================================
                // RENDER HIGH-RES
                //======================================================================================

                if (strictTranslate()) {

                    val xIntersectQuadCoords : FloatArray
                    val yIntersectQuadCoords : FloatArray
                    val xIntersectViewCoords : FloatArray
                    val yIntersectViewCoords : FloatArray

                    val xComplementViewCoordsA : FloatArray
                    val yComplementViewCoordsA : FloatArray

                    val xComplementViewCoordsB = floatArrayOf(-1.0f, 1.0f)
                    val yComplementViewCoordsB : FloatArray


                    if (xQuadCoords[0] > -1.0) {
                        xIntersectQuadCoords   = floatArrayOf( xQuadCoords[0].toFloat(),   1.0f )
                        xIntersectViewCoords   = floatArrayOf( -1.0f, -xQuadCoords[0].toFloat() )
                        xComplementViewCoordsA = floatArrayOf( -1.0f,  xQuadCoords[0].toFloat() )
                    }
                    else {
                        xIntersectQuadCoords   = floatArrayOf( -1.0f,  xQuadCoords[1].toFloat() )
                        xIntersectViewCoords   = floatArrayOf( -xQuadCoords[1].toFloat(),  1.0f )
                        xComplementViewCoordsA = floatArrayOf(  xQuadCoords[1].toFloat(),  1.0f )
                    }

                    if (yQuadCoords[0] > -1.0) {
                        yIntersectQuadCoords   = floatArrayOf( yQuadCoords[0].toFloat(),   1.0f )
                        yIntersectViewCoords   = floatArrayOf( -1.0f, -yQuadCoords[0].toFloat() )
                        yComplementViewCoordsA = floatArrayOf( yQuadCoords[0].toFloat(),   1.0f )
                        yComplementViewCoordsB = floatArrayOf( -1.0f,  yQuadCoords[0].toFloat() )
                    }
                    else {
                        yIntersectQuadCoords   = floatArrayOf( -1.0f, yQuadCoords[1].toFloat() )
                        yIntersectViewCoords   = floatArrayOf( -yQuadCoords[1].toFloat(), 1.0f )
                        yComplementViewCoordsA = floatArrayOf( -1.0f, yQuadCoords[1].toFloat() )
                        yComplementViewCoordsB = floatArrayOf(  yQuadCoords[1].toFloat(), 1.0f )
                    }


//                    val complementViewCoordsA = floatArrayOf(
//                            xComplementViewCoordsA[0].toFloat(),  yComplementViewCoordsA[1].toFloat(),  0.0f,     // top left
//                            xComplementViewCoordsA[0].toFloat(),  yComplementViewCoordsA[0].toFloat(),  0.0f,     // bottom left
//                            xComplementViewCoordsA[1].toFloat(),  yComplementViewCoordsA[0].toFloat(),  0.0f,     // bottom right
//                            xComplementViewCoordsA[1].toFloat(),  yComplementViewCoordsA[1].toFloat(),  0.0f )    // top right
//                    val complementViewCoordsB = floatArrayOf(
//                            xComplementViewCoordsB[0].toFloat(),  yComplementViewCoordsB[1].toFloat(),  0.0f,     // top left
//                            xComplementViewCoordsB[0].toFloat(),  yComplementViewCoordsB[0].toFloat(),  0.0f,     // bottom left
//                            xComplementViewCoordsB[1].toFloat(),  yComplementViewCoordsB[0].toFloat(),  0.0f,     // bottom right
//                            xComplementViewCoordsB[1].toFloat(),  yComplementViewCoordsB[1].toFloat(),  0.0f )    // top right




                    //===================================================================================
                    // NOVEL RENDER -- TRANSLATION COMPLEMENT
                    //===================================================================================

                    GL.glViewport(0, 0, textures[intIndex].res[0], textures[intIndex].res[1])
                    GL.glUniform1fv(bgScaleHandle, 1, floatArrayOf(1.0f), 0)
                    GL.glVertexAttribPointer(
                            viewCoordsHandle,           // index
                            3,                          // coordinates per vertex
                            GL.GL_FLOAT,                // type
                            false,                      // normalized
                            12,                         // coordinates per vertex * bytes per float
                            viewChunkBuffer             // coordinates
                    )
                    GL.glFramebufferTexture2D(
                            GL.GL_FRAMEBUFFER,              // target
                            GL.GL_COLOR_ATTACHMENT0,        // attachment
                            GL.GL_TEXTURE_2D,               // texture target
                            textures[intIndex].id,          // texture
                            0                               // level
                    )
                    GL.glClear(GL.GL_COLOR_BUFFER_BIT)

                    val chunksA = splitCoords(xComplementViewCoordsA, yComplementViewCoordsA)
                    val chunksB = splitCoords(xComplementViewCoordsB, yComplementViewCoordsB)
                    val totalChunks = chunksA.size + chunksB.size
                    var chunksRendered = 0
                    for (complementViewChunkCoordsA in chunksA) {

                        viewChunkBuffer.put(complementViewChunkCoordsA).position(0)
                        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)
                        GL.glFinish()
                        chunksRendered++
                        if (!f.settingsConfig.continuousRender()) {
                            context.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }
                    for (complementViewChunkCoordsB in chunksB) {

                        viewChunkBuffer.put(complementViewChunkCoordsB).position(0)
                        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)
                        GL.glFinish()
                        chunksRendered++
                        if (!f.settingsConfig.continuousRender()) {
                            context.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }





                    //===================================================================================
                    // SAMPLE -- TRANSLATION INTERSECTION
                    //===================================================================================

                    GL.glUseProgram(sampleProgram)
                    GL.glViewport(0, 0, textures[intIndex].res[0], textures[intIndex].res[1])

                    val intersectQuadCoords = floatArrayOf(
                            xIntersectQuadCoords[0],  yIntersectQuadCoords[1],  0.0f,     // top left
                            xIntersectQuadCoords[0],  yIntersectQuadCoords[0],  0.0f,     // bottom left
                            xIntersectQuadCoords[1],  yIntersectQuadCoords[0],  0.0f,     // bottom right
                            xIntersectQuadCoords[1],  yIntersectQuadCoords[1],  0.0f )    // top right
                    quadBuffer.put(intersectQuadCoords).position(0)

                    val intersectViewCoords = floatArrayOf(
                            xIntersectViewCoords[0],  yIntersectViewCoords[1],  0.0f,     // top left
                            xIntersectViewCoords[0],  yIntersectViewCoords[0],  0.0f,     // bottom left
                            xIntersectViewCoords[1],  yIntersectViewCoords[0],  0.0f,     // bottom right
                            xIntersectViewCoords[1],  yIntersectViewCoords[1],  0.0f )    // top right
                    viewChunkBuffer.put(intersectViewCoords).position(0)


                    GL.glEnableVertexAttribArray(viewCoordsSampleHandle)
                    GL.glEnableVertexAttribArray(quadCoordsSampleHandle)
                    GL.glUniform1i(textureSampleHandle, currIndex)
                    GL.glVertexAttribPointer(
                            viewCoordsSampleHandle,        // index
                            3,                          // coordinates per vertex
                            GL.GL_FLOAT,                // type
                            false,                      // normalized
                            12,                         // coordinates per vertex * bytes per float
                            viewChunkBuffer             // coordinates
                    )
                    GL.glVertexAttribPointer(
                            quadCoordsSampleHandle,        // index
                            3,                          // coordinates per vertex
                            GL.GL_FLOAT,                // type
                            false,                      // normalized
                            12,                         // coordinates per vertex * bytes per float
                            quadBuffer                  // coordinates
                    )


                    GL.glFramebufferTexture2D(
                            GL.GL_FRAMEBUFFER,              // target
                            GL.GL_COLOR_ATTACHMENT0,        // attachment
                            GL.GL_TEXTURE_2D,               // texture target
                            textures[intIndex].id,          // texture
                            0                               // level
                    )

                    GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)

                    GL.glDisableVertexAttribArray(viewCoordsSampleHandle)
                    GL.glDisableVertexAttribArray(quadCoordsSampleHandle)


                    // swap intermediate and current texture indices
                    val temp = intIndex
                    intIndex = currIndex
                    currIndex = temp


                }
                else {

                    //===================================================================================
                    // NOVEL RENDER -- ENTIRE TEXTURE
                    //===================================================================================

                    GL.glViewport(0, 0, textures[currIndex].res[0], textures[currIndex].res[1])
                    GL.glUniform1fv(bgScaleHandle, 1, floatArrayOf(1.0f), 0)
                    GL.glFramebufferTexture2D(
                            GL.GL_FRAMEBUFFER,              // target
                            GL.GL_COLOR_ATTACHMENT0,        // attachment
                            GL.GL_TEXTURE_2D,               // texture target
                            textures[currIndex].id,         // texture
                            0                               // level
                    )

                    // check framebuffer status
                    val status = GL.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)
                    if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
                        Log.d("FRAMEBUFFER", "$status")
                    }

                    GL.glClear(GL.GL_COLOR_BUFFER_BIT)


                    val chunks = splitCoords(floatArrayOf(-1.0f, 1.0f), floatArrayOf(-1.0f, 1.0f))
                    val totalChunks = chunks.size
                    var chunksRendered = 0
                    for (viewChunkCoords in chunks) {

                        viewChunkBuffer.put(viewChunkCoords)
                        viewChunkBuffer.position(0)

                        GL.glVertexAttribPointer(
                                viewCoordsHandle,           // index
                                3,                          // coordinates per vertex
                                GL.GL_FLOAT,                // type
                                false,                      // normalized
                                12,                         // coordinates per vertex * bytes per float
                                viewChunkBuffer             // coordinates
                        )

                        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)
                        GL.glFinish()   // force chunk to finish rendering before continuing
                        chunksRendered++
                        if(!f.settingsConfig.continuousRender()) {
                            context.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }

                    GL.glDisableVertexAttribArray(viewCoordsHandle)

                }




            }
            fun renderFromTexture() {

//        Log.d("RENDER", "render from texture -- start")

                if (f.colorShaderChanged) {
                    Log.d("RENDER ROUTINE", "color shader changed")
                    GL.glDetachShader(colorProgram, fColorShader)
                    fColorShader = loadShader(GL.GL_FRAGMENT_SHADER, f.colorShader())
                    GL.glAttachShader(colorProgram, fColorShader)
                    GL.glLinkProgram(colorProgram)
                    f.colorShaderChanged = false
                }

                //======================================================================================
                // PRE-RENDER PROCESSING
                //======================================================================================

                GL.glUseProgram(colorProgram)
                GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0)
                GL.glViewport(0, 0, f.screenRes[0], f.screenRes[1])

                // create float array of quad coordinates
                val quadCoords = floatArrayOf(
                        xQuadCoords[0].toFloat(),  yQuadCoords[1].toFloat(),  0.0f,     // top left
                        xQuadCoords[0].toFloat(),  yQuadCoords[0].toFloat(),  0.0f,     // bottom left
                        xQuadCoords[1].toFloat(),  yQuadCoords[0].toFloat(),  0.0f,     // bottom right
                        xQuadCoords[1].toFloat(),  yQuadCoords[1].toFloat(),  0.0f )    // top right
                quadBuffer
                        .put(quadCoords)
                        .position(0)

                // create float array of background quad coordinates
                val bgQuadCoords = floatArrayOf(
                        xBgQuadCoords[0].toFloat(),  yBgQuadCoords[1].toFloat(),  0.0f,     // top left
                        xBgQuadCoords[0].toFloat(),  yBgQuadCoords[0].toFloat(),  0.0f,     // bottom left
                        xBgQuadCoords[1].toFloat(),  yBgQuadCoords[0].toFloat(),  0.0f,     // bottom right
                        xBgQuadCoords[1].toFloat(),  yBgQuadCoords[1].toFloat(),  0.0f )    // top right
                bgQuadBuffer
                        .put(bgQuadCoords)
                        .position(0)

                GL.glUniform1i(numColorsHandle, f.colorConfig.palette().size)
                GL.glUniform3fv(paletteHandle, f.colorConfig.palette().size, f.colorConfig.palette().flatPalette, 0)
                GL.glUniform1fv(frequencyHandle, 1, floatArrayOf(f.colorConfig.frequency()), 0)
                GL.glUniform1fv(phaseHandle, 1, floatArrayOf(f.colorConfig.phase()), 0)

                GL.glEnableVertexAttribArray(viewCoordsColorHandle)
                GL.glEnableVertexAttribArray(quadCoordsColorHandle)




                //======================================================================================
                // RENDER LOW-RES
                //======================================================================================

                GL.glUniform1i(textureColorHandle, 0)    // use GL_TEXTURE0
                GL.glVertexAttribPointer(
                        viewCoordsColorHandle,      // index
                        3,                          // coordinates per vertex
                        GL.GL_FLOAT,                // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        viewBuffer                  // coordinates
                )
                GL.glVertexAttribPointer(
                        quadCoordsColorHandle,      // index
                        3,                          // coordinates per vertex
                        GL.GL_FLOAT,                // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        bgQuadBuffer                // coordinates
                )

                GL.glClear(GL.GL_COLOR_BUFFER_BIT)
                GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)




                //======================================================================================
                // RENDER HIGH-RES
                //======================================================================================

                GL.glUniform1i(textureColorHandle, currIndex)
                GL.glVertexAttribPointer(
                        viewCoordsColorHandle,        // index
                        3,                          // coordinates per vertex
                        GL.GL_FLOAT,                // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        viewBuffer                  // coordinates
                )
                GL.glVertexAttribPointer(
                        quadCoordsColorHandle,        // index
                        3,                          // coordinates per vertex
                        GL.GL_FLOAT,                // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        quadBuffer                  // coordinates
                )

                GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)




                GL.glDisableVertexAttribArray(viewCoordsColorHandle)
                GL.glDisableVertexAttribArray(quadCoordsColorHandle)

//        Log.d("RENDER", "render from texture -- end")

                context.findViewById<ProgressBar>(R.id.progressBar).progress = 0

            }

        }

        var renderToTex = false
        private var hasTranslated = false
        private var hasScaled = false
        private val strictTranslate = { hasTranslated && !hasScaled }
        var ignoreTouch = false


        private val xQuadCoords = doubleArrayOf(-1.0, 1.0)
        private val yQuadCoords = doubleArrayOf(-1.0, 1.0)
        private val quadLength = { xQuadCoords[1] - xQuadCoords[0] }
        private val quadScale = { quadLength() / 2.0 }

        private val bgScaleFloat = floatArrayOf(5.0f)
        private val bgScaleDouble = 5.0

        private val xBgQuadCoords = doubleArrayOf(-bgScaleDouble, bgScaleDouble)
        private val yBgQuadCoords = doubleArrayOf(-bgScaleDouble, bgScaleDouble)

        val quadAnchor = doubleArrayOf(0.0, 0.0)
        val quadFocus = doubleArrayOf(0.0, 0.0)
        val t = doubleArrayOf(0.0, 0.0)

        lateinit var rr : RenderRoutine


        fun setQuadAnchor(screenPos: FloatArray) {

            val screenProp = doubleArrayOf(
                    screenPos[0].toDouble()/f.screenRes[0],
                    screenPos[1].toDouble()/f.screenRes[1]
            )
            quadAnchor[0] = screenProp[0]*2.0 - 1.0
            quadAnchor[1] = 1.0 - screenProp[1]*2.0
            // Log.d("RENDERER", "quadAnchor : (${quadAnchor[0]}, ${quadAnchor[1]})")

        }
        fun setQuadFocus(screenDist: FloatArray) {
            // update texture quad coordinates
            // convert focus coordinates from screen space to quad space
            val screenProp = doubleArrayOf(
                    screenDist[0].toDouble() / f.screenRes[0],
                    screenDist[1].toDouble() / f.screenRes[1]
            )

            quadFocus[0] = quadAnchor[0] + screenProp[0]*(2.0/quadScale())
            quadFocus[1] = quadAnchor[1] - screenProp[1]*(2.0/quadScale())

//        quadFocus[0] = (xQuadCoords[0] - quadFocus[0])*(1.0 - screenProp[0]) + screenProp[0]*(xQuadCoords[1] - quadFocus[0])
//        quadFocus[1] = (yQuadCoords[1] - quadFocus[1])*(1.0 - screenProp[1]) + screenProp[1]*(yQuadCoords[0] - quadFocus[1])
//        Log.d("RENDERER", "quadFocus : (${quadFocus[0]}, ${quadFocus[1]})")
        }
        fun translate(dScreenPos: FloatArray) {

            // update texture quad coordinates
            val dQuadPos = doubleArrayOf(
                    dScreenPos[0].toDouble() / f.screenRes[0].toDouble() * 2.0,
                    dScreenPos[1].toDouble() / f.screenRes[1].toDouble() * 2.0
            )

            xQuadCoords[0] += dQuadPos[0]
            xQuadCoords[1] += dQuadPos[0]
            yQuadCoords[0] -= dQuadPos[1]
            yQuadCoords[1] -= dQuadPos[1]

            xBgQuadCoords[0] += dQuadPos[0]
            xBgQuadCoords[1] += dQuadPos[0]
            yBgQuadCoords[0] -= dQuadPos[1]
            yBgQuadCoords[1] -= dQuadPos[1]

            // still magic
            t[0] += dQuadPos[0]
            t[1] -= dQuadPos[1]

            hasTranslated = true

        }
        fun scale(dScale: Float) {

            val tQuadFocus = doubleArrayOf(quadFocus[0] + t[0], quadFocus[1] + t[1])

            // translate quadFocus to origin in quad coordinates
            xQuadCoords[0] -= tQuadFocus[0]
            xQuadCoords[1] -= tQuadFocus[0]
            yQuadCoords[0] -= tQuadFocus[1]
            yQuadCoords[1] -= tQuadFocus[1]

            // scale quad coordinates
            xQuadCoords[0] *= dScale.toDouble()
            xQuadCoords[1] *= dScale.toDouble()
            yQuadCoords[0] *= dScale.toDouble()
            yQuadCoords[1] *= dScale.toDouble()

            // translate origin back to quadFocus in quad coordinates
            xQuadCoords[0] += tQuadFocus[0]
            xQuadCoords[1] += tQuadFocus[0]
            yQuadCoords[0] += tQuadFocus[1]
            yQuadCoords[1] += tQuadFocus[1]



            // translate quadFocus to origin in quad coordinates
            xBgQuadCoords[0] -= tQuadFocus[0]
            xBgQuadCoords[1] -= tQuadFocus[0]
            yBgQuadCoords[0] -= tQuadFocus[1]
            yBgQuadCoords[1] -= tQuadFocus[1]

            // scale quad coordinates
            xBgQuadCoords[0] *= dScale.toDouble()
            xBgQuadCoords[1] *= dScale.toDouble()
            yBgQuadCoords[0] *= dScale.toDouble()
            yBgQuadCoords[1] *= dScale.toDouble()

            // translate origin back to quadFocus in quad coordinates
            xBgQuadCoords[0] += tQuadFocus[0]
            xBgQuadCoords[1] += tQuadFocus[0]
            yBgQuadCoords[0] += tQuadFocus[1]
            yBgQuadCoords[1] += tQuadFocus[1]

            hasScaled = true

            // Log.d("fractalConfig.coords()", "xQuadCoords: (${xQuadCoords[0]}, ${xQuadCoords[1]}), yQuadCoords: (${yQuadCoords[0]}, ${yQuadCoords[1]})")

        }
        private fun resetQuadParams() {

            xQuadCoords[0] = -1.0
            xQuadCoords[1] = 1.0
            yQuadCoords[0] = -1.0
            yQuadCoords[1] = 1.0

            xBgQuadCoords[0] = -bgScaleDouble
            xBgQuadCoords[1] = bgScaleDouble
            yBgQuadCoords[0] = -bgScaleDouble
            yBgQuadCoords[1] = bgScaleDouble

            quadFocus[0] = 0.0
            quadFocus[1] = 0.0

            t[0] = 0.0
            t[1] = 0.0

        }

        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

            // get OpenGL ES version
            Log.d("SURFACE VIEW", "OpenGL ES version: ${unused.glGetString(GL10.GL_VERSION)}")

            // get fragment shader precision
            val a : IntBuffer = IntBuffer.allocate(2)
            val b : IntBuffer = IntBuffer.allocate(1)
            GL.glGetShaderPrecisionFormat(GL.GL_FRAGMENT_SHADER, GL.GL_HIGH_FLOAT, a, b)
            Log.d("SURFACE VIEW", "float precision: ${b[0]}")

            rr = RenderRoutine()
            rr.renderToTexture()

            val buttonScroll = context.findViewById<HorizontalScrollView>(R.id.buttonScroll)
            buttonScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT)

        }
        override fun onDrawFrame(unused: GL10) {

            // Log.d("RENDER", "DRAW FRAME")

            // render to texture on ACTION_UP
            if (renderToTex) {

                ignoreTouch = !(f.settingsConfig.continuousRender() || reaction.name[0].toString() == "P")
                rr.renderToTexture()
                ignoreTouch = false

                renderToTex = false
                hasTranslated = false
                hasScaled = false
                resetQuadParams()

            }

            // render from texture
            rr.renderFromTexture()

        }
        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {

        }

    }

    val r : FractalRenderer
    var hasTranslated = false
    private val h = Handler()
    private val longPressed = Runnable {
        Log.d("SURFACE VIEW", "wow u pressed that so long")

        // vibrate
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        else {
            //deprecated in API 26
            vib.vibrate(15)
        }

        // toggle uiQuick
        val uiQuick = context.findViewById<LinearLayout>(R.id.uiQuick)
        val v : Int
        if (uiQuick.visibility == LinearLayout.VISIBLE) {
            v = LinearLayout.INVISIBLE
        }
        else {
            v = LinearLayout.VISIBLE
            uiQuick.bringToFront()
        }
        uiQuick.visibility = v

    }

    var reaction = Reaction.TRANSFORM
    val numDisplayParams = {
        when (reaction) {
            Reaction.TRANSFORM -> 3
            Reaction.COLOR -> 2
            else -> 3
        }
    }

    private val prevFocus = floatArrayOf(0.0f, 0.0f)
    private val edgeRightSize = 150
    private var prevFocalLen = 1.0f
    private val minPixelMove = 5f


    init {

        setEGLContextClientVersion(3)               // create OpenGL ES 3.0 context
        r = FractalRenderer(f, context)             // create renderer
        setRenderer(r)                              // set renderer
        renderMode = RENDERMODE_WHEN_DIRTY          // only render on init and explicitly
    }

    fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        systemUiVisibility = (
                SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_FULLSCREEN
        )



    }
    fun showSystemUI() {
        systemUiVisibility = (
                SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        )
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {

        if (!r.ignoreTouch) {

            // monitor for long press
            when(e?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val focus = e.focus()
                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]
                    h.postDelayed(longPressed, 500)
                }
                MotionEvent.ACTION_MOVE -> {
                    val focus = e.focus()
                    val dx: Float = focus[0] - prevFocus[0]
                    val dy: Float = focus[1] - prevFocus[1]
                    if (sqrt(dx*dx + dy*dy) > minPixelMove) { h.removeCallbacks(longPressed) }
                }
                MotionEvent.ACTION_UP -> {
                    h.removeCallbacks(longPressed)
                }
            }

            when (reaction) {
                Reaction.TRANSFORM -> {

                    // actions change fractal
                    when (e?.actionMasked) {

                        MotionEvent.ACTION_DOWN -> {
                            // Log.d("TRANSFORM", "POINTER DOWN -- x: ${e.x}, y: ${e.y}")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]

                            if (!f.settingsConfig.continuousRender()) {
                                r.setQuadAnchor(focus)
                                r.setQuadFocus(floatArrayOf(0.0f, 0.0f))
                            }

                            return true
                        }
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            // Log.d("TRANSFORM", "POINTER ${e.actionIndex} DOWN -- x: ${e.x}, y: ${e.y}")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            prevFocalLen = e.focalLength()
                            // Log.d("TRANSFORM", "focalLen: $prevFocalLen")
                            if (!f.settingsConfig.continuousRender()) {
                                r.setQuadFocus(floatArrayOf(
                                        focus[0] - e.getX(0),
                                        focus[1] - e.getY(0)
                                ))
                            }
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val focus = e.focus()
                            val dx: Float = focus[0] - prevFocus[0]
                            val dy: Float = focus[1] - prevFocus[1]

                            // Log.d("TRANSFORM", "MOVE -- dx: $dx, dy: $dy")
                            f.translate(floatArrayOf(dx, dy))
                            hasTranslated = true
                            if (!f.settingsConfig.continuousRender()) {
                                r.translate(floatArrayOf(dx, dy))
                            }
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            if (e.pointerCount > 1) {   // MULTI-TOUCH
                                val focalLen = e.focalLength()
                                // Log.d("TRANSFORM", "MOVE -- prevFocalLen: $prevFocalLen, focalLen: $focalLen")
                                val dFocalLen = focalLen / prevFocalLen
                                f.scale(dFocalLen, focus)
                                if (!f.settingsConfig.continuousRender()) {
                                    r.scale(dFocalLen)
                                }
                                prevFocalLen = focalLen
                            }

                            if (f.settingsConfig.continuousRender()) {
                                r.renderToTex = true
                            }
                            requestRender()

                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            // Log.d("TRANSFORM", "POINTER UP")
                            if (hasTranslated) {
                                r.renderToTex = true
                                requestRender()
                            }
                            hasTranslated = false
                            return true
                        }
                        MotionEvent.ACTION_POINTER_UP -> {
                            // Log.d("TRANSFORM", "POINTER ${e.actionIndex} UP")
                            if (e.getPointerId(e.actionIndex) == 0) {
                                prevFocus[0] = e.getX(1)
                                prevFocus[1] = e.getY(1)

                                // change quad anchor to remaining pointer
                                if (!f.settingsConfig.continuousRender()) {
                                    r.setQuadAnchor(floatArrayOf(e.getX(1), e.getY(1)))
                                }
                            } else if (e.getPointerId(e.actionIndex) == 1) {
                                prevFocus[0] = e.getX(0)
                                prevFocus[1] = e.getY(0)
                            }
                            return true
                        }

                    }
                }
                Reaction.COLOR -> {
                    // actions change coloring
                    when (e?.actionMasked) {

                        MotionEvent.ACTION_DOWN -> {
                            // Log.d("COLOR", "POINTER DOWN -- x: ${e.x}, y: ${e.y}")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            return true
                        }
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            // Log.d("COLOR", "POINTER ${e.actionIndex} DOWN -- x: ${e.x}, y: ${e.y}")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            prevFocalLen = e.focalLength()
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val focus = e.focus()
                            val dx: Float = focus[0] - prevFocus[0]
                            // Log.d("COLOR", "MOVE -- dx: $dx")
                            when (e.pointerCount) {
                                1 -> {
                                    f.setPhase(dx)
                                    prevFocus[0] = focus[0]
                                    prevFocus[1] = focus[1]
                                }
                                2 -> {
                                    val focalLen = e.focalLength()
                                    val dFocalLen = focalLen / prevFocalLen
                                    f.setFrequency(dFocalLen)
                                    prevFocalLen = focalLen
                                }
                            }
                            requestRender()
                            return true
                        }
                        MotionEvent.ACTION_POINTER_UP -> {
                            // Log.d("COLOR", "POINTER ${e.actionIndex} UP")
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            // Log.d("COLOR", "ACTION UP")
                            return true
                        }

                    }
                }
                Reaction.P1, Reaction.P2, Reaction.P3, Reaction.P4 -> {
                    // actions change light position
                    when (e?.actionMasked) {

                        MotionEvent.ACTION_DOWN -> {
                            // Log.d("PARAMETER", "POINTER DOWN -- x: ${e.x}, y: ${e.y}")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            return true
                        }
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            // Log.d("PARAMETER", "POINTER ${e.actionIndex} DOWN -- x: ${e.x}, y: ${e.y}")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            prevFocalLen = e.focalLength()
                            // Log.d("PARAMETER", "POINTER DOWN -- focalLen: $prevFocalLen")
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val focus = e.focus()
                            val dx = focus[0] - prevFocus[0]
                            val dy = focus[1] - prevFocus[1]
                            // Log.d("PARAMETER", "MOVE -- dx: $dx, dy: $dy")
                            when (e.pointerCount) {
                                1 -> {
                                    f.setMapParam(reaction.name[1].toString().toInt(), floatArrayOf(dx, dy))
                                    prevFocus[0] = focus[0]
                                    prevFocus[1] = focus[1]
                                    r.renderToTex = true
                                    requestRender()
                                }
                                2 -> {
                                    val focalLen = e.focalLength()
                                    val dFocalLen = focalLen / prevFocalLen
                                    f.setMapParamSensetivity(reaction.name[1].toString().toInt(), dFocalLen)
                                    prevFocalLen = focalLen
                                }
                            }
                            return true
                        }
                        MotionEvent.ACTION_POINTER_UP -> {
                            // Log.d("PARAMETER", "POINTER ${e.actionIndex} UP")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            // Log.d("PARAMETER", "POINTER UP")
                            return true
                        }

                    }
                }
            }
        }

        return false

    }

}



class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {

    private val mFragmentList = ArrayList<Fragment>()
    private val mFragmentTitleList = ArrayList<String>()

    override fun getItem(position: Int) : Fragment {
        return mFragmentList[position]
    }

    override fun getCount() : Int {
        return mFragmentList.size
    }

    fun addFrag(fragment: Fragment, title: String) {
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }

    override fun getPageTitle(position: Int) : CharSequence {
        return mFragmentTitleList[position]
    }

}








class MainActivity : AppCompatActivity(),
        EquationFragment.OnParamChangeListener,
        ColorFragment.OnParamChangeListener,
        SettingsFragment.OnParamChangeListener {


    private lateinit var f : Fractal
    private lateinit var fractalView : FractalSurfaceView
    private lateinit var uiQuickButtons : List<View>
    private lateinit var displayParamTextViews : List<TextView>

    private var orientation = Configuration.ORIENTATION_UNDEFINED



    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        orientation = baseContext.resources.configuration.orientation
        Log.d("MAIN ACTIVITY", "orientation: $orientation")
        val orientationChanged = (savedInstanceState?.getInt("orientation") ?: orientation) != orientation

        // get screen dimensions
        val displayMetrics = baseContext.resources.displayMetrics
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val aspectRatio = screenHeight.toDouble() / screenWidth

        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0


        // get saved or default parameters
        val frequency = savedInstanceState?.getFloat("frequency") ?: 1.0f
        val phase = savedInstanceState?.getFloat("phase") ?: 0.0f


        val equationConfig = FractalConfig(mutableMapOf(
                "map"               to  ComplexMap.sine4(resources),
                "p1"                to  doubleArrayOf(0.0, 0.0),
                "p2"                to  doubleArrayOf(0.0, 0.0),
                "p3"                to  doubleArrayOf(0.0, 0.0),
                "p4"                to  doubleArrayOf(0.0, 0.0),
                "texture"           to  TextureAlgorithm.escape(resources),
                "juliaMode"         to  false,
                "paramSensitivity"  to  1.0,
                "coords"            to  doubleArrayOf(0.0, 0.0),
                "scale"             to  doubleArrayOf(1.0, 1.0*aspectRatio),
                "maxIter"           to  255,
                "bailoutRadius"     to  1e5f
        ))
        val colorConfig = ColorConfig(mutableMapOf(
                "palette"           to  ColorPalette.p8,
                "frequency"         to  frequency,
                "phase"             to  phase
        ))
        val settingsConfig = SettingsConfig(mutableMapOf(
                "resolution"        to Resolution.HIGH,
                "precision"         to Precision.AUTO,
                "continuousRender"  to false,
                "displayParams"     to false
        ))


        // create fractal
        f = Fractal(
                this,
                equationConfig,
                colorConfig,
                settingsConfig,
                intArrayOf(screenWidth, screenHeight)
        )

        if (orientationChanged) {
            f.switchOrientation()
            Log.d("MAIN", "orientation changed")
        }


        fractalView = FractalSurfaceView(f, this)
        fractalView.layoutParams = ViewGroup.LayoutParams(screenWidth, screenHeight)
        fractalView.hideSystemUI()

        setContentView(R.layout.activity_main)

        val fractalLayout = findViewById<FrameLayout>(R.id.layout_main)
        fractalLayout.addView(fractalView)


        val displayParams = findViewById<LinearLayout>(R.id.displayParams)
        displayParamTextViews = listOf(
            findViewById(R.id.displayParam1),
            findViewById(R.id.displayParam2),
            findViewById(R.id.displayParam3)
        )
        displayParams.removeViews(1, displayParams.childCount - 1)


        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        val buttonBackgrounds = arrayOf(
            resources.getDrawable(R.drawable.round_button_unselected, null),
            resources.getDrawable(R.drawable.round_button_selected, null)
        )
        uiQuickButtons = listOf(
            findViewById(R.id.transformButton),
            findViewById(R.id.colorButton),
            findViewById(R.id.paramButton1),
            findViewById(R.id.paramButton2),
            findViewById(R.id.paramButton3),
            findViewById(R.id.paramButton4)
        )
        val uiQuickButtonListener = View.OnClickListener {
            val s = when (it) {
                is Button -> it.text.toString()
                is ImageButton -> it.contentDescription.toString()
                else -> ""
            }
            fractalView.reaction = Reaction.valueOf(s)
            (displayParams.getChildAt(0) as TextView).text = when (fractalView.reaction) {
                Reaction.TRANSFORM, Reaction.COLOR -> s
                else -> "PARAMETER ${s[1]}"
            }

            if (fractalView.f.settingsConfig.displayParams()) {
                displayParams.removeViews(1, displayParams.childCount - 1)
                for (i in 0 until fractalView.numDisplayParams()) {
                    displayParams.addView(displayParamTextViews[i])
                }
            }

            for (b in uiQuickButtons) {
                val btd = b.background as TransitionDrawable
                if (b == it) { btd.startTransition(0) }
                else { btd.resetTransition() }
            }
            fractalView.f.updateDisplayParams(Reaction.valueOf(s), true)
        }
        for (b in uiQuickButtons) {
            b.background = TransitionDrawable(buttonBackgrounds)
            b.setOnClickListener(uiQuickButtonListener)
        }
        val diff = f.fractalConfig.map().initMapParams.size - uiQuick.childCount + 2
        uiQuick.removeViews(0, abs(diff))
        uiQuick.bringToFront()
        uiQuickButtons[0].performClick()

        val buttonScroll = findViewById<HorizontalScrollView>(R.id.buttonScroll)
        val leftArrow = findViewById<ImageView>(R.id.leftArrow)
        val rightArrow = findViewById<ImageView>(R.id.rightArrow)
        leftArrow.alpha = 0f
        rightArrow.alpha = 0f

        buttonScroll.viewTreeObserver.addOnScrollChangedListener {
            if (uiQuick.width > buttonScroll.width) {
                val scrollX = buttonScroll.scrollX
                val scrollEnd = uiQuick.width - buttonScroll.width
                // Log.d("MAIN ACTIVITY", "scrollX: $scrollX")
                // Log.d("MAIN ACTIVITY", "scrollEnd: $scrollEnd")
                when {
                    scrollX > 5 -> leftArrow.alpha = 1f
                    scrollX < 5 -> leftArrow.alpha = 0f
                }
                when {
                    scrollX < scrollEnd - 5 -> rightArrow.alpha = 1f
                    scrollX > scrollEnd - 5 -> rightArrow.alpha = 0f
                }
            }
            else {
                leftArrow.alpha = 0f
                rightArrow.alpha = 0f
            }
        }


        val uiFullTabs = findViewById<TabLayout>(R.id.uiFullTabs)
        uiFullTabs.tabGravity = TabLayout.GRAVITY_FILL





        // initialize fragments and set UI params from fractal
        val equationFragment = EquationFragment()
        val colorFragment = ColorFragment()
        val settingsFragment = SettingsFragment()

        equationFragment.config = f.fractalConfig
        colorFragment.config = f.colorConfig
        settingsFragment.config = f.settingsConfig






        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFrag( equationFragment,  "Equation" )
        adapter.addFrag( colorFragment,     "Color"    )
        adapter.addFrag( settingsFragment,  "Settings" )

        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(uiFullTabs))

        uiFullTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
                Log.d("MAIN ACTIVITY", "tab: ${tab.text}")
                when (tab.text) {
                    "fractal" -> {
                        Log.d("MAIN ACTIVITY", "Fractal tab selected")
                        f.updatePositionEditTexts()
                        f.updateMapParamEditTexts()
                    }
                    else -> {}
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })





        val phi = 0.5*(sqrt(5.0) + 1.0)
//        val uiFullHeightOpen = (screenHeight*(1.0 - 1.0/phi)).toInt()
        val uiFullHeightOpen = screenHeight/2 - 200
        val uiFullHeightFullscreen = screenHeight - statusBarHeight
        fractalView.y = -uiFullHeightOpen/2f

        val uiFull = findViewById<LinearLayout>(R.id.uiFull)
        uiFull.layoutParams.height = uiFullHeightOpen
        uiFull.bringToFront()

        val uiFullButton = findViewById<ImageButton>(R.id.uiFullButton)
        uiFullButton.setOnClickListener {
            val hStart : Int
            val hEnd : Int
            if (uiFull.height == 1) {
                hStart = 1
                hEnd = uiFullHeightOpen
            }
            else {
                hStart = uiFullHeightOpen
                hEnd = 1
            }

            val anim = ValueAnimator.ofInt(hStart, hEnd)
            anim.addUpdateListener { animation ->
                val intermediateHeight = animation?.animatedValue as Int
                val c = ConstraintSet()
                c.clone(overlay)
                c.constrainHeight(R.id.uiFull, intermediateHeight)
                c.applyTo(overlay)
                fractalView.y = -uiFull.height/2.0f
            }
            anim.duration = 300
            anim.start()
        }


        val overlay = findViewById<ConstraintLayout>(R.id.overlay)
        overlay.bringToFront()


        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        f.updatePositionEditTexts()
        f.updateMapParamEditTexts()


    }

    fun addMapParams(n: Int) {
        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        for (i in 1..n) { uiQuick.addView(uiQuickButtons[uiQuick.childCount], 0) }
    }
    fun removeMapParams(n: Int) {
        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        for (i in 1..n) { uiQuick.removeView(uiQuickButtons[uiQuick.childCount - 1]) }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(  "colorAlgName",   f.fractalConfig.texture().name    )
        outState?.putFloat(   "frequency",      f.colorConfig.frequency()         )
        outState?.putFloat(   "phase",          f.colorConfig.phase()             )
        outState?.putFloat(   "bailoutRadius",  f.fractalConfig.bailoutRadius()  )
        outState?.putInt(     "orientation",    orientation                       )
        super.onSaveInstanceState(outState)
    }
    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
    }
    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        when (fragment) {
            is ColorFragment    -> fragment.setOnParamChangeListener(this)
            is SettingsFragment -> fragment.setOnParamChangeListener(this)
            is EquationFragment -> fragment.setOnParamChangeListener(this)
        }
    }
    override fun onEquationParamsChanged(key: String, value: Any) {
        if (f.fractalConfig.params[key] != value) {
            Log.d("MAIN ACTIVITY", "$key set from ${f.fractalConfig.params[key]} to $value")
            f.fractalConfig.params[key] = value
            when (key) {
                "map" -> {
                    f.reset()
                    f.renderShaderChanged = true
                    val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
                    removeMapParams(uiQuick.childCount - 2)
                    addMapParams(f.fractalConfig.map().initMapParams.size)
                }
                "p1", "p2", "p3", "p4" -> {
                    f.updateMapParamEditText(key[1].toString().toInt())
                }
                "texture" -> {
                    if (f.fractalConfig.texture().name == "Escape Time Smooth with Lighting") {
                        f.colorShaderChanged = true
                    }
                    f.renderShaderChanged = true
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
                "juliaMode" -> {
                    if (value as Boolean) { addMapParams(1) }
                    else { removeMapParams(1) }
                    f.updateMapParamEditTexts()
                    f.renderShaderChanged = true
                }
                "coords" -> {
                    fractalView.requestFocus()
                }
                "maxIter" -> {

                }
                "bailoutRadius" -> {

                }
            }
            fractalView.r.renderToTex = true
            fractalView.requestRender()
        }
        else {
            Log.d("MAIN ACTIVITY", "$key already set to $value")
        }
    }
    override fun onColorParamsChanged(key: String, value: Any) {
        if (f.colorConfig.params[key] != value) {
            Log.d("MAIN ACTIVITY", "$key set from ${f.colorConfig.params[key]} to $value")
            f.colorConfig.params[key] = value
            when (key) {
                "palette" -> {
                    fractalView.requestRender()
                }
            }
        }
        else {
            Log.d("MAIN ACTIVITY", "$key already set to $value")
        }
    }
    override fun onSettingsParamsChanged(key: String, value: Any) {
        if (f.settingsConfig.params[key] != value) {
            Log.d("MAIN ACTIVITY", "$key set from ${f.settingsConfig.params[key]} to $value")
            f.settingsConfig.params[key] = value
            when (key) {
                "resolution" -> {
                    f.resolutionChanged = true
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
                "precision" -> {
                    f.renderShaderChanged = true
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
                "continuousRender" -> {
                    f.renderProfileChanged = true
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
                "displayParams" -> {
                    val displayParams = findViewById<LinearLayout>(R.id.displayParams)
                    if (value as Boolean) {
                        for (i in 0 until fractalView.numDisplayParams()) {
                            displayParams.addView(displayParamTextViews[i])
                        }
                        fractalView.f.updateDisplayParams(fractalView.reaction, false)
                    }
                    else {
                        displayParams.removeViews(1, displayParams.childCount - 1)
                    }
                }
            }
        }
        else {
            Log.d("MAIN ACTIVITY", "$key already set to $value")
        }
    }


}
