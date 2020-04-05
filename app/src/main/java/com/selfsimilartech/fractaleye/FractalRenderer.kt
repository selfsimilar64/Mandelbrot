package com.selfsimilartech.fractaleye

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import android.os.*
import android.provider.MediaStore
import android.renderscript.*
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
import java.lang.IndexOutOfBoundsException
import java.nio.*
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

const val CPU_CHUNKS = 4
const val MAX_REFERENCES = 20
val MAX_REF_ITER = 2.0.pow(15).toInt()



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
            System.loadLibrary("native-reference")
        }

    }



    private val resources = context.resources
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
    private var prevAngle = 0f
    private var prevFocalLen = 1.0f






    var renderToTex = false
    var renderThumbnails = false
    var isRendering = false

    var renderShaderChanged = false
    var fgResolutionChanged = false
    var bgResolutionChanged = false

    private var hasTranslated = false
    private var hasScaled = false
    private var hasRotated = false

    private val quadCoords = floatArrayOf(0f, 0f)
    private val quadFocus = floatArrayOf(0f, 0f)
    private var quadScale = 1f
    private var quadRotation = 0f

    private val bgSize = 5f
    private var floatPrecisionBits : Int? = null









    private var header       = ""
    private var arithmetic   = ""
    private var init         = ""
    private var loop         = ""
    private var conditional  = ""
    private var algInit      = ""
    private var algLoop      = ""
    private var algFinal     = ""
    private var mapInit      = ""
    private var mapLoop      = ""
    private var mapFinal     = ""

    private val colorHeader = resources.getString(R.string.color_header)
    private val colorIndex = resources.getString(R.string.color_index)
    private var colorPostIndex = ""

    private var renderShader = ""
    private val colorShader =
            """$colorHeader
                void main() {
        
                    vec3 color = solidFillColor;
                    vec4 s = texture(tex, texCoord);

                    if (textureMode == 2 || float(textureMode) == s.y) {
                        $colorIndex
                        $colorPostIndex
                    }
                    if (s.y == 3.0) {
                        color = vec3(0.85, 0.4, 0.4);
                    }
                    if (s.y == 4.0) {
                        color = vec3(0.4, 0.85, 0.4);
                    }
                    if (s.y == 5.0) {
                        color = vec3(0.4, 0.4, 0.85);
                    }
        
                    fragmentColor = color;
        
                }
                """


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
    private var frequencyHandle       : Int = 0
    private var phaseHandle           : Int = 0
    private var textureModeHandle     : Int = 0



    private var vRenderShader : Int = 0
    private var vSampleShader : Int = 0
    private var fRenderShader : Int = 0
    private var fColorShader  : Int = 0
    private var fSampleShader : Int = 0

    // define texture resolutions
    private val bgTexRes = if (sc.continuousRender) Point(1, 1) else Resolution.EIGHTH.scaleRes(screenRes)
    private val fgTexRes = sc.resolution.scaleRes(screenRes)
    private val thumbTexRes = Resolution.THUMB.scaleRes(screenRes)

    private val refData = NativeReferenceReturnData()




    private lateinit var background   : GLTexture
    private lateinit var foreground1  : GLTexture
    private lateinit var foreground2  : GLTexture
    private lateinit var thumbnail    : GLTexture

    private val thumbBuffer = ByteBuffer.allocateDirect(thumbTexRes.x*thumbTexRes.y*4).order(ByteOrder.nativeOrder())
    // private val refArray = DoubleArray(refSize*2) { 0.0 }
    private val refArray = DoubleArray(1) { 0.0 }



    private lateinit var foreground : GLTexture
    private lateinit var auxiliary : GLTexture


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
    private val perturbationImageScript = ScriptC_perturbationImage(rs)

    // memory allocation for reference iteration values
    private val refAllocation = Allocation.createTyped(rs, Type.createX(
            rs,
            Element.F64(rs),
            2*MAX_REF_ITER
    ))
//    private val imageOutAllocation = Allocation.createTyped(rs, Type.createXY(
//            rs,
//            Element.F32_2(rs),
//            foreground.res.x,
//            foreground.res.y
//    ))

    //memory allocation for indices of pixels that need to be iterated
    private var pixelsInAllocation  = Allocation.createTyped(rs, Type.createX(rs, Element.U8(rs), 1))
    private var pixelsOutAllocation = Allocation.createTyped(rs, Type.createX(rs, Element.U8(rs), 1))

    private val rsMsgHandler = object : RenderScript.RSMessageHandler() {
        override fun run() {
            when (mID) {
                0 -> act.findViewById<ProgressBar>(R.id.progressBar).apply {
                    //Log.e("FSV", "updating progress")
                    progress += 2
                }
            }
        }
    }



    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

        // get OpenGL ES version
        val glVersion = unused.glGetString(GL_VERSION).split(" ")[2].toFloat()
        Log.d("SURFACE VIEW", "$glVersion")
        // f.glVersion = glVersion

        // get fragment shader precision
        val a = IntBuffer.allocate(2)
        val b = IntBuffer.allocate(1)
        glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_HIGH_FLOAT, a, b)
        Log.d("FSV", "floating point exponent range: ${a[0]}, ${a[1]}")
        Log.d("FSV", "floating point precision: ${b[0]}")
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
        render()

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

//        System.loadLibrary("gmp")
//        System.loadLibrary("native-reference")

        renderProgram = glCreateProgram()
        colorProgram = glCreateProgram()
        sampleProgram = glCreateProgram()

        background   = GLTexture(bgTexRes,    GL_NEAREST, GL_RG32F, 0)
        foreground1  = GLTexture(fgTexRes,    GL_NEAREST, GL_RG32F, 1)
        foreground2  = GLTexture(fgTexRes,    GL_NEAREST, GL_RG32F, 2)
        thumbnail    = GLTexture(thumbTexRes, GL_NEAREST, GL_RG32F, 3)

        foreground = foreground1
        auxiliary = foreground2


        rs.messageHandler = rsMsgHandler

        val thumbRes = Resolution.THUMB.scaleRes(screenRes)
        Log.d("RENDERER", "thumbnail resolution: ${thumbRes.x} x ${thumbRes.y}")

        // load all vertex and fragment shader code
        var s = act.resources.openRawResource(R.raw.vert_render)
        val vRenderCode = Scanner(s).useDelimiter("\\Z").next()
        s.close()

        s = act.resources.openRawResource(R.raw.vert_sample)
        val vSampleCode = Scanner(s).useDelimiter("\\Z").next()
        s.close()

        // s = act.resources.openRawResource(R.raw.precision_test)
        // val precisionCode = Scanner(s).useDelimiter("\\Z").next()
        // s.close()

        s = act.resources.openRawResource(R.raw.sample)
        val fSampleCode = Scanner(s).useDelimiter("\\Z").next()
        s.close()


        //s = act.resources.openRawResource(R.raw.perturbation)
        //val perturbationCode = Scanner(s).useDelimiter("\\Z").next()
        //s.close()


        // create and compile shaders
        vRenderShader = loadShader(GL_VERTEX_SHADER, vRenderCode)
        vSampleShader = loadShader(GL_VERTEX_SHADER, vSampleCode)

        checkThresholdCross(f.position.scale)
        updateRenderShader()
        fRenderShader = loadShader(GL_FRAGMENT_SHADER, renderShader)
        //fRenderShader = loadShader(GL_FRAGMENT_SHADER, perturbationCode)

        fSampleShader = loadShader(GL_FRAGMENT_SHADER, fSampleCode)
        fColorShader  = loadShader(GL_FRAGMENT_SHADER, colorShader)


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
        frequencyHandle       = glGetUniformLocation(colorProgram, "frequency")
        phaseHandle           = glGetUniformLocation(colorProgram, "phase")
        textureModeHandle     = glGetUniformLocation(colorProgram, "textureMode")

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
    private fun scale(dScale: Float) {

        if (!f.position.scaleLocked) {

            quadCoords[0] -= quadFocus[0]
            quadCoords[1] -= quadFocus[1]

            quadCoords[0] *= dScale
            quadCoords[1] *= dScale

            quadCoords[0] += quadFocus[0]
            quadCoords[1] += quadFocus[1]

            quadScale *= dScale
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
    private fun rotate(dTheta: Float) {

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
    fun checkThresholdCross(prevScale: Double) {

        if (sc.autoPrecision) {

            val prevPrecision = sc.gpuPrecision
            sc.gpuPrecision = when {
                f.position.scale > GpuPrecision.SINGLE.threshold -> GpuPrecision.SINGLE

                f.shape.hasDualFloat
                        && f.position.scale < GpuPrecision.SINGLE.threshold -> GpuPrecision.DUAL

                else -> sc.gpuPrecision
            }


            val prevScaleStrings = "%e".format(Locale.US, prevScale).split("e")
            val prevScaleExponent = -prevScaleStrings[1].toDouble()
            val prevScaleOrdinal = floor(prevScaleExponent/12.0).toLong()
            
            val scaleStrings = "%e".format(Locale.US, f.position.scale).split("e")
            val scaleExponent = -scaleStrings[1].toDouble()
            val scaleOrdinal = floor(scaleExponent/12.0).toLong()

            sc.cpuPrecision = (scaleOrdinal + 2)*12
            if (scaleOrdinal != prevScaleOrdinal) f.position.updatePrecision(sc.cpuPrecision)

            handler.updatePrecisionBits()

            if (sc.gpuPrecision != prevPrecision) renderShaderChanged = true

        }

        // display message
        if (sc.hardwareProfile == HardwareProfile.GPU) {

            val singleThresholdCrossedIn = f.position.scale < GpuPrecision.SINGLE.threshold && prevScale > GpuPrecision.SINGLE.threshold
            val singleThresholdCrossedOut = f.position.scale > GpuPrecision.SINGLE.threshold && prevScale < GpuPrecision.SINGLE.threshold
            val dualThresholdCrossed = f.position.scale < GpuPrecision.DUAL.threshold && prevScale > GpuPrecision.DUAL.threshold

            val msg = when {
                (!f.shape.hasDualFloat && singleThresholdCrossedIn) || (f.shape.hasDualFloat && dualThresholdCrossed) -> resources.getString(R.string.msg_zoom_limit)
                sc.autoPrecision && f.shape.hasDualFloat && singleThresholdCrossedIn -> "${resources.getString(R.string.msg_dual_in1)}\n${resources.getString(R.string.msg_dual_in2)}"
                sc.autoPrecision && f.shape.hasDualFloat && singleThresholdCrossedOut -> "${resources.getString(R.string.msg_dual_out1)}\n${resources.getString(R.string.msg_dual_out2)}"
                else -> null
            }

            if (msg != null) act.showMessage(msg)

        }
    }


    private fun updateRenderShader() {

        when(sc.gpuPrecision) {
            GpuPrecision.SINGLE -> {
                header      = resources.getString(R.string.header_sf)
                arithmetic  = resources.getString(R.string.arithmetic_sf)
                init        = resources.getString(R.string.general_init_sf)
                init += if (f.shape.juliaMode) {
                    resources.getString(R.string.julia_sf)
                } else {
                    resources.getString(R.string.constant_sf)
                }
                loop        = resources.getString(R.string.general_loop_sf)
                conditional = resources.getString(f.shape.conditionalSF)
                mapInit     = resources.getString(f.shape.initSF)
                algInit     = resources.getString(f.texture.initSF)
                mapLoop     = resources.getString(f.shape.loopSF)
                if (f.shape.juliaMode) {
                    mapLoop = mapLoop.replace("C", "J", false)
                }
                algLoop     = resources.getString(f.texture.loopSF)
                mapFinal    = resources.getString(f.shape.finalSF)
                algFinal    = resources.getString(f.texture.finalSF)
            }
            GpuPrecision.DUAL -> {

                header      = resources.getString(R.string.header_df)
                arithmetic  = resources.getString(R.string.arithmetic_util)
                arithmetic += resources.getString(R.string.arithmetic_sf)
                arithmetic += resources.getString(R.string.arithmetic_df)
                init        = resources.getString(R.string.general_init_df)
                init += if (f.shape.juliaMode) { resources.getString(R.string.julia_df) }
                else { resources.getString(R.string.constant_df) }
                loop        = resources.getString(R.string.general_loop_df)
                conditional = resources.getString(f.shape.conditionalDF)
                mapInit     = resources.getString(f.shape.initDF)
                algInit     = resources.getString(f.texture.initDF)
                mapLoop     = resources.getString(f.shape.loopDF)
                if (f.shape.juliaMode) {
                    mapLoop = mapLoop.replace("A", "vec2(J.x, 0.0)", false)
                    mapLoop = mapLoop.replace("B", "vec2(J.y, 0.0)", false)
                }
                algLoop     = resources.getString(f.texture.loopDF)
                mapFinal    = resources.getString(f.shape.finalDF)
                algFinal    = resources.getString(f.texture.finalDF)

            }
        }

        renderShader =
                """$header
                    $arithmetic
                    void main() {
                        $init
                        $mapInit
                        $algInit
                        for (int n = 0; n < maxIter; n++) {
                            if (n == maxIter - 1) {
                                $algFinal
                                colorParams.y = 1.0;
                                break;
                            }
                            $loop
                            $mapLoop
                            $algLoop
                            $conditional {
                                $mapFinal
                                $algFinal
                                break;
                            }
                        }
                        fragmentColor = colorParams;
                    }
                    """

    }
    private fun loadShader(type: Int, shaderCode: String): Int {

        // create a vertex shader type (GL_VERTEX_SHADER)
        // or a fragment shader type (GL_FRAGMENT_SHADER)
        val shader = glCreateShader(type)

        // add the source code to the shader and compile it
        glShaderSource(shader, shaderCode)
        glCompileShader(shader)

//                val a = IntBuffer.allocate(1)
//                glGetShaderiv(shader, GL_COMPILE_STATUS, a)
//                if (a[0] == GL_FALSE) {
//                    Log.e("RENDERER", "shader compile failed")
//                }
//                else if (a[0] == GL_TRUE) {
//                    Log.e("RENDERER", "shader compile succeeded")
//                }

        return shader

    }
    private fun splitCoords(xCoords: FloatArray, yCoords: FloatArray) : List<FloatArray> {

        val xLength = xCoords[1] - xCoords[0]
        val yLength = yCoords[1] - yCoords[0]
        val xPixels = xLength / 2f * fgTexRes.x
        val yPixels = yLength / 2f * fgTexRes.y
        val maxPixelsPerChunk = when (sc.gpuPrecision) {
            GpuPrecision.SINGLE -> screenRes.x*screenRes.y/4
            GpuPrecision.DUAL -> screenRes.x*screenRes.y/8
        }
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
        juliaParamHandle     = glGetUniformLocation(renderProgram, "J")


        // get perturbation uniform locations
//                orbitHandle          =  glGetUniformLocation(  renderProgram, "orbit"       )
//                orbitIterHandle      =  glGetUniformLocation(  renderProgram, "orbitIter"   )
//                skipIterHandle       =  glGetUniformLocation(  renderProgram, "skipIter"    )
//                aHandle              =  glGetUniformLocation(  renderProgram, "A"           )
//                bHandle              =  glGetUniformLocation(  renderProgram, "B"           )
//                cHandle              =  glGetUniformLocation(  renderProgram, "C"           )
//                expShiftHandle       =  glGetUniformLocation(  renderProgram, "expShift"    )

        for (i in mapParamHandles.indices) {
            mapParamHandles[i] = glGetUniformLocation(renderProgram, "P${i + 1}")
        }
        for (i in textureParamHandles.indices) {

            textureParamHandles[i] = glGetUniformLocation(renderProgram, "Q${i + 1}")
        }

    }
    private fun deleteAllTextures() {


    }
    private fun onRenderShaderChanged() {

        Log.d("RENDERER", "render shader changed")

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

    }
    private fun onForegroundResolutionChanged() {

        Log.d("RENDERER", "resolution changed")

        val newRes = sc.resolution.scaleRes(screenRes)
        fgTexRes.x = newRes.x
        fgTexRes.y = newRes.y
        foreground1.delete()
        foreground2.delete()
        foreground1 = GLTexture(fgTexRes, GL_NEAREST, GL_RG32F, 1)
//                    textures.y = GLTexture(texRes, GL_NEAREST(), GL_RGBA, 1)
        if (sc.resolution.ordinal > Resolution.FULL.ordinal) {
            sc.sampleOnStrictTranslate = false
        }
        else {
            foreground2 = GLTexture(fgTexRes, GL_NEAREST, GL_RG32F, 2)
            sc.sampleOnStrictTranslate = true
        }
//                    textures[2] = GLTexture(texRes, GL_NEAREST(), GL_RGBA, 2)

        foreground = foreground1
        auxiliary = foreground2



        if (sc.hardwareProfile == HardwareProfile.CPU) { }



        fgResolutionChanged = false

    }
    private fun onBackgroundResolutionChanged() {

        Log.d("RENDERER", "bg res changed : ${sc.continuousRender}, ${!sc.renderBackground}")

        if (sc.continuousRender || !sc.renderBackground) {
            bgTexRes.x = 1
            bgTexRes.y = 1
        }
        else {
            val res = Resolution.EIGHTH.scaleRes(screenRes)
            bgTexRes.x = res.x
            bgTexRes.y = res.y
        }

        background.delete()
        background = GLTexture(bgTexRes, GL_NEAREST, GL_RG32F, 0)
//                    textures.x = GLTexture(intArrayOf(bgTexWidth(), bgTexHeight()), GL_NEAREST, GL_RGBA, 0)

        bgResolutionChanged = false

    }
    private fun migrate(texture: GLTexture, bitmap: Bitmap) {

        val t = System.currentTimeMillis()

        val bufferSize = texture.res.x*texture.res.y*4

        val imBuffer = if (texture == thumbnail) {
            thumbBuffer.position(0)
            thumbBuffer
        }
        else {
            ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
        }
        imBuffer.rewind()

        glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[1])
        glViewport(0, 0, texture.res.x, texture.res.y)

        val readHeight = if (texture == thumbnail) texture.res.x else texture.res.y
        val heightOffset = if (texture == thumbnail) (0.5*texture.res.y*(1.0 - 1.0/aspectRatio)).roundToInt() else 0

        // Log.d("RENDERER", "readHeight: $readHeight")

        glReadPixels(
                0, heightOffset,
                texture.res.x,
                readHeight,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                imBuffer
        )
        // Log.d("RENDERER", "glReadPixels took ${System.currentTimeMillis() - t} ms")

        // logError()

        bitmap.copyPixelsFromBuffer(imBuffer)

        // Log.d("RENDERER", "bitmap migration took ${System.currentTimeMillis() - t} ms")

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

        val appNameAbbrev = resources.getString(R.string.fe_abbrev)
        val subDirectory = Environment.DIRECTORY_PICTURES + "/" + resources.getString(R.string.app_name)
        val imageName = "${appNameAbbrev}_%4d%02d%02d_%02d%02d%02d".format(year, month + 1, day, hour, minute, second)


        // save image with unique filename
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

            // app external storage directory
            val dir = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES),
                    resources.getString(R.string.app_name
                    ))

            // create directory if not already created
            if (!dir.exists()) {
                Log.d("MAIN ACTIVITY", "Directory does not exist -- creating...")
                if (dir.mkdir()) {
                    Log.d("MAIN ACTIVITY", "Directory created")
                }
                else {
                    Log.e("MAIN ACTIVITY", "Directory could not be created")
                }
            }

            val file = File(dir, "$imageName.jpg")
            file.createNewFile()
            val fos = FileOutputStream(file)
            fos.write(bos.toByteArray())
            fos.close()

            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(file)
            scanIntent.data = contentUri
            act.sendBroadcast(scanIntent)

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
            GL_NO_ERROR -> "nothing 2 c here pls move along"
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


    private external fun iterateReferenceNative(
            z0xIn: String, z0yIn: String,
            d0xIn: DoubleArray, d0yIn: DoubleArray,
            precision: Int, maxIter: Int, refSize: Int, escapeRadius: Double,
            sp: Double, sn: Double,
            returnData: NativeReferenceReturnData) : DoubleArray

    private fun iterateReference(
            z0: Apcomplex, skipIter: IntArray, refIter: IntArray, seriesCoefs: ArrayList<Apcomplex>)
    {


        var z1 = z0
        var z = z0

        val deltaMag = 1.0  // 1.0 is image corners
        var xapprox: Double
        var yapprox: Double
        var error: Double
        val errorThreshold = 1e-12

        val delta = Double2(
                deltaMag * 0.5 * f.position.scale,
                deltaMag * 0.5 * f.position.scale * aspectRatio
        )
        var d1x = 0.0
        var d1y = 0.0
        val d0x = deltaMag * 0.5 * f.position.scale
        val d0y = deltaMag * 0.5 * f.position.scale * aspectRatio
        val d0xSqr = d0x*d0x - d0y*d0y
        val d0ySqr = 2.0*d0x*d0y
        val d0xCube = d0x*d0xSqr - d0y*d0ySqr
        val d0yCube = d0x*d0ySqr + d0y*d0xSqr

        var dx = d0x
        var dy = d0y


        var modSqrZ: Apfloat
        // var seriesAcc: Double

        val one = Apcomplex(Apfloat("1.0", sc.cpuPrecision), Apfloat("0.0", sc.cpuPrecision))
        val two = Apcomplex(Apfloat("2.0", sc.cpuPrecision), Apfloat("0.0", sc.cpuPrecision))
        var a1 = Apcomplex(
                Apfloat("1.0", sc.cpuPrecision),
                Apfloat("0.0", sc.cpuPrecision)
        )
        var b1 = Apcomplex(
                Apfloat("0.0", sc.cpuPrecision),
                Apfloat("0.0", sc.cpuPrecision)
        )
        var c1 = Apcomplex(
                Apfloat("0.0", sc.cpuPrecision),
                Apfloat("0.0", sc.cpuPrecision)
        )

        var a: Apcomplex
        var b: Apcomplex
        var c: Apcomplex



        for (i in 0..f.maxIter) {

            //if (texture != background) {
//            act.findViewById<ProgressBar>(R.id.progressBar).progress =
//                    (i.toFloat() / f.maxIter.toFloat() * 100.0f).toInt()
            //}


            // iterate z
            z = z1.sqr().add(z0)

            modSqrZ = z.modSqr()



            if (skipIter[0] == -1) {  // series approximation still accurate

                // iterate series approximation
                a = two.multiply(z1.multiply(a1)).add(one)
                b = two.multiply(z1.multiply(b1)).add(a1.sqr())
                c = two.multiply(z1.multiply(c1).add(a1.multiply(b1)))

                //seriesAcc = c.multiply(d0[0].cube()).mod().divide(b.multiply(d0[0].sqr()).mod()).toDouble()
                //Log.d("FSV", "seriesAcc := |c*d^3| / |b*d^2| == $seriesAcc")

                //Log.d("FSV", "a$i: ${a.real().toDouble()}, ${a.imag().toDouble()}")
                //Log.d("FSV", "b$i: ${b.real().toDouble()}, ${b.imag().toDouble()}")
                //Log.d("FSV", "c$i: ${c.real().toDouble()}, ${c.imag().toDouble()}")

                for (j in 0 until 1) {

                    // iterate probe points
                    //d[j] = two.multiply(z1.multiply(d1[j])).add(d1[j].sqr()).add(d0[j])
                    val x1 = z1.real().toDouble()
                    val y1 = z1.imag().toDouble()
                    dx = 2.0*(x1*d1x - y1*d1y) + d1x*d1x - d1y*d1y + d0x
                    dy = 2.0*(x1*d1y + y1*d1x + d1x*d1y) + d0y

                    // construct approximation from only initial deltas and calculated series terms
                    //approx = a.multiply(d0[j]).add(b.multiply(d0[j].sqr())).add(c.multiply(d0[j].cube()))
                    xapprox = a.real().toDouble()*d0x - a.imag().toDouble()*d0y + b.real().toDouble()*d0xSqr - b.imag().toDouble()*d0ySqr + c.real().toDouble()*d0xCube - c.imag().toDouble()*d0yCube
                    yapprox = a.real().toDouble()*d0y + a.imag().toDouble()*d0x + b.real().toDouble()*d0ySqr + b.imag().toDouble()*d0xSqr + c.real().toDouble()*d0yCube + c.imag().toDouble()*d0xCube

                    // calculate error between probe point and approximation
                    //error = (d[j].subtract(approx)).modSqr()
                    val xDif = dx - xapprox
                    val yDif = dy - yapprox
                    error = xDif*xDif + yDif*yDif
                    //Log.d("FSV", "probe $j error: $error")

                    if (error > errorThreshold) {

                        //Log.e("FSV", "i: $i -- series error: ${error.toDouble()}")
                        seriesCoefs[0] = a1
                        seriesCoefs[1] = b1
                        seriesCoefs[2] = c1
                        skipIter[0] = i
                        break

                    }

                }



                a1 = a
                b1 = b
                c1 = c

            }
            if (skipIter[0] != -1) {

                refArray[ 2*(i - skipIter[0])     ] = z1.real().toDouble()
                refArray[ 2*(i - skipIter[0]) + 1 ] = z1.imag().toDouble()

            }


            if (i == f.maxIter) {
                Log.d("RENDERER", "maxIter reached")
                refIter[0] = f.maxIter
            }
            else if (modSqrZ.toDouble() > f.bailoutRadius*f.bailoutRadius) {
                Log.d("RENDERER", "bailout at i=$i")
                refIter[0] = i
                break
            }

            z1 = z
            d1x = dx
            d1y = dy

        }


    }

    private fun rsPerturbationImage(
            width: Int, height: Int,
            z0: Apcomplex, refIter: Int, skipIter: Int, seriesCoefs: ArrayList<Apcomplex>, bgScale: Double)
    {

        val d0xOffset = f.position.xap.subtract(z0.real()).toDouble()
        val d0yOffset = f.position.yap.subtract(z0.imag()).toDouble()
        val sinRotation = sin(f.position.rotation)
        val cosRotation = cos(f.position.rotation)

        perturbationImageScript._ref = refAllocation

        perturbationImageScript._width = width.toDouble()
        perturbationImageScript._height = height.toDouble()
        perturbationImageScript._aspectRatio = aspectRatio
        perturbationImageScript._bgScale = bgScale

        perturbationImageScript._d0xOffset = d0xOffset
        perturbationImageScript._d0yOffset = d0yOffset

        perturbationImageScript._refIter = refIter.toLong()
        perturbationImageScript._skipIter = skipIter.toLong()
        perturbationImageScript._maxIter = f.maxIter.toLong()
        perturbationImageScript._escapeRadius = f.bailoutRadius

        perturbationImageScript._scale = f.position.scale / 2.0
        perturbationImageScript._sinRotation = sinRotation
        perturbationImageScript._cosRotation = cosRotation

        perturbationImageScript._alphax = seriesCoefs[0].real().toDouble()
        perturbationImageScript._alphay = seriesCoefs[0].imag().toDouble()
        perturbationImageScript._betax = seriesCoefs[1].real().toDouble()
        perturbationImageScript._betay = seriesCoefs[1].imag().toDouble()
        perturbationImageScript._gammax = seriesCoefs[2].real().toDouble()
        perturbationImageScript._gammay = seriesCoefs[2].imag().toDouble()

        //perturbationImageScript.forEach_iterate(imageOutAllocation)

    }

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
        perturbationPixelsScript._maxIter = f.maxIter.toLong()
        perturbationPixelsScript._escapeRadius = f.bailoutRadius

        perturbationPixelsScript._scale = f.position.scale / 2.0
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
    private fun perturbation() {

//                // TESTING PERTURBATION
//                var orbitArray: FloatArray = floatArrayOf()
//                val z0 = Apcomplex(f.position.xap, f.position.yap)
//                var z1 = z0
//                var z = z0
//
//                val deltaMag = 0.5  // 0.5 is image corners
//                var approx : Apcomplex
//                var error : Apfloat
//                val errorThreshold = 1e-12
//                var skipIter = -1
//
//                val d1 = MutableList<Apcomplex>(4) { Apcomplex.ZERO }   // D_{n-1}
//                val d0 = MutableList<Apcomplex>(4) { Apcomplex.ZERO }   // D_0
//                val delta = Apcomplex(
//                        Apfloat(deltaMag*0.5*f.position.scale,             sc.cpuPrecision),
//                        Apfloat(deltaMag*0.5*f.position.scale*aspectRatio, sc.cpuPrecision)
//                )
//                d0[0] = Apcomplex( delta.real(),           delta.imag()          )
//                d0[1] = Apcomplex( delta.real(),           delta.imag().negate() )
//                d0[2] = Apcomplex( delta.real().negate(),  delta.imag()          )
//                d0[3] = Apcomplex( delta.real().negate(),  delta.imag().negate() )
////                    Log.d("FSV", "D[ 1: ${d0[0]}")
////                    Log.d("FSV", "probe 2: ${d0[1]}")
////                    Log.d("FSV", "probe 3: ${d0[2]}")
////                    Log.d("FSV", "probe 4: ${d0[3]}")
//                val d = MutableList(4) { index -> d0[index] }    // D_{n}
//
//
//                Log.e("FSV", "x_ap: ${f.position.xap}")
//                Log.e("FSV", "y_ap: ${f.position.yap}")
//                // var c = Complex(f.position.x, f.position.y)
//                var modZ: Apfloat
//                var seriesAcc : Double
//
//                val ONE = Apcomplex(Apfloat("1.0", sc.cpuPrecision), Apfloat("0.0", sc.cpuPrecision))
//                val TWO = Apcomplex(Apfloat("2.0", sc.cpuPrecision), Apfloat("0.0", sc.cpuPrecision))
//                var a1 = Apcomplex(
//                        Apfloat("1.0", sc.cpuPrecision),
//                        Apfloat("0.0", sc.cpuPrecision)
//                )
//                var b1 = Apcomplex(
//                        Apfloat("0.0", sc.cpuPrecision),
//                        Apfloat("0.0", sc.cpuPrecision)
//                )
//                var c1 = Apcomplex(
//                        Apfloat("0.0", sc.cpuPrecision),
//                        Apfloat("0.0", sc.cpuPrecision)
//                )
//
//                var a : Apcomplex
//                var b : Apcomplex
//                var c : Apcomplex
//
//                var aBest = a1
//                var bBest = b1
//                var cBest = c1
//
//
//                orbitArray = FloatArray(maxOrbitSize * 2) { 0f }
//                var orbitIter = 0
//
//
//
//
//
//                for (i in 0 until f.maxIter) {
//
//                    //if (texture != background) {
//                        act.findViewById<ProgressBar>(R.id.progressBar).progress =
//                                (i.toFloat() / f.maxIter.toFloat() * 100.0f).toInt()
//                    //}
//
//                    orbitArray[2*i]     = z.real().toFloat()
//                    orbitArray[2*i + 1] = z.imag().toFloat()
//
//                    //Log.d("RENDERER", "i: $i --- (${orbitArray[2*i]}, ${orbitArray[2*i + 1]})")
//
//                    // iterate second derivative
//                    // beta = 2.0*beta*z + alpha*alpha
//
//                    // iterate first derivative
//                    // alpha = 2.0*alpha*z + Complex(1.0, 0.0)
//
//                    // iterate z
//                    z = z1.sqr().add(z0)
//
//                    modZ = z.mod()
//
//
//                    if (skipIter == -1) {
//
//                        // iterate series approximation
//                        a = TWO.multiply(z1.multiply(a1)).add(ONE)
//                        b = TWO.multiply(z1.multiply(b1)).add(a1.sqr())
//                        c = TWO.multiply(z1.multiply(c1).add(a1.multiply(b1)))
//
//                        seriesAcc = c.multiply(d0[0].cube()).mod().divide(b.multiply(d0[0].sqr()).mod()).toDouble()
//                        Log.d("FSV", "seriesAcc := |c*d^3| / |b*d^2| == $seriesAcc")
//
//                        //Log.d("FSV", "a$i: ${a.real().toDouble()}, ${a.imag().toDouble()}")
//                        //Log.d("FSV", "b$i: ${b.real().toDouble()}, ${b.imag().toDouble()}")
//                        //Log.d("FSV", "c$i: ${c.real().toDouble()}, ${c.imag().toDouble()}")
//
////                            for (j in 0 until d.size) {
////
////                                // iterate probe points
////                                d[j] = TWO.multiply(z1.multiply(d1[j])).add(d1[j].sqr()).add(d0[j])
////
////                                // construct approximation from only initial deltas and calculated series terms
////                                approx = a.multiply(d0[j]).add(b.multiply(d0[j].sqr())).add(c.multiply(d0[j].cube()))
////
////                                // calculate error between probe point and approximation
////                                error = (d[j].subtract(approx)).mod()
////                                //Log.d("FSV", "probe $j error: $error")
////
////                            }
//
//                        if (seriesAcc > 0.0005) {
//
//                            //Log.e("FSV", "i: $i -- series error: ${error.toDouble()}")
//                            aBest = a1
//                            bBest = b1
//                            cBest = c1
//                            skipIter = i
//
//                        }
//
//                        a1 = a
//                        b1 = b
//                        c1 = c
//
//
//                    }
//
//
//                    //Log.d("RENDERER", "x: $x, y: $y")
//
//                    if (i == f.maxIter - 1) {
//                        Log.d("RENDERER", "maxIter reached")
//                        orbitIter = f.maxIter
//                    }
//                    if (modZ.toDouble() > f.bailoutRadius) {
//                        Log.d("RENDERER", "bailout at i=$i")
//                        orbitIter = i
//                        break
//                    }
//
//                    z1 = z
//                    //for (j in 0 until d.size) d1[j] = d[j]
//
//                }
//
//
//
//
//
////                orbit.buffer.position(0)
////                orbit.buffer.put(orbitArray)
////                orbit.buffer.position(0)
//
//                // define texture specs
////                glTexImage2D(
////                        GL_TEXTURE_2D,      // target
////                        0,                  // mipmap level
////                        GL_RG32F,           // internal format
////                        maxOrbitSize, 1,    // texture resolution
////                        0,                  // border
////                        GL_RG,              // internalFormat
////                        GL_FLOAT,           // type
////                        orbit.buffer        // memory pointer
////                )
//
////                glUniform1i(orbitHandle, orbit.index)
////                glUniform1i(orbitIterHandle, orbitIter)
////                glUniform1i(skipIterHandle, skipIter)
//                glUniform1i(iterHandle, maxOrbitSize)
//
//                Log.d("FSV", "orbitIter: $orbitIter")
//                Log.d("FSV", "skipIter: $skipIter")
//
//                // shift series term exponents to allow for exponents outside normal range
//                val p = when {
//                    f.position.scale > 1e-5 -> 0
//                    f.position.scale < 1e-5 && f.position.scale > 1e-15 -> 10
//                    f.position.scale < 1e-15 && f.position.scale > 1e-25 -> 20
//                    f.position.scale < 1e-25 && f.position.scale > 1e-35 -> 25
//                    else -> 30
//                }
//                Log.d("FSV", "p: $p")
//                val expShift = Apfloat("1E$p", sc.cpuPrecision).toFloat()
//                aBest = Apfloat("1E-$p",     sc.cpuPrecision).multiply(aBest)
//                bBest = Apfloat("1E-${2*p}", sc.cpuPrecision).multiply(bBest)
//                cBest = Apfloat("1E-${3*p}", sc.cpuPrecision).multiply(cBest)
//
////                glUniform2fv(aHandle, 1, floatArrayOf(aBest.real().toFloat(), aBest.imag().toFloat()), 0)
////                glUniform2fv(bHandle, 1, floatArrayOf(bBest.real().toFloat(), bBest.imag().toFloat()), 0)
////                glUniform2fv(cHandle, 1, floatArrayOf(cBest.real().toFloat(), cBest.imag().toFloat()), 0)
////                glUniform1fv(expShiftHandle, 1, floatArrayOf(expShift), 0)
//
//                Log.d("FSV", "aBest: ${aBest.real().toDouble()}, ${aBest.imag().toDouble()}")
//                Log.d("FSV", "bBest: ${bBest.real().toDouble()}, ${bBest.imag().toDouble()}")
//                Log.d("FSV", "cBest: ${cBest.real().toDouble()}, ${cBest.imag().toDouble()}")
//
////                val xCoordSD = f.position.x
////                val yCoordSD = f.position.y
//
//                val xScaleSD = f.position.scale / 2.0 * expShift
//                val yScaleSD = f.position.scale*aspectRatio / 2.0 * expShift

    }


    private fun render() {

        if (renderShaderChanged) onRenderShaderChanged()
        if (fgResolutionChanged) onForegroundResolutionChanged()
        if (bgResolutionChanged) onBackgroundResolutionChanged()

        // Log.d("RENDERER", "rendering with ${renderProfile.name} profile")

        when (renderProfile) {

            RenderProfile.MANUAL -> {

                if (renderToTex) {

                    isRendering = !(sc.continuousRender || reaction == Reaction.SHAPE)

                    if (sc.renderBackground) renderToTexture(background)
                    renderToTexture(foreground)

                    renderToTex = false
                    hasTranslated = false
                    hasScaled = false
                    hasRotated = false
                    resetQuadParams()

                }

                if (sc.renderBackground) renderFromTexture(background)
                renderFromTexture(foreground)

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
                    renderThumbnails = false
                    handler.updateColorThumbnails()

                }

                renderFromTexture(background)
                renderFromTexture(foreground)



            }
            RenderProfile.TEXTURE_THUMB -> {

                if (renderThumbnails) {

                    val prevTexture = f.texture
                    val prevShowProgress = sc.showProgress
                    sc.showProgress = false

                    f.shape.compatTextures.forEach { texture ->

                        f.texture = texture
                        onRenderShaderChanged()

                        renderToTexture(thumbnail)
                        renderFromTexture(thumbnail, true)
                        migrate(thumbnail, texture.thumbnail!!)

                        handler.updateTextureThumbnail(f.shape.compatTextures.indexOf(texture))

                    }

                    sc.showProgress = prevShowProgress
                    f.texture = prevTexture
                    onRenderShaderChanged()
                    renderThumbnails = false

                }

                if (renderToTex) {
                    renderToTexture(background)
                    renderToTexture(foreground)
                    renderToTex = false
                }
                renderFromTexture(background)
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


                val xScaleSD = f.position.scale / 2.0
                val yScaleSD = f.position.scale * aspectRatio / 2.0
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

                glUniform1i(iterHandle, f.maxIter)
                glUniform1fv(bailoutHandle, 1, floatArrayOf(f.bailoutRadius), 0)
                glUniform1fv(powerHandle, 1, floatArrayOf(power), 0)
                glUniform1fv(x0Handle, 1, floatArrayOf(f.shape.z0.x.toFloat()), 0)
                glUniform1fv(y0Handle, 1, floatArrayOf(f.shape.z0.y.toFloat()), 0)

                glEnableVertexAttribArray(viewCoordsHandle)


                if (sc.sampleOnStrictTranslate
                        && transformIsStrictTranslate()
                        && texture != background
                        && texture != thumbnail) {

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

                    glViewport(0, 0, auxiliary.res.x, auxiliary.res.y)
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
                            auxiliary.id,     // texture
                            0                           // level
                    )
                    glClear(GL_COLOR_BUFFER_BIT)

                    val chunksA = splitCoords(xComplementViewCoordsA, yComplementViewCoordsA)
                    val chunksB = splitCoords(xComplementViewCoordsB, yComplementViewCoordsB)
                    val totalChunks = chunksA.size + chunksB.size
                    var chunksRendered = 0
                    for (complementViewChunkCoordsA in chunksA) {

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
                    glViewport(0, 0, auxiliary.res.x, auxiliary.res.y)

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
                    glUniform1i(textureSampleHandle, texture.index)
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
                            auxiliary.id,   // texture
                            0                               // level
                    )

                    glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)

                    glDisableVertexAttribArray(viewCoordsSampleHandle)
                    glDisableVertexAttribArray(quadCoordsSampleHandle)


                    // change auxiliary texture
                    val temp = auxiliary
                    auxiliary = foreground
                    foreground = temp


                } else {

                    //===================================================================================
                    // NOVEL RENDER -- ENTIRE TEXTURE
                    //===================================================================================

                    glViewport(0, 0, texture.res.x, texture.res.y)
                    glUniform1fv(bgScaleHandle, 1, floatArrayOf(if (texture == background) bgSize else 1f), 0)
                    glFramebufferTexture2D(
                            GL_FRAMEBUFFER,             // target
                            GL_COLOR_ATTACHMENT0,       // attachment
                            GL_TEXTURE_2D,              // texture target
                            texture.id,       // texture
                            0                           // level
                    )

                    // check framebuffer status
                    val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
                    if (status != GL_FRAMEBUFFER_COMPLETE) {
                        Log.d("FRAMEBUFFER", "$status")
                    }

                    glClear(GL_COLOR_BUFFER_BIT)

                    if (texture == background) {

                        glVertexAttribPointer(
                                viewCoordsHandle,       // index
                                3,                      // coordinates per vertex
                                GL_FLOAT,               // type
                                false,                  // normalized
                                12,                     // coordinates per vertex (3) * bytes per float (4)
                                viewBuffer              // coordinates
                        )
                        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)

                    } else {

                        val chunks = splitCoords(floatArrayOf(-1f, 1f), floatArrayOf(-1f, 1f))
                        val totalChunks = chunks.size
                        var chunksRendered = 0
                        for (viewChunkCoords in chunks) {

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

                    }

                    glDisableVertexAttribArray(viewCoordsHandle)

                }


            }
            HardwareProfile.CPU -> {

                act.findViewById<ProgressBar>(R.id.progressBar).progress = 0




                var refCalcTimeTotal = 0L
                var renderTimeTotal = 0L
                var centerCalcTimeTotal = 0L
                var samplesPerRow = 45
                var nativeRefCalcTimeTotal = 0L

                var numGlitchedPxls : Int
                var pixelsInArray : ShortArray


                val maxPixelsPerChunk = screenRes.x*screenRes.y/CPU_CHUNKS


                numGlitchedPxls = texture.res.x*texture.res.y






                val deadPixels = arrayListOf<Point>()

                val glitchedPixels = ShortArray(texture.res.x*texture.res.y*2) { index ->
                    val q = if (index % 2 == 0) (index / 2 % texture.res.x).toShort() else floor((index / 2.0) / texture.res.x).toShort()
                    if (q >= 6240) {
                        Log.e("RENDERER", "${(index / 2.0) / texture.res.x}")
                    }
                    q
                }
                var pixelsOutArray : FloatArray

                //val imArray = FloatArray(numGlitchedPixels*2)

                var z0 = Apcomplex(f.position.xap, f.position.yap)
                val refPixel = Point(texture.res.x/2, texture.res.y/2)
                val refPixels = arrayListOf(Point(refPixel))

                var largestGlitchSize : Int
                var numReferencesUsed = 0

                // val refIter = intArrayOf(0)
                // val skipIter = intArrayOf(-1)
//                val seriesCoefs = arrayListOf(
//                        Apcomplex(Apfloat("1", sc.cpuPrecision), Apfloat("0", sc.cpuPrecision)),
//                        Apcomplex(Apfloat("0", sc.cpuPrecision), Apfloat("0", sc.cpuPrecision)),
//                        Apcomplex(Apfloat("0", sc.cpuPrecision), Apfloat("0", sc.cpuPrecision))
//                )

                val sinRotation = sin(f.position.rotation)
                val cosRotation = cos(f.position.rotation)
                val bgScale = if (texture == background) 5.0 else 1.0
                val sp = if (f.position.scale < 1e-100) 1e300 else 1.0
                val sn = if (f.position.scale < 1e-100) 1e-300 else 1.0

                Log.e("RENDERER", "x0: ${z0.real()}")
                Log.e("RENDERER", "y0: ${z0.imag()}")


                // MAIN LOOP
                while (numReferencesUsed < MAX_REFERENCES) {




                    val d0xOffset = f.position.xap.subtract(z0.real()).toDouble()
                    val d0yOffset = f.position.yap.subtract(z0.imag()).toDouble()



                    // REFERENCE CALCULATION

                    val nativeReferenceStartTime = now()
                    //Log.e("RENDERER", "cpuPrecision: ${sc.cpuPrecision}")
                    val xMag = 0.5*f.position.scale
                    val yMag = 0.5*f.position.scale*aspectRatio
                    val xBasis = xMag*cosRotation - yMag*sinRotation
                    val yBasis = xMag*sinRotation + yMag*cosRotation
                    val d0xIn = doubleArrayOf(
                            -xBasis,
                            xBasis,
                            -xBasis,
                            xBasis,
                            0.5*-xBasis,
                            0.5*xBasis,
                            0.5*-xBasis,
                            0.5*xBasis
                    )
                    val d0yIn = doubleArrayOf(
                            yBasis,
                            yBasis,
                            -yBasis,
                            -yBasis,
                            0.5*yBasis,
                            0.5*yBasis,
                            0.5*-yBasis,
                            0.5*-yBasis
                    )
                    val refArrayNative = iterateReferenceNative(
                            z0.real().toString(),
                            z0.imag().toString(),
                            d0xIn,
                            d0yIn,
                            sc.cpuPrecision.toInt(),
                            f.maxIter,
                            MAX_REF_ITER,
                            f.bailoutRadius.toDouble(),
                            sp, sn,
                            refData
                    )
                    nativeRefCalcTimeTotal += now() - nativeReferenceStartTime
                    refAllocation.copyFrom(refArrayNative)



                    var numGlitchedPxlsRendered = 0
                    for (k in 0 until ceil(numGlitchedPxls.toDouble()/maxPixelsPerChunk).toInt()) {

                        val numGlitchedPxlsRemaining = numGlitchedPxls - numGlitchedPxlsRendered
                        val numChunkPxls = if (numGlitchedPxlsRemaining >= maxPixelsPerChunk) maxPixelsPerChunk else numGlitchedPxlsRemaining
                        Log.e("RENDERER", "numGlitchedPxlsRendered: $numGlitchedPxlsRendered")
                        Log.e("RENDERER", "numGlitchedPxlsRemaining: $numGlitchedPxlsRemaining")
                        Log.e("RENDERER", "numChunkPxls: $numChunkPxls")

                        pixelsInAllocation.destroy()
                        pixelsOutAllocation.destroy()

                        pixelsInArray = glitchedPixels.sliceArray(2*numGlitchedPxlsRendered until 2*(numGlitchedPxlsRendered + numChunkPxls))
                        pixelsInAllocation = Allocation.createTyped(rs, Type.createX(
                                rs,
                                Element.I16_2(rs),
                                numChunkPxls
                        ))
                        pixelsInAllocation.copyFrom(pixelsInArray)

                        pixelsOutArray = FloatArray(numChunkPxls*2)
                        pixelsOutAllocation = Allocation.createTyped(rs, Type.createX(
                                rs,
                                Element.F32_2(rs),
                                numChunkPxls
                        ))






                        // RENDERSCRIPT

                        val renderScriptStartTime = System.currentTimeMillis()
                        rsPerturbationPixels(
                                texture.res,
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
                        for (i in 0 until numChunkPxls*2 - 1 step 2) {
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
                            }
                            else { // pixel no longer glitched -- update image
                                qx = pixelsOutArray[i]
                                qy = pixelsOutArray[i + 1]
                            }
                            //imArray.set(px, py, 0, texture.res.x, 2, qx)
                            //imArray.set(px, py, 1, texture.res.x, 2, qy)
                            //Log.e("RENDERER", "p: ($px, $py)")
                            try {
                                texture.set(px, py, 0, qx)
                            }
                            catch (e: IndexOutOfBoundsException) {
                                Log.e("RENDERER", "p: ($px, $py)")
                            }
                            texture.set(px, py, 1, qy)
                        }
                        //if (numGlitchedPixels == 0) break
                        //Log.e("RENDERER", "total number of glitched pixels: $glitchedPixelsSize")

                        numGlitchedPxlsRendered += numChunkPxls



                    }




                    //val glitchedPixels = ShortArray(numGlitchedPixels * 2)
                    numGlitchedPxls = 0
                    for (i in 0 until numGlitchedPxlsRendered*2 step 2) {
                        //val px = pixelsInArray[i - 1].toInt()
                        //val py = pixelsInArray[i].toInt()
                        val px = glitchedPixels[i].toInt()
                        val py = glitchedPixels[i + 1].toInt()
                        if (texture.get(px, py, 1) == 3f) { // pixel still glitched
                            // set earliest available index to this pixel
                            glitchedPixels[numGlitchedPxls*2] = glitchedPixels[i]
                            glitchedPixels[numGlitchedPxls*2 + 1] = glitchedPixels[i + 1]
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




                    val progress = (100.0 * (1.0 - numGlitchedPxls.toDouble() / (texture.res.x * texture.res.y)).pow(10.0)).toInt()
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






                    // RESIZE IN ALLOCATION

//                    pixelsInArray = glitchedPixels.sliceArray(0 until numGlitchedPixels*2)
//                    pixelsInAllocation = Allocation.createTyped(rs, Type.createX(
//                            rs,
//                            Element.I16_2(rs),
//                            numGlitchedPxls
//                    ))
//                    pixelsInAllocation.copyFrom(pixelsInArray)









                    // GLITCH DETECTION

                    val minGlitchSize = 100
                    var glitch = arrayListOf<Point>()
                    while (samplesPerRow <= texture.res.x) {

                        //glitch = findGlitchMostPixels(imArray, texture.res.x / samplesPerRow, texture.res)
                        glitch = findGlitchMostPixels(texture, texture.res.x / samplesPerRow, texture.res)
                        //Log.e("RENDERER", "largest glitch size (sample rate= $samplesPerRow): ${glitch.size}")
                        if (glitch.size > minGlitchSize || samplesPerRow == texture.res.x) break
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
                    //imArray.set(refPixel.x, refPixel.y, 1, texture.res.x, 2, 5f)

                    val x0DiffAux = bgScale*f.position.scale*(refPixel.x.toDouble()/(texture.res.x) - 0.5)
                    val y0DiffAux = bgScale*f.position.scale*(refPixel.y.toDouble()/(texture.res.y) - 0.5)*aspectRatio

                    z0 = Apcomplex(
                            f.position.xap.add(Apfloat((x0DiffAux*cosRotation - y0DiffAux*sinRotation).toString(), sc.cpuPrecision)),
                            f.position.yap.add(Apfloat((x0DiffAux*sinRotation + y0DiffAux*cosRotation).toString(), sc.cpuPrecision))
                    )

                    numReferencesUsed++









                    // RESIZE OUT ALLOCATION

//                    if (numReferencesUsed != MAX_REFERENCES) {
//                        pixelsOutAllocation.destroy()
//                        pixelsOutAllocation = Allocation.createTyped(rs, Type.createX(
//                                rs,
//                                Element.F32_2(rs),
//                                numGlitchedPxls
//                        ))
//                        pixelsOutArray = FloatArray(numGlitchedPxls*2)
//                    }





                }




                Log.e("RENDERER", "${deadPixels.size} dead pixels")
                for (p in deadPixels) {
                    val neighborX = if (p.x + 1 == texture.res.x) p.x - 1 else p.x + 1
//                    imArray.set(p.x, p.y, 0, texture.res.x, 2,
//                            imArray.get(neighborX, p.y, 0, texture.res.x, 2))
//                    imArray.set(p.x, p.y, 1, texture.res.x, 2,
//                            imArray.get(neighborX, p.y, 1, texture.res.x, 2  ))
                    texture.set(p.x, p.y, 0, texture.get(neighborX, p.y, 0))
                    texture.set(p.x, p.y, 1, texture.get(neighborX, p.y, 1))
                }
                for (i in 0 until numGlitchedPxls step 2) {
                    val p = Point(glitchedPixels[i].toInt(), glitchedPixels[i + 1].toInt())
                    val neighborX = if (p.x + 1 == texture.res.x) p.x - 1 else p.x + 1
//                    imArray.set(p.x, p.y, 0, texture.res.x, 2,
//                            imArray.get(neighborX, p.y, 0, texture.res.x, 2))
//                    imArray.set(p.x, p.y, 1, texture.res.x, 2,
//                            imArray.get(neighborX, p.y, 1, texture.res.x, 2  ))
                    texture.set(p.x, p.y, 0, texture.get(neighborX, p.y, 0))
                    texture.set(p.x, p.y, 1, texture.get(neighborX, p.y, 1))
                }



                calcTimeTotal = refCalcTimeTotal + renderTimeTotal + centerCalcTimeTotal
                Log.d("RENDERER",
                        "[total: ${(now() - renderToTexStartTime)/1000f} sec], " +
                        "[reference: ${nativeRefCalcTimeTotal/1000f} sec], " +
                        "[renderscript: ${renderTimeTotal/1000f} sec], " +
                        "[glitch center: ${centerCalcTimeTotal/1000f} sec], " +
                        "[misc: ${(now() - renderToTexStartTime - calcTimeTotal)/1000f} sec], " +
                        "[num references: $numReferencesUsed]"
                )

                //texture.put(imArray)
                texture.update()

            }

        }

        //Log.e("RENDERER", "misc operations took ${(now() - renderToTexStartTime - calcTimeTotal)/1000f} sec")
        Log.e("RENDERER", "renderToTexture took ${(now() - renderToTexStartTime)/1000f} sec")

    }
    private fun renderFromTexture(texture: GLTexture, external: Boolean = false) {

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
        glUniform3fv(solidFillColorHandle, 1, colorToRGB(getColors(resources, listOf(f.solidFillColor))[0]), 0)
        glUniform1fv(frequencyHandle, 1, floatArrayOf(f.frequency), 0)
        glUniform1fv(phaseHandle, 1, floatArrayOf(f.phase), 0)
        glUniform1i(textureModeHandle, f.textureMode.ordinal)

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




//                if (texture == foreground) {
//
//                    val bufferSize = texture.res.x * texture.res.y * 4
//                    val imBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
//                    imBuffer.rewind()
//
//                    glReadPixels(
//                            0, 0,
//                            texture.res.x,
//                            texture.res.y,
//                            GL_RGBA,
//                            GL_UNSIGNED_BYTE,
//                            imBuffer
//                    )
//
//                    val bmp = Bitmap.createBitmap(texture.res.x, texture.res.y, Bitmap.Config.ARGB_8888)
//                    bmp.copyPixelsFromBuffer(imBuffer)
//
//                    for (i in 0 until texture.res.x step 8) {
//                        for (j in 0 until texture.res.y step 8) {
//
//                            Log.d("FSV", "i: $i, j: $j -- ${bmp.getColor(i, j).red()}")
//
//                        }
//                    }
//
//                }




        // Log.d("RENDERER", "renderFromTexture took ${System.currentTimeMillis() - t} ms")

    }

    fun onTouchEvent(e: MotionEvent?): Boolean {

        if (!isRendering) {

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

                                val prevScale = f.position.scale
                                f.position.scale(dFocalLen, doubleArrayOf(
                                        focus[0].toDouble() / screenRes.x.toDouble() - 0.5,
                                        -(focus[1].toDouble() / screenRes.x.toDouble() - 0.5*aspectRatio)))
                                checkThresholdCross(prevScale)

                                if (!sc.continuousRender) {
                                    scale(dFocalLen)
                                }
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

                            if ( renderProfile == RenderProfile.COLOR_THUMB) {

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
        }

        return false

    }


}