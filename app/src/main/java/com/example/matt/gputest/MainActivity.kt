package com.example.matt.gputest

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.Renderer
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import javax.microedition.khronos.egl.EGLConfig
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.Sampler
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.opengl.GLES32 as GL
import android.widget.FrameLayout.LayoutParams as LP
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
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

}

class ComplexMap (
        val name        : String,
        val z0          : DoubleArray,
        val initCoords  : DoubleArray,
        val initScale   : Double,
        val params      : List<DoubleArray>,
        val initSF      : String?,
        val loopSF      : String?,
        val finalSF     : String?,
        val initDF      : String?,
        val loopDF      : String?,
        val finalDF     : String?
) {
    
    companion object {
        
        val empty       = { ComplexMap(
                "Empty",
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(0.0, 0.0),
                1.0,
                listOf(doubleArrayOf(0.0, 0.0)),
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
                doubleArrayOf(-0.75, 0.0),
                3.5,
                listOf(doubleArrayOf(2.0, 0.0)),
                "",
                res.getString(R.string.mandelbrot_loop_sf),
                "",
                "",
                res.getString(R.string.mandelbrot_loop_df),
                ""
        )}
        val dualpow     = { res: Resources -> ComplexMap(
                "Dual Power",
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, 0.0),
                3.0,
                listOf(doubleArrayOf(0.0, 0.0)),
                res.getString(R.string.dualpow_init_sf),
                res.getString(R.string.dualpow_loop_sf),
                "",
                "",
                "",
                ""
        )}
        val sine1       = { res: Resources -> ComplexMap(
                "Sine 1",
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, 0.0),
                3.5,
                listOf(doubleArrayOf(0.31960705187983646, 0.0)),
                "",
                res.getString(R.string.sine1_loop_sf),
                "",
                "",
                "",
                ""
        ) }
        val sine2       = { res: Resources -> ComplexMap(
                "Sine 2",
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, 0.0),
                3.5,
                listOf(doubleArrayOf(-0.26282883851642613, 2.042520182493586E-6)),
                "",
                res.getString(R.string.sine2_loop_sf),
                "",
                "",
                "",
                ""
        )}
        val sine3       = { res: Resources -> ComplexMap(
                "Sine 3",
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, 0.0),
                3.5,
                listOf(doubleArrayOf(0.0, 0.0)),
                "",
                res.getString(R.string.sine3_loop_sf),
                "",
                "",
                "",
                ""
        )}
        val all         = mapOf(
            "Mandelbrot"  to  mandelbrot,
            "Dual Power"  to  dualpow,
            "Sine 1"      to  sine1,
            "Sine 2"      to  sine2,
            "Sine 3"      to  sine3
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

class ColorAlgorithm (
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
            ColorAlgorithm("Empty", "", "", "", "", "", "")
        }
        val escape              = { res: Resources -> ColorAlgorithm(
                "Escape Time",
                "",
                "",
                res.getString(R.string.escape_final),
                "",
                "",
                res.getString(R.string.escape_final)
        )}
        val escapeSmooth        = { res: Resources -> ColorAlgorithm(
                "Escape Time Smooth",
                "",
                "",
                res.getString(R.string.mandelbrot_smooth_final_sf),
                "",
                "",
                res.getString(R.string.mandelbrot_smooth_final_df)
        )}
        val lighting            = { res: Resources -> ColorAlgorithm(
                "Lighting",
                res.getString(R.string.mandelbrot_light_init_sf),
                res.getString(R.string.mandelbrot_light_loop_sf),
                res.getString(R.string.mandelbrot_light_final),
                res.getString(R.string.mandelbrot_light_init_df),
                res.getString(R.string.mandelbrot_light_loop_df),
                res.getString(R.string.mandelbrot_light_final)
        )}
        val triangleIneqAvg     = { res: Resources -> ColorAlgorithm(
                "Triangle Inequality Average",
                res.getString(R.string.mandelbrot_triangle_init_sf),
                res.getString(R.string.mandelbrot_triangle_loop_sf),
                res.getString(R.string.mandelbrot_triangle_final_sf),
                res.getString(R.string.mandelbrot_triangle_init_df),
                res.getString(R.string.mandelbrot_triangle_loop_df),
                res.getString(R.string.mandelbrot_triangle_final_df)
        )}
        val curvatureAvg        = { res: Resources -> ColorAlgorithm(
                "Curvature Average",
                res.getString(R.string.curvature_init),
                res.getString(R.string.curvature_loop_sf),
                res.getString(R.string.curvature_final_sf),
                res.getString(R.string.curvature_init),
                res.getString(R.string.curvature_loop_df),
                res.getString(R.string.curvature_final_df)
        )}
        val stripeAvg           = { res: Resources -> ColorAlgorithm(
                "Stripe Average",
                res.getString(R.string.stripe_init),
                res.getString(R.string.stripe_loop_sf).format(5.0f),
                res.getString(R.string.stripe_final_sf),
                res.getString(R.string.stripe_init),
                res.getString(R.string.stripe_loop_df).format(5.0f),
                res.getString(R.string.stripe_final_df)
        )}
        val orbitTrap           = { res: Resources -> ColorAlgorithm(
                "Orbit Trap",
                res.getString(R.string.minmod_init),
                res.getString(R.string.minmod_loop_sf),
                res.getString(R.string.minmod_final),
                res.getString(R.string.minmod_init),
                res.getString(R.string.minmod_loop_df),
                res.getString(R.string.minmod_final)
        )}
        val overlayAvg          = { res: Resources -> ColorAlgorithm(
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

    fun add(alg : ColorAlgorithm) : ColorAlgorithm {
        return ColorAlgorithm(
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
        return other is ColorAlgorithm && name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}



enum class Precision { SINGLE, DUAL, QUAD, AUTO }

enum class Reaction { TRANSFORM, COLOR, PARAM1 }

enum class Resolution { LOW, MED, HIGH, ULTRA }



class EquationConfig (val params : MutableMap<String, Any>) {

    val map                 = { params["map"]            as ComplexMap  }
    val coords              = { params["coords"]         as DoubleArray }
    val scale               = { params["scale"]          as DoubleArray }
    val bailoutRadius       = { params["bailoutRadius"]  as FloatArray  }

}

class ColorConfig (val params : MutableMap<String, Any>) {

    val algorithm          = { params["algorithm"]  as  ColorAlgorithm }
    val palette            = { params["palette"]    as  ColorPalette   }
    val frequency          = { params["frequency"]  as  FloatArray     }
    val phase              = { params["phase"]      as  FloatArray     }
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
    return Math.sqrt(Math.pow(dist[0].toDouble(), 2.0) +
            Math.pow(dist[1].toDouble(), 2.0)).toFloat()
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
        val context             : Activity,
        val equationConfig      : EquationConfig,
        val colorConfig         : ColorConfig,
        val settingsConfig      : SettingsConfig,
        val screenRes           : IntArray
) {

    private var header       : String = ""
    private var arithmetic   : String = ""
    private var init         : String = ""
    private var conditional  : String = ""
    private var algInit      : String = ""
    private var algLoop      : String = ""
    private var algFinal     : String = ""
    private var mapInit      : String = ""
    private var mapLoop      : String = ""
    private var mapFinal     : String = ""

    private val colorHeader = context.resources.getString(R.string.color_header)
    private val colorIndex = context.resources.getString(R.string.color_index)
    private var colorPostIndex  : String = ""


//    private val precisionThreshold = 7e-5
    private val precisionThreshold = 0
    private val aspectRatio = screenRes[1].toDouble()/screenRes[0]
    var renderShaderChanged = false
    var colorShaderChanged = false
    var resolutionChanged = false
    var renderProfileChanged = false
    var innerColor = "1.0"

    val autoPrecision = {
        if (equationConfig.scale()[0] > precisionThreshold) Precision.SINGLE else Precision.DUAL
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
    var maxIter = 0


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

        resetPosition()

    }


    fun resetPosition() {
        equationConfig.coords()[0] = equationConfig.map().initCoords[0]
        equationConfig.coords()[1] = equationConfig.map().initCoords[1]
        equationConfig.scale()[0] = equationConfig.map().initScale
        equationConfig.scale()[1] = equationConfig.map().initScale * aspectRatio
        updateDisplayParams()
    }

    @SuppressLint("SetTextI18n")
    fun updateDisplayParams() {

        val xCoordDisplay = context.findViewById<TextView>(R.id.xCoordDisplay)
        val yCoordDisplay = context.findViewById<TextView>(R.id.yCoordDisplay)
        val scaleDisplay = context.findViewById<TextView>(R.id.scaleDisplay)
        val frequencyDisplay = context.findViewById<TextView>(R.id.frequencyDisplay)
        val phaseDisplay = context.findViewById<TextView>(R.id.phaseDisplay)

        xCoordDisplay?.text = "x:  %.17f".format(equationConfig.coords()[0])
        yCoordDisplay?.text = "y:  %.17f".format(equationConfig.coords()[1])
        scaleDisplay?.text = "scale:  %e".format(equationConfig.scale()[0])

        frequencyDisplay?.text = "frequency:  %.2f".format(colorConfig.frequency()[0])
        phaseDisplay?.text = "phase:  %.2f".format(abs(colorConfig.phase()[0] % 1.0))


        val xCoordEdit = context.findViewById<EditText>(R.id.xCoordEdit)
        val yCoordEdit = context.findViewById<EditText>(R.id.yCoordEdit)
        val scaleSignificandEdit = context.findViewById<EditText>(R.id.scaleSignificandEdit)
        val scaleExponentEdit = context.findViewById<EditText>(R.id.scaleExponentEdit)
        val scaleStrings = "%e".format(equationConfig.scale()[0]).split("e")

        xCoordEdit?.setText("%.17f".format(equationConfig.coords()[0]))
        yCoordEdit?.setText("%.17f".format(equationConfig.coords()[1]))
        scaleSignificandEdit?.setText(scaleStrings[0])
        scaleExponentEdit?.setText(scaleStrings[1])

    }

    private fun loadRenderResources() {

        when(precision()) {
            Precision.SINGLE -> {
                header      = context.resources.getString(R.string.header_sf)
                arithmetic  = context.resources.getString(R.string.arithmetic_sf)
                init        = context.resources.getString(R.string.general_init_sf)
                conditional = context.resources.getString(R.string.bailout_sf)
                mapInit     = equationConfig.map().initSF       ?: ""
                algInit     = colorConfig.algorithm().initSF    ?: ""
                mapLoop     = equationConfig.map().loopSF       ?: ""
                algLoop     = colorConfig.algorithm().loopSF    ?: ""
                mapFinal    = equationConfig.map().finalSF      ?: ""
                algFinal    = colorConfig.algorithm().finalSF   ?: ""
            }
            Precision.DUAL -> {

                header      = context.resources.getString(R.string.header_df)
                arithmetic  = context.resources.getString(R.string.arithmetic_util) +
                                context.resources.getString(R.string.arithmetic_sf) +
                                context.resources.getString(R.string.arithmetic_df)
                init        = context.resources.getString(R.string.general_init_df)
                conditional = context.resources.getString(R.string.bailout_df)
                mapInit     = equationConfig.map().initDF       ?: ""
                algInit     = colorConfig.algorithm().initDF    ?: ""
                mapLoop     = equationConfig.map().loopDF       ?: ""
                algLoop     = colorConfig.algorithm().loopDF    ?: ""
                mapFinal    = equationConfig.map().finalDF      ?: ""
                algFinal    = colorConfig.algorithm().finalDF   ?: ""

            }
            else -> {}
        }

    }

    private fun loadColorResources() {

        if (colorConfig.algorithm().name == "Escape Time Smooth with Lighting") {
            colorPostIndex = context.resources.getString(R.string.color_lighting)
            innerColor = "1.0"
        }
        else {
            colorPostIndex = ""
            innerColor = "0.0"
        }

    }

    fun switchOrientation() {

//        val equationConfig.coords() = doubleArrayOf((xCoords[0] + xCoords[1]) / 2.0, -(yCoords[0] + yCoords[1]) / 2.0)
//        translate(equationConfig.coords())
//
//        // rotation by 90 degrees counter-clockwise
//        val xCoordsNew = doubleArrayOf(-yCoords[1], -yCoords[0])
//        val yCoordsNew = doubleArrayOf(xCoords[0], xCoords[1])
//        xCoords[0] = xCoordsNew[0]
//        xCoords[1] = xCoordsNew[1]
//        yCoords[0] = yCoordsNew[0]
//        yCoords[1] = yCoordsNew[1]
//
//        translate(equationConfig.coords().negative())

//        Log.d("FRACTAL", "xCoordsNew:  (${xCoordsNew[0]}, ${xCoordsNew[1]})")
//        Log.d("FRACTAL", "yCoordsNew:  (${yCoordsNew[0]}, ${yCoordsNew[1]})")


    }

    fun setMapParam(dPos: FloatArray) {
        // dx -- [0, screenWidth]
        equationConfig.map().params[0][0] += equationConfig.scale()[0]*dPos[0]/screenRes[0]
        equationConfig.map().params[0][1] += equationConfig.scale()[1]*dPos[1]/screenRes[1]

        // SINE2 :: (-0.26282883851642613, 2.042520182493586E-6)
        // SINE2 :: (-0.999996934286532, 9.232660318047263E-5)
        // SINE2 :: (-0.2287186333845716, 0.1340647963904784)
        // SINE1 :: -0.578539160583084
        // SINE1 :: -0.8717463705274795
        // SINE1 :: 0.2948570315666499
        // SINE1 :: 0.31960705187983646

        Log.d("FRACTAL", "touchPos set to (${equationConfig.map().params[0][0]}, ${equationConfig.map().params[0][1]})")
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
                equationConfig.coords()[0] -= (dScreenPos[0] / screenRes[0])*equationConfig.scale()[0]
                equationConfig.coords()[1] += (dScreenPos[1] / screenRes[1])*equationConfig.scale()[1]
            }
        }

        updateDisplayParams()
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
                equationConfig.coords()[0] += dPos[0]
                equationConfig.coords()[1] += dPos[1]
            }
        }

        updateDisplayParams()
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
                    equationConfig.coords()[0] + (prop[0] - 0.5)*equationConfig.scale()[0],
                    equationConfig.coords()[1] - (prop[1] - 0.5)*equationConfig.scale()[1]
                )
                Log.d("FRACTAL", "focus (coordinates) -- x: ${focus[0]}, y: ${focus[1]}")

                translate(focus.negative())
                equationConfig.coords()[0] = equationConfig.coords()[0] / dScale
                equationConfig.coords()[1] = equationConfig.coords()[1] / dScale
                translate(focus)
                equationConfig.scale()[0] = equationConfig.scale()[0] / dScale
                equationConfig.scale()[1] = equationConfig.scale()[1] / dScale
            }
        }

//        Log.d("FRACTAL", "length of x-interval: ${abs(xCoords[1] - xCoords[0])}")

        val precisionPostScale = precision()
        if (precisionPostScale != precisionPreScale) {
            renderShaderChanged = true
            Log.d("FRACTAL", "precision changed")
        }

        updateDisplayParams()
//        Log.d("FRACTAL", "scale -- dscale: $dScale")

    }

    fun setFrequency(dScale: Float) {
        colorConfig.frequency()[0] = colorConfig.frequency()[0] * dScale
        updateDisplayParams()
    }

    fun setPhase(dx: Float) {
        colorConfig.phase()[0] = colorConfig.phase()[0] + dx/screenRes[0]
        updateDisplayParams()
    }

}



@SuppressLint("ViewConstructor")
class FractalSurfaceView(
        var f                 : Fractal,
        private val context   : Activity
) : GLSurfaceView(context) {

//    val texPixels = f.texRes[0]*f.texRes[1]

    val r : FractalRenderer
    var reaction = Reaction.TRANSFORM

    private val prevFocus = floatArrayOf(0.0f, 0.0f)
    private val edgeRightSize = 150
    private var prevFocalLen = 1.0f


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

            if (f.screenRes[0] - (e?.x ?: 0.0f) < edgeRightSize) {
                when (e?.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {

                        val focus = e.focus()
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]

                        Log.d("UI", "DOWN -- EDGE RIGHT")
                        val quickUI = context.findViewById<LinearLayout>(R.id.quickUI)
                        val v : Int
                        if (quickUI.visibility == LinearLayout.VISIBLE) {
                            v = LinearLayout.INVISIBLE
                        }
                        else {
                            v = LinearLayout.VISIBLE
                            quickUI.bringToFront()
                        }
                        quickUI.visibility = v
                        return true

                    }
                    MotionEvent.ACTION_MOVE -> {

                        val focus = e.focus()
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]

                        Log.d("UI", "MOVE -- EDGE RIGHT")
                        return true

                    }
                    MotionEvent.ACTION_UP -> { return true }
                }
            }

            when (reaction) {
                Reaction.TRANSFORM -> {

                    // actions change fractal
                    when (e?.actionMasked) {

                        MotionEvent.ACTION_DOWN -> {
                            Log.d("TRANSFORM", "POINTER DOWN -- x: ${e.x}, y: ${e.y}")
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
                            Log.d("TRANSFORM", "POINTER ${e.actionIndex} DOWN -- x: ${e.x}, y: ${e.y}")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            prevFocalLen = e.focalLength()
                            Log.d("TRANSFORM", "focalLen: $prevFocalLen")
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
                            f.translate(floatArrayOf(dx, dy))
                            if (!f.settingsConfig.continuousRender()) {
                                r.translate(floatArrayOf(dx, dy))
                            }
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            if (e.pointerCount > 1) {   // MULTI-TOUCH
                                val focalLen = e.focalLength()
                                Log.d("TRANSFORM", "prevFocalLen: $prevFocalLen, focalLen: $focalLen")
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
//                            Log.d("UP", "x: ${e.x}, y: ${e.y}")
                            r.renderToTex = true
                            requestRender()
                            return true
                        }
                        MotionEvent.ACTION_POINTER_UP -> {
//                            Log.d("POINTER UP ${e.actionIndex}", "x: ${e.x}, y: ${e.y}")
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
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
//                            Log.d("DOWN", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                            return true
                        }
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            prevFocalLen = e.focalLength()
//                            Log.d("POINTER DOWN", "focus: $focus, focalLen: $prevFocalLen")
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val focus = e.focus()
                            val dx: Float = focus[0] - prevFocus[0]
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

                    }
                }
                Reaction.PARAM1 -> {
                    // actions change light position
                    when (e?.actionMasked) {

                        MotionEvent.ACTION_DOWN -> {
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
//                            Log.d("DOWN", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val focus = e.focus()
                            val dPos = floatArrayOf(
                                focus[0] - prevFocus[0],
                                focus[1] - prevFocus[1]
                            )
                            when (e.pointerCount) {
                                1, 2 -> {
                                    f.setMapParam(dPos)
                                    prevFocus[0] = focus[0]
                                    prevFocus[1] = focus[1]
                                }
                            }
                            r.renderToTex = true
                            requestRender()
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            requestRender()
                            return true
                        }

                    }
                }
            }
        }

        return false

    }

}

class FractalRenderer(var f: Fractal, val context: Activity) : Renderer {

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
        private val p1Handle         : Int
        private val bgScaleHandle    : Int

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
            p1Handle         =  GL.glGetUniformLocation(  renderProgram, "P1"          )
            bgScaleHandle    =  GL.glGetUniformLocation(  renderProgram, "bgScale"     )

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

            val p1 = floatArrayOf(
                    f.equationConfig.map().params[0][0].toFloat(),
                    f.equationConfig.map().params[0][1].toFloat()
            )

            Log.d("RENDER ROUTINE", "p1 passed in as (${p1[0]}, ${p1[1]})")

            val x0 = floatArrayOf(f.equationConfig.map().z0[0].toFloat())
            val y0 = floatArrayOf(f.equationConfig.map().z0[1].toFloat())


            val xScaleSD = f.equationConfig.scale()[0] / 2.0
            val yScaleSD = f.equationConfig.scale()[1] / 2.0
            val xOffsetSD = f.equationConfig.coords()[0]
            val yOffsetSD = f.equationConfig.coords()[1]

            // calculate scale/offset parameters and pass to fragment shader
            when (f.precision()) {
                Precision.SINGLE -> {

                    val xScaleSF = xScaleSD.toFloat()
                    val yScaleSF = yScaleSD.toFloat()
                    val xOffsetSF = xOffsetSD.toFloat()
                    val yOffsetSF = yOffsetSD.toFloat()

                    Log.d("RENDER ROUTINE", "xScale: $xScaleSF")
                    Log.d("RENDER ROUTINE", "yScale: $yScaleSF")
                    Log.d("RENDER ROUTINE", "xOffset: $xOffsetSF")
                    Log.d("RENDER ROUTINE", "yOffset: $yOffsetSF")

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
            GL.glUniform1i(iterHandle, f.maxIter)
            GL.glUniform1fv(rHandle, 1, f.equationConfig.bailoutRadius(), 0)
            GL.glUniform1fv(xInitHandle, 1, x0, 0)
            GL.glUniform1fv(yInitHandle, 1, y0, 0)
            GL.glUniform2fv(p1Handle,  1,  p1, 0)




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
            GL.glUniform1fv(frequencyHandle, 1, f.colorConfig.frequency(), 0)
            GL.glUniform1fv(phaseHandle, 1, f.colorConfig.phase(), 0)

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
    var ignoreTouch = false
    private var hasTranslated = false
    private var hasScaled = false
    private val strictTranslate = { hasTranslated && !hasScaled }


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

        // Log.d("equationConfig.coords()", "xQuadCoords: (${xQuadCoords[0]}, ${xQuadCoords[1]}), yQuadCoords: (${yQuadCoords[0]}, ${yQuadCoords[1]})")

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

    }

    override fun onDrawFrame(unused: GL10) {

        // Log.d("RENDER", "DRAW FRAME")

        // render to texture on ACTION_UP
        if (renderToTex) {

            ignoreTouch = !f.settingsConfig.continuousRender()
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
//    private lateinit var colorAlgorithms : Map<String, ColorAlgorithm>
    private var orientation = Configuration.ORIENTATION_UNDEFINED



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


        // get saved or default parameters
        val frequency = savedInstanceState?.getFloatArray("frequency") ?: floatArrayOf(1.0f)
        val phase = savedInstanceState?.getFloatArray("phase") ?: floatArrayOf(0.0f)


        val equationConfig = EquationConfig(mutableMapOf(
                "map"               to  ComplexMap.sine2(resources),
                "coords"            to  doubleArrayOf(0.0, 0.0),
                "scale"             to  doubleArrayOf(1.0, 1.0*aspectRatio),
                "bailoutRadius"     to  floatArrayOf(1e5f)
        ))
        val colorConfig = ColorConfig(mutableMapOf(
                "algorithm"         to  ColorAlgorithm.escapeSmooth(resources),
                "palette"           to  ColorPalette.p6,
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

        val quickUI = findViewById<LinearLayout>(R.id.quickUI)




        val buttonBackgrounds = arrayOf(
            resources.getDrawable(R.drawable.round_button_unselected, null),
            resources.getDrawable(R.drawable.round_button_selected, null)
        )
        val transformButtonBackground = TransitionDrawable(buttonBackgrounds)
        val colorButtonBackground = TransitionDrawable(buttonBackgrounds)
        val param1ButtonBackground = TransitionDrawable(buttonBackgrounds)

        val transformButton = findViewById<ImageButton>(R.id.transformButton)
        transformButton.setOnClickListener {
            fractalView.reaction = Reaction.TRANSFORM
            (transformButton.background as TransitionDrawable).startTransition(0)
            (colorButton.background as TransitionDrawable).resetTransition()
            (paramButton1.background as TransitionDrawable).resetTransition()
            val toast = Toast.makeText(baseContext, "TRANSFORM", Toast.LENGTH_SHORT)
            toast.show()
        }
        transformButton.background = transformButtonBackground

        val colorButton = findViewById<ImageButton>(R.id.colorButton)
        colorButton.setOnClickListener {
            fractalView.reaction = Reaction.COLOR
            (colorButton.background as TransitionDrawable).startTransition(0)
            (transformButton.background as TransitionDrawable).resetTransition()
            (paramButton1.background as TransitionDrawable).resetTransition()
            val toast = Toast.makeText(baseContext, "COLOR", Toast.LENGTH_SHORT)
            toast.show()
        }
        colorButton.background = colorButtonBackground

        val paramButton1 = findViewById<Button>(R.id.paramButton1)
        paramButton1.setOnClickListener {
            fractalView.reaction = Reaction.PARAM1
            (paramButton1.background as TransitionDrawable).startTransition(0)
            (colorButton.background as TransitionDrawable).resetTransition()
            (transformButton.background as TransitionDrawable).resetTransition()
            val toast = Toast.makeText(baseContext, "PARAMETER 1", Toast.LENGTH_SHORT)
            toast.show()
        }
        paramButton1.background = param1ButtonBackground




        val maxIterBar = findViewById<SeekBar>(R.id.maxIterBar)
        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val p: Float = i.toFloat() / 100.0f
                fractalView.f.maxIter = ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(11) - 1)*p).toInt()
                if (f.settingsConfig.continuousRender()) {
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (!f.settingsConfig.continuousRender()) {
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
            }

        })
        maxIterBar.progress = 20

        quickUI.bringToFront()



        val displayParams = findViewById<LinearLayout>(R.id.displayParams)
        displayParams.bringToFront()


        val fullUITabs = findViewById<TabLayout>(R.id.fullUITabs)
        fullUITabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
//                fractalView.reaction = when (tab.text.toString()) {
//                    "Equation"  -> Reaction.TRANSFORM
//                    "Color"     -> Reaction.COLOR
//                    "Settings"  -> Reaction.TRANSFORM
//                    else        -> Reaction.TRANSFORM
//                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }

        })
        fullUITabs.tabGravity = TabLayout.GRAVITY_FILL





        // initialize fragments and set UI params from fractal
        val equationFragment = EquationFragment()
        val colorFragment = ColorFragment()
        val settingsFragment = SettingsFragment()

        equationFragment.config = f.equationConfig
        colorFragment.config = f.colorConfig
        settingsFragment.config = f.settingsConfig






        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFrag( equationFragment,  "Equation" )
        adapter.addFrag( colorFragment,     "Color"    )
        adapter.addFrag( settingsFragment,  "Settings" )

        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(fullUITabs))

        fullUITabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
                when (tab.text) {
                    "Equation" -> f.updateDisplayParams()
                    else -> {}
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })



        val phi = 0.5*(sqrt(5.0) + 1.0)
        val fullUIHeight = (screenHeight*(1.0 - 1.0/phi)).toInt()
        Log.d("MAIN ACTIVITY", "fullUIHeight set to $fullUIHeight")


        val fullUI = findViewById<LinearLayout>(R.id.fullUI)
        fullUI.layoutParams.height = fullUIHeight
        Log.d("MAIN ACTIVITY", "fullUI height set to $fullUIHeight")
        fullUI.bringToFront()


        val overlay = findViewById<ConstraintLayout>(R.id.overlay)
        overlay.bringToFront()


        val fullUIButton = findViewById<Button>(R.id.fullUIButton)
        fullUIButton.setOnClickListener {
            val uiIsVisible = fullUI.visibility == FrameLayout.VISIBLE
            val newSurfaceViewPos : Float
            val v : Int
            val c = ConstraintSet()
            c.clone(overlay)
            if (uiIsVisible) {
                v = FrameLayout.INVISIBLE
                c.connect(
                        R.id.fullUIButton, ConstraintSet.BOTTOM,
                        R.id.overlay, ConstraintSet.BOTTOM
                )
                newSurfaceViewPos = 0.0f
            }
            else {
                v = FrameLayout.VISIBLE
                c.connect(
                        R.id.fullUIButton, ConstraintSet.BOTTOM,
                        R.id.fullUI, ConstraintSet.TOP
                )
                newSurfaceViewPos = -fullUIHeight/2.0f
            }
            c.applyTo(overlay)
            fullUI.visibility = v
            quickUI.visibility = LinearLayout.INVISIBLE
            fractalView.y = newSurfaceViewPos
            f.updateDisplayParams()
        }
        fullUI.visibility = FrameLayout.INVISIBLE



        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        f.updateDisplayParams()
        setDisplayParamsVisibility(false)


    }



    private fun setDisplayParamsVisibility(isChecked: Boolean) {
        val v : Int
        val c = ConstraintSet()
        c.clone(overlay)
        if (isChecked) {
            v = LinearLayout.VISIBLE
            c.connect(
                    R.id.quickUI, ConstraintSet.BOTTOM,
                    R.id.displayParams, ConstraintSet.TOP
            )
        }
        else {
            v = LinearLayout.INVISIBLE
            c.connect(
                    R.id.quickUI, ConstraintSet.BOTTOM,
                    R.id.progressBar, ConstraintSet.TOP
            )
        }
        c.applyTo(overlay)
        findViewById<LinearLayout>(R.id.displayParams).visibility = v
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(       "colorAlgName",   f.colorConfig.algorithm().name    )
        outState?.putFloatArray(   "frequency",      f.colorConfig.frequency()         )
        outState?.putFloatArray(   "phase",          f.colorConfig.phase()             )
        outState?.putFloatArray(   "bailoutRadius",  f.equationConfig.bailoutRadius()  )
        outState?.putInt(          "orientation",    orientation                       )
        super.onSaveInstanceState(outState)
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
        if (f.equationConfig.params[key] != value) {
            Log.d("MAIN ACTIVITY", "$key set from ${f.equationConfig.params[key]} to $value")
            f.equationConfig.params[key] = value
            when (key) {
                "map" -> {
                    f.resetPosition()
                }
                "coords" -> {
                    fractalView.requestFocus()
                }
            }
            f.renderShaderChanged = true
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
                "algorithm" -> {
                    if (f.colorConfig.algorithm().name == "Escape Time Smooth with Lighting") {
                        f.colorShaderChanged = true
                    }
                    f.renderShaderChanged = true
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
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
                "displayParams" -> setDisplayParamsVisibility(value as Boolean)
            }
        }
        else {
            Log.d("MAIN ACTIVITY", "$key already set to $value")
        }
    }


}
