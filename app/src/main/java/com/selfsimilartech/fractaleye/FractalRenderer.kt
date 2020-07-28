package com.selfsimilartech.fractaleye

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Log
import android.view.MotionEvent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import org.apfloat.Apcomplex
import org.apfloat.Apfloat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

//const val GPU_SINGLE_FG_CHUNKS = 10
//const val GPU_SINGLE_BG_CHUNKS = 3
//const val GPU_DUAL_FG_CHUNKS = 30
//const val GPU_DUAL_BG_CHUNKS = 10
const val CPU_FG_CHUNKS = 50
const val CPU_BG_CHUNKS = 20
const val CPU_CHUNKS = 5
const val MAX_REFERENCES = 35
const val TEX_SPAN_HIST_CONTINUOUS = 10
const val TEX_SPAN_HIST_DISCRETE = 4
const val TEX_FORMAT = GL_RG32UI
const val MINMAX_XSAMPLES = 4
val MAX_REF_ITER = 2.0.pow(16).toInt()

const val BG1_INDEX = 0
const val BG2_INDEX = 1
const val FG1_INDEX = 2
const val FG2_INDEX = 3
const val THUMB_INDEX = 4

const val GENERAL_INIT = "// generalInit"
const val SEED_INIT = "// seedInit"
const val SHAPE_INIT = "// shapeInit"
const val SHAPE_LOOP = "// shapeLoop"
const val TEXTURE_INIT = "// textureInit"
const val TEXTURE_LOOP = "// textureLoop"
const val TEXTURE_FINAL = "// textureFinal"
const val CONDITIONAL = "/* conditional */"



enum class RenderProfile { MANUAL, SAVE, COLOR_THUMB, TEXTURE_THUMB }
enum class HardwareProfile { GPU, CPU }
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

fun FloatArray.get(i: Int, j: Int, k: Int, width: Int, depth: Int) = get(depth*(j*width + i) + k)
fun FloatArray.set(i: Int, j: Int, k: Int, width: Int, depth: Int, value: Float) { set(depth*(j*width + i) + k, value) }
//fun ShortArray.get(i: Int, j: Int, k: Int, width: Int, depth: Int) = get(depth*(j*width + i) + k)
//fun ShortArray.set(i: Int, j: Int, k: Int, width: Int, depth: Int, value: Short) { set(depth*(j*width + i) + k, value) }


class FractalRenderer(

        val f : Fractal,
        private val sc : SettingsConfig,
        private val act : MainActivity,
        private val context: Context,
        private val handler : MainActivity.ActivityHandler,
        val screenRes : Point

) : GLSurfaceView.Renderer {



    companion object {

        init {
            System.loadLibrary("gmp")
            System.loadLibrary("native-fractalimage")
        }

    }


    private val res = context.resources
    private var overlayHidden = false

    private val longPressed = kotlinx.coroutines.Runnable {

        Log.d("SURFACE VIEW", "wow u pressed that so long")

        // vibrate
        val vib = act.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            vib.vibrate(15)
        }

        val overlay = act.findViewById<ConstraintLayout>(R.id.overlay)

        val anim: AlphaAnimation = if (!overlayHidden) AlphaAnimation(1f, 0f) else AlphaAnimation(0f, 1f)
        anim.duration = 500L
        anim.fillAfter = true
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                overlay.visibility = if (overlayHidden) ConstraintLayout.VISIBLE else ConstraintLayout.GONE
            }

            override fun onAnimationStart(animation: Animation?) {}

        })
        overlay.animation = anim
        overlay.animation.start()
        overlayHidden = !overlayHidden


    }
    private val aspectRatio = screenRes.y.toDouble() / screenRes.x

    var reaction = Reaction.POSITION
    var renderProfile = RenderProfile.MANUAL

    private val prevFocus = floatArrayOf(0f, 0f)
    private var prevZoom = 0.0
    private var prevAngle = 0f
    private var prevFocalLen = 1.0f






    var renderToTex = false
    var renderThumbnails = false
    var colorThumbsRendered = false
    var textureThumbsRendered = false
    var isRendering = false

    var renderShaderChanged = false
    var fgResolutionChanged = false
    var renderBackgroundChanged = false
    var resetTextureMinMax = false
    var interruptRender = false
    var pauseRender = false


    private var hasTranslated = false
    private var hasScaled = false
    private var hasRotated = false

    private val quadCoords = floatArrayOf(0f, 0f)
    private val quadFocus = floatArrayOf(0f, 0f)
    private var quadScale = 1f
    private var quadRotation = 0f

    private val bgSize = 5f
    private var floatPrecisionBits : Int? = null
    private var gpuRendererName = ""





    private var header       = ""
    private var arithmetic   = ""
    private val arithmeticAtan = if (gpuRendererName.contains("PowerVR"))
        res.getString(R.string.arithmetic_atan_powervr) else
        res.getString(R.string.arithmetic_atan)
    private var init         = ""
    private var seedInit     = ""
    private var loop         = ""
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
    private var testCode = ""
    private var colorShader = ""


    // coordinates of default view boundaries
    private val viewCoords = floatArrayOf(
            -1f,   1f,  0f,     // top left
            -1f,  -1f,  0f,     // bottom left
            1f,  -1f,  0f,     // bottom right
            1f,   1f,  0f )    // top right
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)


    // create render program handles
    private var renderProgram     : Int = 0
    private var viewCoordsHandle  : Int = 0
    private var iterHandle        : Int = 0
    private var bailoutHandle     : Int = 0
    private var powerHandle       : Int = 0
    private var x0Handle          : Int = 0
    private var y0Handle          : Int = 0
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


    // create sample program handles
    private var sampleProgram          : Int = 0
    private var viewCoordsSampleHandle : Int = 0
    private var quadCoordsSampleHandle : Int = 0
    private var textureSampleHandle    : Int = 0
    private var yOrientSampleHandle    : Int = 0


    // create color program handles
    private var colorProgram          : Int = 0
    private var viewCoordsColorHandle : Int = 0
    private var quadCoordsColorHandle : Int = 0
    private var yOrientColorHandle    : Int = 0
    private var numColorsHandle       : Int = 0
    private var textureColorHandle    : Int = 0
    private var paletteHandle         : Int = 0
    private var solidFillColorHandle  : Int = 0
    private var textureModeHandle     : Int = 0
    private var frequencyHandle       : Int = 0
    private var phaseHandle           : Int = 0
    private var densityHandle         : Int = 0
    private var minHandle             : Int = 0
    private var maxHandle             : Int = 0



    private var texSpanHistSize = if (sc.continuousRender) TEX_SPAN_HIST_CONTINUOUS else TEX_SPAN_HIST_DISCRETE
    var calcNewTextureSpan = sc.autofitColorRange
    var textureMins = FloatArray(texSpanHistSize) { 0f }
    var textureMaxs = FloatArray(texSpanHistSize) { 1f }
    private var numForegroundChunks = sc.chunkProfile.fgSingle
    private var numBackgroundChunks = sc.chunkProfile.bgSingle



    private var vRenderShader : Int = 0
    private var vSampleShader : Int = 0
    private var fRenderShader : Int = 0
    private var fColorShader  : Int = 0
    private var fSampleShader : Int = 0

    // define texture resolutions
    private val bgTexRes = Resolution.EIGHTH.scaleRes(screenRes)
    private val fgTexRes = sc.resolution.scaleRes(screenRes)
    private val thumbTexRes = Resolution.THUMB.scaleRes(screenRes)

    private val refData = NativeReferenceReturnData()




    private lateinit var background1  : GLTexture
    private lateinit var background2  : GLTexture
    private lateinit var foreground1  : GLTexture
    private lateinit var foreground2  : GLTexture
    private lateinit var thumbnail    : GLTexture

    private val thumbBuffer = ByteBuffer.allocateDirect(thumbTexRes.x*thumbTexRes.y*4).order(ByteOrder.nativeOrder())
    // private val refArray = DoubleArray(refSize*2) { 0.0 }
    private val refArray = DoubleArray(1) { 0.0 }


    private lateinit var background: GLTexture
    private lateinit var bgAuxiliary : GLTexture
    private lateinit var foreground : GLTexture
    private lateinit var fgAuxiliary : GLTexture


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
    private val fboIDs = IntBuffer.allocate(3)
    private val rboIDs = IntBuffer.allocate(1)






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

    private val rs = RenderScript.create(context)
    private val perturbationPixelsScript = ScriptC_perturbationPixels(rs)
    // private val mandelbrotScript = ScriptC_mandelbrot(rs)
    // private val perturbationImageScript = ScriptC_perturbationImage(rs)
    // private val templateScript = ScriptC_template(rs)


    // memory allocation for reference iteration values
    private var refAllocation = Allocation.createTyped(rs, Type.createX(
            rs, Element.F64(rs), 1
    ))


    private var fgOutAllocation = Allocation.createTyped(rs, Type.createXY(
            rs, Element.F32_2(rs), fgTexRes.x, fgTexRes.y
    ))
    private var bgOutAllocation = Allocation.createTyped(rs, Type.createXY(
            rs, Element.F32_2(rs), bgTexRes.x, bgTexRes.y
    ))


    //memory allocation for indices of pixels that need to be iterated
    private var pixelsInAllocation  = Allocation.createTyped(rs, Type.createX(rs, Element.U8(rs), 1))
    private var pixelsOutAllocation = Allocation.createTyped(rs, Type.createX(rs, Element.U8(rs), 1))

    private val rsMsgHandler = object : RenderScript.RSMessageHandler() {
        override fun run() {
            when (mID) {
                0 -> if (!sc.continuousRender) act.findViewById<ProgressBar>(R.id.progressBar).progress += 2
                1 -> Log.d("RENDERER", "renderscript interrupted!")
            }
        }
    }


    //var hackedRes = (MyResources(res.assets, res.displayMetrics, res.configuration).apply { setMyContext(context) } as Resources)

    //    var rsScript = HackedScriptC_template(rs, hackedRes, rs.applicationContext.resources.getIdentifier(
//            "template", "raw", rs.applicationContext.packageName))
    //var rsScript = ScriptC_mandelbrot(rs)
    //var rsScriptQuad = ScriptC_templateQuad(rs)





    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {

        // get GPU info
        gpuRendererName = gl.glGetString(GL10.GL_RENDERER)
        Log.d("RENDERER", "GL_RENDERER = " + gl.glGetString(GL10.GL_RENDERER))
        Log.d("RENDERER", "GL_VENDOR = " + gl.glGetString(GL10.GL_VENDOR))
        Log.d("RENDERER", "GL_VERSION = " + gl.glGetString(GL10.GL_VERSION))
        //Log.d("RENDERER", "EXTENSIONS = " + gl.glGetString(GL10.GL_EXTENSIONS).replace(" ", "\n"))

        // get fragment shader precision
        val a = IntBuffer.allocate(2)
        val b = IntBuffer.allocate(1)
        glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_HIGH_FLOAT, a, b)
        Log.d("FSV", "floating point exponent range: ${a[0]}, ${a[1]}")
        Log.d("FSV", "floating point precision: ${b[0]}")
        //handler.showImageSavedMessage("${a[0]}, ${a[1]}, ${b[0]}")
        floatPrecisionBits = b[0]

        // get texture specs
        // val c = IntBuffer.allocate(1)
        // val d = IntBuffer.allocate(1)
        // glGetIntegerv(GL_MAX_TEXTURE_SIZE, c)
        // glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, d)
        // Log.d("FSV", "max texture size: ${c[0]}")
        // Log.d("FSV", "max texture image units: ${d[0]}")

        initialize()
        renderToTex = true


        //val buttonScroll = act.findViewById<HorizontalScrollView>(R.id.buttonScroll)
        //buttonScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT)

    }
    override fun onDrawFrame(unused: GL10) {

        render()
        isRendering = false

    }
    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {

    }


    private fun initialize() {

        renderProgram = glCreateProgram()
        colorProgram = glCreateProgram()
        sampleProgram = glCreateProgram()

        background1  = GLTexture(bgTexRes,    GL_NEAREST, TEX_FORMAT, BG1_INDEX)
        background2  = GLTexture(bgTexRes,    GL_NEAREST, TEX_FORMAT, BG2_INDEX)
        foreground1  = GLTexture(fgTexRes,    GL_NEAREST, TEX_FORMAT, FG1_INDEX)
        foreground2  = GLTexture(fgTexRes,    GL_NEAREST, TEX_FORMAT, FG2_INDEX)
        thumbnail    = GLTexture(thumbTexRes, GL_NEAREST, TEX_FORMAT, THUMB_INDEX)

        background = background1
        bgAuxiliary = background2
        foreground = foreground1
        fgAuxiliary = foreground2


        rs.messageHandler = rsMsgHandler

        val thumbRes = Resolution.THUMB.scaleRes(screenRes)
        Log.d("RENDERER", "thumbnail resolution: ${thumbRes.x} x ${thumbRes.y}")

        // load all vertex and fragment shader code
        val vRenderCode = readShader(R.raw.vert_render)
        renderShaderInitSF = readShader(R.raw.frag_render_init_sf)
        renderShaderInitDF = readShader(R.raw.frag_render_init_df)
        renderShaderTemplate = readShader(R.raw.frag_render)
        colorShader = readShader(R.raw.frag_color)
        val vSampleCode = readShader(R.raw.vert_sample)
        val fSampleCode = readShader(R.raw.sample)
        // testCode = readShader(R.raw.precision_test)
        // testCode = readShader(R.raw.test)

        checkThresholdCross(f.position.zoom)
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
        glGenFramebuffers(2, fboIDs)
        glGenRenderbuffers(1, rboIDs)
        glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[1])
        glBindRenderbuffer(GL_RENDERBUFFER, rboIDs[0])
        glRenderbufferStorage(
                GL_RENDERBUFFER,
                GL_RGB8,
                Resolution.HIGHEST.scaleRes(screenRes).x,
                Resolution.HIGHEST.scaleRes(screenRes).y
        )
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, rboIDs[0])




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

        viewCoordsSampleHandle = glGetAttribLocation(sampleProgram, "viewCoords")
        quadCoordsSampleHandle = glGetAttribLocation(sampleProgram, "quadCoords")
        textureSampleHandle    = glGetUniformLocation(sampleProgram, "tex")
        yOrientSampleHandle    = glGetUniformLocation(sampleProgram, "yOrient")

        glAttachShader(colorProgram, vSampleShader)
        glAttachShader(colorProgram, fColorShader)
        glLinkProgram(colorProgram)

        viewCoordsColorHandle = glGetAttribLocation(colorProgram, "viewCoords")
        quadCoordsColorHandle = glGetAttribLocation(colorProgram, "quadCoords")
        textureColorHandle    = glGetUniformLocation(colorProgram, "tex")
        yOrientColorHandle    = glGetUniformLocation(colorProgram, "yOrient")
        numColorsHandle       = glGetUniformLocation(colorProgram, "numColors")
        paletteHandle         = glGetUniformLocation(colorProgram, "palette")
        solidFillColorHandle  = glGetUniformLocation(colorProgram, "solidFillColor")
        textureModeHandle     = glGetUniformLocation(colorProgram, "textureMode")
        frequencyHandle       = glGetUniformLocation(colorProgram, "frequency")
        phaseHandle           = glGetUniformLocation(colorProgram, "phase")
        densityHandle         = glGetUniformLocation(colorProgram, "density")
        minHandle             = glGetUniformLocation(colorProgram, "textureMin")
        maxHandle             = glGetUniformLocation(colorProgram, "textureMax")



        setNativeMethods()


    }
    private fun readShader(id: Int) : String {

        var str = ""
        val br = act.resources.openRawResource(id).bufferedReader()
        var line: String?

        while (br.readLine().also { line = it } != null) {
            str += line
            str += "\n"
        }
        br.close()

        //Log.d("RENDERER", str)
        //Log.e("RENDERER", "color shader length: ${str.length}")

        return str

    }
    private fun setNativeMethods() {

        // link native methods with corresponding shapes
        Shape.all.filter { it.isTranscendental }.forEach {
            it.iterateNative = { data ->

                when (it) {
                    Shape.sine2          -> iterateSine2Native(data[0])
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


    fun incrementProgressBar() {
        if (!sc.continuousRender) act.findViewById<ProgressBar>(R.id.progressBar).progress += 2
    }
    fun checkThresholdCross(prevScale: Double) {

        if (sc.autoPrecision) {

            // update hardware profile
            val prevHardwareProfile = sc.hardwareProfile
            sc.hardwareProfile = when {
                f.position.zoom < GpuPrecision.SINGLE.threshold && f.shape.isTranscendental -> HardwareProfile.CPU
                f.position.zoom < GpuPrecision.DUAL.threshold && f.shape.hasPerturbation && BuildConfig.PAID_VERSION && !f.shape.juliaMode -> {
                    f.position.updatePrecision(sc.perturbPrecision)
                    HardwareProfile.CPU
                }
                else -> HardwareProfile.GPU
            }
            if (sc.hardwareProfile != prevHardwareProfile) onHardwareProfileChanged()
            //Log.d("RENDERER", "hardware profile: ${sc.hardwareProfile}")


            // update gpu precision
            val prevGpuPrecision = sc.gpuPrecision
            sc.gpuPrecision = when {
                f.position.zoom > GpuPrecision.SINGLE.threshold -> GpuPrecision.SINGLE

                f.shape.hasDualFloat
                        && f.position.zoom < GpuPrecision.SINGLE.threshold -> GpuPrecision.DUAL

                else -> sc.gpuPrecision
            }
            if (sc.hardwareProfile == HardwareProfile.GPU && sc.gpuPrecision != prevGpuPrecision) {
                onGpuPrecisionChanged()
            }


            if (BuildConfig.PAID_VERSION) {

                // update cpu precision
                val prevCpuPrecision = sc.cpuPrecision
                sc.cpuPrecision = when {
                    f.position.zoom > CpuPrecision.DOUBLE.threshold -> CpuPrecision.DOUBLE
                    f.position.zoom < CpuPrecision.DOUBLE.threshold && f.shape.hasPerturbation -> CpuPrecision.PERTURB
                    else -> CpuPrecision.DOUBLE
                }
                if (sc.hardwareProfile == HardwareProfile.CPU && sc.cpuPrecision != prevCpuPrecision) {
                    onCpuPrecisionChanged()
                }

                val prevScaleStrings = "%e".format(Locale.US, prevScale).split("e")
                val prevScaleExponent = -prevScaleStrings[1].toDouble()
                val prevScaleOrdinal = floor(prevScaleExponent/12.0).toLong()

                val scaleStrings = "%e".format(Locale.US, f.position.zoom).split("e")
                val scaleExponent = -scaleStrings[1].toDouble()
                val scaleOrdinal = floor(scaleExponent/12.0).toLong()

                sc.perturbPrecision = (scaleOrdinal + 2)*12
                if (scaleOrdinal != prevScaleOrdinal) f.position.updatePrecision(sc.perturbPrecision)

            }

            var zoomLimitReached = false
            when (sc.hardwareProfile) {
                HardwareProfile.GPU -> {
                    if (prevScale > GpuPrecision.DUAL.threshold && f.position.zoom < GpuPrecision.DUAL.threshold) zoomLimitReached = true
                }
                HardwareProfile.CPU -> {
                    if (BuildConfig.PAID_VERSION && f.shape.hasPerturbation && !f.shape.juliaMode) {
                        if (prevScale > CpuPrecision.PERTURB.threshold && f.position.zoom < CpuPrecision.PERTURB.threshold) zoomLimitReached = true
                    }
                    else {
                        if (prevScale > CpuPrecision.DOUBLE.threshold && f.position.zoom < CpuPrecision.DOUBLE.threshold) zoomLimitReached = true
                    }
                }
            }
            if (zoomLimitReached) act.showMessage(res.getString(R.string.msg_zoom_limit))


        }

    }




    // ===========================================================================================
    //  OPENGL ES UTILITY FUNCTIONS
    // ===========================================================================================

    private fun updateRenderShader() {

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
        textureInit   = res.getString(f.texture.init)
        shapeLoop     = "z = ${if (f.shape.isCustom) {
            if (sc.gpuPrecision == GpuPrecision.SINGLE) f.shape.customLoopSF else f.shape.customLoopDF
        } else f.shape.loop};"
        textureLoop   = f.texture.loop
        if (f.texture.usesSecondDelta) textureLoop += "beta = mandelbrot_delta2(beta, alpha, z1);"
        if (f.texture.usesFirstDelta) textureLoop += "alpha = mandelbrot_delta1(alpha, z1);"
        shapeFinal    = res.getString(f.shape.final)
        textureFinal  = "colorParams.x = ${f.texture.final};"

        // Log.e("RENDERER", textureLoop)
        // Log.e("RENDERER", textureFinal)

        renderShader = renderShaderTemplate
                .replace( GENERAL_INIT,   generalInit  )
                .replace( SEED_INIT,      seedInit     )
                .replace( SHAPE_INIT,     shapeInit    )
                .replace( SHAPE_LOOP,     shapeLoop    )
                .replace( TEXTURE_INIT,   textureInit  )
                .replace( TEXTURE_LOOP,   textureLoop  )
                .replace( TEXTURE_FINAL,  textureFinal )
                .replace( CONDITIONAL,    conditional  )

        //renderShader = testCode
        //Log.d("RENDERER", renderShader)

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
        }
        logError()

        return shader

    }
    private fun splitCoords(texture: GLTexture, xCoords: FloatArray, yCoords: FloatArray, n: Int) : List<FloatArray> {

        val xLength = xCoords[1] - xCoords[0]
        val yLength = yCoords[1] - yCoords[0]
        val xPixels = xLength / 2f * texture.res.x
        val yPixels = yLength / 2f * texture.res.y
        val maxPixelsPerChunk = texture.res.x*texture.res.y/n
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
    private fun getRenderUniformLocations() {

        viewCoordsHandle     = glGetAttribLocation(renderProgram, "viewCoords")
        iterHandle           = glGetUniformLocation(renderProgram, "maxIter")
        bailoutHandle        = glGetUniformLocation(renderProgram, "R")
        powerHandle          = glGetUniformLocation(renderProgram, "power")
        x0Handle             = glGetUniformLocation(renderProgram, "x0")
        y0Handle             = glGetUniformLocation(renderProgram, "y0")
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

    }
    private fun deleteAllTextures() {


    }





    // ===========================================================================================
    //  NATIVE FUNCTIONS
    // ===========================================================================================

    private fun onRenderShaderChanged() {

        //Log.d("RENDERER", "render shader changed")

        updateRenderShader()

        // load new render shader
        glDetachShader(renderProgram, fRenderShader)
        fRenderShader = loadShader(GL_FRAGMENT_SHADER, renderShader)
        glAttachShader(renderProgram, fRenderShader)
        glLinkProgram(renderProgram)

        // check program link success
        val q = IntBuffer.allocate(1)
        glGetProgramiv(renderProgram, GL_LINK_STATUS, q)
        // Log.e("RENDERER", "program linked: ${q[0] == GL_TRUE}")

        // reassign location handles to avoid bug on Mali GPUs
        getRenderUniformLocations()

        renderShaderChanged = false

        //Log.d("RENDERER", "render shader changing done")

    }
    private fun onForegroundResolutionChanged() {

        Log.d("RENDERER", "resolution changed")

        val newRes = sc.resolution.scaleRes(screenRes)
        fgTexRes.x = newRes.x
        fgTexRes.y = newRes.y
        foreground1.delete()
        foreground2.delete()
        foreground1 = GLTexture(fgTexRes, GL_NEAREST, TEX_FORMAT, FG1_INDEX)
//                    textures.y = GLTexture(texRes, GL_NEAREST(), GL_RGBA, 1)
        if (sc.resolution.ordinal > Resolution.FULL.ordinal) {
            sc.sampleOnStrictTranslate = false
        }
        else {
            foreground2 = GLTexture(fgTexRes, GL_NEAREST, TEX_FORMAT, FG2_INDEX)
            sc.sampleOnStrictTranslate = true
        }
//                    textures[2] = GLTexture(texRes, GL_NEAREST(), GL_RGBA, 2)

        foreground = foreground1
        fgAuxiliary = foreground2



        if (sc.hardwareProfile == HardwareProfile.CPU) {

            fgOutAllocation.destroy()
            fgOutAllocation = Allocation.createTyped(rs, Type.createXY(
                    rs,
                    Element.F32_2(rs),
                    fgTexRes.x,
                    fgTexRes.y
            ))

        }



        fgResolutionChanged = false

    }
    private fun onRenderBackgroundChanged() {

        Log.d("RENDERER", "bg res changed : ${sc.continuousRender}, ${!sc.renderBackground}")

        if (sc.continuousRender || !sc.renderBackground) {
            background1.delete()
            background2.delete()
        }
        else {
            background1 = GLTexture(bgTexRes, GL_NEAREST, TEX_FORMAT, BG1_INDEX)
            background2 = GLTexture(bgTexRes, GL_NEAREST, TEX_FORMAT, BG2_INDEX)
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
                act.enableTextures()
            }
            HardwareProfile.CPU -> {
                msg = "${res.getString(R.string.msg_cpu)}\n${res.getString(R.string.msg_textures_disabled)}"
                when (sc.cpuPrecision) {
                    CpuPrecision.PERTURB -> {
                        refAllocation = Allocation.createTyped(rs, Type.createX(
                                rs, Element.F64(rs), MAX_REF_ITER*2
                        ))
                    }
                    else -> {
                        //sc.resolution = Resolution.FOURTH
                        //fgResolutionChanged = true
                        fgOutAllocation = Allocation.createTyped(rs, Type.createXY(
                                rs, Element.F32_2(rs), foreground.res.x, foreground.res.y
                        ))
                        bgOutAllocation = Allocation.createTyped(rs, Type.createXY(
                                rs, Element.F32_2(rs), background.res.x, background.res.y
                        ))
                    }
                }
                act.disableTextures()
            }
        }

        act.showMessage(msg)

        updateNumRenderChunks()

    }
    private fun onGpuPrecisionChanged() {

        renderShaderChanged = true

        val msg = when (sc.gpuPrecision) {
            GpuPrecision.SINGLE -> "${res.getString(R.string.msg_dual_out1)}\n${res.getString(R.string.msg_dual_out2)}"
            GpuPrecision.DUAL -> "${res.getString(R.string.msg_dual_in1)}\n${res.getString(R.string.msg_dual_in2)}"
        }

        act.showMessage(msg)

        updateNumRenderChunks()

    }
    private fun onCpuPrecisionChanged() {

        val msg : String

        when (sc.cpuPrecision) {
            CpuPrecision.PERTURB -> {
                sc.renderBackground = false
                refAllocation = Allocation.createTyped(rs, Type.createX(
                        rs, Element.F64(rs), MAX_REF_ITER*2
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

        act.showMessage(msg)

        updateNumRenderChunks()

    }
    fun onContinuousRenderChanged() {
        texSpanHistSize = if (sc.continuousRender) TEX_SPAN_HIST_CONTINUOUS else TEX_SPAN_HIST_DISCRETE
        if (sc.continuousRender) {
            textureMins = FloatArray(texSpanHistSize) { 0f }
            textureMaxs = FloatArray(texSpanHistSize) { 1f }
            calcNewTextureSpan = true
        }
        else {
            textureMins = textureMins.slice(0 until texSpanHistSize).toFloatArray()
            textureMaxs = textureMaxs.slice(0 until texSpanHistSize).toFloatArray()
        }
        updateNumRenderChunks()
    }
    private fun resetTextureMinMax() {
//        textureMin = 0f
//        textureMax = 1f
//        resetTextureMinMax = false
    }
    fun updateNumRenderChunks() {

        when (sc.hardwareProfile) {
            HardwareProfile.GPU -> {
                when (sc.gpuPrecision) {
                    GpuPrecision.SINGLE -> {
                        numForegroundChunks = if (sc.continuousRender) 3 else sc.chunkProfile.fgSingle
                        numBackgroundChunks = sc.chunkProfile.bgSingle
                    }
                    GpuPrecision.DUAL -> {
                        numForegroundChunks = if (sc.continuousRender) 10 else sc.chunkProfile.fgDual
                        numBackgroundChunks = sc.chunkProfile.bgDual
                    }
                }
            }
            HardwareProfile.CPU -> {
                numForegroundChunks = CPU_FG_CHUNKS
                numBackgroundChunks = CPU_BG_CHUNKS
            }
        }

    }




    private fun render() {

        if (renderShaderChanged) onRenderShaderChanged()
        if (fgResolutionChanged) onForegroundResolutionChanged()
        if (renderBackgroundChanged) onRenderBackgroundChanged()
        if (resetTextureMinMax) resetTextureMinMax()

        //Log.d("RENDERER", "rendering with ${renderProfile.name} profile")

        when (renderProfile) {

            RenderProfile.MANUAL -> {

                if (renderToTex) {

                    isRendering = !(sc.continuousRender || reaction == Reaction.SHAPE)

                    renderToTex = false
                    colorThumbsRendered = false

                    if (sc.renderBackground) renderToTexture(background)
                    val bgInterrupt = interruptRender
                    if (!bgInterrupt) renderToTexture(foreground)
                    val fgInterrupt = interruptRender
                    if (fgInterrupt && !bgInterrupt) {
                        val temp = bgAuxiliary
                        bgAuxiliary = background
                        background = temp
                    }

                    if (!interruptRender) {
                        resetQuadParams()
                        hasTranslated = false
                        hasScaled = false
                        hasRotated = false
                    }

                }

                if (interruptRender) {
                    Log.d("RENDERER", "render interrupted!!")
                    interruptRender = false
                }
                if (sc.renderBackground) renderFromTexture(background)
                renderFromTexture(foreground)

                textureThumbsRendered = false

            }
            RenderProfile.SAVE -> {

                renderFromTexture(foreground, true)
                val bmp = Bitmap.createBitmap(
                        foreground.res.x,
                        foreground.res.y,
                        Bitmap.Config.ARGB_8888
                )
                migrate(foreground, bmp)
                saveImage(bmp)
                bmp.recycle()
                renderFromTexture(foreground)
                renderProfile = RenderProfile.MANUAL

            }
            RenderProfile.COLOR_THUMB -> {

                if (renderThumbnails) {

                    val prevPalette = f.palette
                    val prevShowProgress = sc.showProgress
                    val prevTextureMin = textureMins[0]
                    val prevTextureMax = textureMaxs[0]
                    sc.showProgress = false
                    if (renderToTex) {
                        renderToTexture(thumbnail)
                        renderToTex = false
                    }
                    sc.showProgress = prevShowProgress

                    ColorPalette.all.forEach { palette ->
                        f.palette = palette
                        renderFromTexture(thumbnail, true)
                        migrate(thumbnail, palette.thumbnail!!)
                    }

                    f.palette = prevPalette
                    textureMins[0] = prevTextureMin
                    textureMaxs[0] = prevTextureMax
                    renderThumbnails = false
                    colorThumbsRendered = true
                    handler.updateColorThumbnails()

                }

                if (sc.renderBackground) renderFromTexture(background)
                renderFromTexture(foreground)



            }
            RenderProfile.TEXTURE_THUMB -> {

                if (renderThumbnails) {

                    isRendering = !(sc.continuousRender || reaction == Reaction.SHAPE)

                    val prevTexture = f.texture
                    val prevShowProgress = sc.showProgress
                    val prevTextureMin = textureMins[0]
                    val prevTextureMax = textureMaxs[0]
                    sc.showProgress = false

                    for (texture in f.shape.compatTextures) {

                        if (pauseRender) {
                            try {
                                Thread.sleep(275L)
                            } catch (e: Exception) {}
                            pauseRender = false
                        }
                        if (interruptRender) {
                            interruptRender = false
                            break
                        }

                        f.texture = texture
                        onRenderShaderChanged()

                        renderToTexture(thumbnail)
                        renderFromTexture(thumbnail, true)
                        migrate(thumbnail, texture.thumbnail!!)

                        renderProfile = RenderProfile.MANUAL

                        handler.updateTextureThumbnail(f.shape.compatTextures.indexOf(texture))

                    }

                    sc.showProgress = prevShowProgress
                    f.texture = prevTexture
                    textureMins[0] = prevTextureMin
                    textureMaxs[0] = prevTextureMax
                    onRenderShaderChanged()
                    renderThumbnails = false
                    textureThumbsRendered = true

                }

                if (renderToTex) {
                    if (sc.renderBackground) renderToTexture(background)
                    renderToTexture(foreground)
                    renderToTex = false
                }
                if (sc.renderBackground) renderFromTexture(background)
                renderFromTexture(foreground)

            }

        }

    }
    private fun renderToTexture(texture: GLTexture) {

        act.findViewById<ProgressBar>(R.id.progressBar).progress = 0
        val renderToTexStartTime = now()
        var calcTimeTotal = 0L


        when (sc.hardwareProfile) {

            HardwareProfile.GPU -> {

                //Log.e("RENDERER", "gpu")

                glUseProgram(renderProgram)
                glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer

                // pass in shape params
                glUniform2fv(juliaParamHandle, 1, f.shape.params.julia.toFloatArray(), 0)
                for (i in mapParamHandles.indices) {
                    val pArray =
                            if (i < f.shape.params.size) f.shape.params.list[i].toFloatArray()
                            else floatArrayOf(0f, 0f)
                    // Log.d("RENDERER", "passing p${i+1} in as (${pArray[0]}, ${pArray[1]})")
                    glUniform2fv(mapParamHandles[i], 1, pArray, 0)
                }

                // pass in texture params
                for (i in textureParamHandles.indices) {
                    val qArray =
                            if (i < f.texture.params.size) floatArrayOf(f.texture.params[i].q.toFloat())
                            else floatArrayOf(0f)
                    // Log.d("RENDERER", "passing in Q${i+1} as ${qArray[0]}")
                    glUniform1fv(textureParamHandles[i], 1, qArray, 0)
                }


                val xScaleSD = f.position.zoom / 2.0
                val yScaleSD = f.position.zoom * aspectRatio / 2.0
                val xCoordSD = f.position.x
                val yCoordSD = f.position.y

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

                        val xScaleDF = splitSD(xScaleSD)
                        val yScaleDF = splitSD(yScaleSD)
                        val xCoordDF = splitSD(xCoordSD)
                        val yCoordDF = splitSD(yCoordSD)

                        glUniform2fv(xScaleHandle, 1, xScaleDF, 0)
                        glUniform2fv(yScaleHandle, 1, yScaleDF, 0)
                        glUniform2fv(xCoordHandle, 1, xCoordDF, 0)
                        glUniform2fv(yCoordHandle, 1, yCoordDF, 0)

                    }
                }
                glUniform1fv(sinRotateHandle, 1, floatArrayOf(sin(f.position.rotation).toFloat()), 0)
                glUniform1fv(cosRotateHandle, 1, floatArrayOf(cos(f.position.rotation).toFloat()), 0)

                // pass in other parameters

                val power = if (f.shape.hasDynamicPower) f.shape.params.at(0).u.toFloat() else f.shape.power

                glUniform1ui(iterHandle, f.shape.maxIter)
                glUniform1fv(bailoutHandle, 1, floatArrayOf(f.bailoutRadius), 0)
                glUniform1fv(powerHandle, 1, floatArrayOf(power), 0)
                glUniform1fv(x0Handle, 1, floatArrayOf(f.shape.seed.x.toFloat()), 0)
                glUniform1fv(y0Handle, 1, floatArrayOf(f.shape.seed.y.toFloat()), 0)

                glEnableVertexAttribArray(viewCoordsHandle)


                if (sc.sampleOnStrictTranslate
                        && transformIsStrictTranslate()
                        && texture == foreground) {

                    //Log.e("RENDERER", "strict translate")

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

                    glViewport(0, 0, foreground.res.x, foreground.res.y)
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

                    val numChunks = when (texture) {
                        background, thumbnail -> numBackgroundChunks
                        foreground -> numForegroundChunks
                        else -> 1
                    }
                    //Log.e("RENDERER", "numChunks: $numChunks")
                    val chunksA = splitCoords(texture, xComplementViewCoordsA, yComplementViewCoordsA, numChunks)
                    val chunksB = splitCoords(texture, xComplementViewCoordsB, yComplementViewCoordsB, numChunks)
                    val totalChunks = chunksA.size + chunksB.size
                    var chunksRendered = 0
                    for (complementViewChunkCoordsA in chunksA) {

                        if (pauseRender) {
                            try {
                                Thread.sleep(275L)
                            } catch (e: Exception) {}
                            pauseRender = false
                        }
                        if (interruptRender) break

                        viewChunkBuffer.put(complementViewChunkCoordsA).position(0)
                        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)
                        glFinish()
                        chunksRendered++
                        if (!sc.continuousRender && sc.showProgress) {
                            act.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }
                    for (complementViewChunkCoordsB in chunksB) {

                        if (pauseRender) {
                            try {
                                Thread.sleep(275L)
                            } catch (e: Exception) {}
                            pauseRender = false
                        }
                        if (interruptRender) break

                        viewChunkBuffer.put(complementViewChunkCoordsB).position(0)
                        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)
                        glFinish()
                        chunksRendered++
                        if (!sc.continuousRender && sc.showProgress) {
                            act.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }


                    //===================================================================================
                    // SAMPLE -- TRANSLATION INTERSECTION
                    //===================================================================================

                    glUseProgram(sampleProgram)
                    glViewport(0, 0, fgAuxiliary.res.x, fgAuxiliary.res.y)

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

                }
                else {

                    //===================================================================================
                    // NOVEL RENDER -- ENTIRE TEXTURE
                    //===================================================================================


//                    if (bgAuxiliary == background1) {
//                        Log.e("RENDERER", "rendering into background 1")
//                    }
//                    else if (bgAuxiliary == background2){
//                        Log.e("RENDERER", "rendering into background 2")
//                    }
                    val auxTexture = when (texture) {
                        foreground -> if (sc.resolution.ordinal > Resolution.FULL.ordinal) foreground else fgAuxiliary
                        background -> bgAuxiliary
                        thumbnail -> thumbnail
                        else -> fgAuxiliary
                    }
                    glViewport(0, 0, texture.res.x, texture.res.y)
                    glUniform1fv(bgScaleHandle, 1, floatArrayOf(if (texture == background) bgSize else 1f), 0)
                    glFramebufferTexture2D(
                            GL_FRAMEBUFFER,             // target
                            GL_COLOR_ATTACHMENT0,       // attachment
                            GL_TEXTURE_2D,              // texture target
                            auxTexture.id,              // texture
                            0                           // level
                    )

                    // check framebuffer status
                    val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
                    if (status != GL_FRAMEBUFFER_COMPLETE) {
                        Log.d("FRAMEBUFFER", "$status")
                    }

                    //glClear(GL_COLOR_BUFFER_BIT)

//                    if (texture == background) {
//
//                        glVertexAttribPointer(
//                                viewCoordsHandle,       // index
//                                3,                      // coordinates per vertex
//                                GL_FLOAT,               // type
//                                false,                  // normalized
//                                12,                     // coordinates per vertex (3) * bytes per float (4)
//                                viewBuffer              // coordinates
//                        )
//                        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)
//
//                    } else {


                    val numChunks = when (texture) {
                        background, thumbnail -> numBackgroundChunks
                        foreground -> numForegroundChunks
                        else -> 1
                    }
                    val chunks = splitCoords(texture, floatArrayOf(-1f, 1f), floatArrayOf(-1f, 1f), numChunks)
                    val totalChunks = chunks.size
                    var chunksRendered = 0
                    for (viewChunkCoords in chunks) {

                        if (pauseRender) {
                            try {
                                Thread.sleep(275L)
                            } catch (e: Exception) {}
                            pauseRender = false
                        }
                        if (interruptRender) break

                        viewChunkBuffer.put(viewChunkCoords)
                        viewChunkBuffer.position(0)

                        glVertexAttribPointer(
                                viewCoordsHandle,           // index
                                3,                          // coordinates per vertex
                                GL_FLOAT,                // type
                                false,                      // normalized
                                12,                         // coordinates per vertex * bytes per float
                                viewChunkBuffer             // coordinates
                        )

                        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)
                        glFinish()   // force chunk to finish rendering before continuing
                        chunksRendered++
                        if (!(sc.continuousRender || reaction == Reaction.SHAPE) && sc.showProgress) {
                            act.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }

//                    }

                    glDisableVertexAttribArray(viewCoordsHandle)

                }


                if (!interruptRender) {

                    // calculate texture min/max
                    if ((texture == foreground || texture == thumbnail) && sc.autofitColorRange) {

                        //var t1 = 0L

                        val pixel = ByteBuffer.allocateDirect(2*4).order(ByteOrder.nativeOrder())
                        pixel.rewind()

                        var textureMin = Float.MAX_VALUE
                        var textureMax = Float.MIN_VALUE
                        val xSamples = MINMAX_XSAMPLES
                        val ySamples = (xSamples*aspectRatio).toInt()
                        //Log.d("RENDERER", "total min-max samples: ${xSamples*ySamples}")

                        for (i in 0 until texture.res.x step texture.res.x/xSamples) {
                            for (j in 0 until texture.res.y step texture.res.y/ySamples) {
                                //val t = now()
                                glReadPixels(i, j, 1, 1, texture.format, texture.type, pixel)
                                //t1 += now() - t
                                val sx = pixel.float
                                val sy = pixel.float
                                pixel.rewind()
                                if (sy == TextureMode.OUT.ordinal.toFloat()) {
                                    if (!sx.isInfinite() && !sx.isNaN()) {
                                        if (sx < textureMin) textureMin = sx
                                        if (sx > textureMax) textureMax = sx
                                    }
                                }
                            }
                        }
                        Log.d("RENDERER", "min-max: ($textureMin, $textureMax)")
                        //Log.e("RENDERER", "minmax pixel read took $t1 ms")

                        if (calcNewTextureSpan) {
                            for (i in 0 until texSpanHistSize) {
                                textureMins[i] = textureMin
                                textureMaxs[i] = textureMax
                            }
                            calcNewTextureSpan = false
                        }
                        else {
                            if (texture == foreground) {
                                for (i in 0 until texSpanHistSize - 1) {
                                    textureMins[texSpanHistSize - i - 1] = textureMins[texSpanHistSize - i - 2]
                                    textureMaxs[texSpanHistSize - i - 1] = textureMaxs[texSpanHistSize - i - 2]
                                }
                            }
                            textureMins[0] = textureMin
                            textureMaxs[0] = textureMax
                        }

                    }

                    // change auxilliary texture
                    if (sc.resolution.ordinal <= Resolution.FULL.ordinal) {
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

                }


            }
            HardwareProfile.CPU -> {

                act.findViewById<ProgressBar>(R.id.progressBar).progress = 0
                //Log.e("RENDERER", "CPU precision: ${sc.cpuPrecision}")


                when (sc.cpuPrecision) {
                    CpuPrecision.PERTURB -> {  // hardcoded for mandelbrot
                        
                        val auxTexture = when (texture) {
                            foreground -> fgAuxiliary
                            background -> bgAuxiliary
                            else -> thumbnail
                        }

                        var refCalcTimeTotal = 0L
                        var renderTimeTotal = 0L
                        var centerCalcTimeTotal = 0L
                        var samplesPerRow = 45
                        var nativeRefCalcTimeTotal = 0L

                        var numGlitchedPxls: Int
                        var pixelsInArray: ShortArray


                        val maxPixelsPerChunk = screenRes.x * screenRes.y / CPU_CHUNKS


                        numGlitchedPxls = auxTexture.res.x * auxTexture.res.y


                        val deadPixels = arrayListOf<Point>()

                        val glitchedPixels = ShortArray(auxTexture.res.x * auxTexture.res.y * 2) { index ->
                            val q = if (index % 2 == 0) (index / 2 % auxTexture.res.x).toShort() else floor((index / 2.0) / auxTexture.res.x).toShort()
                            if (q >= 6240) {
                                Log.d("RENDERER", "${(index / 2.0) / auxTexture.res.x}")
                            }
                            q
                        }
                        var pixelsOutArray: FloatArray

                        var z0 = Apcomplex(f.position.xap, f.position.yap)
                        val refPixel = Point(auxTexture.res.x / 2, auxTexture.res.y / 2)
                        val refPixels = arrayListOf(Point(refPixel))

                        var largestGlitchSize: Int
                        var numReferencesUsed = 0

                        val sinRotation = sin(f.position.rotation)
                        val cosRotation = cos(f.position.rotation)
                        val bgScale = if (auxTexture == background) 5.0 else 1.0
                        val sp = if (f.position.zoom < 1e-100) 1e300 else 1.0
                        val sn = if (f.position.zoom < 1e-100) 1e-300 else 1.0

                        Log.d("RENDERER", "x0: ${z0.real()}")
                        Log.d("RENDERER", "y0: ${z0.imag()}")

                        var auxTextureMin = Float.MAX_VALUE
                        var auxTextureMax = Float.MIN_VALUE


                        // MAIN LOOP
                        while (numReferencesUsed < MAX_REFERENCES) {


                            val d0xOffset = f.position.xap.subtract(z0.real()).toDouble()
                            val d0yOffset = f.position.yap.subtract(z0.imag()).toDouble()


                            // REFERENCE CALCULATION

                            val nativeReferenceStartTime = now()
                            val xMag = 0.5 * f.position.zoom
                            val yMag = 0.5 * f.position.zoom * aspectRatio
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
                                    f.shape.maxIter,
                                    MAX_REF_ITER,
                                    f.bailoutRadius.toDouble(),
                                    sp, sn,
                                    refData
                            )
                            nativeRefCalcTimeTotal += now() - nativeReferenceStartTime
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
                                        auxTexture.res,
                                        numChunkPxls,
                                        d0xOffset,
                                        d0yOffset,
                                        sp, sn,
                                        refData,
                                        bgScale
                                )
                                pixelsOutAllocation.copyTo(pixelsOutArray)
                                renderTimeTotal += now() - renderScriptStartTime


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


                            val progress = (100.0 * (1.0 - numGlitchedPxls.toDouble() / (auxTexture.res.x * auxTexture.res.y)).pow(5.0)).toInt()
                            val estRenderTime = 0.1f / progress.toFloat() * (now() - renderToTexStartTime)

                            Log.d("RENDERER",
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
                            act.findViewById<ProgressBar>(R.id.progressBar).progress = progress


                            // GLITCH DETECTION

                            val minGlitchSize = 100
                            var glitch = arrayListOf<Point>()
                            while (samplesPerRow <= auxTexture.res.x) {

                                //glitch = findGlitchMostPixels(imArray, auxTexture.res.x / samplesPerRow, auxTexture.res)
                                glitch = findGlitchMostPixels(auxTexture, auxTexture.res.x / samplesPerRow, auxTexture.res)
                                //Log.e("RENDERER", "largest glitch size (sample rate= $samplesPerRow): ${glitch.size}")
                                if (glitch.size > minGlitchSize || samplesPerRow == auxTexture.res.x) break
                                samplesPerRow *= 2

                            }
                            largestGlitchSize = glitch.size
                            if (largestGlitchSize == 0) break


                            // CENTER CALCULATION

                            val centerCalcStartTime = now()
                            val center = harmonicMean(glitch)
                            refPixel.apply {
                                x = center.x
                                y = center.y
                            }

                            centerCalcTimeTotal += now() - centerCalcStartTime
                            refPixels.add(Point(refPixel))
                            //imArray.set(refPixel.x, refPixel.y, 1, auxTexture.res.x, 2, 5f)

                            val x0DiffAux = bgScale * f.position.zoom * (refPixel.x.toDouble() / (auxTexture.res.x) - 0.5)
                            val y0DiffAux = bgScale * f.position.zoom * (refPixel.y.toDouble() / (auxTexture.res.y) - 0.5) * aspectRatio

                            z0 = Apcomplex(
                                    f.position.xap.add(Apfloat((x0DiffAux * cosRotation - y0DiffAux * sinRotation).toString(), sc.perturbPrecision)),
                                    f.position.yap.add(Apfloat((x0DiffAux * sinRotation + y0DiffAux * cosRotation).toString(), sc.perturbPrecision))
                            )

                            numReferencesUsed++


                        }




                        Log.d("RENDERER", "${deadPixels.size} dead pixels")
                        for (p in deadPixels) {
                            val neighborX = if (p.x + 1 == auxTexture.res.x) p.x - 1 else p.x + 1
                            auxTexture.set(p.x, p.y, 0, auxTexture.get(neighborX, p.y, 0))
                            auxTexture.set(p.x, p.y, 1, auxTexture.get(neighborX, p.y, 1))
                        }
                        for (i in 0 until numGlitchedPxls step 2) {
                            val p = Point(glitchedPixels[i].toInt(), glitchedPixels[i + 1].toInt())
                            val neighborX = if (p.x + 1 == auxTexture.res.x) p.x - 1 else p.x + 1
                            auxTexture.set(p.x, p.y, 0, auxTexture.get(neighborX, p.y, 0))
                            auxTexture.set(p.x, p.y, 1, auxTexture.get(neighborX, p.y, 1))
                        }



                        calcTimeTotal = refCalcTimeTotal + renderTimeTotal + centerCalcTimeTotal
                        Log.d("RENDERER",
                                "[total: ${(now() - renderToTexStartTime) / 1000f} sec], " +
                                        "[reference: ${nativeRefCalcTimeTotal / 1000f} sec], " +
                                        "[renderscript: ${renderTimeTotal / 1000f} sec], " +
                                        "[glitch center: ${centerCalcTimeTotal / 1000f} sec], " +
                                        "[misc: ${(now() - renderToTexStartTime - calcTimeTotal) / 1000f} sec], " +
                                        "[num references: $numReferencesUsed]"
                        )

                        //auxTexture.put(imArray)
                        auxTexture.update()

                    }
                    CpuPrecision.DOUBLE -> {

                        //Log.e("RENDERER", "CPU double")

                        val auxTexture = when (texture) {
                            foreground -> if (sc.resolution.ordinal > Resolution.FULL.ordinal) foreground else fgAuxiliary
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

                            set_width(0, texture.res.x, true)
                            set_height(0, texture.res.y, true)
                            set_height(0, texture.res.y, true)
                            set_aspectRatio(0, aspectRatio, true)
                            set_bgScale(0, bgScale, true)
                            set_maxIter(0, f.shape.maxIter.toLong(), true)
                            set_escapeRadius(0, f.bailoutRadius, true)
                            set_scale(0, 0.5*f.position.zoom, true)
                            set_xCoord(0, f.position.x, true)
                            set_yCoord(0, f.position.y, true)
                            set_sinRotation(0, sin(f.position.rotation), true)
                            set_cosRotation(0, cos(f.position.rotation), true)
                            set_x0(0, f.shape.seed.x, true)
                            set_y0(0, f.shape.seed.y, true)
                            set_juliaMode(0, f.shape.juliaMode, true)
                            if (f.shape.juliaMode) {
                                set_jx(0, f.shape.params.julia.u, true)
                                set_jy(0, f.shape.params.julia.v, true)
                            }
                            if (f.shape.params.size > 0) {
                                val p1 = f.shape.params.at(0).toDouble2()
                                set_p1x(0, p1.x, true)
                                set_p1y(0, p1.y, true)
                            }
                            if (f.shape.params.size > 1) {
                                val p2 = f.shape.params.at(1).toDouble2()
                                set_p2x(0, p2.x, true)
                                set_p2y(0, p2.y, true)
                            }
                            if (f.shape.params.size > 2) {
                                val p3 = f.shape.params.at(2).toDouble2()
                                set_p3x(0, p3.x, true)
                                set_p3y(0, p3.y, true)
                            }
                            if (f.shape.params.size > 3) {
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
                        //val chunkSize = floor(texture.res.y.toDouble()/numChunks).toInt()
                        var numChunksRendered = 0

                        auxTexture.floatBuffer.position(0)

                        var yStart : Int
                        var yEnd = 0

                        for (i in 0 until numChunks) {

                            if (interruptRender) break

                            yStart = yEnd
                            yEnd = ((i + 1).toDouble()/numChunks*texture.res.y).toInt()
                            //Log.e("RENDERER", "y range: ($yStart, $yEnd)")

                            data.set_yStart(0, yStart, true)
                            data.set_yEnd(0, yEnd, true)
                            auxTexture.put(f.shape.iterateNative(data))
                            numChunksRendered++

                            act.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (100.0 * numChunksRendered.toDouble() / numChunks).toInt()

                        }

                        auxTexture.update()

                    }
                }

                if (!interruptRender) {

                    // change auxilliary texture
                    if (sc.resolution.ordinal <= Resolution.FULL.ordinal) {
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

                        val t = now()

                        var textureMin = Float.MAX_VALUE
                        var textureMax = Float.MIN_VALUE
                        val xSamples = MINMAX_XSAMPLES
                        val ySamples = (xSamples*aspectRatio).toInt()

                        for (i in 0 until texture.res.x step texture.res.x/xSamples) {
                            for (j in 0 until texture.res.y step texture.res.y/ySamples) {
                                val sx = texture.get(i, j, 0)
                                val sy = texture.get(i, j, 1)
                                if (f.textureMode.ordinal.toFloat() == sy || f.textureMode == TextureMode.BOTH) {
                                    if (sx < textureMin) textureMin = sx
                                    if (sx > textureMax) textureMax = sx
                                }
                            }
                        }
                        Log.d("RENDERER", "min-max: ($textureMin, $textureMax)")
                        //Log.e("RENDERER", "minmax calc took ${(now() - t)/1000f} sec")

                    }
                }

            }

        }


        //Log.e("RENDERER", "misc operations took ${(now() - renderToTexStartTime - calcTimeTotal)/1000f} sec")
        Log.d("RENDERER", "renderToTexture took ${(now() - renderToTexStartTime)/1000f} sec")

    }
    private fun renderFromTexture(texture: GLTexture, external: Boolean = false) {

//        if (texture == background1) {
//            Log.e("RENDERER", "rendering from background1")
//        }
//        else if (texture == background2) {
//            Log.e("RENDERER", "rendering from background2")
//        }
//        else if (texture == foreground) {
//            Log.e("RENDERER", "rendering from foreground")
//        }

        // val t = System.currentTimeMillis()

        //======================================================================================
        // PRE-RENDER PROCESSING
        //======================================================================================

        glUseProgram(colorProgram)

        glBindFramebuffer(GL_FRAMEBUFFER, if (external) fboIDs[1] else 0)

        val viewportWidth = if (external) texture.res.x else screenRes.x
        val viewportHeight = if (external) texture.res.y else screenRes.y
        val yOrient = if (external) -1f else 1f

        glViewport(0, 0, viewportWidth, viewportHeight)

        val aspect = aspectRatio.toFloat()
        val buffer : FloatBuffer

        if (texture == background) {

            val bgVert1 = rotate(floatArrayOf(-bgSize*quadScale, bgSize*quadScale*aspect), quadRotation)
            val bgVert2 = rotate(floatArrayOf(-bgSize*quadScale, -bgSize*quadScale*aspect), quadRotation)
            val bgVert3 = rotate(floatArrayOf(bgSize*quadScale, -bgSize*quadScale*aspect), quadRotation)
            val bgVert4 = rotate(floatArrayOf(bgSize*quadScale, bgSize*quadScale*aspect), quadRotation)

            bgVert1[1] /= aspect
            bgVert2[1] /= aspect
            bgVert3[1] /= aspect
            bgVert4[1] /= aspect

            // create float array of background quad coordinates
            val bgQuadVertices = floatArrayOf(
                    bgVert1[0] + quadCoords[0], bgVert1[1] + quadCoords[1], 0f,     // top left
                    bgVert2[0] + quadCoords[0], bgVert2[1] + quadCoords[1], 0f,     // bottom left
                    bgVert3[0] + quadCoords[0], bgVert3[1] + quadCoords[1], 0f,     // bottom right
                    bgVert4[0] + quadCoords[0], bgVert4[1] + quadCoords[1], 0f )    // top right
            bgQuadBuffer
                    .put(bgQuadVertices)
                    .position(0)

            buffer = bgQuadBuffer

        }
        else {

            val vert1 = rotate(floatArrayOf(-quadScale, quadScale*aspect), quadRotation)
            val vert2 = rotate(floatArrayOf(-quadScale, -quadScale*aspect), quadRotation)
            val vert3 = rotate(floatArrayOf(quadScale, -quadScale*aspect), quadRotation)
            val vert4 = rotate(floatArrayOf(quadScale, quadScale*aspect), quadRotation)

            vert1[1] /= aspect
            vert2[1] /= aspect
            vert3[1] /= aspect
            vert4[1] /= aspect

            // create float array of quad coordinates
            val quadVertices = floatArrayOf(
                    vert1[0] + quadCoords[0], vert1[1] + quadCoords[1], 0f,     // top left
                    vert2[0] + quadCoords[0], vert2[1] + quadCoords[1], 0f,     // bottom left
                    vert3[0] + quadCoords[0], vert3[1] + quadCoords[1], 0f,     // bottom right
                    vert4[0] + quadCoords[0], vert4[1] + quadCoords[1], 0f )    // top right
            quadBuffer
                    .put(quadVertices)
                    .position(0)

            buffer = quadBuffer

        }


        glUniform1fv(yOrientColorHandle, 1, floatArrayOf(yOrient), 0)
        glUniform1i(numColorsHandle, f.palette.size)
        glUniform3fv(paletteHandle, f.palette.size, f.palette.flatPalette, 0)
        glUniform3fv(solidFillColorHandle, 1, colorToRGB(getColors(res, listOf(f.solidFillColor))[0]), 0)
        glUniform1i(textureModeHandle, f.textureMode.ordinal)
        glUniform1fv(frequencyHandle, 1, floatArrayOf(f.frequency), 0)
        glUniform1fv(phaseHandle, 1, floatArrayOf(f.phase), 0)
        glUniform1fv(densityHandle, 1, floatArrayOf(f.density), 0)
        val textureMin : Float
        val textureMax : Float
        if (texture == thumbnail) {
            textureMin = textureMins[0]
            textureMax = textureMaxs[0]
        }
        else {
            textureMin = textureMins.average().toFloat()
            textureMax = textureMaxs.average().toFloat()
        }
        glUniform1fv(minHandle, 1, floatArrayOf(textureMin), 0)
        glUniform1fv(maxHandle, 1, floatArrayOf(textureMax), 0)

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


        act.findViewById<ProgressBar>(R.id.progressBar).progress = 0
        // Log.d("RENDERER", "renderFromTexture took ${System.currentTimeMillis() - t} ms")

    }



    private fun setQuadFocus(screenPos: FloatArray) {

        // update texture quad coordinates
        // convert focus coordinates from screen space to quad space

        quadFocus[0] =   2f*(screenPos[0] / screenRes.x) - 1f
        quadFocus[1] = -(2f*(screenPos[1] / screenRes.y) - 1f)

        // Log.d("SURFACE VIEW", "quadFocus: (${quadFocus[0]}, ${quadFocus[1]})")

    }
    private fun translate(dScreenPos: FloatArray) {

        // update texture quad coordinates
        val dQuadPos = floatArrayOf(
                dScreenPos[0] / screenRes.x * 2f,
                -dScreenPos[1] / screenRes.y * 2f
        )

        if (!f.position.xLocked) {
            quadCoords[0] += dQuadPos[0]
            quadFocus[0] += dQuadPos[0]
        }
        if (!f.position.yLocked) {
            quadCoords[1] += dQuadPos[1]
            quadFocus[1] += dQuadPos[1]
        }

        // Log.d("SURFACE VIEW", "TRANSLATE -- quadCoords: (${quadCoords[0]}, ${quadCoords[1]})")
        // Log.d("SURFACE VIEW", "TRANSLATE -- quadFocus: (${quadFocus[0]}, ${quadFocus[1]})")

        hasTranslated = true

    }
    fun zoom(dZoom: Float) {

        if (!f.position.scaleLocked) {

            quadCoords[0] -= quadFocus[0]
            quadCoords[1] -= quadFocus[1]

            quadCoords[0] *= dZoom
            quadCoords[1] *= dZoom

            quadCoords[0] += quadFocus[0]
            quadCoords[1] += quadFocus[1]

            quadScale *= dZoom
            hasScaled = true

        }

    }
    private fun rotate(p: FloatArray, theta: Float) : FloatArray {

        val sinTheta = sin(theta)
        val cosTheta = cos(theta)
        return floatArrayOf(
                p[0]*cosTheta - p[1]*sinTheta,
                p[0]*sinTheta + p[1]*cosTheta
        )

    }
    fun rotate(dTheta: Float) {

        if (!f.position.rotationLocked) {

            quadCoords[0] -= quadFocus[0]
            quadCoords[1] -= quadFocus[1]

            val rotatedQuadCoords = rotate(floatArrayOf(quadCoords[0], quadCoords[1] * aspectRatio.toFloat()), dTheta)
            quadCoords[0] = rotatedQuadCoords[0]
            quadCoords[1] = rotatedQuadCoords[1]
            quadCoords[1] /= aspectRatio.toFloat()

            quadCoords[0] += quadFocus[0]
            quadCoords[1] += quadFocus[1]

            //Log.d("RR", "quadCoords: (${quadCoords[0]}, ${quadCoords[1]})")

            quadRotation += dTheta
            hasRotated = true

        }

    }
    private fun resetQuadParams() {

        quadCoords[0] = 0f
        quadCoords[1] = 0f

        quadFocus[0] = 0f
        quadFocus[1] = 0f

        quadScale = 1f
        quadRotation = 0f

    }
    private fun transformIsStrictTranslate() : Boolean {
        return hasTranslated && !hasScaled && !hasRotated
    }


    private fun migrate(texture: GLTexture, bitmap: Bitmap) {

        val t1 = now()

//        val bufferSize = texture.res.x*texture.res.y*4
//        val imBuffer = if (texture == thumbnail) {
//            thumbBuffer.position(0)
//            thumbBuffer
//        }
//        else {
//            ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
//        }
//        imBuffer.rewind()

        val buffer : ByteBuffer
        val readHeight : Int
        val heightOffset : Int

        if (texture == thumbnail) {
            buffer = thumbBuffer
            readHeight = thumbnail.res.x
            heightOffset = (0.5*texture.res.y*(1.0 - 1.0/aspectRatio)).roundToInt()
        }
        else {
            buffer = texture.byteBuffer
            readHeight = texture.res.y
            heightOffset = 0
        }
        buffer.position(0)

        glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[1])
        glViewport(0, 0, texture.res.x, texture.res.y)

        val t2 = now()

        glReadPixels(
                0, heightOffset,
                texture.res.x,
                readHeight,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                buffer
        )
        Log.d("RENDERER", "glReadPixels took ${now() - t2} ms")

        //logError()

        bitmap.copyPixelsFromBuffer(buffer)

        Log.d("RENDERER", "bitmap migration took ${now() - t1} ms")

    }
    private fun saveImage(im: Bitmap) {

        // convert bitmap to jpeg
        val bos = ByteArrayOutputStream()
        val compressed = im.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        if (!compressed) { Log.e("RENDERER", "could not compress image") }
        im.recycle()

        // get current date and time
        val c = GregorianCalendar(TimeZone.getDefault())
        // Log.d("RENDERER", "${c[Calendar.YEAR]}, ${c[Calendar.MONTH]}, ${c[Calendar.DAY_OF_MONTH]}")
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

            // app external storage directory
            val dir = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES),
                    res.getString(R.string.app_name
                    ))

            // create directory if not already created
            if (!dir.exists()) {
                Log.d("RENDERER", "Directory does not exist -- creating...")
                when {
                    dir.mkdir() -> Log.d("RENDERER", "Directory created")
                    dir.mkdirs() -> Log.d("RENDERER", "Directories created")
                    else -> {
                        Log.e("RENDERER", "Directory could not be created")
                        handler.showErrorMessage()
                        return
                    }
                }
            }

            val file = File(dir, "$imageName.jpg")
            try {
                file.createNewFile() } catch (e: IOException) {
                handler.showErrorMessage()
            }
            if (file.exists()) {
                val fos = FileOutputStream(file)
                fos.write(bos.toByteArray())
                fos.close()

                val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val contentUri = Uri.fromFile(file)
                scanIntent.data = contentUri
                act.sendBroadcast(scanIntent)
            }

        }
        else {

            val resolver = context.contentResolver
            val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val imageDetails = ContentValues().apply {
                put(MediaStore.Images.ImageColumns.RELATIVE_PATH, subDirectory)
                put(MediaStore.Images.ImageColumns.DISPLAY_NAME, imageName)
                put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg")
                //put(MediaStore.Images.ImageColumns.WIDTH, im.width)
                //put(MediaStore.Images.ImageColumns.HEIGHT, im.height)
            }

            val contentUri = resolver.insert(imageCollection, imageDetails)
            val fos = resolver.openOutputStream(contentUri!!, "w")
            fos?.write(bos.toByteArray())
            fos?.close()

        }

        handler.showImageSavedMessage("/$subDirectory")

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

        //log = glGetShaderInfoLog(fRenderShader)
        //Log.e("RENDERER", log)



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

        val sinRotation = sin(f.position.rotation)
        val cosRotation = cos(f.position.rotation)

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
        perturbationPixelsScript._maxIter = f.shape.maxIter.toLong()
        perturbationPixelsScript._escapeRadius = f.bailoutRadius

        perturbationPixelsScript._scale = f.position.zoom / 2.0
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

        val floodFillTimeStart = now()
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
                            if (rowLtMax)  queue.push(Point( p.x,      p.y + s ))
                            if (rowGtZero) queue.push(Point( p.x,      p.y - s ))
                            if (colLtMax)  queue.push(Point( p.x + s,  p.y     ))
                            if (colGtZero) queue.push(Point( p.x - s,  p.y     ))
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

        val floodFillTimeStart = now()
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
                            if (rowLtMax)  queue.push(Point( p.x,      p.y + s ))
                            if (rowGtZero) queue.push(Point( p.x,      p.y - s ))
                            if (colLtMax)  queue.push(Point( p.x + s,  p.y     ))
                            if (colGtZero) queue.push(Point( p.x - s,  p.y     ))
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
        Log.e("RENDERER", "flood-fill took ${(now() - floodFillTimeStart)/1000f} sec")
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

        val floodFillTimeStart = now()
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
                            if (rowLtMax)  queue.push(Point( p.x,      p.y + s ))
                            if (rowGtZero) queue.push(Point( p.x,      p.y - s ))
                            if (colLtMax)  queue.push(Point( p.x + s,  p.y     ))
                            if (colGtZero) queue.push(Point( p.x - s,  p.y     ))

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

        Log.e("RENDERER", "flood-fill took ${(now() - floodFillTimeStart)/1000f} sec")
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
                (1f/(reciprocalSumWidth/glitch.size)).toInt(),
                (1f/(reciprocalSumHeight/glitch.size)).toInt()
        )

        //Log.e("RENDERER", "harmonic mean: (${harmonicMean.x}, ${harmonicMean.y})")

        var minDist = Float.MAX_VALUE
        for (pixel in glitch) {
            val dif1 = pixel.x - harmonicMean.x
            val dif2 = pixel.y - harmonicMean.y
            val dist = sqrt((dif1*dif1 + dif2*dif2).toFloat())
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
            val dist = sqrt((dif1*dif1 + dif2*dif2).toFloat())
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
            val dist = sqrt((dif1*dif1 + dif2*dif2).toFloat())
            if (dist < minDist) {
                minDist = dist
                center = pixel
            }
        }

        return center

    }



    fun onTouchEvent(e: MotionEvent?): Boolean {


//        if (!isRendering) {

            // monitor for long press
//            when(e?.actionMasked) {
//                MotionEvent.ACTION_DOWN -> {
//                    val focus = e.focus()
//                    prevFocus[0] = focus[0]
//                    prevFocus[1] = focus[1]
//                    h.postDelayed(longPressed, 1000)
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    val focus = e.focus()
//                    val dx: Float = focus[0] - prevFocus[0]
//                    val dy: Float = focus[1] - prevFocus[1]
//                    if (sqrt(dx*dx + dy*dy) > minPixelMove) { h.removeCallbacks(longPressed) }
//                }
//                MotionEvent.ACTION_UP -> {
//                    h.removeCallbacks(longPressed)
//                }
//            }

        if (!sc.continuousRender && isRendering && reaction == Reaction.POSITION) {
            interruptRender = true
            //Log.d("RENDERER", "interruptRender = true")
        }

        when (reaction) {
            Reaction.POSITION -> {

                // actions change fractal
                when (e?.actionMasked) {

                    MotionEvent.ACTION_DOWN -> {
                        // Log.d("POSITION", "POINTER DOWN -- x: ${e.x}, y: ${e.y}")
                        val focus = e.focus()
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]

                        if (!sc.continuousRender) {
                            setQuadFocus(focus)
                        }

                        return true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        // Log.d("POSITION", "POINTER ${e.actionIndex} DOWN -- x: ${e.x}, y: ${e.y}")
                        val focus = e.focus()
                        prevZoom = f.position.zoom
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]
                        prevAngle = atan2(e.getY(0) - e.getY(1), e.getX(1) - e.getX(0))
                        prevFocalLen = e.focalLength()
                        // Log.d("POSITION", "focalLen: $prevFocalLen")
                        if (!sc.continuousRender) {
                            setQuadFocus(focus)
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val focus = e.focus()
                        val dx: Float = focus[0] - prevFocus[0]
                        val dy: Float = focus[1] - prevFocus[1]

                        f.position.translate(dx/screenRes.x, dy/screenRes.x)
                        if (!sc.continuousRender) {
                            translate(floatArrayOf(dx, dy))
                        }
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]
                        if (e.pointerCount > 1) {   // MULTI-TOUCH

                            // SCALE
                            val focalLen = e.focalLength()
                            val dFocalLen = focalLen / prevFocalLen

                            if (sc.continuousRender) prevZoom = f.position.zoom

                            f.position.zoom(dFocalLen, doubleArrayOf(
                                    focus[0].toDouble() / screenRes.x.toDouble() - 0.5,
                                    -(focus[1].toDouble() / screenRes.x.toDouble() - 0.5*aspectRatio)))


                            if (sc.continuousRender) checkThresholdCross(prevZoom) else zoom(dFocalLen)
                            prevFocalLen = focalLen

                            // ROTATE
                            val angle = atan2(e.getY(0) - e.getY(1), e.getX(1) - e.getX(0))
                            val dtheta = angle - prevAngle
                            f.position.rotate(dtheta, doubleArrayOf(
                                    focus[0].toDouble() / screenRes.x.toDouble() - 0.5,
                                    -(focus[1].toDouble() / screenRes.x.toDouble() - 0.5*aspectRatio)))
                            rotate(dtheta)

                            prevAngle = angle

                        }

                        if (sc.continuousRender) {
                            renderToTex = true
                        }

                        act.updateDisplayParams()
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        act.updatePositionEditTexts()
                        act.updateDisplayParams()
                        if (!sc.continuousRender) checkThresholdCross(prevZoom)
                        renderToTex = true
                        return true
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        // Log.d("POSITION", "POINTER ${e.actionIndex} UP")
                        when (e.getPointerId(e.actionIndex)) {
                            0 -> {
                                prevFocus[0] = e.getX(1)
                                prevFocus[1] = e.getY(1)
                            }
                            1 -> {
                                prevFocus[0] = e.getX(0)
                                prevFocus[1] = e.getY(0)
                            }
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
                        when (e.pointerCount) {
                            1 -> {
                                f.phase += dx/screenRes.x
                                prevFocus[0] = focus[0]
                                prevFocus[1] = focus[1]
                            }
                            2 -> {
                                val focalLen = e.focalLength()
                                val dFocalLen = focalLen / prevFocalLen
                                f.frequency *= dFocalLen
                                prevFocalLen = focalLen
                            }
                        }
                        act.updateDisplayParams()
                        return true
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        // Log.d("COLOR", "POINTER ${e.actionIndex} UP")
                        when (e.getPointerId(e.actionIndex)) {
                            0 -> {
                                prevFocus[0] = e.getX(1)
                                prevFocus[1] = e.getY(1)
                            }
                            1 -> {
                                prevFocus[0] = e.getX(0)
                                prevFocus[1] = e.getY(0)
                            }
                        }
                        prevFocalLen = 1f
                        return true
                    }
                    MotionEvent.ACTION_UP -> {

                        if (renderProfile == RenderProfile.COLOR_THUMB) {

                            renderThumbnails = true

                        }

                        act.updateColorEditTexts()
                        prevFocalLen = 1f
                        // Log.d("COLOR", "ACTION UP")
                        return true
                    }

                }
            }
            Reaction.SHAPE -> {
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
                                val param = f.shape.params.active
                                param.u += f.sensitivity*dx/screenRes.x
                                if (param is Shape.ComplexParam) {
                                    param.v -= f.sensitivity * dy / screenRes.y
                                }
                                prevFocus[0] = focus[0]
                                prevFocus[1] = focus[1]
                                renderToTex = true
                            }
                            2 -> {
                                val focalLen = e.focalLength()
                                val dFocalLen = focalLen / prevFocalLen
                                f.sensitivity *= dFocalLen
                                prevFocalLen = focalLen
                            }
                        }
                        act.updateDisplayParams()
                        return true
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        // Log.d("PARAMETER", "POINTER ${e.actionIndex} UP")
                        when (e.getPointerId(e.actionIndex)) {
                            0 -> {
                                prevFocus[0] = e.getX(1)
                                prevFocus[1] = e.getY(1)
                            }
                            1 -> {
                                prevFocus[0] = e.getX(0)
                                prevFocus[1] = e.getY(0)
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        act.updateShapeEditTexts()
                        act.updateDisplayParams()
                        renderToTex = true
                        // Log.d("PARAMETER", "POINTER UP")
                        return true
                    }

                }
            }
            Reaction.NONE -> return true
        }


        return false

    }


}