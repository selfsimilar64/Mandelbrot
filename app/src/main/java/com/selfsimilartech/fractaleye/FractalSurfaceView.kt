package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.widget.ProgressBar
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
            GL_TEXTURE_2D,           // target
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

enum class RenderProfile { MANUAL, SAVE, ICON }


@SuppressLint("ViewConstructor")
class FractalSurfaceView(
        val f : Fractal,
        val sc : SettingsConfig, 
        val act : MainActivity,
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
        
                    vec3 color = vec3(1.0);
                    vec4 s = texture(tex, texCoord);
        
                    if (s.y != -1.0) {
                        $colorIndex
                        $colorPostIndex
                    }
        
                    fragmentColor = vec4(color, 1.0);
        
                }
                """






            // coordinates of default view boundaries
            private val viewCoords = floatArrayOf(
                    -1.0f,   1.0f,   0.0f,     // top left
                    -1.0f,  -1.0f,   0.0f,     // bottom left
                    1.0f,  -1.0f,   0.0f,     // bottom right
                    1.0f,   1.0f,   0.0f )    // top right
            private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

            private val renderProgram = glCreateProgram()
            private var viewCoordsHandle : Int
            private var iterHandle       : Int
            private var bailoutHandle    : Int
            private var x0Handle         : Int
            private var y0Handle         : Int
            private var xScaleHandle     : Int
            private var yScaleHandle     : Int
            private var xCoordHandle     : Int
            private var yCoordHandle     : Int
            private var sinRotateHandle  : Int
            private var cosRotateHandle  : Int
            private var bgScaleHandle    : Int
            private val mapParamHandles  : IntArray
            private val textureParamHandles : IntArray


            // private val orbitHandle : Int


            private val sampleProgram = glCreateProgram()
            private val viewCoordsSampleHandle : Int
            private val quadCoordsSampleHandle : Int
            private val textureSampleHandle    : Int
            private val yOrientSampleHandle    : Int

            private val colorProgram = glCreateProgram()
            private val viewCoordsColorHandle : Int
            private val quadCoordsColorHandle : Int
            private val yOrientColorHandle    : Int
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
            private var bgTexRes = if (sc.continuousRender) intArrayOf(1, 1) else intArrayOf(
                    screenRes[0]/Resolution.LOW.scale,
                    screenRes[1]/Resolution.LOW.scale
            )
            private val fgTexRes = intArrayOf(
                    screenRes[0]/sc.resolution.scale,
                    screenRes[1]/sc.resolution.scale
            )
            private val iconTexRes = intArrayOf(
                    screenRes[0]/Resolution.ICON.scale,
                    screenRes[1]/Resolution.ICON.scale
            )
            private val perturbationRes = intArrayOf(f.maxIter, 1)


            // generate textures
            private val background   = GLTexture(bgTexRes,   GL_NEAREST, GL_RG16F, 0)
            private val foreground1  = GLTexture(fgTexRes,   GL_NEAREST, GL_RG16F, 1)
            private val foreground2  = GLTexture(fgTexRes,   GL_NEAREST, GL_RG16F, 2)
            private val icon         = GLTexture(iconTexRes, GL_NEAREST, GL_RG16F, 3)
            private val textures = arrayOf(background, foreground1, foreground2, icon)
//            private val orbit        =  GLTexture(perturbationRes, GL_NEAREST, GL_RG32F, 4)
//            GLTexture(intArrayOf(bgTexWidth(), bgTexHeight()), GL_NEAREST, GL_RGBA, 0),
//            GLTexture(f.texRes(), GL_NEAREST(), GL_RGBA, 1),
//            GLTexture(f.texRes(), GL_NEAREST(), GL_RGBA, 2)

            private var foreground = textures[foreground1.index]
            private var auxiliary = textures[foreground2.index]


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
            private val fboIDs : IntBuffer = IntBuffer.allocate(1)






            private val pboIDs : IntBuffer = IntBuffer.allocate(2)
            private var mPboIndex = 0
            private var mPboNewIndex = 1
            private var mInitRecord = true




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

//                glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS_KHR)
//                glDebugMessageCallbackKHR { source, type, id, severity, message ->
//                    Log.e("RENDER ROUTINE", message)
//                }

                // load all vertex and fragment shader code
                var s = act.resources.openRawResource(R.raw.vert_render)
                val vRenderCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()

                s = act.resources.openRawResource(R.raw.vert_sample)
                val vSampleCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()

                s = act.resources.openRawResource(R.raw.precision_test)
                val precisionCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()

                s = act.resources.openRawResource(R.raw.sample)
                val fSampleCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()


//                s = act.resources.openRawResource(R.raw.perturbation)
//                val perturbationCode = Scanner(s).useDelimiter("\\Z").next()
//                s.close()


                // create and compile shaders
                vRenderShader = loadShader(GL_VERTEX_SHADER, vRenderCode)
                vSampleShader = loadShader(GL_VERTEX_SHADER, vSampleCode)

                checkThresholdCross(f.position.scale)
                updateRenderShader()
                fRenderShader = loadShader(GL_FRAGMENT_SHADER, renderShader)
//                fRenderShader = loadShader(GL_FRAGMENT_SHADER, perturbationCode)

                fSampleShader = loadShader(GL_FRAGMENT_SHADER, fSampleCode)
                fColorShader  = loadShader(GL_FRAGMENT_SHADER, colorShader)


                glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

                // generate texture and framebuffer objects
                glGenFramebuffers(1, fboIDs)




                glGenBuffers(2, pboIDs)

                val pboSize = textures[icon.index].res[0]*textures[icon.index].res[1]*4
                glBindBuffer(GL_PIXEL_PACK_BUFFER, pboIDs.get(0))
                glBufferData(GL_PIXEL_PACK_BUFFER, pboSize, null, GL_STATIC_READ)

                glBindBuffer(GL_PIXEL_PACK_BUFFER, pboIDs.get(1))
                glBufferData(GL_PIXEL_PACK_BUFFER, pboSize, null, GL_STATIC_READ)

                glBindBuffer(GL_PIXEL_PACK_BUFFER, 0)





                // attach shaders and create renderProgram executables
                glAttachShader(renderProgram, vRenderShader)
                glAttachShader(renderProgram, fRenderShader)
                glLinkProgram(renderProgram)

//                val q = IntBuffer.allocate(1)
//                glGetProgramiv(renderProgram, GL_LINK_STATUS, q)
//                Log.e("RENDER ROUTINE", "${q[0] == GL_TRUE}")

                viewCoordsHandle     =  glGetAttribLocation(   renderProgram, "viewCoords"  )
                iterHandle           =  glGetUniformLocation(  renderProgram, "maxIter"     )
                bailoutHandle        =  glGetUniformLocation(  renderProgram, "R"           )
                x0Handle          =  glGetUniformLocation(  renderProgram, "x0"          )
                y0Handle          =  glGetUniformLocation(  renderProgram, "y0"          )
                xScaleHandle         =  glGetUniformLocation(  renderProgram, "xScale"      )
                yScaleHandle         =  glGetUniformLocation(  renderProgram, "yScale"      )
                xCoordHandle        =   glGetUniformLocation(  renderProgram, "xCoord"      )
                yCoordHandle        =   glGetUniformLocation(  renderProgram, "yCoord"      )
                sinRotateHandle      =  glGetUniformLocation(  renderProgram, "sinRotate"   )
                cosRotateHandle      =  glGetUniformLocation(  renderProgram, "cosRotate"   )
                bgScaleHandle        =  glGetUniformLocation(  renderProgram, "bgScale"     )

                mapParamHandles      =  IntArray(NUM_MAP_PARAMS) { i: Int ->
                    glGetUniformLocation(renderProgram, "P${i+1}")
                }
                textureParamHandles  =  IntArray(NUM_TEXTURE_PARAMS) { i: Int ->
                    glGetUniformLocation(renderProgram, "Q${i+1}")
                }

                // orbitHandle          =  glGetUniformLocation(  renderProgram, "orbit"       )

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

                viewCoordsColorHandle = glGetAttribLocation(   colorProgram, "viewCoords"   )
                quadCoordsColorHandle = glGetAttribLocation(   colorProgram, "quadCoords"   )
                textureColorHandle    = glGetUniformLocation(  colorProgram, "tex"          )
                yOrientColorHandle    = glGetUniformLocation(  colorProgram, "yOrient"      )
                numColorsHandle       = glGetUniformLocation(  colorProgram, "numColors"    )
                paletteHandle         = glGetUniformLocation(  colorProgram, "palette"      )
                frequencyHandle       = glGetUniformLocation(  colorProgram, "frequency"    )
                phaseHandle           = glGetUniformLocation(  colorProgram, "phase"        )

            }

            private fun bindPixelBuffer(): Bitmap? {

                val bufferSize = icon.res[0]*icon.res[1]*4
                val imBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
                glBindBuffer(GL_PIXEL_PACK_BUFFER, pboIDs.get(mPboIndex))
                glReadPixels(
                        0, 0,
                        icon.res[0],
                        icon.res[1],
                        GL_RGBA,
                        GL_UNSIGNED_BYTE,
                        imBuffer
                )

                if (mInitRecord) {
                    unbindPixelBuffer()
                    mInitRecord = false
                    return null
                }

                glBindBuffer(GL_PIXEL_PACK_BUFFER, pboIDs.get(mPboNewIndex))
                val byteBuffer = glMapBufferRange(GL_PIXEL_PACK_BUFFER, 0, bufferSize, GL_MAP_READ_BIT) as ByteBuffer
                glUnmapBuffer(GL_PIXEL_PACK_BUFFER)
                unbindPixelBuffer()

                val bmp = Bitmap.createBitmap(icon.res[0], icon.res[1], Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(byteBuffer)

                return bmp

            }
            private fun unbindPixelBuffer() {
                glBindBuffer(GL_PIXEL_PACK_BUFFER, 0)
                mPboIndex = (mPboIndex + 1) % 2
                mPboNewIndex = (mPboNewIndex + 1) % 2
            }


            private fun updateRenderShader() {

                when(sc.precision) {
                    Precision.SINGLE -> {
                        header      = resources.getString(R.string.header_sf)
                        arithmetic  = resources.getString(R.string.arithmetic_sf)
                        init        = resources.getString(R.string.general_init_sf)
                        init += if (f.juliaMode) {
                            resources.getString(R.string.julia_sf)
                        } else {
                            resources.getString(R.string.constant_sf)
                        }
                        loop        = resources.getString(R.string.general_loop_sf)
                        conditional = resources.getString(f.shape.conditionalSF)
                        mapInit     = resources.getString(f.shape.initSF)
                        algInit     = resources.getString(f.texture.initSF)
                        mapLoop     = resources.getString(f.shape.loopSF)
                        if (f.juliaMode && !f.shape.juliaMode) {
                            mapLoop = mapLoop.replace("C", "P${f.numParamsInUse}", false)
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
                        init += if (f.juliaMode) { resources.getString(R.string.julia_df) }
                        else { resources.getString(R.string.constant_df) }
                        loop        = resources.getString(R.string.general_loop_df)
                        conditional = resources.getString(f.shape.conditionalDF)
                        mapInit     = resources.getString(f.shape.initDF)
                        algInit     = resources.getString(f.texture.initDF)
                        mapLoop     = resources.getString(f.shape.loopDF)
                        if (f.juliaMode && !f.shape.juliaMode) {
                            mapLoop = mapLoop.replace("A", "vec2(P${f.shape.numParams + 1}.x, 0.0)", false)
                            mapLoop = mapLoop.replace("B", "vec2(P${f.shape.numParams + 1}.y, 0.0)", false)
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
                                colorParams.y = -1.0;
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
                val xPixels = xLength / 2.0f * fgTexRes[0]
                val yPixels = yLength / 2.0f * fgTexRes[1]
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

                // TESTING PERTURBATION
                val numProbes = 25
                var orbitArray : FloatArray = floatArrayOf()
                var z : Complex
                var c = Complex(f.position.x, f.position.y)
                var modZ = 0.0
                var alpha : Complex
                var beta : Complex
                val iterations = IntArray(numProbes)
                val distances = DoubleArray(numProbes)
                val gradientAngles = DoubleArray(numProbes)

                var maxIterFound = false



                for (j in 0 until numProbes) {

                    Log.d("RENDER ROUTINE", "j: $j")

                    orbitArray = FloatArray(f.maxIter*2) { 0f }
                    z = Complex(0.0, 0.0)
                    alpha = Complex(0.0, 0.0)
                    beta = Complex(0.0, 0.0)

                    for (i in 0 until f.maxIter) {

                        orbitArray[2 * i] = z.x.toFloat()
                        orbitArray[2 * i + 1] = z.y.toFloat()

                        // iterate second derivative
                        beta = 2.0*beta*z + alpha*alpha

                        // iterate first derivative
                        alpha = 2.0*alpha*z + Complex(1.0, 0.0)

                        // iterate z
                        z = z*z + c
                        modZ = z.mod()
                        //Log.d("RENDER ROUTINE", "x: $x, y: $y")

                        if (i == f.maxIter - 1) {
                            Log.d("RENDER ROUTINE", "maxIter !!!!!")
                            maxIterFound = true
                            iterations[j] = i
                        }
                        if (modZ > f.bailoutRadius) {
                            Log.d("RENDER ROUTINE", "i: $i")
                            iterations[j] = i
                            break
                        }

                    }

                    if (maxIterFound) {
                        break
                    }


                    var grad = z/alpha
                    grad /= grad.mod()
                    grad = -grad
                    distances[j] = modZ*log(modZ, Math.E)/alpha.mod()
                    gradientAngles[j] = atan2(grad.y, grad.x)

                    c += 1.5*distances[j]*grad
                    Log.d("RENDER ROUTINE", "c_prime: $c")


                }

                var s = ""
                gradientAngles.forEach { s += "$it   " }
                Log.d("RENDER ROUTINE", "\ngradientAngles: $s")

                s = ""
                iterations.forEach { s += "$it   " }
                Log.d("RENDER ROUTINE", "\niterations: $s")

//                orbit.buffer.position(0)
//                orbit.buffer.put(orbitArray)
//                orbit.buffer.position(0)

                // define texture specs
//                glTexImage2D(
//                        GL_TEXTURE_2D,      // target
//                        0,                  // mipmap level
//                        GL_RG32F,           // internal format
//                        f.maxIter, 1,       // texture resolution
//                        0,                  // border
//                        GL_RG,              // internalFormat
//                        GL_FLOAT,           // type
//                        orbit.buffer        // memory pointer
//                )

                // glUniform1i(orbitHandle, perturbationIndex)
                val xCoordSD = f.position.x - c.x
                val yCoordSD = f.position.y - c.y

            }
            fun render() {

                if (renderShaderChanged) {

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
                    viewCoordsHandle     =  glGetAttribLocation(   renderProgram, "viewCoords"  )
                    iterHandle           =  glGetUniformLocation(  renderProgram, "maxIter"     )
                    bailoutHandle        =  glGetUniformLocation(  renderProgram, "R"           )
                    x0Handle          =  glGetUniformLocation(  renderProgram, "x0"          )
                    y0Handle          =  glGetUniformLocation(  renderProgram, "y0"          )
                    xScaleHandle         =  glGetUniformLocation(  renderProgram, "xScale"      )
                    yScaleHandle         =  glGetUniformLocation(  renderProgram, "yScale"      )
                    xCoordHandle        =  glGetUniformLocation(  renderProgram, "xCoord"     )
                    yCoordHandle        =  glGetUniformLocation(  renderProgram, "yCoord"     )
                    sinRotateHandle      =  glGetUniformLocation(  renderProgram, "sinRotate"   )
                    cosRotateHandle      =  glGetUniformLocation(  renderProgram, "cosRotate"   )
                    bgScaleHandle        =  glGetUniformLocation(  renderProgram, "bgScale"     )
                    for (i in mapParamHandles.indices) {
                        mapParamHandles[i] = glGetUniformLocation(renderProgram, "P${i+1}")
                    }
                    for (i in textureParamHandles.indices) {
                        textureParamHandles[i] = glGetUniformLocation(renderProgram, "Q${i+1}")
                    }

                    renderShaderChanged = false

                }
                if (resolutionChanged) {

                    fgTexRes[0] = screenRes[0]/sc.resolution.scale
                    fgTexRes[1] = screenRes[1]/sc.resolution.scale
                    textures[foreground1.index].delete()
                    textures[foreground1.index] = GLTexture(fgTexRes, GL_NEAREST, GL_RGBA16F, 1)
//                    textures[1] = GLTexture(texRes, GL_NEAREST(), GL_RGBA, 1)
                    textures[foreground2.index].delete()
                    textures[foreground2.index] = GLTexture(fgTexRes, GL_NEAREST, GL_RGBA16F, 2)
//                    textures[2] = GLTexture(texRes, GL_NEAREST(), GL_RGBA, 2)

                    resolutionChanged = false

                }
                if (renderProfileChanged) {

                    if (sc.continuousRender) {
                        bgTexRes[0] = 1
                        bgTexRes[1] = 1
                    }
                    else {
                        bgTexRes[0] = screenRes[0]/Resolution.LOW.scale
                        bgTexRes[1] = screenRes[1]/Resolution.LOW.scale
                    }

                    textures[background.index].delete()
                    textures[background.index] = GLTexture(bgTexRes, GL_NEAREST, GL_RGBA16F, 0)
//                    textures[0] = GLTexture(intArrayOf(bgTexWidth(), bgTexHeight()), GL_NEAREST, GL_RGBA, 0)

                    renderProfileChanged = false

                }

                when (renderProfile) {

                    RenderProfile.MANUAL -> {

                        if (renderToTex) {

                            isRendering = !(sc.continuousRender || reaction == Reaction.SHAPE)

                            renderToTexture(background.index)
                            renderToTexture(foreground.index)

                            renderToTex = false
                            hasTranslated = false
                            hasScaled = false
                            hasRotated = false
                            resetQuadParams()

                        }

                        renderFromTexture(background.index)
                        renderFromTexture(foreground.index)

                    }
                    RenderProfile.SAVE -> {

                        renderFromTexture(foreground.index, false, -1f)
                        val im = migrateToBitmap(foreground)
                        saveImage(im)
                        renderFromTexture(foreground.index)
                        renderProfile = RenderProfile.MANUAL

                    }
                    RenderProfile.ICON -> {

                        val prevPalette = f.palette
                        val prevShowProgress = sc.showProgress
                        sc.showProgress = false
                        renderToTexture(icon.index)
                        sc.showProgress = prevShowProgress

                        ColorPalette.all.forEach { palette ->
                            f.palette = palette
                            Log.d("RENDER ROUTINE", "palette changed to ${f.palette.name}")
                            renderFromTexture(icon.index, false, -1f)
                            f.palette.icon = migrateToBitmap(icon)
                        }

                        f.palette = prevPalette
                        renderFromTexture(background.index)
                        renderFromTexture(foreground.index)
                        renderProfile = RenderProfile.MANUAL

                        // renderFromTexture(background)
                        // renderFromTexture(foreground)
                        // profile = RenderProfile.MANUAL

                    }

                }

            }
            private fun renderToTexture(texture: Int) {

                act.findViewById<ProgressBar>(R.id.progressBar).progress = 0

                glUseProgram(renderProgram)

                // perturbation()
                glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer

                // pass in shape params
                for (i in mapParamHandles.indices) {
                    val pArray =
                            if (i < f.numParamsInUse) floatArrayOf(f.shape.params[i].u.toFloat(), f.shape.params[i].v.toFloat())
                            else floatArrayOf(0f, 0f)
                    // Log.d("RENDER ROUTINE", "passing p${i+1} in as (${pArray[0]}, ${pArray[1]})")
                    glUniform2fv(mapParamHandles[i], 1, pArray, 0)
                }

                // pass in texture params
                for (i in textureParamHandles.indices) {
                    // Log.d("RENDER ROUTINE", "passing q${i + 1} in as ${f.params["q${i+1}"]}")
                    glUniform1fv(textureParamHandles[i], 1, floatArrayOf(f.texture.params[i].t.toFloat()), 0)
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
                glUniform1i(  iterHandle,         f.maxIter                               )
                glUniform1fv( bailoutHandle,  1,  floatArrayOf(f.bailoutRadius),        0 )
                glUniform1fv( x0Handle,       1,  floatArrayOf(f.shape.z0.x.toFloat()),  0 )
                glUniform1fv( y0Handle,       1,  floatArrayOf(f.shape.z0.y.toFloat()),  0 )

                glEnableVertexAttribArray(viewCoordsHandle)


                if (strictTranslate() && texture != background.index && renderProfile == RenderProfile.MANUAL) {

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

                    glViewport(0, 0, textures[auxiliary.index].res[0], textures[auxiliary.index].res[1])
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
                            textures[auxiliary.index].id,     // texture
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
                    glViewport(0, 0, textures[auxiliary.index].res[0], textures[auxiliary.index].res[1])

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
                    glUniform1i(textureSampleHandle, texture)
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
                            GL_FRAMEBUFFER,             // target
                            GL_COLOR_ATTACHMENT0,       // attachment
                            GL_TEXTURE_2D,              // texture target
                            textures[auxiliary.index].id,     // texture
                            0                           // level
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

                    glViewport(0, 0, textures[texture].res[0], textures[texture].res[1])
                    glUniform1fv(bgScaleHandle, 1, floatArrayOf(if (texture == background.index) bgSize else 1f), 0)
                    glFramebufferTexture2D(
                            GL_FRAMEBUFFER,             // target
                            GL_COLOR_ATTACHMENT0,       // attachment
                            GL_TEXTURE_2D,              // texture target
                            textures[texture].id,       // texture
                            0                           // level
                    )

                    // check framebuffer status
                    val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
                    if (status != GL_FRAMEBUFFER_COMPLETE) {
                        Log.d("FRAMEBUFFER", "$status")
                    }

                    glClear(GL_COLOR_BUFFER_BIT)

                    if (texture == background.index) {

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

                        val chunks = splitCoords(floatArrayOf(-1.0f, 1.0f), floatArrayOf(-1.0f, 1.0f))
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
                            if (!sc.continuousRender && sc.showProgress) {
                                act.findViewById<ProgressBar>(R.id.progressBar).progress =
                                        (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                            }

                        }

                    }

                    glDisableVertexAttribArray(viewCoordsHandle)

                }

            }
            private fun renderFromTexture(texture: Int, fitToScreen: Boolean = true, yOrient: Float = 1f) {

                //======================================================================================
                // PRE-RENDER PROCESSING
                //======================================================================================

                glUseProgram(colorProgram)

                glBindFramebuffer(GL_FRAMEBUFFER, 0)
                if (fitToScreen) {
                    glViewport(0, 0, screenRes[0], screenRes[1])
                }
                else {
                    glViewport(0, 0, textures[texture].res[0], textures[texture].res[1])
                }

                val aspect = aspectRatio.toFloat()
                val buffer : FloatBuffer

                if (texture == background.index) {

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
                glUniform1fv(frequencyHandle, 1, floatArrayOf(f.frequency), 0)
                glUniform1fv(phaseHandle, 1, floatArrayOf(f.phase), 0)

                glEnableVertexAttribArray(viewCoordsColorHandle)
                glEnableVertexAttribArray(quadCoordsColorHandle)



                glUniform1i(textureColorHandle, texture)    // use GL_TEXTURE0
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

                if (texture == background.index) glClear(GL_COLOR_BUFFER_BIT)
                glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)

                glDisableVertexAttribArray(viewCoordsColorHandle)
                glDisableVertexAttribArray(quadCoordsColorHandle)


                act.findViewById<ProgressBar>(R.id.progressBar).progress = 0

            }
            private fun renderToTexture2(current: Int) {

                if (renderShaderChanged) {

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
                    viewCoordsHandle     =  glGetAttribLocation(   renderProgram, "viewCoords"  )
                    iterHandle           =  glGetUniformLocation(  renderProgram, "maxIter"     )
                    bailoutHandle        =  glGetUniformLocation(  renderProgram, "R"           )
                    x0Handle          =  glGetUniformLocation(  renderProgram, "x0"          )
                    y0Handle          =  glGetUniformLocation(  renderProgram, "y0"          )
                    xScaleHandle         =  glGetUniformLocation(  renderProgram, "xScale"      )
                    yScaleHandle         =  glGetUniformLocation(  renderProgram, "yScale"      )
                    xCoordHandle        =  glGetUniformLocation(  renderProgram, "xCoord"     )
                    yCoordHandle        =  glGetUniformLocation(  renderProgram, "yCoord"     )
                    sinRotateHandle      =  glGetUniformLocation(  renderProgram, "sinRotate"   )
                    cosRotateHandle      =  glGetUniformLocation(  renderProgram, "cosRotate"   )
                    bgScaleHandle        =  glGetUniformLocation(  renderProgram, "bgScale"     )
                    for (i in mapParamHandles.indices) {
                        mapParamHandles[i] = glGetUniformLocation(renderProgram, "P${i+1}")
                    }
                    for (i in textureParamHandles.indices) {
                        textureParamHandles[i] = glGetUniformLocation(renderProgram, "Q${i+1}")
                    }

                    renderShaderChanged = false

                }
                if (resolutionChanged) {

                    fgTexRes[0] = screenRes[0]/sc.resolution.scale
                    fgTexRes[1] = screenRes[1]/sc.resolution.scale
                    textures[foreground1.index].delete()
                    textures[foreground1.index] = GLTexture(fgTexRes, GL_NEAREST, GL_RGBA16F, 1)
//                    textures[1] = GLTexture(texRes, GL_NEAREST(), GL_RGBA, 1)
                    textures[foreground2.index].delete()
                    textures[foreground2.index] = GLTexture(fgTexRes, GL_NEAREST, GL_RGBA16F, 2)
//                    textures[2] = GLTexture(texRes, GL_NEAREST(), GL_RGBA, 2)

                    resolutionChanged = false

                }
                if (renderProfileChanged) {

                    if (sc.continuousRender) {
                        bgTexRes[0] = 1
                        bgTexRes[1] = 1
                    }
                    else {
                        bgTexRes[0] = screenRes[0]/Resolution.LOW.scale
                        bgTexRes[1] = screenRes[1]/Resolution.LOW.scale
                    }

                    textures[background.index].delete()
                    textures[background.index] = GLTexture(bgTexRes, GL_NEAREST, GL_RGBA16F, 0)
//                    textures[0] = GLTexture(intArrayOf(bgTexWidth(), bgTexHeight()), GL_NEAREST, GL_RGBA, 0)

                    renderProfileChanged = false

                }

                act.findViewById<ProgressBar>(R.id.progressBar).progress = 0

                glUseProgram(renderProgram)

                // perturbation()
                glBindFramebuffer(GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer

                // pass in shape params
                for (i in mapParamHandles.indices) {
                    val pArray =
                            if (i < f.numParamsInUse) floatArrayOf(f.shape.params[i].u.toFloat(), f.shape.params[i].v.toFloat())
                            else floatArrayOf(0f, 0f)
                    // Log.d("RENDER ROUTINE", "passing p${i+1} in as (${pArray[0]}, ${pArray[1]})")
                    glUniform2fv(mapParamHandles[i], 1, pArray, 0)
                }

                // pass in texture params
                for (i in textureParamHandles.indices) {
                    // Log.d("RENDER ROUTINE", "passing q${i + 1} in as ${f.params["q${i+1}"]}")
                    glUniform1fv(textureParamHandles[i], 1, floatArrayOf(f.texture.params[i].t.toFloat()), 0)
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
                glUniform1i(  iterHandle,         f.maxIter                               )
                glUniform1fv( bailoutHandle,  1,  floatArrayOf(f.bailoutRadius),        0 )
                glUniform1fv( x0Handle,       1,  floatArrayOf(f.shape.z0.x.toFloat()),  0 )
                glUniform1fv( y0Handle,       1,  floatArrayOf(f.shape.z0.y.toFloat()),  0 )

                glEnableVertexAttribArray(viewCoordsHandle)


                //======================================================================================
                // RENDER BACKGROUND
                //======================================================================================

                // current = background

                glViewport(0, 0, textures[current].res[0], textures[current].res[1])
                glUniform1fv(bgScaleHandle, 1, floatArrayOf(if (current == background.index) bgSize else 1f), 0)
                glVertexAttribPointer(
                        viewCoordsHandle,       // index
                        3,                      // coordinates per vertex
                        GL_FLOAT,               // type
                        false,                  // normalized
                        12,                     // coordinates per vertex (3) * bytes per float (4)
                        viewBuffer              // coordinates
                )
                glFramebufferTexture2D(
                        GL_FRAMEBUFFER,              // target
                        GL_COLOR_ATTACHMENT0,        // attachment
                        GL_TEXTURE_2D,               // texture target
                        textures[current].id,     // texture
                        0                            // level
                )

                glClear(GL_COLOR_BUFFER_BIT)
                glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)


                //======================================================================================
                // RENDER FOREGROUND
                //======================================================================================

                // current = if (auxForeground == foreground1) foreground2 else foreground1

                if (strictTranslate()) {

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

                    glViewport(0, 0, textures[auxiliary.index].res[0], textures[auxiliary.index].res[1])
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
                            textures[auxiliary.index].id,     // texture
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
                        if (!sc.continuousRender) {
                            act.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }
                    for (complementViewChunkCoordsB in chunksB) {

                        viewChunkBuffer.put(complementViewChunkCoordsB).position(0)
                        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)
                        glFinish()
                        chunksRendered++
                        if (!sc.continuousRender) {
                            act.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }





                    //===================================================================================
                    // SAMPLE -- TRANSLATION INTERSECTION
                    //===================================================================================

                    glUseProgram(sampleProgram)
                    glViewport(0, 0, textures[auxiliary.index].res[0], textures[auxiliary.index].res[1])

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
                    glUniform1i(textureSampleHandle, current)
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
                            GL_FRAMEBUFFER,             // target
                            GL_COLOR_ATTACHMENT0,       // attachment
                            GL_TEXTURE_2D,              // texture target
                            textures[auxiliary.index].id,     // texture
                            0                           // level
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

                    glViewport(0, 0, textures[current].res[0], textures[current].res[1])
                    glUniform1fv(bgScaleHandle, 1, floatArrayOf(1f), 0)
                    glFramebufferTexture2D(
                            GL_FRAMEBUFFER,             // target
                            GL_COLOR_ATTACHMENT0,       // attachment
                            GL_TEXTURE_2D,              // texture target
                            textures[current].id,       // texture
                            0                           // level
                    )

                    // check framebuffer status
                    val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
                    if (status != GL_FRAMEBUFFER_COMPLETE) {
                        Log.d("FRAMEBUFFER", "$status")
                    }

                    glClear(GL_COLOR_BUFFER_BIT)


                    if (renderBackground) {

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

                        val chunks = splitCoords(floatArrayOf(-1.0f, 1.0f), floatArrayOf(-1.0f, 1.0f))
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
                            if (!sc.continuousRender) {
                                act.findViewById<ProgressBar>(R.id.progressBar).progress =
                                        (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                            }

                        }

                    }

                    glDisableVertexAttribArray(viewCoordsHandle)

                }



            }
            private fun renderFromTexture2(current: Int, fitToScreen: Boolean = true, yOrient: Float = 1f) {

                //======================================================================================
                // PRE-RENDER PROCESSING
                //======================================================================================

                glUseProgram(colorProgram)

                glBindFramebuffer(GL_FRAMEBUFFER, 0)
                if (fitToScreen) {
                    glViewport(0, 0, screenRes[0], screenRes[1])
                }
                else {
                    glViewport(0, 0, textures[current].res[0], textures[current].res[1])
                }


                val aspect = aspectRatio.toFloat()
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

                glUniform1fv(yOrientColorHandle, 1, floatArrayOf(yOrient), 0)
                glUniform1i(numColorsHandle, f.palette.size)
                glUniform3fv(paletteHandle, f.palette.size, f.palette.getFlatPalette(resources), 0)
                glUniform1fv(frequencyHandle, 1, floatArrayOf(f.frequency), 0)
                glUniform1fv(phaseHandle, 1, floatArrayOf(f.phase), 0)

                glEnableVertexAttribArray(viewCoordsColorHandle)
                glEnableVertexAttribArray(quadCoordsColorHandle)




                //======================================================================================
                // RENDER BACKGROUND
                //======================================================================================

                // current = background

                glUniform1i(textureColorHandle, current)    // use GL_TEXTURE0
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
                        bgQuadBuffer                // coordinates
                )

                glClear(GL_COLOR_BUFFER_BIT)
                glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)




                //======================================================================================
                // RENDER HIGH-RES
                //======================================================================================

                // current = if (auxForeground == foreground1) foreground2 else foreground1

                glUniform1i(textureColorHandle, current)
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
                        quadBuffer                  // coordinates
                )

                glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)

                glDisableVertexAttribArray(viewCoordsColorHandle)
                glDisableVertexAttribArray(quadCoordsColorHandle)


                act.findViewById<ProgressBar>(R.id.progressBar).progress = 0

            }
            fun migrateToBitmap(texture: GLTexture) : Bitmap {
                val bufferSize = texture.res[0]*texture.res[1]*4
                val imBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())

                Log.d("RENDER ROUTINE", "migrating bitmap with ${f.palette.name} palette")

                val t = System.currentTimeMillis()
                glReadPixels(
                        0, 0,
                        texture.res[0],
                        texture.res[1],
                        GL_RGBA,
                        GL_UNSIGNED_BYTE,
                        imBuffer
                )
                Log.d("RENDER ROUTINE", "glReadPixels took ${System.currentTimeMillis() - t} ms")

                val e = glGetError()
                if (e != GL_NO_ERROR) {
                    val s = when (e) {
                        GL_INVALID_ENUM -> "invalid enum"
                        GL_INVALID_VALUE -> "invalid value"
                        GL_INVALID_OPERATION -> "invalid operation"
                        GL_INVALID_FRAMEBUFFER_OPERATION -> "invalid framebuffer operation"
                        GL_OUT_OF_MEMORY -> "out of memory"
                        else -> "something else I guess"
                    }
                    Log.w("RENDER ROUTINE", s)
                }

                val bmp = Bitmap.createBitmap(texture.res[0], texture.res[1], Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(imBuffer)

                Log.d("RENDER ROUTINE", "done migrating bitmap with ${f.palette.name} palette")

                return bmp
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




        var renderToTex = false
        var renderBackground = true
        var isRendering = false

        var renderShaderChanged = false
        var resolutionChanged = false
        var renderProfileChanged = false

        private var hasTranslated = false
        private var hasScaled = false
        private var hasRotated = false
        private val strictTranslate = { hasTranslated && !hasScaled && !hasRotated }


        private val quadCoords = floatArrayOf(0f, 0f)
        private val quadFocus = floatArrayOf(0f, 0f)
        private var quadScale = 1f
        private var quadRotation = 0f

        private val bgSize = 5f

        lateinit var rr : RenderRoutine

        fun setQuadFocus(screenPos: FloatArray) {

            // update texture quad coordinates
            // convert focus coordinates from screen space to quad space

            quadFocus[0] =   2f*(screenPos[0] / screenRes[0]) - 1f
            quadFocus[1] = -(2f*(screenPos[1] / screenRes[1]) - 1f)

            Log.d("SURFACE VIEW", "quadFocus: (${quadFocus[0]}, ${quadFocus[1]})")

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

            //Log.d("SURFACE VIEW", "TRANSLATE -- quadCoords: (${quadCoords[0]}, ${quadCoords[1]})")
            Log.d("SURFACE VIEW", "TRANSLATE -- quadFocus: (${quadFocus[0]}, ${quadFocus[1]})")

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
        private fun saveImage(im: Bitmap) {

            // convert bitmap to png
            val bos = ByteArrayOutputStream()
            val compressed = im.compress(Bitmap.CompressFormat.PNG, 100, bos)
            if (!compressed) { Log.e("RENDERER", "could not compress image") }

            // app external storage directory
            val dir = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), resources.getString(R.string.app_name))

            // create directory if not already created
            if (!dir.exists()) {
                Log.d("MAIN ACTIVITY", "Directory does not exist")
                if (!dir.mkdirs()) {
                    Log.e("MAIN ACTIVITY", "Directory not created")
                }
            }

            // get current date and time
            val c = GregorianCalendar(TimeZone.getDefault())
            Log.d("RENDERER", "${c[Calendar.YEAR]}, ${c[Calendar.MONTH]}, ${c[Calendar.DAY_OF_MONTH]}")
            val year = c[Calendar.YEAR]
            val month = c[Calendar.MONTH]
            val day = c[Calendar.DAY_OF_MONTH]
            val hour = c[Calendar.HOUR_OF_DAY]
            val minute = c[Calendar.MINUTE]
            val second = c[Calendar.SECOND]

            // save image with unique filename
            val file = File(dir, "/FE_%4d%02d%02d_%02d%02d%02d.png".format(year, month+1, day, hour, minute, second))
            file.createNewFile()
            val fos = FileOutputStream(file)
            fos.write(bos.toByteArray())
            fos.close()

            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(file)
            scanIntent.data = contentUri
            act.sendBroadcast(scanIntent)

        }

        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

            // get OpenGL ES version
            val glVersion = unused.glGetString(GL_VERSION).split(" ")[2].toFloat()
            Log.d("SURFACE VIEW", "$glVersion")
            // f.glVersion = glVersion

            // get fragment shader precision
            val a : IntBuffer = IntBuffer.allocate(2)
            val b : IntBuffer = IntBuffer.allocate(1)
            glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_HIGH_FLOAT, a, b)
            // Log.e("FSV", "something: ${a[0]}, ${a[1]}")
            // Log.e("FSV", "float precision: ${b[0]}")

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

    fun vibrate() {

        // var hasTranslated = false
        // private val h = Handler()
//    private val longPressed = Runnable {
//        Log.d("SURFACE VIEW", "wow u pressed that so long")
//
//        // vibrate
//        val vib = act.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            vib.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
//        }
//        else {
//            //deprecated in API 26
//            vib.vibrate(15)
//        }
//
//        // toggle uiQuick
//        val uiQuick = act.findViewById<LinearLayout>(R.id.uiQuick)
//        val v : Int
//        if (uiQuick.visibility == LinearLayout.VISIBLE) {
//            v = LinearLayout.INVISIBLE
//        }
//        else {
//            v = LinearLayout.VISIBLE
//            uiQuick.bringToFront()
//        }
//        uiQuick.visibility = v
//
//    }

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


    fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
    fun checkThresholdCross(prevScale: Double) {

        if (sc.autoPrecision) {

            val prevPrecision = sc.precision

            sc.precision = when {
                f.position.scale > Precision.SINGLE.threshold -> Precision.SINGLE

                f.shape.hasDualFloat
                        && f.position.scale < Precision.SINGLE.threshold -> Precision.DUAL

                else -> sc.precision
            }

            if (sc.precision != prevPrecision) r.renderShaderChanged = true

            // display message
            val singleThresholdCrossedIn = f.position.scale < Precision.SINGLE.threshold && prevScale > Precision.SINGLE.threshold
            val singleThresholdCrossedOut = f.position.scale > Precision.SINGLE.threshold && prevScale < Precision.SINGLE.threshold
            val dualThresholdCrossed = f.position.scale < Precision.DUAL.threshold && prevScale > Precision.DUAL.threshold

            val msg = when {
                (!f.shape.hasDualFloat && singleThresholdCrossedIn) || (f.shape.hasDualFloat && dualThresholdCrossed) -> "Zoom limit reached"
                f.shape.hasDualFloat && singleThresholdCrossedIn -> "Switching to dual-precision...\nImage generation will be slower"
                f.shape.hasDualFloat && singleThresholdCrossedOut -> "Switching to single-precision...\nImage generation will be faster"
                else -> null
            }

            if (msg != null) act.displayMessage(msg)

        }

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
                            // Log.d("POSITION", "POINTER UP")
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
                            Log.e("FSV", "down")
                            return true
                        }
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            // Log.d("COLOR", "POINTER ${e.actionIndex} DOWN -- x: ${e.x}, y: ${e.y}")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            prevFocalLen = e.focalLength()
                            Log.e("FSV", "pointer down -- focal len: $prevFocalLen")
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
                            act.updateColorEditTexts()
                            prevFocalLen = 1f
                            // Log.d("COLOR", "ACTION UP")
                            return true
                        }

                    }
                }
                Reaction.SHAPE -> {
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
                                    f.shape.activeParam.u += f.sensitivity*dx/screenRes[0]
                                    f.shape.activeParam.v -= f.sensitivity*dy/screenRes[1]
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
                            act.updateShapeParamEditTexts()
                            act.updateDisplayParams()
                            r.renderToTex = true
                            requestRender()
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