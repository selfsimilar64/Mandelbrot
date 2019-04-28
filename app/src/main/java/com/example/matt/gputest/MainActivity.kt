package com.example.matt.gputest

import android.annotation.SuppressLint
import android.app.Activity
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.Renderer
import android.content.Context
import javax.microedition.khronos.egl.EGLConfig
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.style.LineHeightSpan
import android.util.AttributeSet
import android.opengl.GLES32 as GL
import android.widget.FrameLayout.LayoutParams as LP
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import java.nio.ByteOrder
import java.nio.ByteBuffer.allocateDirect
import java.util.*
import javax.microedition.khronos.opengles.GL10
import java.nio.IntBuffer
import kotlin.math.*
import java.nio.ByteBuffer


const val SPLIT = 8193.0
val MANDEL_XCOORDS = doubleArrayOf(-2.5, 1.0)



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
        val width : Int,
        val height : Int,
        val format : Int,
        index : Int
) {

    val id : Int
    private val buffer : ByteBuffer

    init {
        // create texture id
        val b = IntBuffer.allocate(1)
        GL.glGenTextures(1, b)
        id = b[0]

        // allocate texture memory
        buffer = allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())

        // bind and set texture parameters
        GL.glActiveTexture(GL.GL_TEXTURE0 + index)
        GL.glBindTexture(GL.GL_TEXTURE_2D, id)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)

        val type = when(format) {
            GL.GL_RGBA8 -> GL.GL_UNSIGNED_BYTE
            GL.GL_RGBA32F -> GL.GL_FLOAT
            else -> 0
        }

        // define texture specs
        GL.glTexImage2D(
                GL.GL_TEXTURE_2D,           // target
                0,                          // mipmap level
                format,                     // internal format
                width, height,              // texture resolution
                0,                          // border
                GL.GL_RGBA,                 // format
                type,                       // type
                buffer                      // memory pointer
        )
    }

}

class ColorMap (colors: List<FloatArray>) {

    companion object Color {

        val RED         = floatArrayOf(1.0f, 0.0f, 0.0f)
        val GREEN       = floatArrayOf(0.0f, 1.0f, 0.0f)
        val BLUE        = floatArrayOf(0.0f, 0.0f, 1.0f)
        val YELLOW      = floatArrayOf(1.0f, 1.0f, 0.0f)
        val MAGENTA     = floatArrayOf(1.0f, 0.0f, 1.0f)
        val CYAN        = floatArrayOf(0.0f, 1.0f, 1.0f)
        val BLACK       = floatArrayOf(0.0f, 0.0f, 0.0f)
        val WHITE       = floatArrayOf(1.0f, 1.0f, 1.0f)

        val PURPLE      = floatArrayOf(0.3f, 0.0f, 0.5f)
        val PINK        = floatArrayOf(1.0f, 0.3f, 0.4f)
        val DARKBLUE    = floatArrayOf(0.0f, 0.15f, 0.25f)
        val ORANGE      = floatArrayOf(1.0f, 0.6f, 0.0f)
        val TURQUOISE   = floatArrayOf(0.25f, 0.87f, 0.82f)
        val TUSK        = floatArrayOf(0.93f, 0.8f, 0.73f)
        val YELLOWISH   = floatArrayOf(1.0f, 0.95f, 0.75f)
        val GRASS       = floatArrayOf(0.31f, 0.53f, 0.45f)
        val SOFTGREEN   = floatArrayOf(0.6f, 0.82f, 0.9f)

    }

    val size = colors.size
        
}

class ComplexMap (
        val name : String,
        val init : String,
        val loop : String,
        val final : String
)

class ColorAlgorithm (
        val name : String,
        val init : String,
        val loop : String,
        val final : String
) {

    fun add(alg : ColorAlgorithm) : ColorAlgorithm {
        return ColorAlgorithm(
                name + alg.name,
                init + alg.init,
                loop + alg.loop,
                final + alg.final
        )
    }

}

enum class Precision { SINGLE, DUAL, QUAD }

enum class Reaction { TRANSFORM, COLOR, LIGHT }



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
        val context         : Context,
        val name            : String,
        val precision       : Precision,
        val xCoordsInit     : DoubleArray,
        val yCoordsInit     : DoubleArray,
        val map             : ComplexMap,
        val alg             : ColorAlgorithm,
        val cmap            : ColorMap,
        val texWidth        : Int,
        val texHeight       : Int,
        val screenWidth     : Int,
        val screenHeight    : Int
) {

    val shader : String

    val header       : String
    val arithmetic   : String
    val init         : String
    val conditional  : String

    val xCoords = xCoordsInit
    val yCoords = yCoordsInit
    var maxIter = 0
    val touchPos = doubleArrayOf(0.0, 0.0)


    init {

        when(precision) {
            Precision.SINGLE -> {
                header      = context.resources.getString(R.string.header_sf)
                arithmetic  = context.resources.getString(R.string.arithmetic_sf)
                init        = context.resources.getString(R.string.general_init_sf)
                conditional = context.resources.getString(R.string.bailout_sf)
            }
            Precision.DUAL -> {

                header = context.resources.getString(R.string.header_df)

                arithmetic =
                        context.resources.getString(R.string.arithmetic_util) +
                        context.resources.getString(R.string.arithmetic_sf) +
                        context.resources.getString(R.string.arithmetic_df)

                init = context.resources.getString(R.string.general_init_df)

                conditional = context.resources.getString(R.string.bailout_df)

            }
            Precision.QUAD -> {
                header = ""
                arithmetic = ""
                init = ""
                conditional = ""
            }
        }

        shader =
                """
                $header
                $arithmetic
                void main() {
                    $init
                    ${map.init}
                    ${alg.init}
                    for (int n = 0; n < maxIter; n++) {
                        if (n == maxIter - 1) {
                            colorParams.w = -1.0;
                            break;
                        }
                        ${map.loop}
                        $conditional {
                            ${map.final}
                            ${alg.final}
                            break;
                        }
                        ${alg.loop}
                    }
                    fragmentColor = colorParams;
                }
                """

    }

    fun resetCoords() {
        xCoords[0] = xCoordsInit[0]
        xCoords[1] = xCoordsInit[1]
        yCoords[0] = yCoordsInit[0]
        yCoords[1] = yCoordsInit[1]
    }

    fun setTouchPos(screenPos: DoubleArray) {
        touchPos[0] = xCoords[0] + screenPos[0]*(xCoords[1] - xCoords[0])
        touchPos[1] = yCoords[1] - screenPos[1]*(yCoords[1] - yCoords[0])
    }

    fun translate(dScreenPos: FloatArray) {

        // update complex coordinates
        when (precision) {
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
                        (dScreenPos[0].toDouble() / screenWidth.toDouble()) * (xCoords[1] - xCoords[0]),
                        (dScreenPos[1].toDouble() / screenHeight.toDouble()) * (yCoords[1] - yCoords[0])
                )
                xCoords[0] -= dPos[0]
                xCoords[1] -= dPos[0]
                yCoords[0] += dPos[1]
                yCoords[1] += dPos[1]
            }
        }

    }

    fun scale(dScale: Float, screenFocus: FloatArray) {

        // update complex coordinates
        // convert focus coordinates from screen space to complex space
        val prop = doubleArrayOf(
                screenFocus[0].toDouble() / screenWidth.toDouble(),
                screenFocus[1].toDouble() / screenHeight.toDouble()
        )

        when (precision) {
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

        //// Log.d("COORDS", "xCoords: (${xCoords[0]}, ${xCoords[1]}), yCoords: (${yCoords[0]}, ${yCoords[1]})")

    }

}


@SuppressLint("ViewConstructor")
class FractalSurfaceView(val f: Fractal, context: Context) : GLSurfaceView(context) {

    lateinit var activity : Activity

    val screenWidth : Int
    val screenHeight : Int
    val screenPixels : Int

    val r : FractalRenderer
    var reactionType = Reaction.TRANSFORM
    val continuousRender = false

    private val prevFocus = floatArrayOf(0.0f, 0.0f)
    private val edgeRightSize = 150
    private var prevFocalLen = 1.0f

    private var visibleUI = false


    init {

        val displayMetrics = context.resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        screenPixels = screenWidth*screenHeight

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
        activity.findViewById<LinearLayout>(R.id.linearLayout).bringToFront()
        activity.findViewById<Button>(R.id.transformButton).visibility = Button.INVISIBLE
        activity.findViewById<Button>(R.id.colorButton).visibility = Button.INVISIBLE
        activity.findViewById<SeekBar>(R.id.maxIterBar).visibility = SeekBar.INVISIBLE
    }
    fun showAppUI() {
        activity.findViewById<LinearLayout>(R.id.linearLayout).bringToFront()
        activity.findViewById<Button>(R.id.transformButton).visibility = Button.VISIBLE
        activity.findViewById<Button>(R.id.colorButton).visibility = Button.VISIBLE
        activity.findViewById<SeekBar>(R.id.maxIterBar).visibility = SeekBar.VISIBLE
    }
    fun toggleAppUI() {
        if (visibleUI) { hideAppUI() }
        else { showAppUI() }
        visibleUI = !visibleUI
    }




    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {

        if (screenWidth - (e?.x ?: 0.0f) < edgeRightSize) {
            when (e?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Log.d("UI", "DOWN")

                    val focus = e.focus()
                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]

                    Log.d("UI", "DOWN -- EDGE RIGHT")
                    toggleAppUI()
                    return true

                }
                MotionEvent.ACTION_MOVE -> {
                    // Log.d("UI", "MOVE")

                    val focus = e.focus()
                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]

                    Log.d("UI", "MOVE -- EDGE RIGHT")
                    return true

                }
            }
        }

        when (reactionType) {
            Reaction.TRANSFORM -> {

                // actions change fractal
                when (e?.actionMasked) {

                    MotionEvent.ACTION_DOWN -> {
                        Log.d("TRANSFORM", "DOWN -- x: ${e.x}, y: ${e.y}")

                        val focus = e.focus()
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]

                        return true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        Log.d("TRANSFORM", "POINTER ${e.actionIndex + 1} DOWN -- x: ${e.x}, y: ${e.y}")
                        if (e.actionIndex == 1) {
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            prevFocalLen = e.focalLength()
                            r.newQuadFocus = true
                        }
                        return true

                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Log.d("TRANSFORM", "MOVE -- x: ${e.x}, y: ${e.y}")

                        val focus = e.focus()
                        val dx: Float = focus[0] - prevFocus[0]
                        val dy: Float = focus[1] - prevFocus[1]
                        f.translate(floatArrayOf(dx, dy))
                        r.translate(floatArrayOf(dx, dy))
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]
                        if (e.pointerCount > 1) {   // MULTI-TOUCH
                            val focalLen = e.focalLength()
                            val dFocalLen = focalLen / prevFocalLen
                            f.scale(dFocalLen, focus)
                            r.scale(dFocalLen, focus)
                            prevFocalLen = focalLen
                            // // Log.d("SCALE", "$dScale")
                        }

                        Log.d("MOVE", "x: ${e.x}, y: ${e.y}")
                        if (continuousRender) {
                            r.renderToTex = true
                        }
                        requestRender()

                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        //// Log.d("UP", "x: ${e.x}, y: ${e.y}, count: ${e.pointerCount}")
                        r.renderToTex = true
                        requestRender()
                        return true
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        //// Log.d("POINTER UP", "x: ${e.x}, y: ${e.y}, count: ${e.pointerCount}")
                        if (e.getPointerId(e.actionIndex) == 0) {
                            prevFocus[0] = e.getX(1)
                            prevFocus[1] = e.getY(1)
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
                        //// Log.d("DOWN", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                        return true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        val focus = e.focus()
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]
                        prevFocalLen = e.focalLength()
                        //// Log.d("POINTER DOWN", "focus: $focus, focalLen: $prevFocalLen")
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val focus = e.focus()
                        val dx: Float = focus[0] - prevFocus[0]
                        val dy: Float = focus[1] - prevFocus[1]
                        r.rr.phase += dx/screenWidth
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]
                        if (e.pointerCount > 1) {   // MULTI-TOUCH
                            val focalLen = e.focalLength()
                            val dFocalLen = focalLen / prevFocalLen
                            r.rr.frequency *= dFocalLen
                            prevFocalLen = focalLen
                            // // Log.d("SCALE", "$dScale")
                        }
                        requestRender()
                        return true
                    }

                }
            }
            Reaction.LIGHT -> {
                // actions change light position
                when (e?.actionMasked) {

                    MotionEvent.ACTION_DOWN -> {
                        // Log.d("DOWN", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
//                        val u : Float = e.x - r.screenWidth
//                        val v : Float = e.y - r.screenHeight
//                        val r : Float = sqrt(u*u + v*v)
//                        r.touchPos = floatArrayOf(u/r, v/r)
//                        // Log.d("LIGHTPOS", "u: ${u/r}, v: ${v/r}")

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
                        // Log.d("UP", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                        // Log.d("LIGHTPOS", "u: ${r.touchPos[0]}, v: ${r.touchPos[1]}")
                        requestRender()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Log.d("MOVE", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")

//                        val u : Float = e.x - r.screenWidth/2.0f
//                        val v : Float = e.y - r.screenHeight/2.0f
//                        val r : Float = sqrt(u.pow(2) + v.pow(2))
//                        r.touchPos = floatArrayOf(u/r, -v/r)
//                        // Log.d("LIGHTPOS", "u: ${u/r}, v: ${-v/r}")

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

        return false

    }

}


class FractalRenderer(val f: Fractal, val context: Context) : Renderer {

    inner class RenderRoutine {

        var frequency = 1.0f
        var phase = 0.0f

        private val maxPixelsPerChunk = f.screenWidth*f.screenHeight/10

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
        private val textureColorHandle    : Int
        private val frequencyHandle       : Int
        private val phaseHandle           : Int


        private val v_renderShader : Int
        private val v_sampleShader : Int

        private val f_renderShader : Int
        private val f_sampleShader : Int
        private val f_colorShader  : Int

        // define texture resolutions
        private val bgTexWidth = f.screenWidth/8
        private val bgTexHeight = f.screenHeight/8

        private val textures = arrayOf(
                Texture(bgTexWidth, bgTexHeight, GL.GL_RGBA32F, 0),
                Texture(f.texWidth, f.texHeight, GL.GL_RGBA32F, 1),
                Texture(f.texWidth, f.texHeight, GL.GL_RGBA32F, 2)
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
            val v_renderCode = Scanner(s).useDelimiter("\\Z").next()
            s.close()

            s = context.resources.openRawResource(R.raw.vert_sample)
            val v_sampleCode = Scanner(s).useDelimiter("\\Z").next()
            s.close()

            s = context.resources.openRawResource(R.raw.render_sf)
            val f_renderCodeSF = Scanner(s).useDelimiter("\\Z").next()
            s.close()

            s = context.resources.openRawResource(R.raw.render_df)
            val f_renderCodeDF = Scanner(s).useDelimiter("\\Z").next()
            s.close()

            s = context.resources.openRawResource(R.raw.render_qf)
            val f_renderCodeQF = Scanner(s).useDelimiter("\\Z").next()
            s.close()

            s = context.resources.openRawResource(R.raw.sample)
            val f_sampleCode = Scanner(s).useDelimiter("\\Z").next()
            s.close()

            s = context.resources.openRawResource(R.raw.color)
            val f_colorCode = Scanner(s).useDelimiter("\\Z").next()
            s.close()


            // create and compile shaders
            v_renderShader = loadShader(GL.GL_VERTEX_SHADER, v_renderCode)
            v_sampleShader = loadShader(GL.GL_VERTEX_SHADER, v_sampleCode)

            f_renderShader =
                    when (f.precision) {
                        Precision.SINGLE -> loadShader(GL.GL_FRAGMENT_SHADER, f.shader)
                        Precision.DUAL   -> loadShader(GL.GL_FRAGMENT_SHADER, f.shader)
                        Precision.QUAD   -> loadShader(GL.GL_FRAGMENT_SHADER, f_renderCodeQF)
                    }
            f_sampleShader = loadShader(GL.GL_FRAGMENT_SHADER, f_sampleCode)
            f_colorShader  = loadShader(GL.GL_FRAGMENT_SHADER, f_colorCode)


            GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

            // generate texture and framebuffer objects
            GL.glGenFramebuffers(1, fboIDs)



            // attach shaders and create renderProgram executables
            // render to texture renderProgram
            GL.glAttachShader(renderProgram, v_renderShader)
            GL.glAttachShader(renderProgram, f_renderShader)
            GL.glLinkProgram(renderProgram)

            viewCoordsHandle =  GL.glGetAttribLocation(   renderProgram, "viewCoords"  )
            iterHandle       =  GL.glGetUniformLocation(  renderProgram, "maxIter"     )
            xScaleHandle     =  GL.glGetUniformLocation(  renderProgram, "xScale"      )
            yScaleHandle     =  GL.glGetUniformLocation(  renderProgram, "yScale"      )
            xOffsetHandle    =  GL.glGetUniformLocation(  renderProgram, "xOffset"     )
            yOffsetHandle    =  GL.glGetUniformLocation(  renderProgram, "yOffset"     )
            xTouchHandle     =  GL.glGetUniformLocation(  renderProgram, "xTouchPos"   )
            yTouchHandle     =  GL.glGetUniformLocation(  renderProgram, "yTouchPos"   )
            bgScaleHandle    =  GL.glGetUniformLocation(  renderProgram, "bgScale"     )

            // render from texture renderProgram
            GL.glAttachShader(sampleProgram, v_sampleShader)
            GL.glAttachShader(sampleProgram, f_sampleShader)
            GL.glLinkProgram(sampleProgram)

            viewCoordsSampleHandle = GL.glGetAttribLocation(  sampleProgram, "viewCoords"  )
            quadCoordsSampleHandle = GL.glGetAttribLocation(  sampleProgram, "quadCoords"  )
            textureSampleHandle    = GL.glGetUniformLocation( sampleProgram, "tex"         )

            GL.glAttachShader(colorProgram, v_sampleShader)
            GL.glAttachShader(colorProgram, f_colorShader)
            GL.glLinkProgram(colorProgram)

            viewCoordsColorHandle = GL.glGetAttribLocation(   colorProgram, "viewCoords"  )
            quadCoordsColorHandle = GL.glGetAttribLocation(   colorProgram, "quadCoords"  )
            textureColorHandle    = GL.glGetUniformLocation(  colorProgram, "tex"         )
            frequencyHandle       = GL.glGetUniformLocation(  colorProgram, "frequency"   )
            phaseHandle           = GL.glGetUniformLocation(  colorProgram, "phase"       )

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
            val xPixels = xLength / 2.0f * f.screenWidth
            val yPixels = yLength / 2.0f * f.screenHeight
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

            GL.glUseProgram(renderProgram)
            GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer

            val xTouchPos = floatArrayOf(f.touchPos[0].toFloat())
            val yTouchPos = floatArrayOf(f.touchPos[1].toFloat())

            // calculate scale/offset parameters and pass to fragment shader
            when (f.precision) {
                Precision.SINGLE -> {
                    val xScaleSD = (f.xCoords[1] - f.xCoords[0]) / 2.0
                    val yScaleSD = (f.yCoords[1] - f.yCoords[0]) / 2.0
                    val xOffsetSD = f.xCoords[1] - xScaleSD
                    val yOffsetSD = f.yCoords[1] - yScaleSD

                    val xScaleSF = floatArrayOf(xScaleSD.toFloat())
                    val yScaleSF = floatArrayOf(yScaleSD.toFloat())
                    val xOffsetSF = floatArrayOf(xOffsetSD.toFloat())
                    val yOffsetSF = floatArrayOf(yOffsetSD.toFloat())

                    GL.glUniform1fv(xScaleHandle,  1,  xScaleSF,   0)
                    GL.glUniform1fv(yScaleHandle,  1,  yScaleSF,   0)
                    GL.glUniform1fv(xOffsetHandle, 1,  xOffsetSF,  0)
                    GL.glUniform1fv(yOffsetHandle, 1,  yOffsetSF,  0)
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
            Log.d("RENDER ROUTINE", "maxIter == ${f.maxIter}")
            GL.glUniform1i(iterHandle, f.maxIter)
            GL.glUniform1fv(xTouchHandle,  1,  xTouchPos, 0)
            GL.glUniform1fv(yTouchHandle,  1,  yTouchPos, 0)




            //======================================================================================
            // RENDER LOW-RES
            //======================================================================================

            GL.glViewport(0, 0, textures[0].width, textures[0].height)
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

                GL.glViewport(0, 0, textures[intIndex].width, textures[intIndex].height)
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
                for (complementViewChunkCoordsA in chunksA) {

                    viewChunkBuffer.put(complementViewChunkCoordsA).position(0)
                    GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)
                    GL.glFinish()

                }
                val chunksB = splitCoords(xComplementViewCoordsB, yComplementViewCoordsB)
                for (complementViewChunkCoordsB in chunksB) {

                    viewChunkBuffer.put(complementViewChunkCoordsB).position(0)
                    GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)
                    GL.glFinish()

                }





                //===================================================================================
                // SAMPLE -- TRANSLATION INTERSECTION
                //===================================================================================

                GL.glUseProgram(sampleProgram)
                GL.glViewport(0, 0, textures[intIndex].width, textures[intIndex].height)

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

                GL.glViewport(0, 0, textures[currIndex].width, textures[currIndex].height)
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
                for (viewChunkCoords in chunks) {

//                        val viewChunkCoords = floatArrayOf(
//                                -1.0f +  i*chunkInc,        1.0f, 0.0f,    // top left
//                                -1.0f +  i*chunkInc,       -1.0f, 0.0f,    // bottom left
//                                -1.0f + (i + 1)*chunkInc,  -1.0f, 0.0f,    // bottom right
//                                -1.0f + (i + 1)*chunkInc,   1.0f, 0.0f     // top right
//                        )
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
            GL.glViewport(0, 0, f.screenWidth, f.screenHeight)

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
                    xQuadCoords2[0].toFloat(),  yQuadCoords2[1].toFloat(),  0.0f,     // top left
                    xQuadCoords2[0].toFloat(),  yQuadCoords2[0].toFloat(),  0.0f,     // bottom left
                    xQuadCoords2[1].toFloat(),  yQuadCoords2[0].toFloat(),  0.0f,     // bottom right
                    xQuadCoords2[1].toFloat(),  yQuadCoords2[1].toFloat(),  0.0f )    // top right
            bgQuadBuffer
                    .put(bgQuadCoords)
                    .position(0)

            GL.glUniform1fv(frequencyHandle, 1, floatArrayOf(frequency), 0)
            GL.glUniform1fv(phaseHandle, 1, floatArrayOf(phase), 0)

            GL.glEnableVertexAttribArray(viewCoordsColorHandle)
            GL.glEnableVertexAttribArray(quadCoordsColorHandle)




            //======================================================================================
            // RENDER LOW-RES
            //======================================================================================

            GL.glUniform1i(textureColorHandle, 0)    // use GL_TEXTURE0
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

        }

    }

    var renderToTex = false
    // var renderFromTex = true

    private var hasTranslated = false
    private var hasScaled = false
    private val strictTranslate = { hasTranslated && !hasScaled }


    private val xQuadCoords = doubleArrayOf(-1.0, 1.0)
    private val yQuadCoords = doubleArrayOf(-1.0, 1.0)

    private val bgScaleFloat = floatArrayOf(5.0f)
    private val bgScaleDouble = 5.0

    private val xQuadCoords2 = doubleArrayOf(-bgScaleDouble, bgScaleDouble)
    private val yQuadCoords2 = doubleArrayOf(-bgScaleDouble, bgScaleDouble)

    private var quadFocus = doubleArrayOf(0.0, 0.0)

    var newQuadFocus = false

    lateinit var rr : RenderRoutine



    fun translate(dScreenPos: FloatArray) {

        // update texture quad coordinates
        val dQuadPos = doubleArrayOf(
                dScreenPos[0].toDouble() / f.screenWidth.toDouble() * 2.0,
                dScreenPos[1].toDouble() / f.screenHeight.toDouble() * 2.0
        )

        xQuadCoords[0] += dQuadPos[0]
        xQuadCoords[1] += dQuadPos[0]
        yQuadCoords[0] -= dQuadPos[1]
        yQuadCoords[1] -= dQuadPos[1]

        xQuadCoords2[0] += dQuadPos[0]
        xQuadCoords2[1] += dQuadPos[0]
        yQuadCoords2[0] -= dQuadPos[1]
        yQuadCoords2[1] -= dQuadPos[1]

        // magic
        quadFocus[0] += dQuadPos[0]
        quadFocus[1] -= dQuadPos[1]

        hasTranslated = true

    }

    fun scale(dScale: Float, screenFocus: FloatArray) {

        // update texture quad coordinates
        // convert focus coordinates from screen space to quad space
        if (newQuadFocus) {
            val quadProp = doubleArrayOf(
                    screenFocus[0].toDouble() / f.screenWidth.toDouble(),
                    screenFocus[1].toDouble() / f.screenHeight.toDouble()
            )

            // half magic
            quadFocus[0] = (xQuadCoords[0] - quadFocus[0])*(1.0 - quadProp[0]) + quadProp[0]*(xQuadCoords[1] - quadFocus[0])
            quadFocus[1] = (yQuadCoords[1] - quadFocus[1])*(1.0 - quadProp[1]) + quadProp[1]*(yQuadCoords[0] - quadFocus[1])
            newQuadFocus = false
        }

        // translate quadFocus to origin in quad coordinates
        xQuadCoords[0] -= quadFocus[0]
        xQuadCoords[1] -= quadFocus[0]
        yQuadCoords[0] -= quadFocus[1]
        yQuadCoords[1] -= quadFocus[1]

        // scale quad coordinates
        xQuadCoords[0] = xQuadCoords[0]*dScale
        xQuadCoords[1] = xQuadCoords[1]*dScale
        yQuadCoords[0] = yQuadCoords[0]*dScale
        yQuadCoords[1] = yQuadCoords[1]*dScale

        // translate origin back to quadFocus in quad coordinates
        xQuadCoords[0] += quadFocus[0]
        xQuadCoords[1] += quadFocus[0]
        yQuadCoords[0] += quadFocus[1]
        yQuadCoords[1] += quadFocus[1]



        // translate quadFocus to origin in quad coordinates
        xQuadCoords2[0] -= quadFocus[0]
        xQuadCoords2[1] -= quadFocus[0]
        yQuadCoords2[0] -= quadFocus[1]
        yQuadCoords2[1] -= quadFocus[1]

        // scale quad coordinates
        xQuadCoords2[0] = xQuadCoords2[0]*dScale
        xQuadCoords2[1] = xQuadCoords2[1]*dScale
        yQuadCoords2[0] = yQuadCoords2[0]*dScale
        yQuadCoords2[1] = yQuadCoords2[1]*dScale

        // translate origin back to quadFocus in quad coordinates
        xQuadCoords2[0] += quadFocus[0]
        xQuadCoords2[1] += quadFocus[0]
        yQuadCoords2[0] += quadFocus[1]
        yQuadCoords2[1] += quadFocus[1]

        hasScaled = true

        // Log.d("COORDS", "xQuadCoords: (${xQuadCoords[0]}, ${xQuadCoords[1]}), yQuadCoords: (${yQuadCoords[0]}, ${yQuadCoords[1]})")

    }

    private fun resetQuadCoords() {

        xQuadCoords[0] = -1.0
        xQuadCoords[1] = 1.0
        yQuadCoords[0] = -1.0
        yQuadCoords[1] = 1.0

        xQuadCoords2[0] = -bgScaleDouble
        xQuadCoords2[1] = bgScaleDouble
        yQuadCoords2[0] = -bgScaleDouble
        yQuadCoords2[1] = bgScaleDouble

        quadFocus[0] = 0.0
        quadFocus[1] = 0.0

    }



    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

        // get OpenGL ES version
        Log.d("OPENGL ES", "VERSION == ${unused.glGetString(GL10.GL_VERSION)}")

        // get fragment shader precision
        val a : IntBuffer = IntBuffer.allocate(2)
        val b : IntBuffer = IntBuffer.allocate(1)
        GL.glGetShaderPrecisionFormat(GL.GL_FRAGMENT_SHADER, GL.GL_HIGH_FLOAT, a, b)
        Log.d("OPENGL ES", "FLOAT PRECISION == ${b[0]}")

        rr = RenderRoutine()
        rr.renderToTexture()

    }

    override fun onDrawFrame(unused: GL10) {

        // Log.d("RENDER", "DRAW FRAME")

        // render to texture on ACTION_UP
        if (renderToTex) {

            rr.renderToTexture()

            renderToTex = false
            hasTranslated = false
            hasScaled = false
            resetQuadCoords()

        }

        // render from texture
        rr.renderFromTexture()

    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {

    }

}







class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // COMPLEX MAPS
        val mandelbrotMapSF = ComplexMap(
                "Mandelbrot",
                resources.getString(R.string.mandelbrot_init_sf),
                resources.getString(R.string.mandelbrot_loop_sf),
                resources.getString(R.string.mandelbrot_final_sf)
        )
        val mandelbrotMapDF = ComplexMap(
                "Mandelbrot",
                resources.getString(R.string.mandelbrot_init_df),
                resources.getString(R.string.mandelbrot_loop_df),
                resources.getString(R.string.mandelbrot_final_df)
        )

        val dualpowSF = ComplexMap(
                "dualpow sf",
                resources.getString(R.string.dualpow_init_sf),
                resources.getString(R.string.dualpow_loop_sf),
                resources.getString(R.string.mandelbrot_final_sf)
        )

        val exponentialSF = ComplexMap(
                "Exponential",
                """
                    float il = 1.0/log(2.0);
                    float llr = log(log(R)/2.0);
                """.trimIndent(),
                """
                    Z2 = Z1;
                    Z1 = Z;
                    Z = sine(Z) + C;
                    modZ = modSF(Z);
                """.trimIndent(),
                ""
        )


        // COLORING ALGORITHMS
        val escapeTimeSF = ColorAlgorithm(
                "Escape Time",
                "",
                "",
                resources.getString(R.string.mandelbrot_smooth_final_sf)
        )
        val smoothSF = ColorAlgorithm(
                "Smooth",
                "",
                "",
                resources.getString(R.string.mandelbrot_smooth_final_sf)
        )
        val smoothDF = ColorAlgorithm(
                "Smooth",
                "",
                "",
                resources.getString(R.string.mandelbrot_smooth_final_df)
        )
        val lightingSF = ColorAlgorithm(
                "Lighting",
                resources.getString(R.string.mandelbrot_light_init_sf),
                resources.getString(R.string.mandelbrot_light_loop_sf),
                resources.getString(R.string.mandelbrot_light_final_sf)
        )
        val lightingDF = ColorAlgorithm(
                "Lighting",
                resources.getString(R.string.mandelbrot_light_init_df),
                resources.getString(R.string.mandelbrot_light_loop_df),
                resources.getString(R.string.mandelbrot_light_final_sf)
        )
        val triangleIneqAvgSF = ColorAlgorithm(
                "Triangle Inequality Average",
                resources.getString(R.string.mandelbrot_triangle_init_sf),
                resources.getString(R.string.mandelbrot_triangle_loop_sf),
                resources.getString(R.string.mandelbrot_triangle_final_sf)
        )
        val triangleIneqAvgDF = ColorAlgorithm(
                "Triangle Inequality Average",
                resources.getString(R.string.mandelbrot_triangle_init_df),
                resources.getString(R.string.mandelbrot_triangle_loop_df),
                resources.getString(R.string.mandelbrot_triangle_final_df)
        )
        val curvatureAvgSF = ColorAlgorithm(
                "Curvature Average",
                resources.getString(R.string.curvature_init_sf),
                resources.getString(R.string.curvature_loop_sf),
                resources.getString(R.string.curvature_final_sf)
        )
        val curvatureAvgDF = ColorAlgorithm(
                "Curvature Average",
                resources.getString(R.string.curvature_init_df),
                resources.getString(R.string.curvature_loop_df),
                resources.getString(R.string.curvature_final_df)
        )
        val stripeAvgSF = ColorAlgorithm(
                "Stripe Average",
                resources.getString(R.string.stripe_init_sf),
                resources.getString(R.string.stripe_loop_sf).format(5.0f),
                resources.getString(R.string.stripe_final_sf)
        )
        val stripeAvgDF = ColorAlgorithm(
                "Stripe Average",
                resources.getString(R.string.stripe_init_df2),
                resources.getString(R.string.stripe_loop_df2).format(5.0f),
                resources.getString(R.string.stripe_final_df2)
        )

        val testSF = ColorAlgorithm(
                "test",
                resources.getString(R.string.test_init_sf),
                resources.getString(R.string.test_loop_sf),
                resources.getString(R.string.test_final_sf)
        )
        val testDF = ColorAlgorithm(
                "test",
                resources.getString(R.string.test_init_df),
                resources.getString(R.string.test_loop_df),
                resources.getString(R.string.test_final_df)
        )
        
        
        
        
        val displayMetrics = baseContext.resources.displayMetrics
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val aspectRatio = screenHeight.toDouble() / screenWidth.toDouble()

        val f = Fractal(
                this,
                "Current Fractal",
                Precision.SINGLE,
                doubleArrayOf(-1.75, 1.75),
                doubleArrayOf(-1.75*aspectRatio, 1.75*aspectRatio),
                dualpowSF,
                smoothSF,
                ColorMap(listOf(ColorMap.YELLOWISH, ColorMap.DARKBLUE, ColorMap.BLACK)),
                screenWidth, screenHeight,
                screenWidth, screenHeight
        )


        val fractalView = FractalSurfaceView(f, this)
        fractalView.hideSystemUI()
        fractalView.activity = this

        setContentView(R.layout.activity_main)

        val frameLayout = findViewById<FrameLayout>(R.id.layout_main)
        frameLayout.addView(fractalView)

        val transformButton = findViewById<Button>(R.id.transformButton)
        transformButton.setOnClickListener { fractalView.reactionType = Reaction.TRANSFORM }
        transformButton.visibility = Button.INVISIBLE

        val colorButton = findViewById<Button>(R.id.colorButton)
        colorButton.setOnClickListener { fractalView.reactionType = Reaction.COLOR }
        colorButton.visibility = Button.INVISIBLE

        val maxIterBar = findViewById<SeekBar>(R.id.maxIterBar)
        maxIterBar.visibility = SeekBar.INVISIBLE
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

    }


}
