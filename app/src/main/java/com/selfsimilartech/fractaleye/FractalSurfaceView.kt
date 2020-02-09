package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import android.os.*
import android.provider.MediaStore
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.Log
import android.view.MotionEvent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ProgressBar
import android.widget.TextView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*




private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
private class ContextFactory : GLSurfaceView.EGLContextFactory {

    override fun createContext(egl: EGL10, display: EGLDisplay, eglConfig: EGLConfig): EGLContext {

        return egl.eglCreateContext(
                display,
                eglConfig,
                EGL10.EGL_NO_CONTEXT,
                intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE)
        ) // returns null if 3.0 is not supported

    }
    override fun destroyContext(egl: EGL10?, display: EGLDisplay?, context: EGLContext?) {
        egl?.eglDestroyContext(display, context)
    }

}

class GLTexture (
        val res         : IntArray,
        interpolation   : Int,
        internalFormat  : Int,
        val index       : Int
) {

    val id : Int
    val buffer : FloatBuffer

    init {
        // create texture id
        val b = IntBuffer.allocate(1)
        glGenTextures(1, b)
        id = b[0]

        // allocate texture memory
        val bytesPerTexel = when(internalFormat) {
                           // # of components   # of bytes per component
            GL_RGBA8 ->    4                 * 1
            GL_RGBA16F ->  4                 * 2
            GL_RGBA32F ->  4                 * 4
            GL_RG16F ->    2                 * 2
            GL_RG32F ->    2                 * 4
//            GL_RGBA -> ByteBuffer.allocateDirect(res[0] * res[1] * 4).order(ByteOrder.nativeOrder())
            else -> 0
        }
        Log.d("RENDER ROUTINE", "index: $index, bytesPerTexel: $bytesPerTexel, totalBytes: ${res[0]*res[1]*bytesPerTexel}")
        buffer = ByteBuffer.allocateDirect(res[0] * res[1] * bytesPerTexel).order(ByteOrder.nativeOrder()).asFloatBuffer()

        // bind and set texture parameters
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, id)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, interpolation)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, interpolation)

        val type = when(internalFormat) {
            GL_RGBA8 -> GL_UNSIGNED_BYTE
            GL_RGBA16F -> GL_HALF_FLOAT
            GL_RG16F -> GL_HALF_FLOAT
            GL_RGBA32F -> GL_FLOAT
            GL_RG32F -> GL_FLOAT
//            GL_RGBA -> GL_UNSIGNED_BYTE
            else -> 0
        }

        val format = when(internalFormat) {
            GL_RG16F, GL_RG32F -> GL_RG
            else -> GL_RGBA
        }

        // define texture specs
        glTexImage2D(
            GL_TEXTURE_2D,              // target
            0,                          // mipmap level
            internalFormat,             // internal format
            res[0], res[1],             // texture resolution
            0,                          // border
            format,                     // internalFormat
            type,                       // type
            buffer                      // memory pointer
        )

    }

    fun delete() { glDeleteTextures(1, intArrayOf(id), 0) }

}

enum class RenderProfile { MANUAL, SAVE, COLOR_THUMB, TEXTURE_THUMB }


@SuppressLint("ViewConstructor")
class FractalSurfaceView(
        val f : Fractal,
        val sc : SettingsConfig, 
        val act : MainActivity,
        val handler : MainActivity.ActivityHandler,
        val screenRes : IntArray
) : GLSurfaceView(act) {

    inner class FractalRenderer : Renderer {

        inner class RenderRoutine {

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

            var renderShader = ""
            val colorShader =
                """$colorHeader
                void main() {
        
                    vec3 color = solidFillColor;
                    vec4 s = texture(tex, texCoord);

                    if (textureMode == 2 || float(textureMode) == s.y) {
                        $colorIndex
                        $colorPostIndex
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
            private val renderProgram = glCreateProgram()
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


            // create perturbation handles
//            private val orbitHandle : Int
//            private val orbitIterHandle : Int
//            private val skipIterHandle: Int
//            private val aHandle : Int
//            private val bHandle : Int
//            private val cHandle : Int
//            private val expShiftHandle : Int


            // create sample program handles
            private val sampleProgram = glCreateProgram()
            private val viewCoordsSampleHandle : Int
            private val quadCoordsSampleHandle : Int
            private val textureSampleHandle    : Int
            private val yOrientSampleHandle    : Int


            // create color program handles
            private val colorProgram = glCreateProgram()
            private val viewCoordsColorHandle : Int
            private val quadCoordsColorHandle : Int
            private val yOrientColorHandle    : Int
            private val numColorsHandle       : Int
            private val textureColorHandle    : Int
            private val paletteHandle         : Int
            private val solidFillColorHandle  : Int
            private val frequencyHandle       : Int
            private val phaseHandle           : Int
            private val textureModeHandle     : Int



            private val vRenderShader : Int
            private val vSampleShader : Int

            private var fRenderShader : Int
            private var fColorShader  : Int
            private val fSampleShader : Int

            // define texture resolutions
            private val bgTexRes = if (sc.continuousRender) intArrayOf(1, 1) else Resolution.EIGHTH.scaleRes(screenRes)
            private val fgTexRes = sc.resolution.scaleRes(screenRes)
            private val thumbTexRes = Resolution.THUMB.scaleRes(screenRes)

//            private val maxOrbitSize = 8192
//            private val perturbationRes = intArrayOf(maxOrbitSize, 1)


            // generate textures
            private var background   = GLTexture(bgTexRes,    GL_NEAREST, GL_RG16F, 0)
            private var foreground1  = GLTexture(fgTexRes,    GL_NEAREST, GL_RG16F, 1)
            private var foreground2  = GLTexture(fgTexRes,    GL_NEAREST, GL_RG16F, 2)
            private val thumbnail    = GLTexture(thumbTexRes, GL_NEAREST, GL_RG16F, 3)
            private val textures = arrayOf(background, foreground1, foreground2, thumbnail)
            //private val orbit        =  GLTexture(perturbationRes, GL_NEAREST, GL_RG32F, 4)

            private val thumbBuffer = ByteBuffer.allocateDirect(thumbnail.res[0]*thumbnail.res[1]*4).order(ByteOrder.nativeOrder())


            private var foreground = foreground1
            private var auxiliary = foreground2


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


            init {

                val thumbRes = Resolution.THUMB.scaleRes(screenRes)
                Log.d("RENDER ROUTINE", "thumbnail resolution: ${thumbRes[0]} x ${thumbRes[1]}")

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
                        Resolution.HIGHEST.scaleRes(screenRes)[0],
                        Resolution.HIGHEST.scaleRes(screenRes)[1]
                )
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, rboIDs[0])




                // attach shaders and create renderProgram executables
                glAttachShader(renderProgram, vRenderShader)
                glAttachShader(renderProgram, fRenderShader)
                glLinkProgram(renderProgram)

//                val q = IntBuffer.allocate(1)
//                glGetProgramiv(renderProgram, GL_LINK_STATUS, q)
//                Log.e("RENDER ROUTINE", "${q[0] == GL_TRUE}")


                getRenderUniformLocations()

                glAttachShader(sampleProgram, vSampleShader)
                glAttachShader(sampleProgram, fSampleShader)
                glLinkProgram(sampleProgram)

                viewCoordsSampleHandle = glGetAttribLocation(  sampleProgram, "viewCoords"  )
                quadCoordsSampleHandle = glGetAttribLocation(  sampleProgram, "quadCoords"  )
                textureSampleHandle    = glGetUniformLocation( sampleProgram, "tex"         )
                yOrientSampleHandle    = glGetUniformLocation( sampleProgram, "yOrient"     )

                glAttachShader(colorProgram, vSampleShader)
                glAttachShader(colorProgram, fColorShader)
                glLinkProgram(colorProgram)

                viewCoordsColorHandle = glGetAttribLocation(   colorProgram, "viewCoords"     )
                quadCoordsColorHandle = glGetAttribLocation(   colorProgram, "quadCoords"     )
                textureColorHandle    = glGetUniformLocation(  colorProgram, "tex"            )
                yOrientColorHandle    = glGetUniformLocation(  colorProgram, "yOrient"        )
                numColorsHandle       = glGetUniformLocation(  colorProgram, "numColors"      )
                paletteHandle         = glGetUniformLocation(  colorProgram, "palette"        )
                solidFillColorHandle  = glGetUniformLocation(  colorProgram, "solidFillColor" )
                frequencyHandle       = glGetUniformLocation(  colorProgram, "frequency"      )
                phaseHandle           = glGetUniformLocation(  colorProgram, "phase"          )
                textureModeHandle     = glGetUniformLocation(  colorProgram, "textureMode"    )

            }


            private fun updateRenderShader() {

                when(sc.precision) {
                    Precision.SINGLE -> {
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
                    Precision.DUAL -> {

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
                    Precision.QUAD -> {}
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
//                    Log.e("RENDER ROUTINE", "shader compile failed")
//                }
//                else if (a[0] == GL_TRUE) {
//                    Log.e("RENDER ROUTINE", "shader compile succeeded")
//                }

                return shader

            }
            private fun splitCoords(xCoords: FloatArray, yCoords: FloatArray) : List<FloatArray> {

                val xLength = xCoords[1] - xCoords[0]
                val yLength = yCoords[1] - yCoords[0]
                val xPixels = xLength / 2f * fgTexRes[0]
                val yPixels = yLength / 2f * fgTexRes[1]
                val maxPixelsPerChunk = when (sc.precision) {
                    Precision.SINGLE -> screenRes[0]*screenRes[1]/4
                    Precision.DUAL -> screenRes[0]*screenRes[1]/8
                    else -> screenRes[0]*screenRes[1]
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
//                        Apfloat(deltaMag*0.5*f.position.scale,             AP_DIGITS),
//                        Apfloat(deltaMag*0.5*f.position.scale*aspectRatio, AP_DIGITS)
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
//                val ONE = Apcomplex(Apfloat("1.0", AP_DIGITS), Apfloat("0.0", AP_DIGITS))
//                val TWO = Apcomplex(Apfloat("2.0", AP_DIGITS), Apfloat("0.0", AP_DIGITS))
//                var a1 = Apcomplex(
//                        Apfloat("1.0", AP_DIGITS),
//                        Apfloat("0.0", AP_DIGITS)
//                )
//                var b1 = Apcomplex(
//                        Apfloat("0.0", AP_DIGITS),
//                        Apfloat("0.0", AP_DIGITS)
//                )
//                var c1 = Apcomplex(
//                        Apfloat("0.0", AP_DIGITS),
//                        Apfloat("0.0", AP_DIGITS)
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
//                    //Log.d("RENDER ROUTINE", "i: $i --- (${orbitArray[2*i]}, ${orbitArray[2*i + 1]})")
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
//                    //Log.d("RENDER ROUTINE", "x: $x, y: $y")
//
//                    if (i == f.maxIter - 1) {
//                        Log.d("RENDER ROUTINE", "maxIter reached")
//                        orbitIter = f.maxIter
//                    }
//                    if (modZ.toDouble() > f.bailoutRadius) {
//                        Log.d("RENDER ROUTINE", "bailout at i=$i")
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
//                val expShift = Apfloat("1E$p", AP_DIGITS).toFloat()
//                aBest = Apfloat("1E-$p",     AP_DIGITS).multiply(aBest)
//                bBest = Apfloat("1E-${2*p}", AP_DIGITS).multiply(bBest)
//                cBest = Apfloat("1E-${3*p}", AP_DIGITS).multiply(cBest)
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
            private fun getRenderUniformLocations() {

                viewCoordsHandle     =  glGetAttribLocation(   renderProgram, "viewCoords"  )
                iterHandle           =  glGetUniformLocation(  renderProgram, "maxIter"     )
                bailoutHandle        =  glGetUniformLocation(  renderProgram, "R"           )
                powerHandle          =  glGetUniformLocation(  renderProgram, "power"       )
                x0Handle             =  glGetUniformLocation(  renderProgram, "x0"          )
                y0Handle             =  glGetUniformLocation(  renderProgram, "y0"          )
                xScaleHandle         =  glGetUniformLocation(  renderProgram, "xScale"      )
                yScaleHandle         =  glGetUniformLocation(  renderProgram, "yScale"      )
                xCoordHandle         =  glGetUniformLocation(  renderProgram, "xCoord"      )
                yCoordHandle         =  glGetUniformLocation(  renderProgram, "yCoord"      )
                sinRotateHandle      =  glGetUniformLocation(  renderProgram, "sinRotate"   )
                cosRotateHandle      =  glGetUniformLocation(  renderProgram, "cosRotate"   )
                bgScaleHandle        =  glGetUniformLocation(  renderProgram, "bgScale"     )
                juliaParamHandle     =  glGetUniformLocation(  renderProgram, "J"           )


                // get perturbation uniform locations
//                orbitHandle          =  glGetUniformLocation(  renderProgram, "orbit"       )
//                orbitIterHandle      =  glGetUniformLocation(  renderProgram, "orbitIter"   )
//                skipIterHandle       =  glGetUniformLocation(  renderProgram, "skipIter"    )
//                aHandle              =  glGetUniformLocation(  renderProgram, "A"           )
//                bHandle              =  glGetUniformLocation(  renderProgram, "B"           )
//                cHandle              =  glGetUniformLocation(  renderProgram, "C"           )
//                expShiftHandle       =  glGetUniformLocation(  renderProgram, "expShift"    )

                for (i in mapParamHandles.indices) {
                    mapParamHandles[i] = glGetUniformLocation(renderProgram, "P${i+1}")
                }
                for (i in textureParamHandles.indices) {

                    textureParamHandles[i] = glGetUniformLocation(renderProgram, "Q${i+1}")
                }

            }
            private fun deleteAllTextures() {

                textures.forEach { it.delete() }

            }

            private fun onRenderShaderChanged() {

                Log.d("RENDER ROUTINE", "render shader changed")

                updateRenderShader()

                // load new render shader
                glDetachShader(renderProgram, fRenderShader)
                fRenderShader = loadShader(GL_FRAGMENT_SHADER, renderShader)
                glAttachShader(renderProgram, fRenderShader)
                glLinkProgram(renderProgram)

                // check program link success
                val q = IntBuffer.allocate(1)
                glGetProgramiv(renderProgram, GL_LINK_STATUS, q)
                // Log.e("RENDER ROUTINE", "program linked: ${q[0] == GL_TRUE}")

                // reassign location handles to avoid bug on Mali GPUs
                getRenderUniformLocations()

                renderShaderChanged = false

            }
            private fun onForegroundResolutionChanged() {

                Log.d("RENDER ROUTINE", "resolution changed")

                val newRes = sc.resolution.scaleRes(screenRes)
                fgTexRes[0] = newRes[0]
                fgTexRes[1] = newRes[1]
                foreground1.delete()
                foreground1 = GLTexture(fgTexRes, GL_NEAREST, GL_RG16F, 1)
//                    textures[1] = GLTexture(texRes, GL_NEAREST(), GL_RGBA, 1)
                foreground2.delete()
                if (sc.resolution.ordinal > Resolution.FULL.ordinal) {
                    sc.sampleOnStrictTranslate = false
                }
                else {
                    foreground2 = GLTexture(fgTexRes, GL_NEAREST, GL_RG16F, 2)
                    sc.sampleOnStrictTranslate = true
                }
//                    textures[2] = GLTexture(texRes, GL_NEAREST(), GL_RGBA, 2)

                foreground = foreground1
                auxiliary = foreground2

                fgResolutionChanged = false

            }
            private fun onBackgroundResolutionChanged() {

                Log.d("RENDER ROUTINE", "bg res changed : ${sc.continuousRender}, ${!sc.renderBackground}")

                if (sc.continuousRender || !sc.renderBackground) {
                    bgTexRes[0] = 1
                    bgTexRes[1] = 1
                }
                else {
                    val res = Resolution.EIGHTH.scaleRes(screenRes)
                    bgTexRes[0] = res[0]
                    bgTexRes[1] = res[1]
                }

                background.delete()
                background = GLTexture(bgTexRes, GL_NEAREST, GL_RG16F, 0)
//                    textures[0] = GLTexture(intArrayOf(bgTexWidth(), bgTexHeight()), GL_NEAREST, GL_RGBA, 0)

                bgResolutionChanged = false

            }


            fun render() {

                if (renderShaderChanged) onRenderShaderChanged()
                if (fgResolutionChanged) onForegroundResolutionChanged()
                if (bgResolutionChanged) onBackgroundResolutionChanged()

                // Log.d("RENDER ROUTINE", "rendering with ${renderProfile.name} profile")

                when (renderProfile) {

                    RenderProfile.MANUAL -> {

                        if (renderToTex) {

                            isRendering = !(sc.continuousRender || reaction == Reaction.SHAPE)

                            renderToTexture(background)
                            renderToTexture(foreground)

                            renderToTex = false
                            hasTranslated = false
                            hasScaled = false
                            hasRotated = false
                            resetQuadParams()

                        }

                        renderFromTexture(background)
                        renderFromTexture(foreground)

                    }
                    RenderProfile.SAVE -> {

                        renderFromTexture(foreground, true)
                        val bmp = Bitmap.createBitmap(
                                foreground.res[0],
                                foreground.res[1],
                                Bitmap.Config.ARGB_8888
                        )
                        migrate(foreground, bmp)
                        saveImage(bmp)
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

                        }

                        renderFromTexture(background)
                        renderFromTexture(foreground)

                        handler.updateColorThumbnails()


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

                // val t = System.currentTimeMillis()
                act.findViewById<ProgressBar>(R.id.progressBar).progress = 0

                glUseProgram(renderProgram)


                glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer

                // pass in shape params
                glUniform2fv(juliaParamHandle, 1, f.shape.params.julia.toFloatArray(), 0)
                for (i in mapParamHandles.indices) {
                    val pArray =
                            if (i < f.shape.params.size) f.shape.params.at(i).toFloatArray()
                            else floatArrayOf(0f, 0f)
                    // Log.d("RENDER ROUTINE", "passing p${i+1} in as (${pArray[0]}, ${pArray[1]})")
                    glUniform2fv(mapParamHandles[i], 1, pArray, 0)
                }

                // pass in texture params
                for (i in textureParamHandles.indices) {
                    val qArray =
                            if (i < f.texture.params.size) floatArrayOf(f.texture.params[i].q.toFloat())
                            else floatArrayOf(0f)
                    // Log.d("RENDER ROUTINE", "passing in Q${i+1} as ${qArray[0]}")
                    glUniform1fv(textureParamHandles[i], 1, qArray, 0)
                }


                val xScaleSD = f.position.scale / 2.0
                val yScaleSD = f.position.scale*aspectRatio / 2.0
                val xCoordSD = f.position.x
                val yCoordSD = f.position.y

                // pass in position params
                when (sc.precision) {
                    Precision.SINGLE -> {

                        val xScaleSF = xScaleSD.toFloat()
                        val yScaleSF = yScaleSD.toFloat()
                        val xCoordSF = xCoordSD.toFloat()
                        val yCoordSF = yCoordSD.toFloat()

                        glUniform2fv(xScaleHandle,  1,  floatArrayOf(xScaleSF,  0.0f),  0)
                        glUniform2fv(yScaleHandle,  1,  floatArrayOf(yScaleSF,  0.0f),  0)
                        glUniform2fv(xCoordHandle,  1,  floatArrayOf(xCoordSF,  0.0f),  0)
                        glUniform2fv(yCoordHandle,  1,  floatArrayOf(yCoordSF,  0.0f),  0)

                    }
                    Precision.DUAL -> {

                        val xScaleDF = splitSD(xScaleSD)
                        val yScaleDF = splitSD(yScaleSD)
                        val xCoordDF = splitSD(xCoordSD)
                        val yCoordDF = splitSD(yCoordSD)

                        glUniform2fv(xScaleHandle,  1,  xScaleDF,   0)
                        glUniform2fv(yScaleHandle,  1,  yScaleDF,   0)
                        glUniform2fv(xCoordHandle,  1,  xCoordDF,   0)
                        glUniform2fv(yCoordHandle,  1,  yCoordDF,   0)

                    }
                    Precision.QUAD -> {

//                val xScaleQF = splitDD(xScaleDD)
//                val yScaleQF = splitDD(yScaleDD)
//                val xCoordQF = splitDD(xCoordDD)
//                val yCoordQF = splitDD(yCoordDD)
//
//                glUniform4fv(xScaleHandle,  1,  xScaleQF,   0)
//                glUniform4fv(yScaleHandle,  1,  yScaleQF,   0)
//                glUniform4fv(xCoordHandle, 1,  xCoordQF,  0)
//                glUniform4fv(yCoordHandle, 1,  yCoordQF,  0)

                    }
                }
                glUniform1fv( sinRotateHandle, 1, floatArrayOf(sin(f.position.rotation).toFloat()), 0 )
                glUniform1fv( cosRotateHandle, 1, floatArrayOf(cos(f.position.rotation).toFloat()), 0 )

                // pass in other parameters

                val power = if (f.shape.hasDynamicPower) f.shape.params.at(0).u.toFloat() else f.shape.power

                glUniform1i(  iterHandle,         f.maxIter                                )
                glUniform1fv( bailoutHandle,  1,  floatArrayOf(f.bailoutRadius),         0 )
                glUniform1fv( powerHandle,    1,  floatArrayOf(power),                   0 )
                glUniform1fv( x0Handle,       1,  floatArrayOf(f.shape.z0.x.toFloat()),  0 )
                glUniform1fv( y0Handle,       1,  floatArrayOf(f.shape.z0.y.toFloat()),  0 )

                glEnableVertexAttribArray(viewCoordsHandle)


                if (sc.sampleOnStrictTranslate
                        && transformIsStrictTranslate()
                        && texture != background
                        && texture != thumbnail) {

                    val xIntersectQuadCoords : FloatArray
                    val yIntersectQuadCoords : FloatArray
                    val xIntersectViewCoords : FloatArray
                    val yIntersectViewCoords : FloatArray

                    val xComplementViewCoordsA : FloatArray
                    val yComplementViewCoordsA : FloatArray

                    val xComplementViewCoordsB = floatArrayOf(-1.0f, 1.0f)
                    val yComplementViewCoordsB : FloatArray


                    if (quadCoords[0] - quadScale > -1.0) {
                        xIntersectQuadCoords   = floatArrayOf( quadCoords[0] - quadScale,   1.0f )
                        xIntersectViewCoords   = floatArrayOf( -1.0f, -quadCoords[0] + quadScale )
                        xComplementViewCoordsA = floatArrayOf( -1.0f,  quadCoords[0] - quadScale )
                    }
                    else {
                        xIntersectQuadCoords   = floatArrayOf( -1.0f,  quadCoords[0] + quadScale )
                        xIntersectViewCoords   = floatArrayOf( -quadCoords[0] - quadScale,  1.0f )
                        xComplementViewCoordsA = floatArrayOf(  quadCoords[0] + quadScale,  1.0f )
                    }

                    if (quadCoords[1] - quadScale > -1.0) {
                        yIntersectQuadCoords   = floatArrayOf( quadCoords[1] - quadScale,   1.0f )
                        yIntersectViewCoords   = floatArrayOf( -1.0f, -quadCoords[1] + quadScale )
                        yComplementViewCoordsA = floatArrayOf( quadCoords[1] - quadScale,   1.0f )
                        yComplementViewCoordsB = floatArrayOf( -1.0f,  quadCoords[1] - quadScale )
                    }
                    else {
                        yIntersectQuadCoords   = floatArrayOf( -1.0f, quadCoords[1] + quadScale )
                        yIntersectViewCoords   = floatArrayOf( -quadCoords[1] - quadScale, 1.0f )
                        yComplementViewCoordsA = floatArrayOf( -1.0f, quadCoords[1] + quadScale )
                        yComplementViewCoordsB = floatArrayOf(  quadCoords[1] + quadScale, 1.0f )
                    }




                    //===================================================================================
                    // NOVEL RENDER -- TRANSLATION COMPLEMENT
                    //===================================================================================

                    glViewport(0, 0, auxiliary.res[0], auxiliary.res[1])
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
                    glViewport(0, 0, auxiliary.res[0], auxiliary.res[1])

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


                }
                else {

                    //===================================================================================
                    // NOVEL RENDER -- ENTIRE TEXTURE
                    //===================================================================================

                    glViewport(0, 0, texture.res[0], texture.res[1])
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

                    }
                    else {

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

                // Log.d("RENDER ROUTINE", "renderToTexture took ${System.currentTimeMillis() - t} ms")

            }
            private fun renderFromTexture(texture: GLTexture, external: Boolean = false) {

                // val t = System.currentTimeMillis()

                //======================================================================================
                // PRE-RENDER PROCESSING
                //======================================================================================

                glUseProgram(colorProgram)

                glBindFramebuffer(GL_FRAMEBUFFER, if (external) fboIDs[1] else 0)

                val viewportWidth = if (external) texture.res[0] else screenRes[0]
                val viewportHeight = if (external) texture.res[1] else screenRes[1]
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
                glUniform3fv(paletteHandle, f.palette.size, f.palette.getFlatPalette(resources), 0)
                glUniform3fv(solidFillColorHandle, 1, ColorPalette.intToFloatArray(
                        ColorPalette.getColors(resources, listOf(f.solidFillColor))[0]), 0)
                glUniform1fv(frequencyHandle, 1, floatArrayOf(f.frequency), 0)
                glUniform1fv(phaseHandle, 1, floatArrayOf(f.phase), 0)
                glUniform1i(textureModeHandle, f.textureMode.ordinal)

                glEnableVertexAttribArray(viewCoordsColorHandle)
                glEnableVertexAttribArray(quadCoordsColorHandle)



                glUniform1i(textureColorHandle, texture.index)    // use GL_TEXTURE0
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

                if (texture == background) glClear(GL_COLOR_BUFFER_BIT)
                glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)

                glDisableVertexAttribArray(viewCoordsColorHandle)
                glDisableVertexAttribArray(quadCoordsColorHandle)


                act.findViewById<ProgressBar>(R.id.progressBar).progress = 0




//                if (texture == foreground) {
//
//                    val bufferSize = texture.res[0] * texture.res[1] * 4
//                    val imBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
//                    imBuffer.rewind()
//
//                    glReadPixels(
//                            0, 0,
//                            texture.res[0],
//                            texture.res[1],
//                            GL_RGBA,
//                            GL_UNSIGNED_BYTE,
//                            imBuffer
//                    )
//
//                    val bmp = Bitmap.createBitmap(texture.res[0], texture.res[1], Bitmap.Config.ARGB_8888)
//                    bmp.copyPixelsFromBuffer(imBuffer)
//
//                    for (i in 0 until texture.res[0] step 8) {
//                        for (j in 0 until texture.res[1] step 8) {
//
//                            Log.d("FSV", "i: $i, j: $j -- ${bmp.getColor(i, j).red()}")
//
//                        }
//                    }
//
//                }




                // Log.d("RENDER ROUTINE", "renderFromTexture took ${System.currentTimeMillis() - t} ms")

            }
            private fun migrate(texture: GLTexture, bitmap: Bitmap) {

                val t = System.currentTimeMillis()

                val bufferSize = texture.res[0]*texture.res[1]*4

                val imBuffer = if (texture == thumbnail) {
                    thumbBuffer.position(0)
                    thumbBuffer
                }
                else {
                    ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
                }
                imBuffer.rewind()

                glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[1])
                glViewport(0, 0, texture.res[0], texture.res[1])

                val readHeight = if (texture == thumbnail) texture.res[0] else texture.res[1]
                val heightOffset = if (texture == thumbnail) (0.5*texture.res[1]*(1.0 - 1.0/aspectRatio)).roundToInt() else 0

                // Log.d("RENDER ROUTINE", "readHeight: $readHeight")

                glReadPixels(
                        0, heightOffset,
                        texture.res[0],
                        readHeight,
                        GL_RGBA,
                        GL_UNSIGNED_BYTE,
                        imBuffer
                )
                // Log.d("RENDER ROUTINE", "glReadPixels took ${System.currentTimeMillis() - t} ms")

                // logError()

                bitmap.copyPixelsFromBuffer(imBuffer)

                // Log.d("RENDER ROUTINE", "bitmap migration took ${System.currentTimeMillis() - t} ms")

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
                Log.e("RENDER ROUTINE", s)

                var log = glGetProgramInfoLog(renderProgram)
                Log.e("RENDER ROUTINE", log)

                log = glGetShaderInfoLog(fRenderShader)
                Log.e("RENDER ROUTINE", log)



            }


        }




        var deleteTextures = false

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

        private lateinit var rr : RenderRoutine

        fun setQuadFocus(screenPos: FloatArray) {

            // update texture quad coordinates
            // convert focus coordinates from screen space to quad space

            quadFocus[0] =   2f*(screenPos[0] / screenRes[0]) - 1f
            quadFocus[1] = -(2f*(screenPos[1] / screenRes[1]) - 1f)

            // Log.d("SURFACE VIEW", "quadFocus: (${quadFocus[0]}, ${quadFocus[1]})")

        }
        fun translate(dScreenPos: FloatArray) {

            // update texture quad coordinates
            val dQuadPos = floatArrayOf(
                    dScreenPos[0] / screenRes[0] * 2f,
                    -dScreenPos[1] / screenRes[1] * 2f
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
        fun scale(dScale: Float) {

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
        fun transformIsStrictTranslate() : Boolean {
            return hasTranslated && !hasScaled && !hasRotated
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

            rr = RenderRoutine()
            renderToTex = true
            rr.render()

            //val buttonScroll = act.findViewById<HorizontalScrollView>(R.id.buttonScroll)
            //buttonScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT)

        }
        override fun onDrawFrame(unused: GL10) {

            rr.render()
            isRendering = false

        }
        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {

        }

    }



    val r : FractalRenderer

    private val minPixelMove = 2
    private val h = Handler()
    private var overlayHidden = false
    private val longPressed = Runnable {

        Log.d("SURFACE VIEW", "wow u pressed that so long")

        // vibrate
        val vib = act.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        else {
            //deprecated in API 26
            vib.vibrate(15)
        }

        val overlay = act.findViewById<ConstraintLayout>(R.id.overlay)

        val anim : AlphaAnimation = if (!overlayHidden) AlphaAnimation(1f, 0f) else AlphaAnimation(0f, 1f)
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

    val aspectRatio = screenRes[1].toDouble() / screenRes[0]
    var reaction = Reaction.POSITION
    var renderProfile = RenderProfile.MANUAL

    private val prevFocus = floatArrayOf(0f, 0f)
    private var prevAngle = 0f
    // private val edgeRightSize = 150
    private var prevFocalLen = 1.0f
    // private val minPixelMove = 5f


    init {


        // setEGLContextClientVersion(3)              // create OpenGL ES 3.0 act
        setEGLContextFactory(ContextFactory())

        r = FractalRenderer()                       // create renderer
        setRenderer(r)                              // set renderer
        renderMode = RENDERMODE_WHEN_DIRTY          // only render on init and explicitly

    }


    fun updateSystemUI() {

        act.recalculateSurfaceViewLayout()
        if (sc.hideNavBar) {
            systemUiVisibility = (
                SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
        else {
            act.window.navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                resources.getColor(R.color.menu2, null) else resources.getColor(R.color.menu2)
            systemUiVisibility = (
                SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or SYSTEM_UI_FLAG_FULLSCREEN
            )
        }

    }
    fun checkThresholdCross(prevScale: Double) {

        if (sc.autoPrecision) {

            val prevPrecision = sc.precision

            sc.precision = when {
                f.position.scale > Precision.SINGLE.threshold -> Precision.SINGLE

                f.shape.hasDualFloat
                        && f.position.scale < Precision.SINGLE.threshold -> Precision.DUAL

                else -> sc.precision
            }

            act.findViewById<TextView>(R.id.precisionBitsText)?.text = "${sc.precision.bits}-bit"

            if (sc.precision != prevPrecision) r.renderShaderChanged = true

        }

        // display message
        val singleThresholdCrossedIn = f.position.scale < Precision.SINGLE.threshold && prevScale > Precision.SINGLE.threshold
        val singleThresholdCrossedOut = f.position.scale > Precision.SINGLE.threshold && prevScale < Precision.SINGLE.threshold
        val dualThresholdCrossed = f.position.scale < Precision.DUAL.threshold && prevScale > Precision.DUAL.threshold

        val msg = when {
            (!f.shape.hasDualFloat && singleThresholdCrossedIn) || (f.shape.hasDualFloat && dualThresholdCrossed) -> resources.getString(R.string.msg_zoom_limit)
            sc.autoPrecision && f.shape.hasDualFloat && singleThresholdCrossedIn -> "${resources.getString(R.string.msg_dual_in1)}\n${resources.getString(R.string.msg_dual_in2)}"
            sc.autoPrecision && f.shape.hasDualFloat && singleThresholdCrossedOut -> "${resources.getString(R.string.msg_dual_out1)}\n${resources.getString(R.string.msg_dual_out2)}"
            else -> null
        }

        if (msg != null) act.showMessage(msg)

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {

        if (!r.isRendering) {

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
                                r.setQuadFocus(focus)
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
                                r.setQuadFocus(focus)
                            }
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val focus = e.focus()
                            val dx: Float = focus[0] - prevFocus[0]
                            val dy: Float = focus[1] - prevFocus[1]

                            f.position.translate(dx/screenRes[0], dy/screenRes[0])
                            if (!sc.continuousRender) {
                                r.translate(floatArrayOf(dx, dy))
                            }
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            if (e.pointerCount > 1) {   // MULTI-TOUCH

                                // SCALE
                                val focalLen = e.focalLength()
                                val dFocalLen = focalLen / prevFocalLen

                                val prevScale = f.position.scale
                                f.position.scale(dFocalLen, doubleArrayOf(
                                        focus[0].toDouble() / screenRes[0].toDouble() - 0.5,
                                        -(focus[1].toDouble() / screenRes[0].toDouble() - 0.5*aspectRatio)))
                                checkThresholdCross(prevScale)

                                if (!sc.continuousRender) {
                                    r.scale(dFocalLen)
                                }
                                prevFocalLen = focalLen

                                // ROTATE
                                val angle = atan2(e.getY(0) - e.getY(1), e.getX(1) - e.getX(0))
                                val dtheta = angle - prevAngle
                                f.position.rotate(dtheta, doubleArrayOf(
                                        focus[0].toDouble() / screenRes[0].toDouble() - 0.5,
                                        -(focus[1].toDouble() / screenRes[0].toDouble() - 0.5*aspectRatio)))
                                r.rotate(dtheta)

                                prevAngle = angle

                            }

                            if (sc.continuousRender) {
                                r.renderToTex = true
                            }

                            act.updateDisplayParams()
                            requestRender()

                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            act.updatePositionEditTexts()
                            act.updateDisplayParams()
                            r.renderToTex = true
                            requestRender()
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
                                    f.phase += dx/screenRes[0]
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
                            requestRender()
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

                                r.renderThumbnails = true
                                requestRender()

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
                                    f.shape.params.active.u += f.sensitivity*dx/screenRes[0]
                                    f.shape.params.active.v -= f.sensitivity*dy/screenRes[1]
                                    prevFocus[0] = focus[0]
                                    prevFocus[1] = focus[1]
                                    r.renderToTex = true
                                    requestRender()
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
                            r.renderToTex = true
                            requestRender()
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