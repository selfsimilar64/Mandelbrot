package com.example.matt.gputest

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.Renderer
import android.content.res.Configuration
import android.graphics.Bitmap
import javax.microedition.khronos.egl.EGLConfig
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.opengl.GLES32 as GL
import android.widget.FrameLayout.LayoutParams as LP
import android.util.Log
import android.view.*
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

class ColorPalette (colors: List<FloatArray>) {

    companion object Color {

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

    }

    private val palette = colors.plus(colors[0])
    val size = palette.size
    val flatPalette = FloatArray(palette.size * 3) {i: Int ->
        val a = floor(i / 3.0f).toInt()
        val b = i % 3
        palette[a][b]
    }

}

class ComplexMap (
        val name     : String,
        val z0       : DoubleArray,
        val initSF   : String,
        val loopSF   : String,
        val finalSF  : String,
        val initDF   : String,
        val loopDF   : String,
        val finalDF  : String
)

class ColorAlgorithm (
        val name     : String,
        val initSF   : String,
        val loopSF   : String,
        val finalSF  : String,
        val initDF   : String,
        val loopDF   : String,
        val finalDF  : String
) {

    fun add(alg : ColorAlgorithm) : ColorAlgorithm {
        return ColorAlgorithm(
                name + alg.name,
                initSF + alg.initSF,
                loopSF + alg.loopSF,
                finalSF + alg.finalSF,
                initDF + alg.initDF,
                loopDF + alg.loopDF,
                finalDF + alg.finalDF
        )
    }

}

enum class Precision { SINGLE, DUAL, QUAD }

enum class Reaction { TRANSFORM, COLOR, PARAM }

enum class Resolution { LOW, MED, HIGH, ULTRA }



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
        var map                    : ComplexMap,
        var alg                    : ColorAlgorithm,
        var palette                : ColorPalette,
        private val context        : Activity,
        private val xCoordsInit    : DoubleArray,
        private val yCoordsInit    : DoubleArray,
        val screenRes              : IntArray,
        val colorParams            : MutableMap<String, Any>,
        val settingsParams         : MutableMap<String, String>
) {

    private var header       : String
    private var arithmetic   : String
    private var init         : String
    private var conditional  : String
    private var mapInit      : String
    private var algInit      : String
    private var mapLoop      : String
    private var algLoop      : String
    private var mapFinal     : String
    private var algFinal     : String

    private val precisionThreshold = 6e-5
    var renderShaderChanged = false
    var resolutionChanged = false
    val resolution = {
        Resolution.valueOf( settingsParams["resolution"].toString() )
    }
    val precision = {
        val p = settingsParams["precision"].toString()
        if (p == "AUTO") {
            if (abs(xCoords[1] - xCoords[0]) > precisionThreshold) Precision.SINGLE else Precision.DUAL
        }
        else { Precision.valueOf( settingsParams["precision"].toString() ) }
    }

    var texRes = {
        when (resolution()) {
            Resolution.LOW -> intArrayOf(screenRes[0]/8, screenRes[1]/8)
            Resolution.MED -> intArrayOf(screenRes[0]/4, screenRes[1]/4)
            Resolution.HIGH -> screenRes
            Resolution.ULTRA -> intArrayOf((7*screenRes[0])/4, (7*screenRes[1])/4)
        }
    }
    val xCoords = xCoordsInit.clone()
    val yCoords = yCoordsInit.clone()
    val touchPos = doubleArrayOf(1.0, 1.0)
    var maxIter = 0

    val shader = {

        when (precision()) {
            Precision.SINGLE -> {
                header      = context.resources.getString(R.string.header_sf)
                arithmetic  = context.resources.getString(R.string.arithmetic_sf)
                init        = context.resources.getString(R.string.general_init_sf)
                conditional = context.resources.getString(R.string.bailout_sf)
                mapInit     = map.initSF
                algInit     = alg.initSF
                mapLoop     = map.loopSF
                algLoop     = alg.loopSF
                mapFinal    = map.finalSF
                algFinal    = alg.finalSF
            }
            Precision.DUAL -> {

                header = context.resources.getString(R.string.header_df)
                arithmetic =
                        context.resources.getString(R.string.arithmetic_util) +
                        context.resources.getString(R.string.arithmetic_sf)   +
                        context.resources.getString(R.string.arithmetic_df)
                init = context.resources.getString(R.string.general_init_df)
                conditional = context.resources.getString(R.string.bailout_df)
                mapInit     = map.initDF
                algInit     = alg.initDF
                mapLoop     = map.loopDF
                algLoop     = alg.loopDF
                mapFinal    = map.finalDF
                algFinal    = alg.finalDF

            }
            Precision.QUAD -> {
                header = ""
                arithmetic = ""
                init = ""
                conditional = ""
                mapInit = ""
                algInit = ""
                mapLoop = ""
                algLoop = ""
                mapFinal = ""
                algFinal = ""
            }
        }

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


    init {

        when(precision()) {
            Precision.SINGLE -> {
                header      = context.resources.getString(R.string.header_sf)
                arithmetic  = context.resources.getString(R.string.arithmetic_sf)
                init        = context.resources.getString(R.string.general_init_sf)
                conditional = context.resources.getString(R.string.bailout_sf)
                mapInit     = map.initSF
                algInit     = alg.initSF
                mapLoop     = map.loopSF
                algLoop     = alg.loopSF
                mapFinal    = map.finalSF
                algFinal    = alg.finalSF
            }
            Precision.DUAL -> {

                header = context.resources.getString(R.string.header_df)

                arithmetic =
                        context.resources.getString(R.string.arithmetic_util) +
                                context.resources.getString(R.string.arithmetic_sf) +
                                context.resources.getString(R.string.arithmetic_df)

                init = context.resources.getString(R.string.general_init_df)

                conditional = context.resources.getString(R.string.bailout_df)

                mapInit     = map.initDF
                algInit     = alg.initDF
                mapLoop     = map.loopDF
                algLoop     = alg.loopDF
                mapFinal    = map.finalDF
                algFinal    = alg.finalDF

            }
            Precision.QUAD -> {
                header = ""
                arithmetic = ""
                init = ""
                conditional = ""
                mapInit = ""
                algInit = ""
                mapLoop = ""
                algLoop = ""
                mapFinal = ""
                algFinal = ""
            }
        }

    }



    fun resetCoords() {
        xCoords[0] = xCoordsInit[0]
        xCoords[1] = xCoordsInit[1]
        yCoords[0] = yCoordsInit[0]
        yCoords[1] = yCoordsInit[1]
    }

    @SuppressLint("SetTextI18n")
    private fun updateDisplayParams() {

        val xCoordView = context.findViewById<TextView>(R.id.xCoordText)
        val yCoordView = context.findViewById<TextView>(R.id.yCoordText)
        val scaleView = context.findViewById<TextView>(R.id.scaleText)

        xCoordView.text = "x:  ${(xCoords[1] + xCoords[0])/2.0}"
        yCoordView.text = "y:  ${(yCoords[1] + yCoords[0])/2.0}"

        val scale = (xCoords[1] - xCoords[0])/(xCoordsInit[1] - xCoordsInit[0])
        scaleView.text = "scale:  %e".format(scale)

    }

    fun switchOrientation() {

        val center = doubleArrayOf((xCoords[0] + xCoords[1]) / 2.0, -(yCoords[0] + yCoords[1]) / 2.0)
        translate(center)

        // rotation by 90 degrees counter-clockwise
        val xCoordsNew = doubleArrayOf(-yCoords[1], -yCoords[0])
        val yCoordsNew = doubleArrayOf(xCoords[0], xCoords[1])
        xCoords[0] = xCoordsNew[0]
        xCoords[1] = xCoordsNew[1]
        yCoords[0] = yCoordsNew[0]
        yCoords[1] = yCoordsNew[1]

        translate(center.negative())

//        Log.d("FRACTAL", "xCoordsNew:  (${xCoordsNew[0]}, ${xCoordsNew[1]})")
//        Log.d("FRACTAL", "yCoordsNew:  (${yCoordsNew[0]}, ${yCoordsNew[1]})")


    }

    fun setTouchPos(screenPos: DoubleArray) {
        touchPos[0] = xCoords[0] + screenPos[0]*(xCoords[1] - xCoords[0])
        touchPos[1] = yCoords[1] - screenPos[1]*(yCoords[1] - yCoords[0])
    }

    fun translate(dScreenPos: FloatArray) {

        // update complex coordinates
        when (precision()) {
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
                val dPos = doubleArrayOf(
                        (dScreenPos[0].toDouble() / screenRes[0].toDouble()) * (xCoords[1] - xCoords[0]),
                        (dScreenPos[1].toDouble() / screenRes[1].toDouble()) * (yCoords[1] - yCoords[0])
                )
                xCoords[0] -= dPos[0]
                xCoords[1] -= dPos[0]
                yCoords[0] += dPos[1]
                yCoords[1] += dPos[1]
            }
        }

        updateDisplayParams()
        Log.d("FRACTAL", "translation (pixels) -- dx: ${dScreenPos[0]}, dy: ${dScreenPos[1]}")

    }

    fun translate(dPos: DoubleArray) {

        // update complex coordinates
        when (precision()) {
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
                xCoords[0] -= dPos[0]
                xCoords[1] -= dPos[0]
                yCoords[0] += dPos[1]
                yCoords[1] += dPos[1]
            }
        }

        updateDisplayParams()
        Log.d("FRACTAL", "translation (coordinates) -- dx: ${dPos[0]}, dy: ${dPos[1]}")

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
                        xCoords[0] * (1.0 - prop[0]) + prop[0] * xCoords[1],
                        yCoords[1] * (1.0 - prop[1]) + prop[1] * yCoords[0]
                )

                // translate focus to origin in complex coordinates
                xCoords[0] -= focus[0]
                xCoords[1] -= focus[0]
                yCoords[0] -= focus[1]
                yCoords[1] -= focus[1]

                // scale complex coordinates
                xCoords[0] = xCoords[0] / dScale
                xCoords[1] = xCoords[1] / dScale
                yCoords[0] = yCoords[0] / dScale
                yCoords[1] = yCoords[1] / dScale

                // translate origin back to focus in complex coordinates
                xCoords[0] += focus[0]
                xCoords[1] += focus[0]
                yCoords[0] += focus[1]
                yCoords[1] += focus[1]
            }
        }

//        Log.d("FRACTAL", "length of x-interval: ${abs(xCoords[1] - xCoords[0])}")

        val precisionPostScale = precision()
        if (precisionPostScale != precisionPreScale) {
            renderShaderChanged = true
            Log.d("FRACTAL", "precision changed")
        }

        updateDisplayParams()
        Log.d("FRACTAL", "scale -- dscale: $dScale")

    }

}

@SuppressLint("ViewConstructor")
class FractalSurfaceView(
        var f               : Fractal,
        val context         : Activity,
        val screenWidth     : Int,
        val screenHeight    : Int
) : GLSurfaceView(context) {
    
//    val texPixels = f.texRes[0]*f.texRes[1]

    val r : FractalRenderer
    var reactionType = Reaction.TRANSFORM
    val continuousRender = true

    private val minPointerDist = 500.0f
    private val prevFocus = floatArrayOf(0.0f, 0.0f)
    private val edgeRightSize = 150
    private var prevFocalLen = 1.0f

    private var visibleUI = false


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

    fun hideAppUI() {
        context.findViewById<Button>(R.id.transformButton).visibility = Button.INVISIBLE
        context.findViewById<Button>(R.id.colorButton).visibility = Button.INVISIBLE
        context.findViewById<Button>(R.id.paramButton1).visibility = Button.INVISIBLE
        context.findViewById<Button>(R.id.fullUIButton).visibility = SeekBar.INVISIBLE
        context.findViewById<SeekBar>(R.id.maxIterBar).visibility = SeekBar.INVISIBLE
    }
    fun showAppUI() {
        context.findViewById<ConstraintLayout>(R.id.test).bringToFront()
        context.findViewById<Button>(R.id.transformButton).visibility = Button.VISIBLE
        context.findViewById<Button>(R.id.colorButton).visibility = Button.VISIBLE
        context.findViewById<Button>(R.id.paramButton1).visibility = Button.VISIBLE
        context.findViewById<Button>(R.id.fullUIButton).visibility = SeekBar.VISIBLE
        context.findViewById<SeekBar>(R.id.maxIterBar).visibility = SeekBar.VISIBLE
    }
    fun toggleAppUI() {
        if (visibleUI) { hideAppUI() }
        else { showAppUI() }
        visibleUI = !visibleUI
    }




    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {

        if (!r.ignoreTouch) {

            if (screenWidth - (e?.x ?: 0.0f) < edgeRightSize) {
                when (e?.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {

                        val focus = e.focus()
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]

                        Log.d("UI", "DOWN -- EDGE RIGHT")
                        toggleAppUI()
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

            when (reactionType) {
                Reaction.TRANSFORM -> {

                    // actions change fractal
                    when (e?.actionMasked) {

                        MotionEvent.ACTION_DOWN -> {
                            Log.d("TRANSFORM", "POINTER DOWN -- x: ${e.x}, y: ${e.y}")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]

                            if (!continuousRender) {
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
                            if (!continuousRender) {
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
                            if (!continuousRender) {
                                r.translate(floatArrayOf(dx, dy))
                            }
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            if (e.pointerCount > 1) {   // MULTI-TOUCH
                                val focalLen = e.focalLength()
                                Log.d("TRANSFORM", "prevFocalLen: $prevFocalLen, focalLen: $focalLen")
                                val dFocalLen = focalLen / prevFocalLen
                                f.scale(dFocalLen, focus)
                                if (!continuousRender) {
                                    r.scale(dFocalLen)
                                }
                                prevFocalLen = focalLen
                            }

                            if (continuousRender) {
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
                                if (!continuousRender) {
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
                            f.colorParams["phase"] = (f.colorParams["phase"] as Float) + dx / screenWidth
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            if (e.pointerCount > 1) {   // MULTI-TOUCH
                                val focalLen = e.focalLength()
                                val dFocalLen = focalLen / prevFocalLen
                                f.colorParams["frequency"] = (f.colorParams["frequency"] as Float) * dFocalLen
                                prevFocalLen = focalLen
                            }
                            requestRender()
                            return true
                        }

                    }
                }
                Reaction.PARAM -> {
                    // actions change light position
                    when (e?.actionMasked) {

                        MotionEvent.ACTION_DOWN -> {

                            val screenPos = doubleArrayOf(
                                    e.x.toDouble() / screenWidth.toDouble(),
                                    e.y.toDouble() / screenHeight.toDouble()
                            )
                            f.setTouchPos(screenPos)
                            r.renderToTex = true
                            requestRender()
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            requestRender()
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {

                            val screenPos = doubleArrayOf(
                                    e.x.toDouble() / screenWidth.toDouble(),
                                    e.y.toDouble() / screenHeight.toDouble()
                            )
                            f.setTouchPos(screenPos)
                            r.renderToTex = true

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

        private val maxPixelsPerChunk = f.screenRes[0]*f.screenRes[1]/10

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
        private val xInitHandle      : Int
        private val yInitHandle      : Int
        private val xScaleHandle     : Int
        private val yScaleHandle     : Int
        private val xOffsetHandle    : Int
        private val yOffsetHandle    : Int
        private val xTouchHandle     : Int
        private val yTouchHandle     : Int
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
        private val xTouchColorHandle     : Int
        private val yTouchColorHandle     : Int


        private val vRenderShader : Int
        private val vSampleShader : Int

        private var fRenderShader : Int
        private val fSampleShader : Int
        private val fColorShader  : Int

        // define texture resolutions
//        private val bgTexWidth = f.screenRes[0]/8
//        private val bgTexHeight = f.screenRes[1]/8
        private val bgTexWidth = 1
        private val bgTexHeight = 1

        private val interpolation = {
            if (f.resolution() == Resolution.ULTRA) GL.GL_LINEAR
            else GL.GL_NEAREST
        }

        private val textures = arrayOf(
                Texture(intArrayOf(bgTexWidth, bgTexHeight), interpolation(), GL.GL_RGBA16F, 0),
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

            s = context.resources.openRawResource(R.raw.render_qf)
            val fRenderCodeQF = Scanner(s).useDelimiter("\\Z").next()
            s.close()

            s = context.resources.openRawResource(R.raw.sample)
            val fSampleCode = Scanner(s).useDelimiter("\\Z").next()
            s.close()

            s = context.resources.openRawResource(R.raw.color)
            val fColorCode = Scanner(s).useDelimiter("\\Z").next()
            s.close()


            // create and compile shaders
            vRenderShader = loadShader(GL.GL_VERTEX_SHADER, vRenderCode)
            vSampleShader = loadShader(GL.GL_VERTEX_SHADER, vSampleCode)

            fRenderShader = loadShader(GL.GL_FRAGMENT_SHADER, f.shader())
            fSampleShader = loadShader(GL.GL_FRAGMENT_SHADER, fSampleCode)
            fColorShader  = loadShader(GL.GL_FRAGMENT_SHADER, fColorCode)


            GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

            // generate texture and framebuffer objects
            GL.glGenFramebuffers(1, fboIDs)



            // attach shaders and create renderProgram executables
            // render to texture renderProgram
            GL.glAttachShader(renderProgram, vRenderShader)
            GL.glAttachShader(renderProgram, fRenderShader)
            GL.glLinkProgram(renderProgram)

            viewCoordsHandle =  GL.glGetAttribLocation(   renderProgram, "viewCoords"  )
            iterHandle       =  GL.glGetUniformLocation(  renderProgram, "maxIter"     )
            xInitHandle      =  GL.glGetUniformLocation(  renderProgram, "x0"          )
            yInitHandle      =  GL.glGetUniformLocation(  renderProgram, "y0"          )
            xScaleHandle     =  GL.glGetUniformLocation(  renderProgram, "xScale"      )
            yScaleHandle     =  GL.glGetUniformLocation(  renderProgram, "yScale"      )
            xOffsetHandle    =  GL.glGetUniformLocation(  renderProgram, "xOffset"     )
            yOffsetHandle    =  GL.glGetUniformLocation(  renderProgram, "yOffset"     )
            xTouchHandle     =  GL.glGetUniformLocation(  renderProgram, "xTouchPos"   )
            yTouchHandle     =  GL.glGetUniformLocation(  renderProgram, "yTouchPos"   )
            bgScaleHandle    =  GL.glGetUniformLocation(  renderProgram, "bgScale"     )

            // render from texture renderProgram
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
            xTouchColorHandle     = GL.glGetUniformLocation(  colorProgram, "xTouchPos" )
            yTouchColorHandle     = GL.glGetUniformLocation(  colorProgram, "yTouchPos" )

        }

        private fun loadShader(type: Int, shaderCode: String): Int {

            // create a vertex shader type (GL.GL_VERTEX_SHADER)
            // or a fragment shader type (GL.GL_FRAGMENT_SHADER)
            val shader = GL.glCreateShader(type)

            // add the source code to the shader and compile it
            GL.glShaderSource(shader, shaderCode)
            GL.glCompileShader(shader)

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
                fRenderShader = loadShader(GL.GL_FRAGMENT_SHADER, f.shader())
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

            context.findViewById<ProgressBar>(R.id.progressBar).progress = 0

            GL.glUseProgram(renderProgram)
            GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer

            val xTouchPos = floatArrayOf(f.touchPos[0].toFloat())
            val yTouchPos = floatArrayOf(f.touchPos[1].toFloat())

            val x0 = floatArrayOf(f.map.z0[0].toFloat())
            val y0 = floatArrayOf(f.map.z0[1].toFloat())

            // calculate scale/offset parameters and pass to fragment shader
            when (f.precision()) {
                Precision.SINGLE -> {
                    val xScaleSD = (f.xCoords[1] - f.xCoords[0]) / 2.0
                    val yScaleSD = (f.yCoords[1] - f.yCoords[0]) / 2.0
                    val xOffsetSD = f.xCoords[1] - xScaleSD
                    val yOffsetSD = f.yCoords[1] - yScaleSD

//                    val xScaleSF = floatArrayOf(xScaleSD.toFloat())
//                    val yScaleSF = floatArrayOf(yScaleSD.toFloat())
//                    val xOffsetSF = floatArrayOf(xOffsetSD.toFloat())
//                    val yOffsetSF = floatArrayOf(yOffsetSD.toFloat())

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
                    val xScaleSD = (f.xCoords[1] - f.xCoords[0]) / 2.0
                    val yScaleSD = (f.yCoords[1] - f.yCoords[0]) / 2.0
                    val xOffsetSD = f.xCoords[1] - xScaleSD
                    val yOffsetSD = f.yCoords[1] - yScaleSD

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
//                val xScaleDD = (xCoordsDD[1] - xCoordsDD[0]) * DualDouble(0.5, 0.0)
//                val yScaleDD = (yCoordsDD[1] - yCoordsDD[0]) * DualDouble(0.5, 0.0)
//                val xOffsetDD = xCoordsDD[1] - xScaleDD
//                val yOffsetDD = yCoordsDD[1] - yScaleDD
//
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
            }

            GL.glEnableVertexAttribArray(viewCoordsHandle)
            GL.glUniform1i(iterHandle, f.maxIter)
            GL.glUniform1fv(xInitHandle, 1, x0, 0)
            GL.glUniform1fv(yInitHandle, 1, y0, 0)
            GL.glUniform1fv(xTouchHandle,  1,  xTouchPos, 0)
            GL.glUniform1fv(yTouchHandle,  1,  yTouchPos, 0)




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
                    context.findViewById<ProgressBar>(R.id.progressBar).progress =
                            (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()

                }
                for (complementViewChunkCoordsB in chunksB) {

                    viewChunkBuffer.put(complementViewChunkCoordsB).position(0)
                    GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)
                    GL.glFinish()
                    chunksRendered++
                    context.findViewById<ProgressBar>(R.id.progressBar).progress =
                            (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()

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
                    context.findViewById<ProgressBar>(R.id.progressBar).progress =
                            (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()

                }

                GL.glDisableVertexAttribArray(viewCoordsHandle)

            }




        }

        fun renderFromTexture() {

//        Log.d("RENDER", "render from texture -- start")


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

            GL.glUniform1i(numColorsHandle, f.palette.size)
            GL.glUniform3fv(paletteHandle, f.palette.size, f.palette.flatPalette, 0)
            GL.glUniform1fv(frequencyHandle, 1, floatArrayOf(f.colorParams["frequency"] as Float), 0)
            GL.glUniform1fv(phaseHandle, 1, floatArrayOf(f.colorParams["phase"] as Float), 0)
            GL.glUniform1fv(xTouchColorHandle, 1, floatArrayOf(f.touchPos[0].toFloat()), 0)
            GL.glUniform1fv(yTouchColorHandle, 1, floatArrayOf(f.touchPos[1].toFloat()), 0)

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

        // Log.d("COORDS", "xQuadCoords: (${xQuadCoords[0]}, ${xQuadCoords[1]}), yQuadCoords: (${yQuadCoords[0]}, ${yQuadCoords[1]})")

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

//            ignoreTouch = true
            rr.renderToTexture()
//            ignoreTouch = false

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
        ColorFragment.OnParamChangeListener,
        SettingsFragment.OnParamChangeListener {


    private lateinit var f : Fractal
    private lateinit var fractalView : FractalSurfaceView
    private lateinit var colorAlgorithms : Map<String, ColorAlgorithm>
    private var orientation = Configuration.ORIENTATION_UNDEFINED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        // COMPLEX MAPS
        // =======================================================================================
        val mandelbrot  = ComplexMap(
                "Mandelbrot",
                doubleArrayOf(0.0, 0.0),
                resources.getString(R.string.mandelbrot_init_sf),
                resources.getString(R.string.mandelbrot_loop_sf),
                resources.getString(R.string.mandelbrot_final_sf),
                resources.getString(R.string.mandelbrot_init_df),
                resources.getString(R.string.mandelbrot_loop_df),
                resources.getString(R.string.mandelbrot_final_df)
        )
        val dualpow     = ComplexMap(
                "Dual Power",
                doubleArrayOf(1.0, 0.0),
                resources.getString(R.string.dualpow_init_sf),
                resources.getString(R.string.dualpow_loop_sf),
                resources.getString(R.string.dualpow_final_sf),
                "",
                "",
                ""
        )


        // COLORING ALGORITHMS
        // =======================================================================================
        val empty            = ColorAlgorithm(
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        )
        val escape           = ColorAlgorithm(
                "Escape Time",
                "",
                "",
                resources.getString(R.string.escape_final),
                "",
                "",
                resources.getString(R.string.escape_final)
        )
        val escapeSmooth     = ColorAlgorithm(
                "Escape Time Smooth",
                "",
                "",
                resources.getString(R.string.mandelbrot_smooth_final_sf),
                "",
                "",
                resources.getString(R.string.mandelbrot_smooth_final_df)
        )
        val lighting         = ColorAlgorithm(
                "Lighting",
                resources.getString(R.string.mandelbrot_light_init_sf),
                resources.getString(R.string.mandelbrot_light_loop_sf),
                resources.getString(R.string.mandelbrot_light_final),
                resources.getString(R.string.mandelbrot_light_init_df),
                resources.getString(R.string.mandelbrot_light_loop_df),
                resources.getString(R.string.mandelbrot_light_final)
        )
        val triangleIneqAvg  = ColorAlgorithm(
                "Triangle Inequality Average",
                resources.getString(R.string.mandelbrot_triangle_init_sf),
                resources.getString(R.string.mandelbrot_triangle_loop_sf),
                resources.getString(R.string.mandelbrot_triangle_final_sf),
                resources.getString(R.string.mandelbrot_triangle_init_df),
                resources.getString(R.string.mandelbrot_triangle_loop_df),
                resources.getString(R.string.mandelbrot_triangle_final_df)
        )
        val curvatureAvg     = ColorAlgorithm(
                "Curvature Average",
                resources.getString(R.string.curvature_init),
                resources.getString(R.string.curvature_loop_sf),
                resources.getString(R.string.curvature_final_sf),
                resources.getString(R.string.curvature_init),
                resources.getString(R.string.curvature_loop_df),
                resources.getString(R.string.curvature_final_df)
        )
        val stripeAvg        = ColorAlgorithm(
                "Stripe Average",
                resources.getString(R.string.stripe_init),
                resources.getString(R.string.stripe_loop_sf).format(5.0f),
                resources.getString(R.string.stripe_final_sf),
                resources.getString(R.string.stripe_init),
                resources.getString(R.string.stripe_loop_df).format(5.0f),
                resources.getString(R.string.stripe_final_df)
        )
        val minmod           = ColorAlgorithm(
                "Minmod Iteration",
                resources.getString(R.string.minmod_init),
                resources.getString(R.string.minmod_loop_sf),
                resources.getString(R.string.minmod_final),
                resources.getString(R.string.minmod_init),
                resources.getString(R.string.minmod_loop_df),
                resources.getString(R.string.minmod_final)
        )
        val overlayAvg       = ColorAlgorithm(
                "Overlay Average",
                resources.getString(R.string.overlay_init),
                resources.getString(R.string.overlay_loop_sf),
                resources.getString(R.string.overlay_final_sf),
                resources.getString(R.string.overlay_init),
                resources.getString(R.string.overlay_loop_df),
                resources.getString(R.string.overlay_final_df)
        )
        colorAlgorithms      = mapOf(
                empty.name            to  empty           ,
                escape.name           to  escape          ,
                escapeSmooth.name     to  escapeSmooth    ,
                lighting.name         to  lighting        ,
                triangleIneqAvg.name  to  triangleIneqAvg ,
                curvatureAvg.name     to  curvatureAvg    ,
                stripeAvg.name        to  stripeAvg       ,
                minmod.name           to  minmod          ,
                overlayAvg.name       to  overlayAvg
        )
        
        
        // COLOR PALETTES
        // =======================================================================================
        val beach   = ColorPalette(listOf(
            ColorPalette.YELLOWISH,
            ColorPalette.DARKBLUE1,
            ColorPalette.BLACK,
            ColorPalette.TURQUOISE,
            ColorPalette.TUSK
        ))
        val p1      = ColorPalette(listOf(
            ColorPalette.WHITE,
            ColorPalette.PURPLE,
            ColorPalette.BLACK,
            ColorPalette.DEEPRED,
            ColorPalette.WHITE
        ))
        val p2      = ColorPalette(listOf(
            ColorPalette.TURQUOISE, // 0.6
            ColorPalette.PURPLE,
            ColorPalette.BLACK,
            ColorPalette.DEEPRED,
            ColorPalette.WHITE
        ))
        val p3      = ColorPalette(listOf(
            floatArrayOf(0.0f, 0.1f, 0.2f),
            ColorPalette.DARKBLUE1,
            ColorPalette.WHITE,
            ColorPalette.ORAGNE2,
            ColorPalette.PURPLE
        ))
        val p4      = ColorPalette(listOf(
                ColorPalette.YELLOWISH,
                ColorPalette.DARKBLUE2,
                ColorPalette.BLACK,
                ColorPalette.PURPLE.mult(0.7f),
                ColorPalette.DEEPRED
        ))
        val p5      = ColorPalette(listOf(
                ColorPalette.YELLOWISH.mult(1.1f),
                ColorPalette.MAGENTA.mult(0.4f),
                ColorPalette.WHITE,
                ColorPalette.DARKBLUE1,
                ColorPalette.BLACK,
                ColorPalette.DARKBLUE1
        ))
        val p6      = ColorPalette(listOf(
                ColorPalette.YELLOWISH.mult(1.2f),
                ColorPalette.MAGENTA.mult(0.6f),
                ColorPalette.DARKBLUE1.mult(1.2f)
        ))
        val royal   = ColorPalette(listOf(
                ColorPalette.YELLOWISH,
                ColorPalette.DARKBLUE1,
                ColorPalette.SOFTGREEN.mult(0.5f),
                ColorPalette.PURPLE.mult(0.35f),
                ColorPalette.MAROON
        ))


        orientation = baseContext.resources.configuration.orientation
        Log.d("MAIN ACTIVITY", "orientation: $orientation")
        val orientationChanged = (savedInstanceState?.getInt("orientation") ?: orientation) != orientation

        val displayMetrics = baseContext.resources.displayMetrics
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val aspectRatio = screenHeight.toDouble() / screenWidth.toDouble()


        // get saved or default parameters
        val colorAlgName = savedInstanceState?.getString("colorAlgName") ?: escapeSmooth.name
        val xCoords = savedInstanceState?.getDoubleArray("xCoords") ?: doubleArrayOf(-2.5, 1.0)
        val yCoords = savedInstanceState?.getDoubleArray("yCoords") ?: doubleArrayOf(-1.75, 1.75).mult(aspectRatio)
        val frequency = savedInstanceState?.getFloat("frequency") ?: 1.0f
        val phase = savedInstanceState?.getFloat("phase") ?: 0.0f
        val colorParams : MutableMap<String, Any> = mutableMapOf(
                "colorAlg" to colorAlgName,
                "frequency" to 1.0f,
                "phase" to 0.0f
        )
        val settingsParams : MutableMap<String, String> = mutableMapOf(
                "resolution" to Resolution.LOW.name,
                "precision" to "AUTO"
        )


        // create fractal
        f = Fractal(
                mandelbrot,
                colorAlgorithms[colorAlgName] ?: empty,
                p5,
                this,
                xCoords, yCoords,
                intArrayOf(screenWidth, screenHeight),
                colorParams,
                settingsParams
        )

        if (orientationChanged) {
            f.switchOrientation()
            Log.d("MAIN", "orientation changed")
        }


        fractalView = FractalSurfaceView(f, this, screenWidth, screenHeight)
        fractalView.layoutParams = ViewGroup.LayoutParams(screenWidth, screenHeight)
        fractalView.hideSystemUI()

        setContentView(R.layout.activity_main)

        val fractalLayout = findViewById<FrameLayout>(R.id.layout_main)
        fractalLayout.addView(fractalView)

        val quickUI = findViewById<LinearLayout>(R.id.quickUI)

        val transformButton = findViewById<Button>(R.id.transformButton)
        transformButton.setOnClickListener { fractalView.reactionType = Reaction.TRANSFORM }

        val colorButton = findViewById<Button>(R.id.colorButton)
        colorButton.setOnClickListener { fractalView.reactionType = Reaction.COLOR }

        val paramButton1 = findViewById<Button>(R.id.paramButton1)
        paramButton1.setOnClickListener { fractalView.reactionType = Reaction.PARAM }






        val maxIterBar = findViewById<SeekBar>(R.id.maxIterBar)
        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val p: Float = i.toFloat() / 100.0f
                fractalView.f.maxIter = ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(11) - 1)*p).toInt()
                if (fractalView.continuousRender) {
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (!fractalView.continuousRender) {
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
            }

        })
        maxIterBar.progress = 20

        fractalView.hideAppUI()


        quickUI.bringToFront()



        val displayParams = findViewById<LinearLayout>(R.id.displayParams)
        displayParams.bringToFront()


        val fullUITabs = findViewById<TabLayout>(R.id.fullUITabs)
        fullUITabs.addTab(fullUITabs.newTab().setText("Equation"))
        fullUITabs.addTab(fullUITabs.newTab().setText("Color"))
        fullUITabs.addTab(fullUITabs.newTab().setText("Settings"))
        fullUITabs.tabGravity = TabLayout.GRAVITY_FILL


        // initialize fragments and set UI params from fractal
        val equationFragment = EquationFragment()
        val colorFragment = ColorFragment()
        val settingsFragment = SettingsFragment()

        colorFragment.initParams = colorParams
        settingsFragment.initParams = settingsParams


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
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })



        val phi = 0.5*(sqrt(5.0) + 1.0)
        val fullUIHeight = (screenHeight*(1.0 - 1.0/phi)).toInt()
        Log.d("MAIN ACTIVITY", "fullUIHeight set to $fullUIHeight")

//        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
//        progressBar.visibility = ProgressBar.VISIBLE
        val statusBarHeight = 103
//        progressBar.y = (screenHeight - fullUIHeight - progressBar.layoutParams.height).toFloat()
//        Log.d("MAIN ACTIVITY", "progressBar height set to ${progressBar.y}")
//        progressBar.requestLayout()
//        progressBar.bringToFront()

        val fullUI = findViewById<LinearLayout>(R.id.fullUI)
        fullUI.layoutParams.height = fullUIHeight
        Log.d("MAIN ACTIVITY", "fullUI height set to $fullUIHeight")
        fullUI.bringToFront()

        val fullUIButton = findViewById<Button>(R.id.fullUIButton)
        fullUIButton.setOnClickListener {
            val uiIsVisible = fullUI.visibility == FrameLayout.VISIBLE
            val newSurfaceViewPos = if (uiIsVisible) -fullUIHeight/2.0f else 0.0f
            fullUI.visibility = FrameLayout.INVISIBLE
            fractalView.y = newSurfaceViewPos
        }
        fullUI.visibility = FrameLayout.INVISIBLE



        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)


    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putDoubleArray("xCoords", f.xCoords)
        outState?.putDoubleArray("yCoords", f.yCoords)
        outState?.putString("colorAlgName", f.colorParams["colorAlg"] as String)
        outState?.putFloat("frequency", f.colorParams["frequency"] as Float)
        outState?.putFloat("phase", f.colorParams["phase"] as Float)
        outState?.putInt("orientation", orientation)
        super.onSaveInstanceState(outState)
    }


    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        if (fragment is ColorFragment) { fragment.setOnParamChangeListener(this) }
        else if (fragment is SettingsFragment) { fragment.setOnParamChangeListener(this) }
    }


    override fun onColorParamsChanged(key: String, value: Any) {
        if (f.colorParams[key] != value) {
            Log.d("MAIN ACTIVITY", "$key set from ${f.colorParams[key]} to $value")
            f.colorParams[key] = value
            when (key) {
                "colorAlg" -> {
                    f.alg = colorAlgorithms[value] ?: ColorAlgorithm("", "", "", "", "", "", "")
                    f.renderShaderChanged = true
                }
            }
            fractalView.r.renderToTex = true
            fractalView.requestRender()
        }
        else {
            Log.d("MAIN ACTIVITY", "$key is ${f.colorParams[key]}, not set to $value")
        }
    }

    override fun onSettingsParamsChanged(key: String, value: String) {
        if (f.settingsParams[key] != value) {
            Log.d("MAIN ACTIVITY", "$key set from ${f.settingsParams[key]} to $value")
            f.settingsParams[key] = value
            when (key) {
                "resolution" -> f.resolutionChanged = true
                "precision" -> f.renderShaderChanged = true
            }
            fractalView.r.renderToTex = true
            fractalView.requestRender()
        }
        else {
            Log.d("MAIN ACTIVITY", "$key is ${f.settingsParams[key]}, not set to $value")
        }
    }


}


