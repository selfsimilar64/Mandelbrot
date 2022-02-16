package com.selfsimilartech.fractaleye

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.ThumbnailUtils
import android.net.Uri
import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Log
import org.apfloat.Apcomplex
import org.apfloat.Apfloat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*


const val DOUBLE_TAP_TIMEOUT = 300L

//const val GPU_SINGLE_FG_CHUNKS = 10
//const val GPU_SINGLE_BG_CHUNKS = 3
//const val GPU_DUAL_FG_CHUNKS = 30
//const val GPU_DUAL_BG_CHUNKS = 10
const val CPU_FG_CHUNKS = 50
const val CPU_BG_CHUNKS = 20
const val CPU_CHUNKS = 5
const val MAX_REFERENCES = 35
const val TEX_SPAN_HIST_CONTINUOUS = 100
const val TEX_SPAN_HIST_DISCRETE = 4
const val TEX_FORMAT = GL_R32UI
const val MINMAX_XSAMPLES = 4
val MAX_REF_ITER = 2.0.pow(16).toInt()
const val CHUNKS_PER_SECOND = 25
const val MIN_CONTINUOUS_SIZE = 0.05
const val MAX_CONTINUOUS_SIZE = 1.0

const val BG1_INDEX = 0
const val BG2_INDEX = 1
const val FG1_INDEX = 2
const val FG2_INDEX = 3
const val THUMB_INDEX = 4
const val IMAGE_INDEX = 5

const val IMAGE_UNIFORM = "// imageUniform"
const val SPLIT_HANDLE = "// splitHandle"
const val CUSTOM_SHAPE_HANDLE_SINGLE = "// customShapeHandleSingle"
const val CUSTOM_SHAPE_HANDLE_DUAL = "// customShapeHandleDual"
const val GENERAL_INIT = "// generalInit"
const val SEED_INIT = "// seedInit"
const val SHAPE_INIT = "// shapeInit"
const val SHAPE_LOOP = "// shapeLoop"
const val TEXTURE_INIT = "// textureInit"
const val TEXTURE_LOOP = "// textureLoop"
const val TEXTURE_FINAL = "// textureFinal"
const val CONDITIONAL = "/* conditional */"

const val SHAPE_FUN_SINGLE = "vec2 customshape_loop(vec2 z, vec2 c) { return %s; }"
const val SHAPE_FUN_DUAL   = "vec4 customshape_loop(vec4 z, vec4 c) { return %s; }"



enum class RenderProfile {
    CONTINUOUS,
    DISCRETE,
    SAVE_THUMBNAIL,
    SAVE_IMAGE,
    SHARE_IMAGE,
    COLOR_THUMB,
    TEXTURE_THUMB,
    SHAPE_THUMB,
    KEYFRAME_THUMB,
    VIDEO
}
enum class HardwareProfile { GPU, CPU }
enum class ColorMode { MANUAL, MINMAX, HISTOGRAM }
class NativeReferenceReturnData {

    var refIter = 0
    var skipIter = 0
    var ax = 0.0
    var ay = 0.0
    var bx = 0.0
    var by = 0.0
    var cx = 0.0
    var cy = 0.0

}
class NativeIterateData(
        val width: Int,
        val height: Int,
        val aspectRatio: Double,
        val bgScale: Double,
        val maxIter: Int,
        val escapeRadius: Float,
        val zoom: Double,
        val xCoord: Double,
        val yCoord: Double,
        val sinRotation: Double,
        val cosRotation: Double,
        val xCoordHi: Double = 0.0,
        val xCoordLo: Double = 0.0,
        val yCoordHi: Double = 0.0,
        val yCoordLo: Double = 0.0
)

fun FloatArray.get(i: Int, j: Int, k: Int, width: Int, depth: Int) = get(depth * (j * width + i) + k)
fun FloatArray.set(i: Int, j: Int, k: Int, width: Int, depth: Int, value: Float) { set(depth * (j * width + i) + k, value) }
//fun ShortArray.get(i: Int, j: Int, k: Int, width: Int, depth: Int) = get(depth*(j*width + i) + k)
//fun ShortArray.set(i: Int, j: Int, k: Int, width: Int, depth: Int, value: Short) { set(depth*(j*width + i) + k, value) }


interface RenderFinishedListener {
    fun onRenderFinished(buffer: ByteArray?)
}


class FractalRenderer(

    private val context: Context,
    private val handler: MainActivity.ActivityHandler

) : GLSurfaceView.Renderer {

    private val TAG = "RENDERER"

    companion object {

        init {
            System.loadLibrary("gmp")
            System.loadLibrary("native-fractalimage")
        }

    }

    val f = Fractal.default
    val sc = SettingsConfig

    var isRenderingVideo = false
    var isPreprocessingVideo = false
    var renderFinishedListener : RenderFinishedListener? = null
    
    var hasTranslated = false
    var hasZoomed = false
    var hasRotated = false

    val quadCoords = floatArrayOf(0f, 0f)
    val quadFocus = floatArrayOf(0f, 0f)
    var quadScale = 1f
    var quadRotation = 0f
    private val bgSize = 5f

    fun setQuadFocus(screenPos: FloatArray) {

        // update texture quad coordinates
        // convert focus coordinates from screen space to quad space

        quadFocus[0] =   2f*(screenPos[0] / Resolution.SCREEN.w) - 1f
        quadFocus[1] = -(2f*(screenPos[1] / Resolution.SCREEN.h) - 1f)

        // Log.v("SURFACE VIEW", "quadFocus: (${quadFocus[0]}, ${quadFocus[1]})")

    }
    fun translate(dScreenPos: FloatArray) {

        // update texture quad coordinates
        val dQuadPos = floatArrayOf(
                dScreenPos[0] / Resolution.SCREEN.w * 2f,
                -dScreenPos[1] / Resolution.SCREEN.h * 2f
        )

        if (!f.shape.position.xLocked) {
            quadCoords[0] += dQuadPos[0]
            quadFocus[0] += dQuadPos[0]
        }
        if (!f.shape.position.yLocked) {
            quadCoords[1] += dQuadPos[1]
            quadFocus[1] += dQuadPos[1]
        }

        // Log.v("SURFACE VIEW", "TRANSLATE -- quadCoords: (${quadCoords[0]}, ${quadCoords[1]})")
        // Log.v("SURFACE VIEW", "TRANSLATE -- quadFocus: (${quadFocus[0]}, ${quadFocus[1]})")

        hasTranslated = true

    }
    fun translate(dx: Float, dy: Float) {

        if (!f.shape.position.xLocked) {
            quadCoords[0] += dx
            quadFocus[0] += dx
        }
        if (!f.shape.position.yLocked) {
            quadCoords[1] += dy
            quadFocus[1] += dy
        }

        hasTranslated = true

    }
    fun zoom(dZoom: Float) {

        if (!f.shape.position.zoomLocked) {

            quadCoords[0] -= quadFocus[0]
            quadCoords[1] -= quadFocus[1]

            quadCoords[0] *= dZoom
            quadCoords[1] *= dZoom

            quadCoords[0] += quadFocus[0]
            quadCoords[1] += quadFocus[1]

            quadScale *= dZoom
            hasZoomed = true

        }

    }
    private fun rotate(p: FloatArray, theta: Float) : FloatArray {

        val sinTheta = sin(theta)
        val cosTheta = cos(theta)
        return floatArrayOf(
                p[0] * cosTheta - p[1] * sinTheta,
                p[0] * sinTheta + p[1] * cosTheta
        )

    }
    fun rotate(dTheta: Float) {

        if (!f.shape.position.rotationLocked) {

            quadCoords[0] -= quadFocus[0]
            quadCoords[1] -= quadFocus[1]

            val rotatedQuadCoords = rotate(floatArrayOf(quadCoords[0], quadCoords[1] * aspectRatio.toFloat()), dTheta)
            quadCoords[0] = rotatedQuadCoords[0]
            quadCoords[1] = rotatedQuadCoords[1]
            quadCoords[1] /= aspectRatio.toFloat()

            quadCoords[0] += quadFocus[0]
            quadCoords[1] += quadFocus[1]

            //Log.v("RR", "quadCoords: (${quadCoords[0]}, ${quadCoords[1]})")

            quadRotation += dTheta
            hasRotated = true

        }

    }
    fun resetQuad() {

        quadCoords[0] = 0f
        quadCoords[1] = 0f

        quadFocus[0] = 0f
        quadFocus[1] = 0f

        quadScale = 1f
        quadRotation = 0f

        hasTranslated = false
        hasZoomed = false
        hasRotated = false

    }
    private fun transformIsStrictTranslate() : Boolean {
        return hasTranslated && !hasZoomed && !hasRotated && quadCoords.none { abs(it) > 2f }
    }


    private val res = context.resources
    private var overlayHidden = false

//    private val vibrate = kotlinx.coroutines.Runnable {
//
//        Log.v("RENDERER", "wow u pressed that so long")
//
//        // vibrate
//        val vib = act.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            vib.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
//        } else {
//            //deprecated in API 26
//            vib.vibrate(15)
//        }
//
//        val overlay = act.findViewById<ConstraintLayout>(R.id.overlay)
//
//        val anim: AlphaAnimation = if (!overlayHidden) AlphaAnimation(1f, 0f) else AlphaAnimation(0f, 1f)
//        anim.duration = 500L
//        anim.fillAfter = true
//        anim.setAnimationListener(object : Animation.AnimationListener {
//            override fun onAnimationRepeat(animation: Animation?) {}
//            override fun onAnimationEnd(animation: Animation?) {
//                overlay.visibility = if (overlayHidden) ConstraintLayout.VISIBLE else ConstraintLayout.GONE
//            }
//
//            override fun onAnimationStart(animation: Animation?) {}
//
//        })
//        overlay.animation = anim
//        overlay.animation.start()
//        overlayHidden = !overlayHidden
//
//
//    }
    val aspectRatio = Resolution.SCREEN.run { h.toDouble() / w }

    var renderProfile = RenderProfile.DISCRETE
        set (value) {
            field = value
            Log.v(TAG, "renderPofile: ${value.name}")
        }


    var renderToTex = false

    var renderSingleThumbnail = false
    var renderAllThumbnails = false

    var validTextureThumbs = false

    var isRendering = false
    var isAnimating = false

    var renderShaderChanged = false
    var fgResolutionChanged = false
    var renderBackgroundChanged = false
    var loadTextureImage = false
    var resetContinuousSize = false
    var autofitColorSelected = false

    var interruptRender = false
    var interruptSuccessful = false
    var pauseRender = false
    var validAuxiliary = true


    private var floatPrecisionBits : Int? = null
    private var gpuRendererName = ""





    // private var arithmetic   = ""
    // private val arithmeticAtan = if (gpuRendererName.contains("PowerVR"))
    //     res.getString(R.string.arithmetic_atan_powervr) else
    //     res.getString(R.string.arithmetic_atan)
    private var seedInit     = ""
    private var conditional  = ""
    private var textureInit      = ""
    private var textureLoop      = ""
    private var textureFinal     = ""
    private var shapeInit      = ""
    private var shapeLoop      = ""
    private var shapeFinal     = ""

    private var renderShaderTemplate = ""
    private var renderShaderInitSF = ""
    private var renderShaderInitDF = ""
    private var renderShader = ""
    private var colorShader = ""


    // coordinates of default view boundaries
    private val viewCoords = floatArrayOf(
            -1f, 1f, 0f,     // top left
            -1f, -1f, 0f,     // bottom left
            1f, -1f, 0f,     // bottom right
            1f, 1f, 0f)    // top right
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)


    // create render program handles
    private var renderProgram     : Int = 0
    private var viewCoordsHandle  : Int = 0
    private var iterHandle        : Int = 0
    private var bailoutHandle     : Int = 0
    private var powerHandle       : Int = 0
    private var x0Handle          : Int = 0
    private var y0Handle          : Int = 0
    private var alpha0Handle      : Int = 0
    private var xScaleHandle      : Int = 0
    private var yScaleHandle      : Int = 0
    private var xCoordHandle      : Int = 0
    private var yCoordHandle      : Int = 0
    private var sinRotateHandle   : Int = 0
    private var cosRotateHandle   : Int = 0
    private var bgScaleHandle     : Int = 0
    private var juliaParamHandle  : Int = 0
    private val mapParamHandles   : IntArray = IntArray(MAX_SHAPE_PARAMS)
    private val textureParamHandles : IntArray = IntArray(MAX_TEXTURE_PARAMS)
    private var imageRenderHandle    : Int = 0




    // create sample program handles
    private var sampleProgram               : Int = 0
    private var viewCoordsSampleHandle      : Int = 0
    private var quadCoordsSampleHandle      : Int = 0
    private var textureSampleHandle         : Int = 0
    private var yOrientSampleHandle         : Int = 0
    private var texCoordScaleSampleHandle   : Int = 0
    private var texCoordShiftSampleHandle   : Int = 0


    // create color program handles
    private var colorProgram                : Int = 0
    private var colorModeHandle             : Int = 0
    private var viewCoordsColorHandle       : Int = 0
    private var quadCoordsColorHandle       : Int = 0
    private var yOrientColorHandle          : Int = 0
    private var texCoordScaleColorHandle    : Int = 0
    private var texCoordShiftColorHandle    : Int = 0
    private var numColorsHandle             : Int = 0
    private var textureColorHandle          : Int = 0
    private var paletteHandle               : Int = 0
    private var accent1Handle               : Int = 0
    private var accent2Handle               : Int = 0
    private var textureModeHandle           : Int = 0
    private var frequencyHandle             : Int = 0
    private var phaseHandle                 : Int = 0
    private var minHandle                   : Int = 0
    private var maxHandle                   : Int = 0
    private var convertYUVHandle            : Int = 0
    private var convert565Handle            : Int = 0
    private var imageTextureColorHandle     : Int = 0
    private var densityHandle               : Int = 0
    private var zoomColorHandle             : Int = 0
    private var adjustWithZoomHandle        : Int = 0



    private var texSpanHistSize = if (sc.continuousPosRender) TEX_SPAN_HIST_CONTINUOUS else TEX_SPAN_HIST_DISCRETE
    var calcNewTextureSpan = sc.autofitColorRange

    val textureSpan = TextureSpan(if (sc.continuousPosRender) MotionValue.DELTA_CONTINUOUS else MotionValue.DELTA_DISCRETE)

//    var textureMins = FloatArray(texSpanHistSize) { 0f }
//    var textureMaxs = FloatArray(texSpanHistSize) { 1f }
    private var numForegroundChunks = sc.chunkProfile.fgSingle
    private var numBackgroundChunks = sc.chunkProfile.bgSingle

    var videoTextureSpans : List<PointF>? = null



    private var vRenderShader : Int = 0
    private var vSampleShader : Int = 0
    private var fRenderShader : Int = 0
    private var fColorShader  : Int = 0
    private var fSampleShader : Int = 0

    private val refData = NativeReferenceReturnData()




    private lateinit var background1   : GLTexture
    private lateinit var background2   : GLTexture
    private lateinit var foreground1   : GLTexture
    private lateinit var foreground2   : GLTexture
    private lateinit var thumbnail     : GLTexture


    var fps = sc.targetFramerate.toDouble()
    // val fps = MotionValue(sc.targetFramerate.toFloat(), delta = 0.5f)
    var renderStartTime = 0L
    var renderDuration = 0L
    var continuousFrame = 0
    var continuousSize = 0.25
    var continuousRes = Point()
    var chunkCount = CHUNKS_PER_SECOND



    private val thumbBuffer = ByteBuffer.allocate(
            Resolution.THUMB.w * Resolution.THUMB.w * 4  // RGBA_8888
//            Resolution.THUMB.n*2  // RGB_5_6_5
    ).order(ByteOrder.nativeOrder())

    // private val refArray = DoubleArray(refSize*2) { 0.0 }
    private val refArray = DoubleArray(1) { 0.0 }

    private lateinit var background   : GLTexture
    private lateinit var bgAuxiliary  : GLTexture
    private lateinit var foreground   : GLTexture
    private lateinit var fgAuxiliary  : GLTexture

    private lateinit var image : GLTexture


    // allocate memory for textures
    private val quadBuffer =
            ByteBuffer.allocateDirect(viewCoords.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
    private val bgQuadBuffer =
            ByteBuffer.allocateDirect(viewCoords.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()

    // create variables to store texture and fbo IDs
    private val fboIDs = IntBuffer.allocate(1)
//    private val rboIDs = IntBuffer.allocate(1)






    // initialize byte buffer for the draw list
    // num coord values * 2 bytes/short
    private val drawListBuffer =
            ByteBuffer.allocateDirect(drawOrder.size * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .put(drawOrder)
                    .position(0)

    // initialize byte buffer for view coordinates
    // num coord values * 4 bytes/float
    private val viewBuffer =
            ByteBuffer.allocateDirect(viewCoords.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(viewCoords)
                    .position(0)
    private val viewChunkBuffer =
            ByteBuffer.allocateDirect(viewCoords.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()





    // RENDERSCRIPT INITIALIZERS

    val rs = RenderScript.create(context)
    private val perturbationPixelsScript = ScriptC_perturbationPixels(rs)
    val compactYUVScript = ScriptC_compactYUV(rs)
    val compactRGB565Script = ScriptC_compactRGB565(rs)
    // private val mandelbrotScript = ScriptC_mandelbrot(rs)
    // private val perturbationImageScript = ScriptC_perturbationImage(rs)
    // private val templateScript = ScriptC_template(rs)


    var rgbAllocation : Allocation? = null
    var yuvAllocation : Allocation? = null

    var rgbaThumb8888Allocation = Allocation.createTyped(rs, Type.createXY(rs,
            Element.RGBA_8888(rs),
            Resolution.THUMB.w,
            Resolution.THUMB.w
    ))
    var rgbThumb565Allocation = Allocation.createTyped(rs, Type.createXY(rs,
            Element.RGB_565(rs),
            Resolution.THUMB.w,
            Resolution.THUMB.w
    ))


    // memory allocation for reference iteration values
    private var refAllocation = Allocation.createTyped(rs, Type.createX(
            rs, Element.F64(rs), 1
    ))


//    private var fgOutAllocation = Allocation.createTyped(rs, Type.createXY(
//            rs, Element.F32_2(rs), sc.resolution.w, sc.resolution.h
//    ))
//    private var bgOutAllocation = Allocation.createTyped(rs, Type.createXY(
//            rs, Element.F32_2(rs), Resolution.BG.w, Resolution.BG.h
//    ))


    //memory allocation for indices of pixels that need to be iterated
    private var pixelsInAllocation  = Allocation.createTyped(rs, Type.createX(rs, Element.U8(rs), 1))
    private var pixelsOutAllocation = Allocation.createTyped(rs, Type.createX(rs, Element.U8(rs), 1))

    private val rsMsgHandler = object : RenderScript.RSMessageHandler() {
        override fun run() {
//            when (mID) {
//                0 -> if (!sc.continuousPosRender) act.findViewById<ProgressBar>(R.id.progressBar).progress += 2
//                1 -> Log.v("RENDERER", "renderscript interrupted!")
//            }
        }
    }





    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {

        // get GPU info
        gpuRendererName = gl.glGetString(GL10.GL_RENDERER)
        Log.v("RENDERER", "GL_RENDERER = " + gl.glGetString(GL10.GL_RENDERER))
        Log.v("RENDERER", "GL_VENDOR = " + gl.glGetString(GL10.GL_VENDOR))
        Log.v("RENDERER", "GL_VERSION = " + gl.glGetString(GL10.GL_VERSION))
        //Log.v("RENDERER", "EXTENSIONS = " + gl.glGetString(GL10.GL_EXTENSIONS).replace(" ", "\n"))

        // get fragment shader precision
        val fRange = IntBuffer.allocate(2)
        val fPrec = IntBuffer.allocate(1)
        val iRange = IntBuffer.allocate(2)
        val iPrec = IntBuffer.allocate(1)
        glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_HIGH_FLOAT, fRange, fPrec)
        glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_HIGH_INT, iRange, iPrec)
        Log.v("FSV", "float exponent range: ${fRange[0]}, ${fRange[1]}")
        Log.v("FSV", "float precision bits: ${fPrec[0]}")
        Log.v("FSV", "int exponent range: ${iRange[0]}, ${iRange[1]}")
        Log.v("FSV", "int precision bits: ${iPrec[0]}")
        floatPrecisionBits = fPrec[0]

        // get texture specs
        // val c = IntBuffer.allocate(1)
        // val d = IntBuffer.allocate(1)
        // glGetIntegerv(GL_MAX_TEXTURE_SIZE, c)
        // glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, d)
        // Log.v("FSV", "max texture size: ${c[0]}")
        // Log.v("FSV", "max texture image units: ${d[0]}")

        renderProgram = glCreateProgram()
        colorProgram = glCreateProgram()
        sampleProgram = glCreateProgram()

        background1 = GLTexture(Resolution.BG, GL_NEAREST, TEX_FORMAT, BG1_INDEX, "background1")
        background2 = GLTexture(Resolution.BG, GL_NEAREST, TEX_FORMAT, BG2_INDEX, "background2")
        foreground1 = GLTexture(sc.resolution, GL_NEAREST, TEX_FORMAT, FG1_INDEX, "foreground1")
        foreground2 = GLTexture(sc.resolution, GL_NEAREST, TEX_FORMAT, FG2_INDEX, "foreground2")
        thumbnail   = GLTexture(Resolution.THUMB, GL_NEAREST, TEX_FORMAT, THUMB_INDEX, "thumbnail")
        onLoadTextureImage(true)

        // Log.v("MAIN", "available heap size in MB: ${act.getAvailableHeapMemory()}")

        background = background1
        bgAuxiliary = background2
        foreground = foreground1
        fgAuxiliary = foreground2

        updateContinuousRes()


        rs.messageHandler = rsMsgHandler

        // load all vertex and fragment shader code
        val vRenderCode = readShader(R.raw.vert_render)
        renderShaderInitSF = readShader(R.raw.frag_render_init_sf)
        renderShaderInitDF = readShader(R.raw.frag_render_init_df)
        renderShaderTemplate = readShader(R.raw.frag_render)
        // renderShaderTemplate = readShader(R.raw.frag_test_image)
        colorShader = readShader(R.raw.frag_color)
        val vSampleCode = readShader(R.raw.vert_sample)
        val fSampleCode = readShader(R.raw.sample)
        // testCode = readShader(R.raw.precision_test)
        // testCode = readShader(R.raw.test)

        checkThresholdCross()
        updateRenderShader()
        fRenderShader = loadShader(GL_FRAGMENT_SHADER, renderShader)

        // create and compile shaders
        vRenderShader = loadShader(GL_VERTEX_SHADER, vRenderCode)
        vSampleShader = loadShader(GL_VERTEX_SHADER, vSampleCode)


        //fRenderShader = loadShader(GL_FRAGMENT_SHADER, perturbationCode)
        //fRenderShader = loadShader(GL_FRAGMENT_SHADER, precisionCode)

        fSampleShader = loadShader(GL_FRAGMENT_SHADER, fSampleCode)
        fColorShader  = loadShader(GL_FRAGMENT_SHADER, colorShader)
        //fColorShader  = loadShader(GL_FRAGMENT_SHADER, fSampleCode)


        glClearColor(0f, 0f, 0f, 1f)

        // generate framebuffer and renderbuffer objects
        glGenFramebuffers(1, fboIDs)
//        glGenRenderbuffers(1, rboIDs)
//        glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[1])
//        glBindRenderbuffer(GL_RENDERBUFFER, rboIDs[0])
//        glRenderbufferStorage(
//                GL_RENDERBUFFER,
//                GL_RGB8,
//                Resolution.SCREEN.w,
//                Resolution.SCREEN.h
//        )
//        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, rboIDs[0])




        // attach shaders and create renderProgram executables
        glAttachShader(renderProgram, vRenderShader)
        glAttachShader(renderProgram, fRenderShader)
        glLinkProgram(renderProgram)

//                val q = IntBuffer.allocate(1)
//                glGetProgramiv(renderProgram, GL_LINK_STATUS, q)
//                Log.e("RENDERER", "${q[0] == GL_TRUE}")


        getRenderUniformLocations()

        glAttachShader(sampleProgram, vSampleShader)
        glAttachShader(sampleProgram, fSampleShader)
        glLinkProgram(sampleProgram)

        viewCoordsSampleHandle      = glGetAttribLocation(sampleProgram, "viewCoords")
        quadCoordsSampleHandle      = glGetAttribLocation(sampleProgram, "quadCoords")
        textureSampleHandle         = glGetUniformLocation(sampleProgram, "tex")
        yOrientSampleHandle         = glGetUniformLocation(sampleProgram, "yOrient")
        texCoordScaleSampleHandle   = glGetUniformLocation(sampleProgram, "scale")
        texCoordShiftSampleHandle   = glGetUniformLocation(sampleProgram, "shift")

        glAttachShader(colorProgram, vSampleShader)
        glAttachShader(colorProgram, fColorShader)
        glLinkProgram(colorProgram)

        viewCoordsColorHandle       = glGetAttribLocation(colorProgram, "viewCoords")
        quadCoordsColorHandle       = glGetAttribLocation(colorProgram, "quadCoords")
        textureColorHandle          = glGetUniformLocation(colorProgram, "tex")
        yOrientColorHandle          = glGetUniformLocation(colorProgram, "yOrient")
        texCoordScaleColorHandle    = glGetUniformLocation(colorProgram, "scale")
        texCoordShiftColorHandle    = glGetUniformLocation(colorProgram, "shift")
        colorModeHandle             = glGetUniformLocation(colorProgram, "colorMode")
        numColorsHandle             = glGetUniformLocation(colorProgram, "numColors")
        paletteHandle               = glGetUniformLocation(colorProgram, "palette")
        accent1Handle               = glGetUniformLocation(colorProgram, "accent1")
        accent2Handle               = glGetUniformLocation(colorProgram, "accent2")
        textureModeHandle           = glGetUniformLocation(colorProgram, "textureMode")
        frequencyHandle             = glGetUniformLocation(colorProgram, "frequency")
        phaseHandle                 = glGetUniformLocation(colorProgram, "phase")
        minHandle                   = glGetUniformLocation(colorProgram, "textureMin")
        maxHandle                   = glGetUniformLocation(colorProgram, "textureMax")
        convertYUVHandle            = glGetUniformLocation(colorProgram, "convertYUV")
        convert565Handle            = glGetUniformLocation(colorProgram, "convert565")
        imageTextureColorHandle     = glGetUniformLocation(colorProgram, "imageTexture")
        densityHandle               = glGetUniformLocation(colorProgram, "density")
        zoomColorHandle             = glGetUniformLocation(colorProgram, "zoom")
        adjustWithZoomHandle        = glGetUniformLocation(colorProgram, "adjustWithZoom")

        renderToTex = true

    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {

    }

    override fun onDrawFrame(unused: GL10) {

        onPreDraw()
        Log.v(TAG, "renderProfile: ${renderProfile.name}")

        when (renderProfile) {

            RenderProfile.CONTINUOUS -> {

                isRendering = false

                // if (fps > sc.targetFramerate + 8.0 || fps < sc.targetFramerate - 5.0) {
                if (resetContinuousSize) onResetContinuousSize()
                else {
                    continuousSize =
                        (continuousSize * sqrt(fps / sc.targetFramerate.toFloat())).clamp(
                            MIN_CONTINUOUS_SIZE, MAX_CONTINUOUS_SIZE
                        )
                    updateContinuousRes()
                }
                // }

                renderStartTime = currentTimeNs()
                renderToTexture(foreground, continuous = true)
                renderDuration = currentTimeNs() - renderStartTime

                val t = renderDuration / 1e9
                if (t > 0.0) {

                    fps = 1.0/t
                    Log.v(TAG, "continuous fps -- total: %5f, this frame: %5f".format(fps, 1.0/t))

                }

                renderFromTexture(
                    foreground,
                    continuous = true,
                    texCoordScale = continuousRes.x.toFloat() / foreground.res.w.toFloat()
                )

                continuousFrame++
                val estRenderTime = 1e-9 * renderDuration.toDouble() / continuousSize.pow(2.0)
                chunkCount = (estRenderTime*CHUNKS_PER_SECOND).toInt().clamp(CHUNKS_PER_SECOND, foreground.res.h)

                Log.v(TAG, "estimated render time: $estRenderTime sec, chunks: ${chunkCount}")
                handler.updateRenderStats(estRenderTime, -1.0, "${continuousRes.x} x ${continuousRes.y}")


            }
            RenderProfile.DISCRETE -> {

                if (renderToTex) {

                    isRendering = true
                    renderToTex = false

                    if (sc.continuousPosRender) {

                        renderToTexture(foreground)

                    } else {

                        if (sc.renderBackground) renderToTexture(bgAuxiliary)
                        val bgInterrupt = interruptRender
                        val t = currentTimeNs()
                        if (!bgInterrupt) renderToTexture(fgAuxiliary)
                        val duration = (t.toDouble() - currentTimeNs().toDouble()) / 1e9
                        if (!interruptRender) {
                            resetQuad()
                            hasTranslated = false
                            hasZoomed = false
                            hasRotated = false
                            background = bgAuxiliary.also { bgAuxiliary = background }
                            foreground = fgAuxiliary.also { fgAuxiliary = foreground }
                            chunkCount = (duration*CHUNKS_PER_SECOND).toInt().clamp(CHUNKS_PER_SECOND, foreground.res.h)
                            validAuxiliary = true
                        }

                    }

                }

                if (sc.renderBackground) {
                    Log.v(TAG, "discrete -- render from background")
                    renderFromTexture(background)
                }

                when (renderProfile) {
                    RenderProfile.DISCRETE -> {
                        Log.v(TAG, "discrete -- render from foreground")
                        renderFromTexture(foreground, actualSize = isRenderingVideo)
                    }
                    RenderProfile.CONTINUOUS -> {
                        Log.v(TAG, "hybrid !!")  // discrete render was interrupted and profile changed to continuous
                        interruptRender = false
                        onPreDraw()
                        renderToTexture(foreground, continuous = true)
                        renderFromTexture(
                            foreground,
                            continuous = true,
                            texCoordScale = continuousRes.x.toFloat() / foreground.res.w.toFloat()
                        )
                    }
                    else -> {}
                }
                if (interruptRender) interruptRender = false

                validTextureThumbs = false

            }
            RenderProfile.SAVE_IMAGE, RenderProfile.SAVE_THUMBNAIL, RenderProfile.SHARE_IMAGE -> {

                // Log.v("RENDERER", "available heap size: ${act.getAvailableHeapMemory()} MB")
                System.gc()

                // save local copy of bookmark thumb
                var width = sc.resolution.w
                var height = sc.resolution.h
                val aspectScale = (sc.aspectRatio.r / AspectRatio.RATIO_SCREEN.r).toFloat()
                if (aspectScale > 1f) width = (height / sc.aspectRatio.r).toInt()
                if (aspectScale < 1f) height = (width * sc.aspectRatio.r).toInt()

                val bmp = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
                )

                if (sc.resolution.w <= Resolution.SCREEN.w) {
                    renderFromTexture(foreground, true)
                    migrate(foreground, bmp)
                } else {
                    val builder = sc.resolution.getBuilder()
                    val scale = builder.w / sc.resolution.w.toDouble()
                    val shifts = 0.0..(1.0 - scale) step scale
                    shifts.forEachIndexed { i, dx ->
                        shifts.reversed().forEachIndexed { j, dy ->

                            val d = PointF(dx.toFloat(), dy.toFloat())
                            if (aspectScale > 1f) d.x = d.x / aspectScale + (1f - scale.toFloat()) * 0.5f * (1f - 1f / aspectScale)
                            if (aspectScale < 1f) d.y = d.y * aspectScale + (1f - scale.toFloat()) * 0.5f * (1f - aspectScale)

                            renderFromTexture(foreground, actualSize = true, texCoordScale = scale.toFloat(), texCoordShift = d)
                            migrate(foreground, bmp, Point(i, j))

                        }
                    }
                }

                if (renderProfile == RenderProfile.SAVE_THUMBNAIL) {
                    saveImage(ThumbnailUtils.extractThumbnail(bmp, 240, 240))
                    bmp.recycle()
                } else {
                    saveImage(bmp)
                }
                renderFromTexture(foreground)

                renderProfile = RenderProfile.DISCRETE

            }
            RenderProfile.COLOR_THUMB -> {

                val t = currentTimeMs()


                val prevPalette = f.palette
                val prevShowProgress = sc.showProgress
                sc.showProgress = false
                if (renderToTex) {
                    renderToTexture(thumbnail)
                    renderToTex = false
                }
                sc.showProgress = prevShowProgress

                when {
                    renderSingleThumbnail -> listOf(f.palette)
                    renderAllThumbnails -> Palette.all
                    else -> listOf()
                }.forEach { palette ->
                    f.palette = palette
                    renderFromTexture(thumbnail, true)
                    migrate(thumbnail, palette.thumbnail!!)
                }

                f.palette = prevPalette
                if (renderAllThumbnails || renderSingleThumbnail) {
                    handler.updateColorThumbnails(renderAllThumbnails)
                }
                renderSingleThumbnail = false
                renderAllThumbnails = false


                if (sc.renderBackground) renderFromTexture(background)
                renderFromTexture(foreground)

                Log.v(TAG, "color thumbs took ${currentTimeMs() - t} ms")

            }
            RenderProfile.TEXTURE_THUMB -> {

                if (renderAllThumbnails) {

                    isRendering = true

                    val prevTexture = f.texture
//                    val prevPosition = f.shape.position.clone()
                    val prevAutoColor = sc.autofitColorRange
                    val prevTextureMin = textureSpan.min()
                    val prevTextureMax = textureSpan.max()
//                    val prevDetail = f.shape.params.detail.u

//                    checkThresholdCross { f.shape.position.reset() }
//                    f.shape.params.detail.u = 8.0
                    sc.showProgress = false
//                    sc.autofitColorRange = true


                    var i = 0
                    for (texture in f.shape.compatTextures) {

                        if (pauseRender) {
                            try {
                                Log.e("RENDERER", "about to sleep")
                                Thread.sleep(300L)
                            } catch (e: Exception) {
                            }
                            pauseRender = false
                        }
                        if (interruptRender) {
                            Log.d(TAG, "thumbnail render interrupted")
                            handler.updateTextureThumbnail(-1)
                            break
                        }

                        f.texture = texture
                        Log.v("RENDERER", "texture: ${f.texture.name}, radius: %e".format(f.texture.params.radius.scaledValue))

//                        val prevColorConfig = f.color.clone()
//                        f.color.reset()

                        // Log.v(TAG, "rendering texture thumbnail for ${f.texture.name}")
                        onRenderShaderChanged()
//                        calcNewTextureSpan = true
                        renderToTexture(thumbnail)
                        renderFromTexture(thumbnail, true)
                        migrate(thumbnail, texture.thumbnail!!)

                        renderProfile = RenderProfile.DISCRETE

//                        f.color.setFrom(prevColorConfig)

                        handler.updateTextureThumbnail(i)
                        i++

                    }

                    sc.autofitColorRange = prevAutoColor
                    sc.showProgress = true

                    if (interruptRender) interruptRender = false
                    else {
                        f.texture = prevTexture
                        setTextureSpan(prevTextureMin, prevTextureMax)
                        onRenderShaderChanged()
                        renderAllThumbnails = false
                        if (i == f.shape.compatTextures.size) validTextureThumbs = true
                    }

                }

//                if (renderToTex) {
//                    if (sc.renderBackground) renderToTexture(background)
//                    renderToTexture(foreground)
//                    renderToTex = false
//                }
                if (sc.renderBackground) renderFromTexture(background)
                renderFromTexture(foreground)

            }
            RenderProfile.SHAPE_THUMB -> {

                isRendering = !(sc.continuousPosRender || sc.editMode == EditMode.SHAPE)

                val prevPrecision = sc.gpuPrecision
                if (sc.gpuPrecision == GpuPrecision.DUAL) {
                    sc.gpuPrecision = GpuPrecision.SINGLE
                    onRenderShaderChanged()
                }

                val prevShape = f.shape
                val prevPalette = f.palette
                val prevFreq = f.color.frequency
                val prevPhase = f.color.phase
                val prevTexture = f.texture
                val prevTextureMode = f.textureRegion
                val prevFillColor = f.color.fillColor
                val prevShowProgress = sc.showProgress
                sc.showProgress = false

                f.texture = Texture.escape
                f.palette = Palette.yinyang
                f.textureRegion = TextureRegion.OUT
                f.color.apply {
                    fillColor = Color.WHITE
                    frequency = 0.0
                    phase = 0.0
                }


                val shapes = if (renderAllThumbnails) Shape.custom
                else listOf(f.shape)

                var i = 0
                for (shape in shapes) {
                    if (interruptRender) break
                    if (renderAllThumbnails) {
                        f.shape = shape
                        onRenderShaderChanged()
                    }
                    renderToTexture(thumbnail)
                    renderFromTexture(thumbnail, true)
                    migrate(thumbnail, shape.thumbnail!!)
                    handler.updateShapeThumbnail(shape, if (renderAllThumbnails) i else -1)
                    i++
                }

                renderProfile = RenderProfile.DISCRETE



                sc.showProgress = prevShowProgress
                f.texture = prevTexture
                f.textureRegion = prevTextureMode
                f.palette = prevPalette
                f.color.apply {
                    frequency = prevFreq
                    phase = prevPhase
                    fillColor = prevFillColor
                }
                f.shape = prevShape
                if (renderAllThumbnails) onRenderShaderChanged()
                renderAllThumbnails = false

                if (prevPrecision == GpuPrecision.DUAL) {
                    sc.gpuPrecision = GpuPrecision.DUAL
                    onRenderShaderChanged()
                }

                if (sc.renderBackground) renderFromTexture(background)
                renderFromTexture(foreground)

            }
            RenderProfile.KEYFRAME_THUMB -> {

                // Log.v("RENDERER", "available heap size: ${act.getAvailableHeapMemory()} MB")
                System.gc()

                // save local copy of bookmark thumb
                var width = sc.resolution.w
                var height = sc.resolution.h
                val aspectScale = (sc.aspectRatio.r / AspectRatio.RATIO_SCREEN.r).toFloat()
                if (aspectScale > 1f) width = (height / sc.aspectRatio.r).toInt()
                if (aspectScale < 1f) height = (width * sc.aspectRatio.r).toInt()

                val bmpThumb = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
                )

                if (sc.resolution.w <= Resolution.SCREEN.w) {
                    renderFromTexture(foreground, true)
                    migrate(foreground, bmpThumb)
                } else {
                    val builder = sc.resolution.getBuilder()
                    val scale = builder.w / sc.resolution.w.toDouble()
                    val shifts = 0.0..(1.0 - scale) step scale
                    shifts.forEachIndexed { i, dx ->
                        shifts.reversed().forEachIndexed { j, dy ->

                            val d = PointF(dx.toFloat(), dy.toFloat())
                            if (aspectScale > 1f) d.x = d.x / aspectScale + (1f - scale.toFloat()) * 0.5f * (1f - 1f / aspectScale)
                            if (aspectScale < 1f) d.y = d.y * aspectScale + (1f - scale.toFloat()) * 0.5f * (1f - aspectScale)

                            renderFromTexture(foreground, actualSize = true, texCoordScale = scale.toFloat(), texCoordShift = d)
                            migrate(foreground, bmpThumb, Point(i, j))

                        }
                    }
                }

                // saveImage(ThumbnailUtils.extractThumbnail(bmpThumb, 240, 240), asBookmarkThumb = true)
                Fractal.tempBookmark1.thumbnail = ThumbnailUtils.extractThumbnail(bmpThumb, 240, 240)
                bmpThumb.recycle()
                handler.addKeyframe()
                renderFromTexture(foreground)

                renderProfile = RenderProfile.DISCRETE

            }
            RenderProfile.VIDEO -> {

                if (renderToTex) {
                    renderToTex = false
                    renderToTexture(foreground)
                }
                renderFromTexture(foreground)

            }

        }

        isRendering = false

    }

    private fun onPreDraw() {
        if (renderShaderChanged)        onRenderShaderChanged()
        if (fgResolutionChanged)        onForegroundResolutionChanged()
        if (renderBackgroundChanged)    onRenderBackgroundChanged()
        if (loadTextureImage)           onLoadTextureImage()
        // if (resetContinuousSize)        onResetContinuousSize()
    }




    private fun renderToTexture(texture: GLTexture, continuous: Boolean = false) {

        Log.v(TAG, "TO; $texture, continuous: $continuous")

        if (!isRenderingVideo) handler.updateProgress(0.0)
        val renderToTexStartTime = currentTimeNs()
        var calcTimeTotal = 0L

        when (sc.hardwareProfile) {
            HardwareProfile.GPU -> {

//                when (texture) {
//                    foreground -> Log.e("RENDERER", "foreground")
//                    background -> Log.e("RENDERER", "background")
//                    else -> Log.e("RENDERER", "else")
//                }

                // Log.v(TAG, "render to texture")

                glUseProgram(renderProgram)
                glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer


                // pass in shape params
                glUniform2fv(juliaParamHandle, 1, f.shape.params.julia.toFloatArray(), 0)
                for (i in mapParamHandles.indices) {
                    val pArray =
                        if (i < f.shape.params.list.size) f.shape.params.list[i].toFloatArray().apply {
                            if (f.shape.params.list[i].toRadians) {
                                this[0] = this[0].inRadians()
                                this[1] = this[1].inRadians()
                            }
                        }
                        else floatArrayOf(0f, 0f)
                    // Log.v("RENDERER", "passing p${i+1} in as (${pArray[0]}, ${pArray[1]})")
                    glUniform2fv(mapParamHandles[i], 1, pArray, 0)
                }

                // pass in texture params
                for (i in textureParamHandles.indices) {
                    val qArray =
                        if (i < f.texture.params.list.size) f.texture.params.list[i].toFloatArray().apply {
                            if (f.texture.params.list[i].toRadians) {
                                this[0] = this[0].inRadians()
                                this[1] = this[1].inRadians()
                            }
                        }
                        else floatArrayOf(0f, 0f)
                    // Log.v(TAG, "passing in q${i+1} as (${qArray[0]}, ${qArray[1]})")
                    // handler.showMessage("q${i + 1}: ${qArray[0]}")
                    glUniform2fv(textureParamHandles[i], 1, qArray, 0)
                }


                val xScaleSD = f.shape.position.zoom / 2.0
                val yScaleSD = f.shape.position.zoom * aspectRatio / 2.0
                val xCoordSD = f.shape.position.x
                val yCoordSD = f.shape.position.y

                // pass in position params
                when (sc.gpuPrecision) {
                    GpuPrecision.SINGLE -> {

                        val xScaleSF = xScaleSD.toFloat()
                        val yScaleSF = yScaleSD.toFloat()
                        val xCoordSF = xCoordSD.toFloat()
                        val yCoordSF = yCoordSD.toFloat()

                        glUniform2fv(xScaleHandle, 1, floatArrayOf(xScaleSF, 0.0f), 0)
                        glUniform2fv(yScaleHandle, 1, floatArrayOf(yScaleSF, 0.0f), 0)
                        glUniform2fv(xCoordHandle, 1, floatArrayOf(xCoordSF, 0.0f), 0)
                        glUniform2fv(yCoordHandle, 1, floatArrayOf(yCoordSF, 0.0f), 0)

                    }
                    GpuPrecision.DUAL -> {

                        val xScaleDF = xScaleSD.split()
                        val yScaleDF = yScaleSD.split()
                        val xCoordDF = xCoordSD.split()
                        val yCoordDF = yCoordSD.split()

                        glUniform2fv(xScaleHandle, 1, xScaleDF, 0)
                        glUniform2fv(yScaleHandle, 1, yScaleDF, 0)
                        glUniform2fv(xCoordHandle, 1, xCoordDF, 0)
                        glUniform2fv(yCoordHandle, 1, yCoordDF, 0)

                    }
                }
                glUniform1fv(sinRotateHandle, 1, floatArrayOf(sin(f.shape.position.rotation).toFloat()), 0)
                glUniform1fv(cosRotateHandle, 1, floatArrayOf(cos(f.shape.position.rotation).toFloat()), 0)

                // pass in other parameters

                val power = if (f.shape.hasDynamicPower) f.shape.params.at(0).u.toFloat() else f.shape.power

                glUniform1ui(iterHandle, floor(f.shape.params.detail.scaledValue).toInt())
                glUniform1fv(bailoutHandle, 1, floatArrayOf(f.texture.params.radius.scaledValue.toFloat()), 0)
                glUniform1fv(powerHandle, 1, floatArrayOf(power), 0)
                glUniform1fv(x0Handle, 1, floatArrayOf(f.shape.params.seed.u.toFloat()), 0)
                glUniform1fv(y0Handle, 1, floatArrayOf(f.shape.params.seed.v.toFloat()), 0)
                glUniform2fv(alpha0Handle, 1, floatArrayOf(f.shape.alphaSeed.x.toFloat(), f.shape.alphaSeed.y.toFloat()), 0)

                glUniform1i(imageRenderHandle, if (f.texture.hasRawOutput) image.index else 0)


                glEnableVertexAttribArray(viewCoordsHandle)

                if (
                    sc.sampleOnStrictTranslate              &&
                    transformIsStrictTranslate()            &&
                    texture == fgAuxiliary
                ) {

                    Log.v("RENDERER", "strict translate")

                    val xIntersectQuadCoords: FloatArray
                    val yIntersectQuadCoords: FloatArray
                    val xIntersectViewCoords: FloatArray
                    val yIntersectViewCoords: FloatArray

                    val xComplementViewCoordsA: FloatArray
                    val yComplementViewCoordsA: FloatArray

                    val xComplementViewCoordsB = floatArrayOf(-1.0f, 1.0f)
                    val yComplementViewCoordsB: FloatArray


                    if (quadCoords[0] - quadScale > -1.0) {
                        xIntersectQuadCoords = floatArrayOf(quadCoords[0] - quadScale, 1.0f)
                        xIntersectViewCoords = floatArrayOf(-1.0f, -quadCoords[0] + quadScale)
                        xComplementViewCoordsA = floatArrayOf(-1.0f, quadCoords[0] - quadScale)
                    } else {
                        xIntersectQuadCoords = floatArrayOf(-1.0f, quadCoords[0] + quadScale)
                        xIntersectViewCoords = floatArrayOf(-quadCoords[0] - quadScale, 1.0f)
                        xComplementViewCoordsA = floatArrayOf(quadCoords[0] + quadScale, 1.0f)
                    }

                    if (quadCoords[1] - quadScale > -1.0) {
                        yIntersectQuadCoords = floatArrayOf(quadCoords[1] - quadScale, 1.0f)
                        yIntersectViewCoords = floatArrayOf(-1.0f, -quadCoords[1] + quadScale)
                        yComplementViewCoordsA = floatArrayOf(quadCoords[1] - quadScale, 1.0f)
                        yComplementViewCoordsB = floatArrayOf(-1.0f, quadCoords[1] - quadScale)
                    } else {
                        yIntersectQuadCoords = floatArrayOf(-1.0f, quadCoords[1] + quadScale)
                        yIntersectViewCoords = floatArrayOf(-quadCoords[1] - quadScale, 1.0f)
                        yComplementViewCoordsA = floatArrayOf(-1.0f, quadCoords[1] + quadScale)
                        yComplementViewCoordsB = floatArrayOf(quadCoords[1] + quadScale, 1.0f)
                    }


                    //===================================================================================
                    // NOVEL RENDER -- TRANSLATION COMPLEMENT
                    //===================================================================================

                    glViewport(0, 0, foreground.res.w, foreground.res.h)
                    glUniform1fv(bgScaleHandle, 1, floatArrayOf(1f), 0)
                    glVertexAttribPointer(
                        viewCoordsHandle,           // index
                        3,                          // coordinates per vertex
                        GL_FLOAT,                   // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        viewChunkBuffer             // coordinates
                    )
                    glFramebufferTexture2D(
                        GL_FRAMEBUFFER,             // target
                        GL_COLOR_ATTACHMENT0,       // attachment
                        GL_TEXTURE_2D,              // texture target
                        fgAuxiliary.id,             // texture
                        0                           // level
                    )
                    glClear(GL_COLOR_BUFFER_BIT)

                    //Log.e("RENDERER", "numChunks: $numChunks")
                    val areaA = (xComplementViewCoordsA[1] - xComplementViewCoordsA[0])*(yComplementViewCoordsA[1] - yComplementViewCoordsA[0])
                    val areaB = (xComplementViewCoordsB[1] - xComplementViewCoordsB[0])*(yComplementViewCoordsB[1] - yComplementViewCoordsB[0])
                    val chunksA = splitCoords(texture, xComplementViewCoordsA, yComplementViewCoordsA, (chunkCount*areaA).toInt().clamp(1, 100))
                    val chunksB = splitCoords(texture, xComplementViewCoordsB, yComplementViewCoordsB, (chunkCount*areaB).toInt().clamp(1, 100))
                    val totalChunks = chunksA.size + chunksB.size
                    var chunksRendered = 0
                    for (complementViewChunkCoordsA in chunksA) {

                        if (pauseRender) {
                            try {
                                Log.e("RENDERER", "about to sleep")
                                Thread.sleep(300L)
                            } catch (e: Exception) {
                            }
                            pauseRender = false
                        }
                        if (interruptRender) break

                        viewChunkBuffer.put(complementViewChunkCoordsA).position(0)
                        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)
                        glFinish()
                        chunksRendered++
                        if (sc.showProgress && renderProfile != RenderProfile.CONTINUOUS) {
                            handler.updateProgress(chunksRendered.toDouble()/totalChunks.toDouble())
                        }

                    }
                    for (complementViewChunkCoordsB in chunksB) {

                        if (pauseRender) {
                            try {
                                Log.e("RENDERER", "about to sleep")
                                Thread.sleep(300L)
                            } catch (e: Exception) {
                            }
                            pauseRender = false
                        }
                        if (interruptRender) break

                        viewChunkBuffer.put(complementViewChunkCoordsB).position(0)
                        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)
                        glFinish()
                        chunksRendered++
                        if (sc.showProgress && renderProfile != RenderProfile.CONTINUOUS) {
                            handler.updateProgress(chunksRendered.toDouble()/totalChunks.toDouble())
                        }

                    }


                    //===================================================================================
                    // SAMPLE -- TRANSLATION INTERSECTION
                    //===================================================================================

                    glUseProgram(sampleProgram)
                    glViewport(0, 0, fgAuxiliary.res.w, fgAuxiliary.res.h)

                    val intersectQuadCoords = floatArrayOf(
                        xIntersectQuadCoords[0], yIntersectQuadCoords[1], 0.0f,     // top left
                        xIntersectQuadCoords[0], yIntersectQuadCoords[0], 0.0f,     // bottom left
                        xIntersectQuadCoords[1], yIntersectQuadCoords[0], 0.0f,     // bottom right
                        xIntersectQuadCoords[1], yIntersectQuadCoords[1], 0.0f)    // top right
                    quadBuffer.put(intersectQuadCoords).position(0)

                    val intersectViewCoords = floatArrayOf(
                        xIntersectViewCoords[0], yIntersectViewCoords[1], 0.0f,     // top left
                        xIntersectViewCoords[0], yIntersectViewCoords[0], 0.0f,     // bottom left
                        xIntersectViewCoords[1], yIntersectViewCoords[0], 0.0f,     // bottom right
                        xIntersectViewCoords[1], yIntersectViewCoords[1], 0.0f)    // top right
                    viewChunkBuffer.put(intersectViewCoords).position(0)


                    glEnableVertexAttribArray(viewCoordsSampleHandle)
                    glEnableVertexAttribArray(quadCoordsSampleHandle)
                    glUniform1fv(yOrientSampleHandle, 1, floatArrayOf(1f), 0)
                    glUniform1fv(texCoordScaleSampleHandle, 1, floatArrayOf(1f), 0)
                    glUniform2fv(texCoordShiftSampleHandle, 1, floatArrayOf(0f, 0f), 0)
                    glUniform1i(textureSampleHandle, foreground.index)
                    glVertexAttribPointer(
                        viewCoordsSampleHandle,     // index
                        3,                          // coordinates per vertex
                        GL_FLOAT,                   // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        viewChunkBuffer             // coordinates
                    )
                    glVertexAttribPointer(
                        quadCoordsSampleHandle,     // index
                        3,                          // coordinates per vertex
                        GL_FLOAT,                   // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        quadBuffer                  // coordinates
                    )


                    glFramebufferTexture2D(
                        GL_FRAMEBUFFER,                 // target
                        GL_COLOR_ATTACHMENT0,           // attachment
                        GL_TEXTURE_2D,                  // texture target
                        fgAuxiliary.id,                 // texture
                        0                               // level
                    )

                    glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)

                    glDisableVertexAttribArray(viewCoordsSampleHandle)
                    glDisableVertexAttribArray(quadCoordsSampleHandle)

                } else {

                    //===================================================================================
                    // NOVEL RENDER -- ENTIRE TEXTURE
                    //===================================================================================

                    if (continuous) glViewport(0, 0, continuousRes.x, continuousRes.y)
                    else            glViewport(0, 0, texture.res.w, texture.res.h)

                    glUniform1fv(bgScaleHandle, 1, floatArrayOf(if (texture == bgAuxiliary) bgSize else 1f), 0)
                    glFramebufferTexture2D(
                        GL_FRAMEBUFFER,             // target
                        GL_COLOR_ATTACHMENT0,       // attachment
                        GL_TEXTURE_2D,              // texture target
                        texture.id,                 // texture
                        0                           // level
                    )

                    // check framebuffer status
                    val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
                    if (status != GL_FRAMEBUFFER_COMPLETE) {
                        Log.v("FRAMEBUFFER", "$status")
                    }

                    //glClear(GL_COLOR_BUFFER_BIT)

                    val chunks = splitCoords(texture, floatArrayOf(-1f, 1f), floatArrayOf(-1f, 1f), if (continuous) 1 else chunkCount)
                    //Log.e("RENDERER", "chunks: ${texture.chunks}")
                    val totalChunks = chunks.size
                    var chunksRendered = 0
                    for (viewChunkCoords in chunks) {

                        if (pauseRender) {
                            try {
                                Log.e("RENDERER", "about to sleep")
                                Thread.sleep(300L)
                            } catch (e: Exception) {}
                            pauseRender = false
                        }
                        if (interruptRender) {
                            Log.d(TAG, "renderToTex interrupted")
                            break
                        }

                        viewChunkBuffer.put(viewChunkCoords)
                        viewChunkBuffer.position(0)

                        glVertexAttribPointer(
                            viewCoordsHandle,   // index
                            3,                  // coordinates per vertex
                            GL_FLOAT,           // type
                            false,              // normalized
                            12,                 // coordinates per vertex * bytes per float
                            viewChunkBuffer     // coordinates
                        )


                        // Log.v(TAG, "drawing chunk ${chunksRendered + 1}/$totalChunks...")
                        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)
                        glFinish()   // force chunk to finish rendering before continuing

                        chunksRendered++
                        if (sc.showProgress && renderProfile == RenderProfile.DISCRETE) {
                            handler.updateProgress(chunksRendered.toDouble()/totalChunks.toDouble())
                        }

                    }

                    glDisableVertexAttribArray(viewCoordsHandle)

                }


                if (!interruptRender) {
                    if (sc.autofitColorRange && !isRenderingVideo) {
                        if (
                            continuous ||
                            (texture == foreground && renderProfile != RenderProfile.CONTINUOUS) ||
                            renderProfile == RenderProfile.TEXTURE_THUMB ||
                            isPreprocessingVideo
                        ) {

                            val t = currentTimeMs()

                            var min = Float.MAX_VALUE
                            var max = Float.MIN_VALUE

                            val pixel = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
                            pixel.rewind()

                            val xSamples = MINMAX_XSAMPLES
                            val ySamples = (xSamples * aspectRatio).toInt()

                            var outSamples = 0
                            val textureWidth = if (continuous) continuousRes.x else foreground.res.w
                            val textureHeight = if (continuous) continuousRes.y else foreground.res.h

                            for (i in 0..textureWidth step textureWidth / xSamples) {
                                for (j in 0..textureHeight step textureHeight / ySamples) {

                                    glReadPixels(i, j, 1, 1, texture.format, texture.type, pixel)

                                    val sx = pixel.float
                                    val sy = sx.toBits() and 1

                                    pixel.rewind()
                                    if (sy == TextureRegion.OUT.ordinal) {
                                        if (!sx.isInfinite() && !sx.isNaN()) {
                                            if (sx < min) min = sx
                                            if (sx > max) max = sx
                                            outSamples++
                                        }
                                    }

                                }
                            }

//                            autoBuffer.rewind()
//                            glReadPixels(0, 0, sample.res.w, sample.res.h, sample.format, sample.type, autoBuffer)
//                            autoBuffer.rewind()
//                            while (autoBuffer.hasRemaining()) {
//
//                                val sx = autoBuffer.float
//                                val sy = sx.toBits() and 1
//
//                                if (sy == TextureMode.OUT.ordinal) {
//                                    if (!sx.isInfinite() && !sx.isNaN()) {
//                                        if (sx < min) min = sx
//                                        if (sx > max) max = sx
//                                        outSamples++
//                                    }
//                                }
//
//                            }


                            // don't process extreme values
                            if (min != Float.MAX_VALUE && max != Float.MIN_VALUE) {

                                if (calcNewTextureSpan) {
                                    setTextureSpan(min, max)
//                                    textureSpan.center.set(0.5f * (max + min))
//                                    textureSpan.size.set(abs(max - min))
//                                    calcNewTextureSpan = false
                                } else {
                                    if (texture == foreground) {
                                        val weight = outSamples / (xSamples * ySamples).toFloat()
                                        textureSpan.center.impulse(0.5f * (max + min), weight)
                                        textureSpan.size.impulse(abs(max - min), weight)
                                        // Log.e("RENDERER", "$textureSpan")
                                    }
                                }

                                if (autofitColorSelected && !f.texture.hasRawOutput) {

                                    // adjust frequency and phase to match new fit

                                    val M = max
                                    val m = min
                                    val L = M - m
                                    val prevFreq = f.color.frequency
                                    val prevPhase = f.color.phase

                                    f.color.apply {
                                        frequency = prevFreq * L
                                        phase = prevPhase + prevFreq * m
                                        Log.e("RENDERER", "frequency set $frequency")
                                    }

                                    autofitColorSelected = false
                                }

                            }

                            Log.e("RENDERER", "min: ${textureSpan.min()}, max: ${textureSpan.max()}, t: ${currentTimeMs() - t} ms")

                        }
                    }
                }


            }
            HardwareProfile.CPU -> {

                handler.updateProgress(0.0)
                //Log.e("RENDERER", "CPU precision: ${sc.cpuPrecision}")


                when (sc.cpuPrecision) {
                    CpuPrecision.PERTURB -> {  // hardcoded for mandelbrot

                        val auxTexture = when (texture) {
                            foreground -> fgAuxiliary
                            background -> bgAuxiliary
                            else -> texture
                        }

                        var refCalcTimeTotal = 0L
                        var renderTimeTotal = 0L
                        var centerCalcTimeTotal = 0L
                        var samplesPerRow = 45
                        var nativeRefCalcTimeTotal = 0L

                        var numGlitchedPxls: Int
                        var pixelsInArray: ShortArray


                        val maxPixelsPerChunk = Resolution.SCREEN.run { w * h } / CPU_CHUNKS


                        numGlitchedPxls = auxTexture.res.w * auxTexture.res.h


                        val deadPixels = arrayListOf<Point>()

                        val glitchedPixels = ShortArray(auxTexture.res.w * auxTexture.res.h * 2) { index ->
                            val q = if (index % 2 == 0) (index / 2 % auxTexture.res.w).toShort() else floor((index / 2.0) / auxTexture.res.w).toInt().toShort()
                            if (q >= 6240) {
                                Log.v("RENDERER", "${(index / 2.0) / auxTexture.res.w}")
                            }
                            q
                        }
                        var pixelsOutArray: FloatArray

                        var z0 = Apcomplex(f.shape.position.xap, f.shape.position.yap)
                        val refPixel = Point(auxTexture.res.w / 2, auxTexture.res.h / 2)
                        val refPixels = arrayListOf(Point(refPixel))

                        var largestGlitchSize: Int
                        var numReferencesUsed = 0

                        val sinRotation = sin(f.shape.position.rotation)
                        val cosRotation = cos(f.shape.position.rotation)
                        val bgScale = if (auxTexture == background) 5.0 else 1.0
                        val sp = if (f.shape.position.zoom < 1e-100) 1e300 else 1.0
                        val sn = if (f.shape.position.zoom < 1e-100) 1e-300 else 1.0

                        Log.v("RENDERER", "x0: ${z0.real()}")
                        Log.v("RENDERER", "y0: ${z0.imag()}")

                        var auxTextureMin = Float.MAX_VALUE
                        var auxTextureMax = Float.MIN_VALUE


                        // MAIN LOOP
                        while (numReferencesUsed < MAX_REFERENCES) {


                            val d0xOffset = f.shape.position.xap.subtract(z0.real()).toDouble()
                            val d0yOffset = f.shape.position.yap.subtract(z0.imag()).toDouble()


                            // REFERENCE CALCULATION

                            val nativeReferenceStartTime = currentTimeMs()
                            val xMag = 0.5 * f.shape.position.zoom
                            val yMag = 0.5 * f.shape.position.zoom * aspectRatio
                            val xBasis = xMag * cosRotation - yMag * sinRotation
                            val yBasis = xMag * sinRotation + yMag * cosRotation
                            val d0xIn = doubleArrayOf(
                                -xBasis,
                                xBasis,
                                -xBasis,
                                xBasis,
                                0.5 * -xBasis,
                                0.5 * xBasis,
                                0.5 * -xBasis,
                                0.5 * xBasis
                            )
                            val d0yIn = doubleArrayOf(
                                yBasis,
                                yBasis,
                                -yBasis,
                                -yBasis,
                                0.5 * yBasis,
                                0.5 * yBasis,
                                0.5 * -yBasis,
                                0.5 * -yBasis
                            )
                            val refArrayNative = iterateReferenceNative(
                                z0.real().toString(),
                                z0.imag().toString(),
                                d0xIn,
                                d0yIn,
                                sc.perturbPrecision.toInt(),
                                f.shape.params.detail.u.toInt(),
                                MAX_REF_ITER,
                                f.texture.params.radius.scaledValue,
                                sp, sn,
                                refData
                            )
                            nativeRefCalcTimeTotal += currentTimeMs() - nativeReferenceStartTime
                            refAllocation.copyFrom(refArrayNative)


                            var numGlitchedPxlsRendered = 0
                            for (k in 0 until ceil(numGlitchedPxls.toDouble() / maxPixelsPerChunk).toInt()) {

                                val numGlitchedPxlsRemaining = numGlitchedPxls - numGlitchedPxlsRendered
                                val numChunkPxls = if (numGlitchedPxlsRemaining >= maxPixelsPerChunk) maxPixelsPerChunk else numGlitchedPxlsRemaining
                                Log.e("RENDERER", "pass ${k + 1}: ${(100.0 * numGlitchedPxlsRendered.toDouble() / numGlitchedPxls).toInt()}%")
                                //Log.e("RENDERER", "numGlitchedPxlsRemaining: $numGlitchedPxlsRemaining")
                                //Log.e("RENDERER", "numChunkPxls: $numChunkPxls")


                                pixelsInArray = glitchedPixels.sliceArray(2 * numGlitchedPxlsRendered until 2 * (numGlitchedPxlsRendered + numChunkPxls))
                                pixelsInAllocation = Allocation.createTyped(rs, Type.createX(
                                    rs,
                                    Element.I16_2(rs),
                                    numChunkPxls
                                ))
                                pixelsInAllocation.copyFrom(pixelsInArray)

                                pixelsOutArray = FloatArray(numChunkPxls * 2)
                                pixelsOutAllocation = Allocation.createTyped(rs, Type.createX(
                                    rs,
                                    Element.F32_2(rs),
                                    numChunkPxls
                                ))


                                // RENDERSCRIPT

                                val renderScriptStartTime = System.currentTimeMillis()
                                rsPerturbationPixels(
                                    auxTexture.res.run { Point(w, h) },
                                    numChunkPxls,
                                    d0xOffset,
                                    d0yOffset,
                                    sp, sn,
                                    refData,
                                    bgScale
                                )
                                pixelsOutAllocation.copyTo(pixelsOutArray)
                                renderTimeTotal += currentTimeMs() - renderScriptStartTime


                                // MARK GLITCHED PIXELS

                                //val glitchedPixels = ShortArray(numGlitchedPixels * 2)
                                //numGlitchedPixels = 0


                                for (i in 0 until numChunkPxls * 2 - 1 step 2) {
                                    val px = pixelsInArray[i].toInt()
                                    val py = pixelsInArray[i + 1].toInt()
                                    val qx: Float
                                    val qy: Float
                                    if (pixelsOutArray[i + 1] == 3f) {  // pixel is still glitched
                                        qx = 1f
                                        if (px == refPixel.x && py == refPixel.y) {  // even reference pixel still glitched (wtf??)
                                            qy = 1f
                                            deadPixels.add(Point(refPixel))
                                        } else {
                                            qy = 3f
                                            //glitchedPixels[2 * numGlitchedPixels] = pixelsInArray[i - 1]
                                            //glitchedPixels[2 * numGlitchedPixels + 1] = pixelsInArray[i]
                                            //numGlitchedPixels++
                                        }
                                    } else { // pixel no longer glitched -- update image
                                        qx = pixelsOutArray[i]
                                        qy = pixelsOutArray[i + 1]
                                        if (sc.autofitColorRange && (auxTexture == foreground || auxTexture == thumbnail)) {
                                            if (qx > auxTextureMax) auxTextureMax = qx
                                            if (qx < auxTextureMin) auxTextureMin = qx
                                        }
                                    }
                                    try {
                                        auxTexture.set(px, py, 0, qx)
                                    } catch (e: IndexOutOfBoundsException) {
                                        Log.e("RENDERER", "p: ($px, $py)")
                                    }
                                    auxTexture.set(px, py, 1, qy)
                                }
                                //if (numGlitchedPixels == 0) break
                                //Log.e("RENDERER", "total number of glitched pixels: $glitchedPixelsSize")

                                numGlitchedPxlsRendered += numChunkPxls


                            }


                            //val glitchedPixels = ShortArray(numGlitchedPixels * 2)
                            numGlitchedPxls = 0
                            for (i in 0 until numGlitchedPxlsRendered * 2 step 2) {
                                //val px = pixelsInArray[i - 1].toInt()
                                //val py = pixelsInArray[i].toInt()
                                val px = glitchedPixels[i].toInt()
                                val py = glitchedPixels[i + 1].toInt()
                                if (auxTexture.get(px, py, 1) == 3f) { // pixel still glitched
                                    // set earliest available index to this pixel
                                    glitchedPixels[numGlitchedPxls * 2] = glitchedPixels[i]
                                    glitchedPixels[numGlitchedPxls * 2 + 1] = glitchedPixels[i + 1]
                                    numGlitchedPxls++
                                }
                            }
                            Log.e("RENDERER", "numGlitchedPxls: $numGlitchedPxls")
                            if (numGlitchedPxls == 0) break
//                    Log.e("RENDER ROUTINE", "there are only $numGlitchedPxls glitched pixels -- clearing indices $numGlitchedPxls until $numGlitchedPxlsRendered")
//                    for (i in numGlitchedPxls*2 until numGlitchedPxlsRendered*2 - 1) {
//                        glitchedPixels[i] = 0.toShort()
//                        glitchedPixels[i + 1] = 0.toShort()
//                    }


                            val progress = (1.0 - numGlitchedPxls.toDouble() / (auxTexture.res.w * auxTexture.res.h)).pow(5.0)
                            val estRenderTime = 0.1f / progress.toFloat() * (currentTimeMs() - renderToTexStartTime)

                            Log.v("RENDERER",
                                "[refIter: ${refData.refIter}], " +
                                        "[skipIter: ${refData.skipIter}], " +
                                        "[refArray full: ${refData.refIter - refData.skipIter + 1 == MAX_REF_ITER}], " +
                                        "[est render time: %.3f]".format(estRenderTime)
//                            "[ax: ${refData.ax}], " +
//                            "[ay: ${refData.ay}], " +
//                            "[bx: ${refData.bx}], " +
//                            "[by: ${refData.by}], " +
//                            "[cx: ${refData.cx}], " +
//                            "[cy: ${refData.cy}]"
                            )
                            //Log.e("RENDERER", "estimated remaining render time: ${estRenderTime - (now() - renderToTexStartTime)/1000f}")
                            //Log.e("RENDERER", "progress: $progress")
                            handler.updateProgress(progress)


                            // GLITCH DETECTION

                            val minGlitchSize = 100
                            var glitch = arrayListOf<Point>()
                            while (samplesPerRow <= auxTexture.res.w) {

                                //glitch = findGlitchMostPixels(imArray, auxTexture.res.w / samplesPerRow, auxTexture.res)
                                glitch = findGlitchMostPixels(auxTexture, auxTexture.res.w / samplesPerRow, auxTexture.res.run { Point(w, h) })
                                //Log.e("RENDERER", "largest glitch size (sample rate= $samplesPerRow): ${glitch.size}")
                                if (glitch.size > minGlitchSize || samplesPerRow == auxTexture.res.w) break
                                samplesPerRow *= 2

                            }
                            largestGlitchSize = glitch.size
                            if (largestGlitchSize == 0) break


                            // CENTER CALCULATION

                            val centerCalcStartTime = currentTimeMs()
                            val center = harmonicMean(glitch)
                            refPixel.apply {
                                x = center.x
                                y = center.y
                            }

                            centerCalcTimeTotal += currentTimeMs() - centerCalcStartTime
                            refPixels.add(Point(refPixel))
                            //imArray.set(refPixel.x, refPixel.y, 1, auxTexture.res.w, 2, 5f)

                            val x0DiffAux = bgScale * f.shape.position.zoom * (refPixel.x.toDouble() / (auxTexture.res.w) - 0.5)
                            val y0DiffAux = bgScale * f.shape.position.zoom * (refPixel.y.toDouble() / (auxTexture.res.h) - 0.5) * aspectRatio

                            z0 = Apcomplex(
                                f.shape.position.xap.add(Apfloat((x0DiffAux * cosRotation - y0DiffAux * sinRotation).toString(), sc.perturbPrecision)),
                                f.shape.position.yap.add(Apfloat((x0DiffAux * sinRotation + y0DiffAux * cosRotation).toString(), sc.perturbPrecision))
                            )

                            numReferencesUsed++


                        }




                        Log.v("RENDERER", "${deadPixels.size} dead pixels")
                        for (p in deadPixels) {
                            val neighborX = if (p.x + 1 == auxTexture.res.w) p.x - 1 else p.x + 1
                            auxTexture.set(p.x, p.y, 0, auxTexture.get(neighborX, p.y, 0))
                            auxTexture.set(p.x, p.y, 1, auxTexture.get(neighborX, p.y, 1))
                        }
                        for (i in 0 until numGlitchedPxls step 2) {
                            val p = Point(glitchedPixels[i].toInt(), glitchedPixels[i + 1].toInt())
                            val neighborX = if (p.x + 1 == auxTexture.res.w) p.x - 1 else p.x + 1
                            auxTexture.set(p.x, p.y, 0, auxTexture.get(neighborX, p.y, 0))
                            auxTexture.set(p.x, p.y, 1, auxTexture.get(neighborX, p.y, 1))
                        }



                        calcTimeTotal = refCalcTimeTotal + renderTimeTotal + centerCalcTimeTotal
                        Log.v("RENDERER",
                            "[total: ${(currentTimeMs() - renderToTexStartTime) / 1000f} sec], " +
                                    "[reference: ${nativeRefCalcTimeTotal / 1000f} sec], " +
                                    "[renderscript: ${renderTimeTotal / 1000f} sec], " +
                                    "[glitch center: ${centerCalcTimeTotal / 1000f} sec], " +
                                    "[misc: ${(currentTimeMs() - renderToTexStartTime - calcTimeTotal) / 1000f} sec], " +
                                    "[num references: $numReferencesUsed]"
                        )

                        //auxTexture.put(imArray)
                        auxTexture.update()

                    }
                    CpuPrecision.DOUBLE -> {

                        //Log.e("RENDERER", "CPU double")

                        val auxTexture = when (texture) {
                            foreground -> if (sc.resolution.w > Resolution.R1440.w) foreground else fgAuxiliary
                            background -> bgAuxiliary
                            thumbnail -> thumbnail
                            else -> {
                                Log.e("RENDERER", "else")
                                fgAuxiliary
                            }
                        }
                        //val allocation = if (texture == background) bgOutAllocation else fgOutAllocation
                        val bgScale = if (texture == background) 5.0 else 1.0

                        val data = ScriptField_IterateData(rs, 1).apply {

                            set_width(0, texture.res.w, true)
                            set_height(0, texture.res.h, true)
                            set_height(0, texture.res.h, true)
                            set_aspectRatio(0, aspectRatio, true)
                            set_bgScale(0, bgScale, true)
                            set_maxIter(0, f.shape.params.detail.u.toLong(), true)
                            set_escapeRadius(0, f.texture.params.radius.scaledValue.toFloat(), true)
                            set_scale(0, 0.5 * f.shape.position.zoom, true)
                            set_xCoord(0, f.shape.position.x, true)
                            set_yCoord(0, f.shape.position.y, true)
                            set_sinRotation(0, sin(f.shape.position.rotation), true)
                            set_cosRotation(0, cos(f.shape.position.rotation), true)
                            set_x0(0, f.shape.params.seed.u, true)
                            set_y0(0, f.shape.params.seed.v, true)
                            set_juliaMode(0, f.shape.juliaMode, true)
                            if (f.shape.juliaMode) {
                                set_jx(0, f.shape.params.julia.u, true)
                                set_jy(0, f.shape.params.julia.v, true)
                            }
                            if (f.shape.params.list.size > 0) {
                                val p1 = f.shape.params.at(0).toDouble2()
                                set_p1x(0, p1.x, true)
                                set_p1y(0, p1.y, true)
                            }
                            if (f.shape.params.list.size > 1) {
                                val p2 = f.shape.params.at(1).toDouble2()
                                set_p2x(0, p2.x, true)
                                set_p2y(0, p2.y, true)
                            }
                            if (f.shape.params.list.size > 2) {
                                val p3 = f.shape.params.at(2).toDouble2()
                                set_p3x(0, p3.x, true)
                                set_p3y(0, p3.y, true)
                            }
                            if (f.shape.params.list.size > 3) {
                                val p4 = f.shape.params.at(3).toDouble2()
                                set_p4x(0, p4.x, true)
                                set_p4y(0, p4.y, true)
                            }

                        }

                        val numChunks = when (texture) {
                            background, thumbnail -> numBackgroundChunks
                            foreground -> numForegroundChunks
                            else -> 1
                        }
                        //val chunkSize = floor(texture.res.h.toDouble()/numChunks).toInt()
                        var numChunksRendered = 0

                        auxTexture.floatBuffer?.position(0)

                        var yStart: Int
                        var yEnd = 0

                        for (i in 0 until numChunks) {

                            if (interruptRender) break

                            yStart = yEnd
                            yEnd = ((i + 1).toDouble() / numChunks * texture.res.h).toInt()
                            //Log.e("RENDERER", "y range: ($yStart, $yEnd)")

                            data.set_yStart(0, yStart, true)
                            data.set_yEnd(0, yEnd, true)
                            auxTexture.put(f.shape.iterateNative(data))
                            numChunksRendered++

                            handler.updateProgress(numChunksRendered.toDouble()/numChunks.toDouble())

                        }

                        auxTexture.update()

                    }
                }

                if (!interruptRender) {

                    // change auxilliary texture
                    if (sc.resolution.w <= Resolution.R1440.w) {
                        if (texture == foreground) {
                            val temp = fgAuxiliary
                            fgAuxiliary = foreground
                            foreground = temp
                        }
                        if (texture == background) {
                            val temp = bgAuxiliary
                            bgAuxiliary = background
                            background = temp
                        }
                    }

                    // calculate texture min/max
                    if ((texture == foreground || texture == thumbnail) && sc.autofitColorRange) {

                        val t = currentTimeMs()

                        var textureMin = Float.MAX_VALUE
                        var textureMax = Float.MIN_VALUE
                        val xSamples = MINMAX_XSAMPLES
                        val ySamples = (xSamples * aspectRatio).toInt()

                        for (i in 0 until texture.res.w step texture.res.w / xSamples) {
                            for (j in 0 until texture.res.h step texture.res.h / ySamples) {
                                val sx = texture.get(i, j, 0)
                                val sy = texture.get(i, j, 1)
                                if (f.textureRegion.ordinal.toFloat() == sy || f.textureRegion == TextureRegion.BOTH) {
                                    if (sx < textureMin) textureMin = sx
                                    if (sx > textureMax) textureMax = sx
                                }
                            }
                        }
                        Log.v("RENDERER", "min-max: ($textureMin, $textureMax)")
                        Log.e("RENDERER", "minmax calc took ${(currentTimeMs() - t)/1000f} sec")

                    }
                }

            }
        }


        //Log.e("RENDERER", "misc operations took ${(now() - renderToTexStartTime - calcTimeTotal)/1000f} sec")

        val renderTime = (currentTimeNs() - renderToTexStartTime).toDouble() / 1e9
        Log.v("RENDERER", "renderToTexture took $renderTime sec")
        handler.updateRenderStats(-1.0, renderTime, "")
        // if (texture == continuous) Log.v("RENDERER", "fps: ${1000f / (now() - renderToTexStartTime)}")

    }

    private fun renderFromTexture(
        texture: GLTexture,
        actualSize: Boolean = false,
        continuous: Boolean = false,
        texCoordScale: Float = 1f,
        texCoordShift: PointF = PointF(0f, 0f),
        blockRenderFinishedListener: Boolean = false
    ) {

        Log.v(TAG, "FROM; $texture, continuous: $continuous, size: $continuousSize")

        val t = System.currentTimeMillis()

        //======================================================================================
        // PRE-RENDER PROCESSING
        //======================================================================================

        glUseProgram(colorProgram)

        // glBindFramebuffer(GL_FRAMEBUFFER, if (trueSize) fboIDs[1] else 0)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)

        val viewport = when {
            isRenderingVideo -> texture.res.videoCompanions!!.first  // companion is target video resolution
            actualSize -> if (texture.res.w > Resolution.SCREEN.w) texture.res.getBuilder() else texture.res
            else -> Resolution.SCREEN
        }.run { Point(w, h) }

        val yOrient = if (actualSize || isRenderingVideo) -1f else 1f

//        if (continuous) glViewport(0, 0, (foreground.res.w*foreground.res.w/continuousRes.x), (foreground.res.h*foreground.res.h/continuousRes.y))
//        else            glViewport(0, 0, viewport.x, viewport.y)

        Log.v("RENDERER", "viewport: ${viewport.x} x ${viewport.y}")
        glViewport(0, 0, viewport.x, viewport.y)

        val aspect = aspectRatio.toFloat()
        val buffer : FloatBuffer

        if (texture == background) {

            val bgVert1 = rotate(floatArrayOf(-bgSize * quadScale, bgSize * quadScale * aspect), quadRotation)
            val bgVert2 = rotate(floatArrayOf(-bgSize * quadScale, -bgSize * quadScale * aspect), quadRotation)
            val bgVert3 = rotate(floatArrayOf(bgSize * quadScale, -bgSize * quadScale * aspect), quadRotation)
            val bgVert4 = rotate(floatArrayOf(bgSize * quadScale, bgSize * quadScale * aspect), quadRotation)

            bgVert1[1] /= aspect
            bgVert2[1] /= aspect
            bgVert3[1] /= aspect
            bgVert4[1] /= aspect

            // create float array of background quad coordinates
            val bgQuadVertices = floatArrayOf(
                bgVert1[0] + quadCoords[0], bgVert1[1] + quadCoords[1], 0f,     // top left
                bgVert2[0] + quadCoords[0], bgVert2[1] + quadCoords[1], 0f,     // bottom left
                bgVert3[0] + quadCoords[0], bgVert3[1] + quadCoords[1], 0f,     // bottom right
                bgVert4[0] + quadCoords[0], bgVert4[1] + quadCoords[1], 0f)    // top right
            bgQuadBuffer
                .put(bgQuadVertices)
                .position(0)

            buffer = bgQuadBuffer

        }
        else {

            val vert1 = rotate(floatArrayOf(-quadScale, quadScale * aspect), quadRotation)
            val vert2 = rotate(floatArrayOf(-quadScale, -quadScale * aspect), quadRotation)
            val vert3 = rotate(floatArrayOf(quadScale, -quadScale * aspect), quadRotation)
            val vert4 = rotate(floatArrayOf(quadScale, quadScale * aspect), quadRotation)

            vert1[1] /= aspect
            vert2[1] /= aspect
            vert3[1] /= aspect
            vert4[1] /= aspect

            // create float array of quad coordinates
            val quadVertices = floatArrayOf(
                vert1[0] + quadCoords[0], vert1[1] + quadCoords[1], 0f,     // top left
                vert2[0] + quadCoords[0], vert2[1] + quadCoords[1], 0f,     // bottom left
                vert3[0] + quadCoords[0], vert3[1] + quadCoords[1], 0f,     // bottom right
                vert4[0] + quadCoords[0], vert4[1] + quadCoords[1], 0f)    // top right
            quadBuffer
                .put(quadVertices)
                .position(0)

            buffer = quadBuffer

        }

        // vertex shader uniforms
        glUniform1fv(yOrientColorHandle, 1, floatArrayOf(yOrient), 0)
        glUniform1fv(texCoordScaleColorHandle, 1, floatArrayOf(texCoordScale), 0)
        glUniform2fv(texCoordShiftColorHandle, 1, floatArrayOf(texCoordShift.x, texCoordShift.y), 0)

        // fragment shader uniforms
        glUniform1i(colorModeHandle, ColorMode.MINMAX.ordinal)
        glUniform1i(numColorsHandle, f.palette.size)
        glUniform3fv(paletteHandle, f.palette.size, f.palette.flatPalette, 0)
        glUniform3fv(accent1Handle, 1, colorToRGB(f.color.fillColor), 0)
        glUniform3fv(accent2Handle, 1, colorToRGB(f.color.outlineColor), 0)
        glUniform1ui(textureModeHandle, f.textureRegion.ordinal)
        glUniform1fv(frequencyHandle, 1, floatArrayOf(f.color.frequency.toFloat()), 0)
        glUniform1fv(phaseHandle, 1, floatArrayOf(f.color.phase.toFloat()), 0)
        glUniform1fv(densityHandle, 1, floatArrayOf(if (f.texture.usesDensity) f.color.density.toFloat() else 0f), 0)

//        if (sc.autofitColorRange) {
//            if (renderProfile == RenderProfile.TEXTURE_THUMB) {
//                textureMin = textureMins[0]
//                textureMax = textureMaxs[0]
//            }
//            else if (f.texture.hasRawOutput || textureMin == textureMax) {
//                textureMin = 0f
//                textureMax = 1f
//            }
//            else {
//                textureMin = textureMins.average().toFloat()
//                textureMax = textureMaxs.average().toFloat()
//                if (isRenderingVideo) Log.e("RENDERER", "min: $textureMin, max: $textureMax")
//            }
//        }

        glUniform1fv(minHandle, 1, floatArrayOf(textureSpan.min()), 0)
        glUniform1fv(maxHandle, 1, floatArrayOf(textureSpan.max()), 0)
        glUniform1iv(convertYUVHandle, 1, intArrayOf(if (isRenderingVideo) 1 else 0), 0)
        glUniform1iv(convert565Handle, 1, intArrayOf(if (texture == thumbnail) 1 else 0), 0)
        glUniform1iv(imageTextureColorHandle, 1, intArrayOf(if (f.texture.hasRawOutput) 1 else 0), 0)
        glUniform1iv(adjustWithZoomHandle, 1, intArrayOf(if (f.texture.auto) 1 else 0), 0)
        glUniform1fv(zoomColorHandle, 1, floatArrayOf(quadScale), 0)

        glEnableVertexAttribArray(viewCoordsColorHandle)
        glEnableVertexAttribArray(quadCoordsColorHandle)



        glUniform1i(textureColorHandle, texture.index)
        glVertexAttribPointer(
            viewCoordsColorHandle,      // index
            3,                          // coordinates per vertex
            GL_FLOAT,                   // type
            false,                      // normalized
            12,                         // coordinates per vertex * bytes per float
            viewBuffer                  // coordinates
        )
        glVertexAttribPointer(
            quadCoordsColorHandle,      // index
            3,                          // coordinates per vertex
            GL_FLOAT,                   // type
            false,                      // normalized
            12,                         // coordinates per vertex * bytes per float
            buffer                      // coordinates
        )

        if (!sc.renderBackground || texture == background) glClear(GL_COLOR_BUFFER_BIT)
        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)

        glDisableVertexAttribArray(viewCoordsColorHandle)
        glDisableVertexAttribArray(quadCoordsColorHandle)

        if (!isRenderingVideo) handler.updateProgress(0.0)
        // Log.v("RENDERER", "renderFromTexture took ${System.currentTimeMillis() - t} ms")

        if (isRenderingVideo) {

            val outputResolution = sc.resolution.videoCompanions!!.first
            val height16x9 = outputResolution.w*16/9
            val skipRows = (outputResolution.h - height16x9)/2
            val bb = ByteBuffer.allocate(outputResolution.n*4)
            glReadPixels(
                0,
                0,
                outputResolution.w,
                outputResolution.h,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                bb
            )
            bb.position(0)
            renderFinishedListener?.onRenderFinished(bb.array())

        }
        else if (!blockRenderFinishedListener) {
            renderFinishedListener?.onRenderFinished(null)
        }

    }

    private fun migrate(texture: GLTexture, bitmap: Bitmap, subImageIndex: Point? = null) {

        val t1 = currentTimeMs()

        val buffer          : ByteBuffer?
        val readWidth       : Int
        val readHeight      : Int
        val widthOffset     : Int
        val heightOffset    : Int

        if (texture == thumbnail) {
            buffer = thumbBuffer
            readWidth = thumbnail.res.w
            readHeight = thumbnail.res.w
            widthOffset = 0
            heightOffset = (0.5*texture.res.h*(1.0 - 1.0/aspectRatio)).roundToInt()
        }
        else {
            buffer = texture.byteBuffer
            if (texture.res.w > Resolution.SCREEN.w) {
                val builder = texture.res.getBuilder()
                if (sc.aspectRatio.r >= AspectRatio.RATIO_SCREEN.r) {
                    readWidth = (builder.h / sc.aspectRatio.r).toInt()
                    readHeight = builder.h
                }
                else {
                    readWidth = builder.w
                    readHeight = (builder.w * sc.aspectRatio.r).toInt()
                }
                val croppedBuilderDims = sc.aspectRatio.getDimensions(builder)
                widthOffset = (builder.w - croppedBuilderDims.x)/2
                heightOffset = (builder.h - croppedBuilderDims.y)/2
            }
            else {
                readWidth = bitmap.width
                readHeight = bitmap.height
                widthOffset = (texture.res.w - bitmap.width)/2
                heightOffset = (texture.res.h - bitmap.height)/2
            }
        }
        buffer?.position(0)

//        glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[1])
//        glViewport(0, 0, texture.res.w, texture.res.h)

        val t2 = currentTimeMs()

        // Log.e(TAG, "heightOffset: $heightOffset, readWidth: $readWidth, readHeight: $readHeight")
        glReadPixels(
            widthOffset,
            heightOffset,
            readWidth,
            readHeight,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            buffer
        )
        Log.v("RENDERER", "glReadPixels took ${currentTimeMs() - t2} ms")

        //logError()

        Log.v("RENDERER", "bitmap migration took ${currentTimeMs() - t1} ms")

        if (texture == thumbnail) {

            thumbBuffer.position(0)
            rgbaThumb8888Allocation.copyFrom(thumbBuffer.array())

            compactRGB565Script.apply {
                _gOut = rgbThumb565Allocation
                forEach_compact(rgbaThumb8888Allocation)
            }

            rgbThumb565Allocation.copyTo(bitmap)

//            var k = 0
//            for (j in 0 until bitmap.height) {
//                for (i in 0 until bitmap.width) {
//
//                    val p = thumbBuffer.getInt(4 * k)
//                    thumbBuffer.putShort(2 * k, p.toShort())
//                    k++
//
//                }
//            }
//            bitmap.copyPixelsFromBuffer(thumbBuffer)

        }
        else if (subImageIndex != null) {

            // Log.v("RENDERER", "subImageIndex: $subImageIndex")

            val builder = sc.resolution.getBuilder()
            var writeWidth = builder.w
            var writeHeight = builder.h
            if (sc.aspectRatio.r > AspectRatio.RATIO_SCREEN.r) {
                writeWidth = (builder.h / sc.aspectRatio.r).toInt()
            }
            else if (sc.aspectRatio.r < AspectRatio.RATIO_SCREEN.r) {
                writeHeight = (builder.w * sc.aspectRatio.r).toInt()
            }

            val pixels = IntArray(writeWidth * writeHeight)
            texture.byteBuffer?.asIntBuffer()?.get(pixels)
            pixels.apply { forEachIndexed { i, c ->
                set(i, Color.argb(255, Color.blue(c), Color.green(c), Color.red(c)))
            }}
            Log.v("RENDERER", "y: ${subImageIndex.y * builder.h}, height: ${builder.h}, bitmap height: ${bitmap.height}")
            bitmap.setPixels(
                pixels,
                0, writeWidth,
                subImageIndex.x * writeWidth,
                subImageIndex.y * writeHeight,
                writeWidth,
                writeHeight
            )

        }
        else {
            buffer?.position(0)
            bitmap.copyPixelsFromBuffer(buffer)
        }

    }
    private fun saveImage(im: Bitmap) {

        // Log.v("RENDERER", "available heap size: ${act.getAvailableHeapMemory()} MB")

        // convert bitmap to jpeg
        val bos = ByteArrayOutputStream()
        val compressed = im.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        if (!compressed) { Log.e("RENDERER", "could not compress image") }

        // Log.v("RENDERER", "available heap size: ${act.getAvailableHeapMemory()} MB")

        if (renderProfile != RenderProfile.SAVE_THUMBNAIL) im.recycle()

        // get current date and time
        val c = GregorianCalendar(TimeZone.getDefault())
        // Log.v("RENDERER", "${c[Calendar.YEAR]}, ${c[Calendar.MONTH]}, ${c[Calendar.DAY_OF_MONTH]}")
        val year = c[Calendar.YEAR]
        val month = c[Calendar.MONTH]
        val day = c[Calendar.DAY_OF_MONTH]
        val hour = c[Calendar.HOUR_OF_DAY]
        val minute = c[Calendar.MINUTE]
        val second = c[Calendar.SECOND]

        val appNameAbbrev = res.getString(R.string.fe_abbrev)
        val subDirectory = Environment.DIRECTORY_PICTURES + "/" + res.getString(R.string.app_name)
        val imageName = "${appNameAbbrev}_%4d%02d%02d_%02d%02d%02d".format(year, month + 1, day, hour, minute, second)



        // save image with unique filename
        when (renderProfile) {
            RenderProfile.SAVE_IMAGE -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

                    // app external storage directory
                    val dir = File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES),
                        res.getString(R.string.app_name
                        ))

                    // create directory if not already created
                    if (!dir.exists()) {
                        Log.v("RENDERER", "Directory does not exist -- creating...")
                        when {
                            dir.mkdir() -> Log.v("RENDERER", "Directory created")
                            dir.mkdirs() -> Log.v("RENDERER", "Directories created")
                            else -> {
                                Log.e("RENDERER", "Directory could not be created")
                                handler.showMessage(R.string.msg_error)
                                return
                            }
                        }
                    }

                    val file = File(dir, "$imageName.jpg")
                    try {
                        file.createNewFile()
                    } catch (e: IOException) {
                        handler.showMessage(R.string.msg_error)
                    }
                    if (file.exists()) {
                        val fos = FileOutputStream(file)
                        fos.write(bos.toByteArray())
                        fos.close()

                        val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        val contentUri = Uri.fromFile(file)
                        scanIntent.data = contentUri
                        context.sendBroadcast(scanIntent)
                    }

                } else {

                    val resolver = context.contentResolver
                    val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val imageDetails = ContentValues().apply {
                        put(MediaStore.Images.ImageColumns.RELATIVE_PATH, subDirectory)
                        put(MediaStore.Images.ImageColumns.DISPLAY_NAME, imageName)
                        put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg")
                    }

                    val contentUri = resolver.insert(imageCollection, imageDetails)
                    val fos = resolver.openOutputStream(contentUri!!, "w")

                    fos?.write(bos.toByteArray())
//            val compressedSize = compressNative(texture!!.byteBuffer!!.array(), texture.res.w, texture.res.h)
//            fos?.write(texture.byteBuffer?.array()?.sliceArray(0 until compressedSize))
                    fos?.close()

                }
                handler.showMessage("${res.getString(R.string.msg_save_successful)} /$subDirectory")
            }
            RenderProfile.SAVE_THUMBNAIL -> {
                val fos = context.openFileOutput("bm_thumb_$imageName.jpg", Context.MODE_PRIVATE)
                fos?.write(bos.toByteArray())
                fos?.close()
                Fractal.tempBookmark1.thumbnailPath = "bm_thumb_$imageName.jpg"
                Log.v("RENDERER", "thumbnailPath: ${Fractal.tempBookmark1.thumbnailPath}")
                Fractal.tempBookmark1.thumbnail = im
                handler.showBookmarkDialog()
            }
            RenderProfile.SHARE_IMAGE -> {
                context.deleteFile("temp_image.jpg")
                val fos = context.openFileOutput("temp_image.jpg", Context.MODE_PRIVATE)
                fos?.write(bos.toByteArray())
                fos?.close()
                im.recycle()
                handler.shareImage()
            }
            else -> {}
        }

    }

    private var testCode = ""
    private fun readShader(id: Int) : String {

        var str = ""
        val br = res.openRawResource(id).bufferedReader()
        var line: String?

        while (br.readLine().also { line = it } != null) {
            str += line
            str += "\n"
        }
        br.close()

        //Log.v("RENDERER", str)
        //Log.e("RENDERER", "color shader length: ${str.length}")

        return str

    }
    private fun setNativeMethods() {

        // link native methods with corresponding shapes
        Shape.default.filter { it.isTranscendental }.forEach {
            it.iterateNative = { data ->

                when (it) {
                    Shape.sine -> iterateSine2Native(data[0])
//                    Shape.clover         -> iterateCloverNative(data[0])
//                    Shape.sine           -> iterateSineNative(data[0])
//                    Shape.horseshoeCrab  -> iterateHorseshoeCrabNative(data[0])
//                    Shape.kleinian       -> iterateKleinianNative(data[0])
//                    Shape.nova2          -> iterateNova2Native(data[0])
//                    Shape.collatz        -> iterateCollatzNative(data[0])
//                    Shape.mandelbrotPow  -> iterateMandelbrotPowNative(data[0])
                    else                 -> floatArrayOf()
                }

            }
        }

    }
    fun setTextureSpan(min: Float, max: Float) {
        Log.v(TAG, "setting texture span to: ($min, $max)")
        calcNewTextureSpan = false
        textureSpan.size.set(abs(max - min))
        textureSpan.center.set(0.5f*(max + min))
    }


    fun checkThresholdCross(event: () -> Unit = {}) {

        if (sc.autoPrecision) {

            val prevZoom = f.shape.position.zoom
            event()

            // update hardware profile
//            val prevHardwareProfile = sc.hardwareProfile
//            sc.hardwareProfile = when {
//                f.position.zoom < GpuPrecision.SINGLE.threshold && f.shape.isTranscendental -> HardwareProfile.CPU
//                f.position.zoom < GpuPrecision.DUAL.threshold && f.shape.hasPerturbation && BuildConfig.PAID_VERSION && !f.shape.juliaMode -> {
//                    f.position.updatePrecision(sc.perturbPrecision)
//                    HardwareProfile.CPU
//                }
//                else -> HardwareProfile.GPU
//            }
//            if (sc.hardwareProfile != prevHardwareProfile) onHardwareProfileChanged()


            // update gpu precision
            val prevGpuPrecision = sc.gpuPrecision
            sc.gpuPrecision = when {
                f.shape.position.zoom <= GpuPrecision.SINGLE.threshold && (!f.shape.slowDualFloat || sc.allowSlowDualfloat) -> GpuPrecision.DUAL
                else -> GpuPrecision.SINGLE
            }
            if (sc.hardwareProfile == HardwareProfile.GPU && sc.gpuPrecision != prevGpuPrecision) {
                onGpuPrecisionChanged()
            }

            // if (f.shape.position.zoom <= GpuPrecision.SINGLE.threshold && !sc.allowSlowDualfloat) handler.showSlowDualfloatDialog()


//            if (BuildConfig.DEV_VERSION) {
//
//                // update cpu precision
//                val prevCpuPrecision = sc.cpuPrecision
//                sc.cpuPrecision = when {
//                    f.shape.position.zoom > CpuPrecision.DOUBLE.threshold -> CpuPrecision.DOUBLE
//                    // f.position.zoom < CpuPrecision.DOUBLE.threshold && f.shape.hasPerturbation -> CpuPrecision.PERTURB
//                    else -> CpuPrecision.DOUBLE
//                }
//                if (sc.hardwareProfile == HardwareProfile.CPU && sc.cpuPrecision != prevCpuPrecision) {
//                    onCpuPrecisionChanged()
//                }
//
//                val prevScaleStrings = "%e".format(Locale.US, prevZoom).split("e")
//                val prevScaleExponent = -prevScaleStrings[1].toDouble()
//                val prevScaleOrdinal = floor(prevScaleExponent / 12.0).toLong()
//
//                val scaleStrings = "%e".format(Locale.US, f.shape.position.zoom).split("e")
//                val scaleExponent = -scaleStrings[1].toDouble()
//                val scaleOrdinal = floor(scaleExponent / 12.0).toLong()
//
//                sc.perturbPrecision = (scaleOrdinal + 2)*12
//                if (scaleOrdinal != prevScaleOrdinal) f.shape.position.updatePrecision(sc.perturbPrecision)
//
//            }

            var zoomLimitReached = false
            when (sc.hardwareProfile) {
                HardwareProfile.GPU -> {
                    if (!f.shape.slowDualFloat || sc.allowSlowDualfloat) {
                        if (prevZoom > GpuPrecision.DUAL.threshold && f.shape.position.zoom < GpuPrecision.DUAL.threshold) zoomLimitReached = true
                    } else {
                        if (prevZoom > GpuPrecision.SINGLE.threshold && f.shape.position.zoom < GpuPrecision.SINGLE.threshold) zoomLimitReached = true
                    }
                }
                HardwareProfile.CPU -> {
//                    if (BuildConfig.PAID_VERSION && f.shape.hasPerturbation && !f.shape.juliaMode) {
//                        if (prevScale > CpuPrecision.PERTURB.threshold && f.position.zoom < CpuPrecision.PERTURB.threshold) zoomLimitReached = true
//                    }
//                    else {
                    if (prevZoom > CpuPrecision.DOUBLE.threshold && f.shape.position.zoom < CpuPrecision.DOUBLE.threshold) zoomLimitReached = true
//                    }
                }
            }
            if (zoomLimitReached) handler.showMessage(res.getString(R.string.msg_zoom_limit))


        }

    }




    // ===========================================================================================
    //  OPENGL ES UTILITY FUNCTIONS
    // ===========================================================================================

    private fun updateRenderShader() {

//        val imageUniform = if (f.texture == Texture.orbitTrapImage) "uniform highp sampler2D image;" else ""
        val splitHandle = "vec2 split(float a) { return split${if (sc.useAlternateSplit) "2" else "1"}(a); }"

        val customShapeHandleSingle =
                if (f.shape.customLoopSingle == "") ""
                else SHAPE_FUN_SINGLE.format(f.shape.customLoopSingle)

        val customShapeHandleDual =
                if (f.shape.customLoopDual == "") ""
                else SHAPE_FUN_DUAL.format(f.shape.customLoopDual)

        val generalInit = when (sc.gpuPrecision) {
            GpuPrecision.SINGLE -> renderShaderInitSF
            GpuPrecision.DUAL -> renderShaderInitDF
        }
        seedInit = when {
            f.shape.juliaMode -> { "julia_mode(z, c);" }
            f.shape.juliaSeed -> { "julia_seed(z, c);" }
            else -> ""
        }
        conditional   = "if(${f.shape.conditional})"
        shapeInit     = f.shape.init
        textureInit   = f.texture.init
        shapeLoop     = "z = ${f.shape.loop};"
        textureLoop   = f.texture.loop

        if (f.texture.usesSecondDelta) textureLoop += "beta = mandelbrot_delta2(beta, alpha, z1);"

        val delta1 = if (f.shape.hasAnalyticDelta && f.shape.juliaMode) f.shape.deltaJulia1 else f.shape.delta1
        if (f.texture.usesFirstDelta) textureLoop += "alpha = $delta1;"

        shapeFinal    = res.getString(f.shape.final)
        textureFinal  = "textureValueInt = " + (if (f.texture.hasRawOutput) f.texture.final else "floatBitsToUint(excludeSpecial(${f.texture.final}))") + ";"

        // Log.e("RENDERER", textureLoop)
        // Log.e("RENDERER", textureFinal)

        renderShader = renderShaderTemplate
                // .replace( IMAGE_UNIFORM, imageUniform )
                .replace(SPLIT_HANDLE, splitHandle)
                .replace(CUSTOM_SHAPE_HANDLE_SINGLE, customShapeHandleSingle)
                .replace(CUSTOM_SHAPE_HANDLE_DUAL, customShapeHandleDual)
                .replace(GENERAL_INIT, generalInit)
                .replace(SEED_INIT, seedInit)
                .replace(SHAPE_INIT, shapeInit)
                .replace(SHAPE_LOOP, shapeLoop)
                .replace(TEXTURE_INIT, textureInit)
                .replace(TEXTURE_LOOP, textureLoop)
                .replace(TEXTURE_FINAL, textureFinal)
                .replace(CONDITIONAL, conditional)

//        renderShader.lines().forEach {
//            Log.e("RENDERER", it)
//        }

        // renderShader = testCode

    }
    private fun loadShader(type: Int, shaderCode: String): Int {

        // create a vertex shader type (GL_VERTEX_SHADER)
        // or a fragment shader type (GL_FRAGMENT_SHADER)
        val shader = glCreateShader(type)
        if (shader == 0 || shader == GL_INVALID_ENUM) {
            Log.e("RENDERER", "create shader failed")
            logError()
        }

        // add the source code to the shader and compile it
        glShaderSource(shader, shaderCode)
        glCompileShader(shader)

        val a = IntBuffer.allocate(1)
        glGetShaderiv(shader, GL_COMPILE_STATUS, a)
        if (a[0] == GL_FALSE) {
            Log.e("RENDERER", "shader compile failed")
            Log.e("RENDERER", glGetShaderInfoLog(shader))
            logError()
        }

        return shader

    }
    private fun splitCoords(texture: GLTexture, xCoords: FloatArray, yCoords: FloatArray, numChunks: Int) : List<FloatArray> {

        val xLength = xCoords[1] - xCoords[0]
        val yLength = yCoords[1] - yCoords[0]
        val xPixels = xLength / 2f * texture.res.w
        val yPixels = yLength / 2f * texture.res.h

        // val numChunks = ceil((xPixels * yPixels) / maxPixelsPerChunk).toInt()
        val chunkInc = if (xLength >= yLength) xLength/numChunks else yLength/numChunks

        return if (xPixels >= yPixels) {
            List(numChunks) { i: Int ->
                floatArrayOf(
                        xCoords[0] + i * chunkInc, yCoords[1], 0.0f,    // top left
                        xCoords[0] + i * chunkInc, yCoords[0], 0.0f,    // bottom left
                        xCoords[0] + (i + 1) * chunkInc, yCoords[0], 0.0f,    // bottom right
                        xCoords[0] + (i + 1) * chunkInc, yCoords[1], 0.0f     // top right
                )
            }
        }
        else {
            List(numChunks) { i: Int ->
                floatArrayOf(
                        xCoords[0], yCoords[0] + (i + 1) * chunkInc, 0.0f,    // top left
                        xCoords[0], yCoords[0] + i * chunkInc, 0.0f,    // bottom left
                        xCoords[1], yCoords[0] + i * chunkInc, 0.0f,    // bottom right
                        xCoords[1], yCoords[0] + (i + 1) * chunkInc, 0.0f     // top right
                )
            }
        }

    }
    private fun getRenderUniformLocations() {

        Log.v(TAG, "getting render uniform locations")

        imageRenderHandle = glGetUniformLocation(renderProgram, "image")
        Log.v(TAG, "imageRenderHandle: $imageRenderHandle")

        viewCoordsHandle     = glGetAttribLocation(renderProgram, "viewCoords")
        iterHandle           = glGetUniformLocation(renderProgram, "maxIter")
        bailoutHandle        = glGetUniformLocation(renderProgram, "R")
        powerHandle          = glGetUniformLocation(renderProgram, "power")
        x0Handle             = glGetUniformLocation(renderProgram, "x0")
        y0Handle             = glGetUniformLocation(renderProgram, "y0")
        alpha0Handle         = glGetUniformLocation(renderProgram, "alpha0")
        xScaleHandle         = glGetUniformLocation(renderProgram, "xScale")
        yScaleHandle         = glGetUniformLocation(renderProgram, "yScale")
        xCoordHandle         = glGetUniformLocation(renderProgram, "xCoord")
        yCoordHandle         = glGetUniformLocation(renderProgram, "yCoord")
        sinRotateHandle      = glGetUniformLocation(renderProgram, "sinRotate")
        cosRotateHandle      = glGetUniformLocation(renderProgram, "cosRotate")
        bgScaleHandle        = glGetUniformLocation(renderProgram, "bgScale")
        juliaParamHandle     = glGetUniformLocation(renderProgram, "j")

        for (i in mapParamHandles.indices) {
            mapParamHandles[i] = glGetUniformLocation(renderProgram, "p${i + 1}")
        }
        for (i in textureParamHandles.indices) {

            textureParamHandles[i] = glGetUniformLocation(renderProgram, "q${i + 1}")
        }
//        imageTextureRenderHandle = glGetUniformLocation(renderProgram, "imageTexture")

    }
    private fun deleteAllTextures() {


    }





    // ===========================================================================================
    //  RENDER UTILITY FUNCTIONS
    // ===========================================================================================

    private fun onRenderShaderChanged() {

        Log.v("RENDERER", "render shader changed")


        updateRenderShader()

        // load new render shader
        glDetachShader(renderProgram, fRenderShader)
        fRenderShader = loadShader(GL_FRAGMENT_SHADER, renderShader)
        glAttachShader(renderProgram, fRenderShader)
        glLinkProgram(renderProgram)
        // logError()

        // check program link success
        val q = IntBuffer.allocate(1)
        glGetProgramiv(renderProgram, GL_LINK_STATUS, q)
        // Log.e("RENDERER", "program linked: ${q[0] == GL_TRUE}")

        // reassign location handles to avoid bug on Mali GPUs
        getRenderUniformLocations()

        renderShaderChanged = false

        //Log.v("RENDERER", "render shader changing done")

    }
    private fun onForegroundResolutionChanged() {

        Log.v("RENDERER", "resolution changed -- old res: ${foreground.res}, new res: ${sc.resolution}")

        val prevTextureSize = foreground.res.n.toDouble()
        foreground1.delete()
        foreground2.delete()
        foreground1 = GLTexture(sc.resolution, GL_NEAREST, TEX_FORMAT, FG1_INDEX, "foreground1")
        if (sc.resolution.w > Resolution.SCREEN.w) {
            System.gc()
            sc.sampleOnStrictTranslate = false
        }
        else {
            foreground2 = GLTexture(sc.resolution, GL_NEAREST, TEX_FORMAT, FG2_INDEX, "foreground2")
            sc.sampleOnStrictTranslate = true
        }

        foreground = foreground1
        fgAuxiliary = foreground2

        val newTextureSize = foreground.res.n.toDouble()
        Log.v(TAG, "prev chunkCount: $chunkCount")
        chunkCount = (chunkCount.toDouble()*newTextureSize/prevTextureSize).toInt().clamp(
            CHUNKS_PER_SECOND, foreground.res.h)
        Log.v(TAG, "new chunkCount: $chunkCount")

//        if (sc.hardwareProfile == HardwareProfile.CPU) {
//            fgOutAllocation.destroy()
//            fgOutAllocation = Allocation.createTyped(rs, Type.createXY(
//                    rs,
//                    Element.F32_2(rs),
//                    sc.resolution.w,
//                    sc.resolution.h
//            ))
//        }

        fgResolutionChanged = false

        // Log.v(TAG, "available heap size in MB: ${act.getAvailableHeapMemory()}")


    }
    private fun onRenderBackgroundChanged() {

        // Log.v(TAG, "bg res changed : ${sc.continuousPosRender}, ${!sc.renderBackground}")

        if (sc.continuousPosRender || !sc.renderBackground) {
            background1.delete()
            background2.delete()
        }
        else {
            background1 = GLTexture(Resolution.BG, GL_NEAREST, TEX_FORMAT, BG1_INDEX, "background1")
            background2 = GLTexture(Resolution.BG, GL_NEAREST, TEX_FORMAT, BG2_INDEX, "background2")
        }

        background = background1
        bgAuxiliary = background2

        renderBackgroundChanged = false

    }
    private fun onHardwareProfileChanged() {

        val msg : String

        when (sc.hardwareProfile) {
            HardwareProfile.GPU -> {
                msg = "${res.getString(R.string.msg_gpu)}\n${res.getString(R.string.msg_textures_enabled)}"
            }
            HardwareProfile.CPU -> {
                msg = "${res.getString(R.string.msg_cpu)}\n${res.getString(R.string.msg_textures_disabled)}"
                when (sc.cpuPrecision) {
                    CpuPrecision.PERTURB -> {
                        refAllocation = Allocation.createTyped(rs, Type.createX(
                                rs, Element.F64(rs), MAX_REF_ITER * 2
                        ))
                    }
                    else -> {
                        //sc.resolution = Resolution.FOURTH
                        //fgResolutionChanged = true
//                        fgOutAllocation = Allocation.createTyped(rs, Type.createXY(
//                                rs, Element.F32_2(rs), foreground.res.w, foreground.res.h
//                        ))
//                        bgOutAllocation = Allocation.createTyped(rs, Type.createXY(
//                                rs, Element.F32_2(rs), background.res.w, background.res.h
//                        ))
                    }
                }
            }
        }

        handler.showMessage(msg)

    }
    private fun onGpuPrecisionChanged() {

        renderShaderChanged = true
        if (sc.gpuPrecision == GpuPrecision.DUAL) {
            resetContinuousSize = true
        }

    }
    private fun onCpuPrecisionChanged() {

        val msg : String

        when (sc.cpuPrecision) {
            CpuPrecision.PERTURB -> {
                sc.renderBackground = false
                refAllocation = Allocation.createTyped(rs, Type.createX(
                        rs, Element.F64(rs), MAX_REF_ITER * 2
                ))
                msg = "${res.getString(R.string.msg_perturb)}\n${res.getString(R.string.msg_textures_disabled)}"
            }
            else -> {
                refAllocation = Allocation.createTyped(rs, Type.createX(
                        rs, Element.F64(rs), 1
                ))
                msg = "${res.getString(R.string.msg_classic)}\n${res.getString(R.string.msg_textures_enabled)}"
            }
        }

        handler.showMessage(msg)

    }
    fun onContinuousPositionRenderChanged() {
        if (sc.continuousPosRender) {
            textureSpan.center.delta = MotionValue.DELTA_CONTINUOUS
            textureSpan.size.delta = MotionValue.DELTA_CONTINUOUS
        }
        else {
            textureSpan.center.delta = MotionValue.DELTA_DISCRETE
            textureSpan.size.delta = MotionValue.DELTA_DISCRETE
        }
    }
    private fun onLoadTextureImage(firstLoad: Boolean = false) {

        loadTextureImage = false
        if (!firstLoad) image.delete()

        val bmp : Bitmap?
        val justDecodeOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        when {
            f.imagePath != "" -> {
                val fis = context.openFileInput(f.imagePath)
                BitmapFactory.decodeStream(fis, null, justDecodeOptions)
                fis?.close()
            }
            f.imageId != -1 -> BitmapFactory.decodeResource(res, f.imageId, justDecodeOptions)
        }

        // load local texture image copy
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize =
                    if (f.texture.hasRawOutput) 1
                    else ceil(justDecodeOptions.outWidth/90.0).toInt()  // only displaying as texture thumbnail
        }
        bmp = try {
            when {
                f.imagePath != "" -> {
                    val fis = context.openFileInput(f.imagePath)
                    BitmapFactory.decodeStream(fis, null, options).let {
                        fis?.close()
                        it
                    }
                }
                f.imageId != -1   -> BitmapFactory.decodeResource(res, f.imageId, options)
                else -> {
                    Log.e("RENDERER", "no image path or id")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("RENDERER", "image path invalid")
            null
        }

        val readWidth = options.outWidth
        val readHeight = options.outHeight

        // Log.v("RENDERER", "w: $readWidth, h: $readHeight")

        image = GLTexture(Resolution(readWidth, readHeight), GL_LINEAR, GL_RGBA8, IMAGE_INDEX, "image")
        image.byteBuffer?.position(0)

        bmp?.copyPixelsToBuffer(image.byteBuffer)

        image.update()


    }
    private fun onResetContinuousSize() {
        continuousSize = MIN_CONTINUOUS_SIZE
        updateContinuousRes()
        resetContinuousSize = false
    }
    private fun updateContinuousRes() {
        continuousRes.x = (continuousSize*Resolution.SCREEN.w).round(10).clamp(30, foreground.res.w)
        continuousRes.y = (continuousRes.x*aspectRatio).roundToInt()
        Log.v(TAG, "continuous res: ${continuousRes.x} x ${continuousRes.y}, ratio: ${continuousRes.x.toFloat() / foreground.res.w.toFloat()}")
    }
    private fun logError() {

        val e = glGetError()
        val s = when (e) {
            GL_NO_ERROR -> ""
            GL_INVALID_ENUM -> "invalid enum"
            GL_INVALID_VALUE -> "invalid value"
            GL_INVALID_OPERATION -> "invalid operation"
            GL_INVALID_FRAMEBUFFER_OPERATION -> "invalid framebuffer operation"
            GL_OUT_OF_MEMORY -> "out of memory"
            else -> "idfk"
        }
        Log.e("RENDERER", s)

        var log = glGetProgramInfoLog(renderProgram)
        Log.e("RENDERER", log)

        log = glGetShaderInfoLog(fRenderShader)
        Log.e("RENDERER", log)



    }




    // ===========================================================================================
    //  NATIVE FUNCTIONS
    // ===========================================================================================

    private external fun iterateReferenceNative(
            z0xIn: String, z0yIn: String,
            d0xIn: DoubleArray, d0yIn: DoubleArray,
            precision: Int, maxIter: Int, refSize: Int, escapeRadius: Double,
            sp: Double, sn: Double,
            returnData: NativeReferenceReturnData) : DoubleArray

    private external fun compressNative(
            imIn: ByteArray,
            width: Int,
            height: Int
    ) : Int

    private external fun iterateSine2Native(data: ScriptField_IterateData.Item) : FloatArray
//    private external fun iterateCloverNative(data: ScriptField_IterateData.Item) : FloatArray
//    private external fun iterateSineNative(data: ScriptField_IterateData.Item) : FloatArray
//    private external fun iterateHorseshoeCrabNative(data: ScriptField_IterateData.Item) : FloatArray
//    private external fun iterateKleinianNative(data: ScriptField_IterateData.Item) : FloatArray
//    private external fun iterateNova2Native(data: ScriptField_IterateData.Item) : FloatArray
//    private external fun iterateCollatzNative(data: ScriptField_IterateData.Item) : FloatArray
//    private external fun iterateMandelbrotPowNative(data: ScriptField_IterateData.Item) : FloatArray






    private fun rsPerturbationPixels(
            res: Point, glitchedPixelsSize: Int,
            d0xOffset: Double, d0yOffset: Double,
            sp: Double, sn: Double, refData: NativeReferenceReturnData, bgScale: Double)
    {

        val sinRotation = sin(f.shape.position.rotation)
        val cosRotation = cos(f.shape.position.rotation)

        perturbationPixelsScript._ref = refAllocation
        perturbationPixelsScript._pixels = pixelsInAllocation

        perturbationPixelsScript._width = res.x.toDouble()
        perturbationPixelsScript._height = res.y.toDouble()
        perturbationPixelsScript._pixelsSize = glitchedPixelsSize.toDouble()
        perturbationPixelsScript._aspectRatio = aspectRatio
        perturbationPixelsScript._bgScale = bgScale

        perturbationPixelsScript._d0xOffset = d0xOffset
        perturbationPixelsScript._d0yOffset = d0yOffset

        perturbationPixelsScript._maxRefIter = MAX_REF_ITER.toLong()
        perturbationPixelsScript._refIter = refData.refIter.toLong()
        perturbationPixelsScript._skipIter = if (refData.skipIter == -1) 0L else refData.skipIter.toLong()
        perturbationPixelsScript._maxIter = f.shape.params.detail.u.toLong()
        perturbationPixelsScript._escapeRadius = f.texture.params.radius.scaledValue.toFloat()

        perturbationPixelsScript._scale = f.shape.position.zoom / 2.0
        perturbationPixelsScript._sinRotation = sinRotation
        perturbationPixelsScript._cosRotation = cosRotation

        perturbationPixelsScript._alphax = refData.ax
        perturbationPixelsScript._alphay = refData.ay
        perturbationPixelsScript._betax  = refData.bx
        perturbationPixelsScript._betay  = refData.by
        perturbationPixelsScript._gammax = refData.cx
        perturbationPixelsScript._gammay = refData.cy

        perturbationPixelsScript._sp = sp
        perturbationPixelsScript._sn = sn

        perturbationPixelsScript.forEach_iterate(pixelsOutAllocation)

    }




    // ===========================================================================================
    //  PERTURBATION UTILITY FUNCTIONS
    // ===========================================================================================

    private fun findGlitchMostPixels(
            texture: GLTexture, s: Int, res: Point) : ArrayList<Point>
    {

        val glitch = arrayListOf<Point>()
        var largestGlitch = glitch

        val floodFillTimeStart = currentTimeMs()
        var numConnectedComponents = 0
        var componentSize : Int
        var maxComponentSize = 0
        val queue = LinkedList<Point>()

        for (j in 0 until res.y step s) {
            for (i in 0 until res.x step s) {

                //if (imArray.get(i, j, 1, res.x, 2) == 3f) {  // glitched pixel found
                if (texture.get(i, j, 1) == 3f) {  // glitched pixel found

                    glitch.clear()
                    componentSize = 0

                    queue.push(Point(i, j))
                    while (queue.isNotEmpty()) {

                        val p = queue.pop()
                        //if (imArray.get(p.x, p.y, 1, res.x, 2) == 3f) {
                        if (texture.get(p.x, p.y, 1) == 3f) {
                            //imArray.set(p.x, p.y, 1, res.x, 2, 4f)  // mark pixel as found
                            texture.set(p.x, p.y, 1, 4f)  // mark pixel as found
                            glitch.add(p)
                            componentSize++

                            val colLtMax = p.x + s < res.x
                            val colGtZero = p.x - s > 0
                            val rowLtMax = p.y + s < res.y
                            val rowGtZero = p.y - s > 0

                            // queue direct neighbors
                            if (rowLtMax)  queue.push(Point(p.x, p.y + s))
                            if (rowGtZero) queue.push(Point(p.x, p.y - s))
                            if (colLtMax)  queue.push(Point(p.x + s, p.y))
                            if (colGtZero) queue.push(Point(p.x - s, p.y))
//                                if (rowLtMax && colLtMax)   queue.push(Pair(p.first + 1, p.second + 1))  // top right
//                                if (rowGtZero && colLtMax)  queue.push(Pair(p.first - 1, p.second + 1))  // bottom right
//                                if (colGtZero && rowLtMax)  queue.push(Pair(p.first + 1, p.second - 1))  // top left
//                                if (colGtZero && rowGtZero) queue.push(Pair(p.first - 1, p.second - 1))  // bottom left

                        }

                    }

                    if (componentSize > maxComponentSize) {
                        largestGlitch = ArrayList(glitch)
                        maxComponentSize = componentSize
                    }
                    numConnectedComponents++

                }

            }
        }

        //Log.e("RENDERER", "flood-fill took ${(now() - floodFillTimeStart)/1000f} sec")
        //Log.e("FSV", "numConnectedComponents: $numConnectedComponents")
        //Log.e("FSV", "maxComponentSize: $maxComponentSize")

        //Log.e("RENDERER", "glitch size: ${glitch.size}")
        //Log.e("RENDERER", "largest glitch size: ${largestGlitch.size}")
        return largestGlitch

    }

    private fun findGlitchMostPixelsContractingNet(
            imArray: FloatArray, s: Int, res: Point, center: Point) : ArrayList<Point>
    {

        val glitch = arrayListOf<Point>()
        var largestGlitch = glitch

        val floodFillTimeStart = currentTimeMs()
        var numConnectedComponents = 0
        var componentSize : Int
        var maxComponentSize = 0
        val queue = LinkedList<Point>()

        // find largest glitch
        for (j in 0 until res.y step s) {
            for (i in 0 until res.x step s) {

                if (imArray.get(i, j, 1, res.x, 2) == 3f) {  // glitched pixel found

                    glitch.clear()
                    componentSize = 0

                    queue.push(Point(i, j))
                    while (queue.isNotEmpty()) {

                        val p = queue.pop()
                        if (imArray.get(p.x, p.y, 1, res.x, 2) == 3f) {
                            imArray.set(p.x, p.y, 1, res.x, 2, 4f)  // mark pixel as found
                            glitch.add(p)
                            componentSize++

                            val colLtMax = p.x + s < res.x
                            val colGtZero = p.x - s > 0
                            val rowLtMax = p.y + s < res.y
                            val rowGtZero = p.y - s > 0

                            // queue direct neighbors
                            if (rowLtMax)  queue.push(Point(p.x, p.y + s))
                            if (rowGtZero) queue.push(Point(p.x, p.y - s))
                            if (colLtMax)  queue.push(Point(p.x + s, p.y))
                            if (colGtZero) queue.push(Point(p.x - s, p.y))
//                                if (rowLtMax && colLtMax)   queue.push(Pair(p.first + 1, p.second + 1))  // top right
//                                if (rowGtZero && colLtMax)  queue.push(Pair(p.first - 1, p.second + 1))  // bottom right
//                                if (colGtZero && rowLtMax)  queue.push(Pair(p.first + 1, p.second - 1))  // top left
//                                if (colGtZero && rowGtZero) queue.push(Pair(p.first - 1, p.second - 1))  // bottom left

                        }

                    }

                    if (componentSize > maxComponentSize) {
                        largestGlitch = ArrayList(glitch)
                        maxComponentSize = componentSize
                    }
                    numConnectedComponents++

                }

            }
        }
        Log.e("RENDERER", "flood-fill took ${(currentTimeMs() - floodFillTimeStart) / 1000f} sec")
        //Log.e("FSV", "numConnectedComponents: $numConnectedComponents")
        //Log.e("FSV", "maxComponentSize: $maxComponentSize")
        //Log.e("RENDERER", "glitch size: ${glitch.size}")
        //Log.e("RENDERER", "largest glitch size: ${largestGlitch.size}")


        if (largestGlitch.size == 1) {
            center.x = largestGlitch[0].x
            center.y = largestGlitch[0].y
        }
        else if (largestGlitch.size > 1) {

            var xmin = Int.MAX_VALUE
            var ymin = Int.MAX_VALUE
            var xmax = -1
            var ymax = -1
            for (p in largestGlitch) {

                if (p.x < xmin) xmin = p.x
                if (p.x > xmax) xmax = p.x
                if (p.y < ymin) ymin = p.y
                if (p.y > ymax) ymax = p.y
                imArray.set(p.x, p.y, 0, res.x, 2, 1f)
                imArray.set(p.x, p.y, 1, res.x, 2, 5f)

            }

            Log.e("RENDERER", "init bounding box: ($xmin $xmax) ($ymin $ymax)")

            var bbLeft = xmin
            var bbRight = xmax
            var bbBottom = ymin
            var bbTop = ymax
            var leftBorderFilled = false
            var rightBorderFilled = false
            var topBorderFilled = false
            var bottomBorderFilled = false
            while (!(leftBorderFilled && rightBorderFilled && bottomBorderFilled && topBorderFilled)) {

                if (!leftBorderFilled) {
                    leftBorderFilled = true
                    for (j in bbBottom..bbTop step s) {
                        if (imArray.get(bbLeft, j, 1, res.x, 2) != 5f) {
                            leftBorderFilled = false
                            bbLeft += s
                            break
                        }
                    }
                }
                if (!rightBorderFilled) {
                    rightBorderFilled = true
                    for (j in bbBottom..bbTop step s) {
                        if (imArray.get(bbRight, j, 1, res.x, 2) != 5f) {
                            rightBorderFilled = false
                            if (bbRight - bbLeft > s) bbRight -= s
                            break
                        }
                    }
                }
                if (!bottomBorderFilled) {
                    bottomBorderFilled = true
                    for (i in bbLeft..bbRight step s) {
                        if (imArray.get(i, bbBottom, 1, res.x, 2) != 5f) {
                            bottomBorderFilled = false
                            bbBottom += s
                            break
                        }
                    }
                }
                if (!topBorderFilled) {
                    topBorderFilled = true
                    for (i in bbLeft..bbRight step s) {
                        if (imArray.get(i, bbTop, 1, res.x, 2) != 5f) {
                            topBorderFilled = false
                            bbTop -= s
                            break
                        }
                    }
                }

            }

            Log.e("RENDERER", "bounding box: ($bbLeft, $bbRight) ($bbBottom, $bbTop)")

            center.x = (bbLeft + bbRight) / 2
            center.y = (bbBottom + bbTop) / 2

        }

        return largestGlitch

    }

    private fun findGlitchLargestBoundingBox(
            imArray: FloatArray, s: Int, res: Point) : ArrayList<Point>
    {

        val glitch = arrayListOf<Point>()
        var largestGlitch = glitch

        val floodFillTimeStart = currentTimeMs()
        var numConnectedComponents = 0
        var componentSize : Int
        val queue = LinkedList<Point>()

        var maxArea = 0
        var xmin : Int
        var ymin : Int
        var xmax : Int
        var ymax : Int

        for (j in 0 until res.y step s) {
            for (i in 0 until res.x step s) {

                if (imArray.get(i, j, 1, res.x, 2) == 3f) {  // glitched pixel found

                    glitch.clear()
                    componentSize = 0
                    xmin = Int.MAX_VALUE
                    ymin = Int.MAX_VALUE
                    xmax = -1
                    ymax = -1

                    queue.push(Point(i, j))
                    while (queue.isNotEmpty()) {

                        val p = queue.pop()
                        if (imArray.get(p.x, p.y, 1, res.x, 2) == 3f) {

                            imArray.set(p.x, p.y, 1, res.x, 2, 4f)  // mark pixel as found
                            if (p.x < xmin) xmin = p.x
                            if (p.x > xmax) xmax = p.x
                            if (p.y < ymin) ymin = p.y
                            if (p.y > ymax) ymax = p.y

                            glitch.add(p)
                            componentSize++

                            val colLtMax = p.x + s < res.x
                            val colGtZero = p.x - s > 0
                            val rowLtMax = p.y + s < res.y
                            val rowGtZero = p.y - s > 0

                            // queue direct neighbors
                            if (rowLtMax)  queue.push(Point(p.x, p.y + s))
                            if (rowGtZero) queue.push(Point(p.x, p.y - s))
                            if (colLtMax)  queue.push(Point(p.x + s, p.y))
                            if (colGtZero) queue.push(Point(p.x - s, p.y))

                        }

                    }

                    val area = (xmax - xmin)*(ymax - ymin)
                    if (area > maxArea) {
                        Log.e("RENDERER", "maxArea = $maxArea")
                        largestGlitch = ArrayList(glitch)
                        maxArea = area
                    }
                    numConnectedComponents++

                }

            }
        }

        Log.e("RENDERER", "flood-fill took ${(currentTimeMs() - floodFillTimeStart) / 1000f} sec")
        //Log.e("FSV", "numConnectedComponents: $numConnectedComponents")
        //Log.e("FSV", "maxComponentSize: $maxComponentSize")

        //Log.e("RENDERER", "glitch size: ${glitch.size}")
        //Log.e("RENDERER", "largest glitch size: ${largestGlitch.size}")
        return largestGlitch

    }

    private fun geometricMedian(glitch: ArrayList<Point>) : Point {

        var minSumDistances = Float.MAX_VALUE
        var center = glitch[0]
        var sumDistances: Float
        for (pixel1 in glitch) {
            sumDistances = 0f
            for (pixel2 in glitch) {
                val dif1 = pixel1.x - pixel2.x
                val dif2 = pixel1.y - pixel2.y
                sumDistances += sqrt((dif1 * dif1 + dif2 * dif2).toFloat())
            }
            if (sumDistances < minSumDistances) {
                minSumDistances = sumDistances
                center = pixel1
            }
        }
        return center

    }
    private fun harmonicMean(glitch: ArrayList<Point>) : Point {

        var center = glitch[0]
        var reciprocalSumWidth = 0f
        var reciprocalSumHeight = 0f

        for (pixel in glitch) {
            reciprocalSumWidth += 1f/(pixel.x + 1)
            reciprocalSumHeight += 1f/(pixel.y + 1)
        }
        val harmonicMean = Point(
                (1f / (reciprocalSumWidth / glitch.size)).toInt(),
                (1f / (reciprocalSumHeight / glitch.size)).toInt()
        )

        //Log.e("RENDERER", "harmonic mean: (${harmonicMean.x}, ${harmonicMean.y})")

        var minDist = Float.MAX_VALUE
        for (pixel in glitch) {
            val dif1 = pixel.x - harmonicMean.x
            val dif2 = pixel.y - harmonicMean.y
            val dist = sqrt((dif1 * dif1 + dif2 * dif2).toFloat())
            if (dist < minDist) {
                minDist = dist
                center = pixel
            }
        }

        return center

    }
    private fun quadraticMean(glitch: ArrayList<Point>) : Point {

        var sqrSumWidth = 0f
        var sqrSumHeight = 0f
        for (pixel in glitch) {
            sqrSumWidth += pixel.x*pixel.x
            sqrSumHeight += pixel.y*pixel.y
        }
        val quadraticMean = Point(
                sqrt(sqrSumWidth / glitch.size).toInt(),
                sqrt(sqrSumHeight / glitch.size).toInt()
        )
        var minDist = Float.MAX_VALUE
        var center = glitch[0]
        for (pixel in glitch) {
            val dif1 = pixel.x - quadraticMean.x
            val dif2 = pixel.y - quadraticMean.y
            val dist = sqrt((dif1 * dif1 + dif2 * dif2).toFloat())
            if (dist < minDist) {
                minDist = dist
                center = pixel
            }
        }

        return center

    }
    private fun arithmeticMean(glitch: ArrayList<Point>) : Point {

        var sumWidth = 0f
        var sumHeight = 0f
        for (pixel in glitch) {
            sumWidth += pixel.x*pixel.x
            sumHeight += pixel.y*pixel.y
        }
        val arithmeticMean = Point(
                (sumWidth / glitch.size).toInt(),
                (sumHeight / glitch.size).toInt()
        )
        var minDist = Float.MAX_VALUE
        var center = glitch[0]
        for (pixel in glitch) {
            val dif1 = pixel.x - arithmeticMean.x
            val dif2 = pixel.y - arithmeticMean.y
            val dist = sqrt((dif1 * dif1 + dif2 * dif2).toFloat())
            if (dist < minDist) {
                minDist = dist
                center = pixel
            }
        }

        return center

    }



}