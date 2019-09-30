package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLES30 as GL
import android.opengl.GLSurfaceView
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.widget.HorizontalScrollView
import android.widget.ProgressBar
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.*
import java.util.*
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.ceil


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
        index           : Int
) {

    val id : Int
    val buffer : FloatBuffer

    init {
        // create texture id
        val b = IntBuffer.allocate(1)
        GL.glGenTextures(1, b)
        id = b[0]

        // allocate texture memory
        val bytesPerTexel = when(internalFormat) {
                           // # of components   # of bytes per component
            GL.GL_RGBA8 ->    4                 * 1
            GL.GL_RGBA16F ->  4                 * 2
            GL.GL_RGBA32F ->  4                 * 4
            GL.GL_RG32F ->    2                 * 4
//            GL.GL_RGBA -> ByteBuffer.allocateDirect(res[0] * res[1] * 4).order(ByteOrder.nativeOrder())
            else -> 0
        }
        buffer = ByteBuffer.allocateDirect(res[0] * res[1] * bytesPerTexel).order(ByteOrder.nativeOrder()).asFloatBuffer()

        // bind and set texture parameters
        GL.glActiveTexture(GL.GL_TEXTURE0 + index)
        GL.glBindTexture(GL.GL_TEXTURE_2D, id)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, interpolation)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, interpolation)

        val type = when(internalFormat) {
            GL.GL_RGBA8 -> GL.GL_UNSIGNED_BYTE
            GL.GL_RGBA16F -> GL.GL_HALF_FLOAT
            GL.GL_RGBA32F -> GL.GL_FLOAT
            GL.GL_RG32F -> GL.GL_FLOAT
//            GL.GL_RGBA -> GL.GL_UNSIGNED_BYTE
            else -> 0
        }

        val format = when(internalFormat) {
            GL.GL_RG32F -> GL.GL_RG
            else -> GL.GL_RGBA
        }

        // define texture specs
        GL.glTexImage2D(
            GL.GL_TEXTURE_2D,           // target
            0,                          // mipmap level
            internalFormat,             // internal format
            res[0], res[1],             // texture resolution
            0,                          // border
            format,                     // internalFormat
            type,                       // type
            buffer                      // memory pointer
        )
    }

    fun delete() { GL.glDeleteTextures(1, intArrayOf(id), 0) }

}

@SuppressLint("ViewConstructor")
class FractalSurfaceView(
        var f           : Fractal,
        val context     : Activity
) : GLSurfaceView(context) {

    inner class FractalRenderer : Renderer {

        inner class RenderRoutine {

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
            private val bailoutHandle    : Int
            private val xInitHandle      : Int
            private val yInitHandle      : Int
            private val xScaleHandle     : Int
            private val yScaleHandle     : Int
            private val xOffsetHandle    : Int
            private val yOffsetHandle    : Int
            private val bgScaleHandle    : Int
            private val mapParamHandles  : IntArray
            private val textureParamHandles : IntArray


            private val orbitHandle : Int


            private val sampleProgram = GL.glCreateProgram()
            private val viewCoordsSampleHandle : Int
            private val quadCoordsSampleHandle : Int
            private val textureSampleHandle    : Int
            private val yOrientSampleHandle    : Int

            private val colorProgram = GL.glCreateProgram()
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
            private val bgTexWidth = { if (f.settingsConfig.continuousRender()) 1 else f.screenRes[0]/8 }
            private val bgTexHeight = { if (f.settingsConfig.continuousRender()) 1 else f.screenRes[1]/8 }

            private val interpolation = { GL.GL_NEAREST }

            private val textures = arrayOf(
                    GLTexture(intArrayOf(bgTexWidth(), bgTexHeight()), GL.GL_NEAREST, GL.GL_RGBA16F, 0),
                    GLTexture(f.texRes(), interpolation(), GL.GL_RGBA16F, 1),
                    GLTexture(f.texRes(), interpolation(), GL.GL_RGBA16F, 2),
                    GLTexture(intArrayOf(f.fractalConfig.maxIter(), 1), GL.GL_NEAREST, GL.GL_RG32F, 3)    // perturbation texture
//                    GLTexture(intArrayOf(bgTexWidth(), bgTexHeight()), GL.GL_NEAREST, GL.GL_RGBA, 0),
//                    GLTexture(f.texRes(), interpolation(), GL.GL_RGBA, 1),
//                    GLTexture(f.texRes(), interpolation(), GL.GL_RGBA, 2)
            )

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
            private var currIndex = 1      // current high-res texture ID index
            private var intIndex = 2       // intermediate high-res texture ID index
            private val perturbationIndex = 3

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

                // load all vertex and fragment shader code
                var s = context.resources.openRawResource(R.raw.vert_render)
                val vRenderCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()

                s = context.resources.openRawResource(R.raw.vert_sample)
                val vSampleCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()

                s = context.resources.openRawResource(R.raw.sample)
                val fSampleCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()


                s = context.resources.openRawResource(R.raw.perturbation)
                val perturbationCode = Scanner(s).useDelimiter("\\Z").next()
                s.close()


                // create and compile shaders
                vRenderShader = loadShader(GL.GL_VERTEX_SHADER, vRenderCode)
                vSampleShader = loadShader(GL.GL_VERTEX_SHADER, vSampleCode)

                fRenderShader = loadShader(GL.GL_FRAGMENT_SHADER, f.renderShader())
//                fRenderShader = loadShader(GL.GL_FRAGMENT_SHADER, perturbationCode)

                fSampleShader = loadShader(GL.GL_FRAGMENT_SHADER, fSampleCode)
                fColorShader  = loadShader(GL.GL_FRAGMENT_SHADER, f.colorShader())


                GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

                // generate texture and framebuffer objects
                GL.glGenFramebuffers(1, fboIDs)



                // attach shaders and create renderProgram executables
                GL.glAttachShader(renderProgram, vRenderShader)
                GL.glAttachShader(renderProgram, fRenderShader)
                GL.glLinkProgram(renderProgram)

                viewCoordsHandle     =  GL.glGetAttribLocation(   renderProgram, "viewCoords"  )
                iterHandle           =  GL.glGetUniformLocation(  renderProgram, "maxIter"     )
                bailoutHandle        =  GL.glGetUniformLocation(  renderProgram, "R"           )
                xInitHandle          =  GL.glGetUniformLocation(  renderProgram, "x0"          )
                yInitHandle          =  GL.glGetUniformLocation(  renderProgram, "y0"          )
                xScaleHandle         =  GL.glGetUniformLocation(  renderProgram, "xScale"      )
                yScaleHandle         =  GL.glGetUniformLocation(  renderProgram, "yScale"      )
                xOffsetHandle        =  GL.glGetUniformLocation(  renderProgram, "xOffset"     )
                yOffsetHandle        =  GL.glGetUniformLocation(  renderProgram, "yOffset"     )
                bgScaleHandle        =  GL.glGetUniformLocation(  renderProgram, "bgScale"     )
                mapParamHandles      =  IntArray(NUM_MAP_PARAMS) { i: Int ->
                    GL.glGetUniformLocation(renderProgram, "P${i+1}")
                }
                textureParamHandles  =  IntArray(NUM_TEXTURE_PARAMS) { i: Int ->
                    GL.glGetUniformLocation(renderProgram, "Q${i+1}")
                }

                orbitHandle          =  GL.glGetUniformLocation(  renderProgram, "orbit"       )



                GL.glAttachShader(sampleProgram, vSampleShader)
                GL.glAttachShader(sampleProgram, fSampleShader)
                GL.glLinkProgram(sampleProgram)

                viewCoordsSampleHandle = GL.glGetAttribLocation(  sampleProgram, "viewCoords"  )
                quadCoordsSampleHandle = GL.glGetAttribLocation(  sampleProgram, "quadCoords"  )
                textureSampleHandle    = GL.glGetUniformLocation( sampleProgram, "tex"         )
                yOrientSampleHandle    = GL.glGetUniformLocation( sampleProgram, "yOrient"     )

                GL.glAttachShader(colorProgram, vSampleShader)
                GL.glAttachShader(colorProgram, fColorShader)
                GL.glLinkProgram(colorProgram)

                viewCoordsColorHandle = GL.glGetAttribLocation(   colorProgram, "viewCoords"   )
                quadCoordsColorHandle = GL.glGetAttribLocation(   colorProgram, "quadCoords"   )
                textureColorHandle    = GL.glGetUniformLocation(  colorProgram, "tex"          )
                yOrientColorHandle    = GL.glGetUniformLocation(  colorProgram, "yOrient"      )
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
                val maxPixelsPerChunk = when (f.autoPrecision()) {
                    Precision.SINGLE -> f.screenRes[0]*f.screenRes[1]/4
                    Precision.DUAL -> f.screenRes[0]*f.screenRes[1]/8
                    else -> f.screenRes[0]*f.screenRes[1]
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
                    textures[1] = GLTexture(texRes, interpolation(), GL.GL_RGBA16F, 1)
//                    textures[1] = GLTexture(texRes, interpolation(), GL.GL_RGBA, 1)
                    textures[2].delete()
                    textures[2] = GLTexture(texRes, interpolation(), GL.GL_RGBA16F, 2)
//                    textures[2] = GLTexture(texRes, interpolation(), GL.GL_RGBA, 2)
                    f.resolutionChanged = false
                }
                if (f.renderProfileChanged) {
                    textures[0].delete()
                    textures[0] = GLTexture(intArrayOf(bgTexWidth(), bgTexHeight()), GL.GL_NEAREST, GL.GL_RGBA16F, 0)
//                    textures[0] = GLTexture(intArrayOf(bgTexWidth(), bgTexHeight()), GL.GL_NEAREST, GL.GL_RGBA, 0)
                    f.renderProfileChanged = false
                }

                context.findViewById<ProgressBar>(R.id.progressBar).progress = 0

                GL.glUseProgram(renderProgram)
                GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer



                // TESTING PERTURBATION
//                val numProbes = 25
//                var orbitArray : FloatArray = floatArrayOf()
//                var z : Complex
//                var c = Complex(f.fractalConfig.coords()[0], f.fractalConfig.coords()[1])
//                var modZ = 0.0
//                var alpha : Complex
//                var beta : Complex
//                val iterations = IntArray(numProbes)
//                val distances = DoubleArray(numProbes)
//                val gradientAngles = DoubleArray(numProbes)
//
//                var maxIterFound = false
//
//
//
//                for (j in 0 until numProbes) {
//
//                    Log.d("RENDER ROUTINE", "j: $j")
//
//                    orbitArray = FloatArray(f.fractalConfig.maxIter()*2) { 0f }
//                    z = Complex(0.0, 0.0)
//                    alpha = Complex(0.0, 0.0)
//                    beta = Complex(0.0, 0.0)
//
//                    for (i in 0 until f.fractalConfig.maxIter()) {
//
//                        orbitArray[2 * i] = z.x.toFloat()
//                        orbitArray[2 * i + 1] = z.y.toFloat()
//
//                        // iterate second derivative
//                        beta = 2.0*beta*z + alpha*alpha
//
//                        // iterate first derivative
//                        alpha = 2.0*alpha*z + Complex(1.0, 0.0)
//
//                        // iterate z
//                        z = z*z + c
//                        modZ = z.mod()
//                        //Log.d("RENDER ROUTINE", "x: $x, y: $y")
//
//                        if (i == f.fractalConfig.maxIter() - 1) {
//                            Log.d("RENDER ROUTINE", "maxIter !!!!!")
//                            maxIterFound = true
//                            iterations[j] = i
//                        }
//                        if (modZ > f.fractalConfig.bailoutRadius()) {
//                            Log.d("RENDER ROUTINE", "i: $i")
//                            iterations[j] = i
//                            break
//                        }
//
//                    }
//
//                    if (maxIterFound) {
//                        break
//                    }
//
//
//                    var grad = z/alpha
//                    grad /= grad.mod()
//                    grad = -grad
//                    distances[j] = modZ*log(modZ, Math.E)/alpha.mod()
//                    gradientAngles[j] = atan2(grad.y, grad.x)
//
//                    c += 1.5*distances[j]*grad
//                    Log.d("RENDER ROUTINE", "c_prime: $c")
//
//
//                }
//
//                var s = ""
//                gradientAngles.forEach { s += "$it   " }
//                Log.d("RENDER ROUTINE", "\ngradientAngles: $s")
//
//                s = ""
//                iterations.forEach { s += "$it   " }
//                Log.d("RENDER ROUTINE", "\niterations: $s")
//
//                textures[perturbationIndex].buffer.position(0)
//                textures[perturbationIndex].buffer.put(orbitArray)
//                textures[perturbationIndex].buffer.position(0)
//
//                // define texture specs
//                GL.glTexImage2D(
//                        GL.GL_TEXTURE_2D,           // target
//                        0,                          // mipmap level
//                        GL.GL_RG32F,                // internal format
//                        f.fractalConfig.maxIter(), 1, // texture resolution
//                        0,                          // border
//                        GL.GL_RG,                     // internalFormat
//                        GL.GL_FLOAT,                       // type
//                        textures[perturbationIndex].buffer                      // memory pointer
//                )
//
//                GL.glUniform1i(orbitHandle, perturbationIndex)
//                val xOffsetSD = f.fractalConfig.coords()[0] - c.x
//                val yOffsetSD = f.fractalConfig.coords()[1] - c.y





                for (i in 1..NUM_MAP_PARAMS) {
                    val p = floatArrayOf(
                            (f.fractalConfig.params["p$i"] as ComplexMap.Param).getU().toFloat(),
                            (f.fractalConfig.params["p$i"] as ComplexMap.Param).getV().toFloat())
                    // Log.d("RENDER ROUTINE", "passing p${i + 1} in as (${p[0]}, ${p[1]})")
                    GL.glUniform2fv(mapParamHandles[i - 1], 1, p, 0)
                }
                for (i in 0 until NUM_TEXTURE_PARAMS) {
                    // Log.d("RENDER ROUTINE", "passing q${i + 1} in as ${f.fractalConfig.params["q${i + 1}"]}")
                    GL.glUniform1fv(textureParamHandles[i], 1, floatArrayOf((f.fractalConfig.params["q${i + 1}"] as Double).toFloat()), 0)
                }

                val x0 = floatArrayOf(f.fractalConfig.map().initZ[0].toFloat())
                val y0 = floatArrayOf(f.fractalConfig.map().initZ[1].toFloat())


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
                GL.glUniform1fv(bailoutHandle, 1, floatArrayOf(f.fractalConfig.bailoutRadius()), 0)
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
                    GL.glUniform1fv(yOrientSampleHandle, 1, floatArrayOf(1f), 0)
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
            fun renderFromTexture(fitToScreen: Boolean = true, yOrient: Float = 1f) {

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
                if (fitToScreen) {
                    GL.glViewport(0, 0, f.screenRes[0], f.screenRes[1])
                }
                else {
                    GL.glViewport(0, 0, textures[currIndex].res[0], textures[currIndex].res[1])
                }

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

                GL.glUniform1fv(yOrientColorHandle, 1, floatArrayOf(yOrient), 0)
                GL.glUniform1i(numColorsHandle, f.fractalConfig.palette().size)
                GL.glUniform3fv(paletteHandle, f.fractalConfig.palette().size, f.fractalConfig.palette().flatPalette, 0)
                GL.glUniform1fv(frequencyHandle, 1, floatArrayOf(f.fractalConfig.frequency()), 0)
                GL.glUniform1fv(phaseHandle, 1, floatArrayOf(f.fractalConfig.phase()), 0)

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
            fun migrateToBitmap() : Bitmap {
                val bufferSize = textures[currIndex].res[0]*textures[currIndex].res[1]*4
                val imBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())

                GL.glReadPixels(
                        0, 0,
                        textures[currIndex].res[0],
                        textures[currIndex].res[1],
                        GL.GL_RGBA,
                        GL.GL_UNSIGNED_BYTE,
                        imBuffer
                )
                val e = GL.glGetError()
                if (e != GL.GL_NO_ERROR) {
                    val s = when (e) {
                        GL.GL_INVALID_ENUM -> "invalid enum"
                        GL.GL_INVALID_VALUE -> "invalid value"
                        GL.GL_INVALID_OPERATION -> "invalid operation"
                        GL.GL_INVALID_FRAMEBUFFER_OPERATION -> "invalid framebuffer operation"
                        GL.GL_OUT_OF_MEMORY -> "out of memory"
                        else -> "something else I guess"
                    }
                    Log.e("RENDER ROUTINE", s)
                }

                val imByteArray = imBuffer.array()
                Log.d("RENDER ROUTINE", "imByteArray size: ${imByteArray.size}")
                val bmp = Bitmap.createBitmap(textures[currIndex].res[0], textures[currIndex].res[1], Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(imBuffer)
                Log.d("RENDER ROUTINE", "bmp is null: ${bmp == null}")
                return bmp
            }

        }




        var renderToTex = false
        var isRendering = false
        var saveImage = false
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

        private val quadAnchor = doubleArrayOf(0.0, 0.0)
        private val quadFocus = doubleArrayOf(0.0, 0.0)
        private val t = doubleArrayOf(0.0, 0.0)

        private lateinit var rr : RenderRoutine


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
            context.sendBroadcast(scanIntent)

        }

        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

            // get OpenGL ES version
            val glVersion = unused.glGetString(GL10.GL_VERSION).split(" ")[2].toFloat()
            Log.d("SURFACE VIEW", "$glVersion")
            Log.d("SURFACE VIEW", "${glVersion > 2f}")
            f.glVersion = glVersion

            // get fragment shader precision
//            val a : IntBuffer = IntBuffer.allocate(2)
//            val b : IntBuffer = IntBuffer.allocate(1)
//            GL.glGetShaderPrecisionFormat(GL.GL_FRAGMENT_SHADER, GL.GL_HIGH_FLOAT, a, b)
//            Log.d("SURFACE VIEW", "float precision: ${b[0]}")

            rr = RenderRoutine()
            rr.renderToTexture()

            val buttonScroll = context.findViewById<HorizontalScrollView>(R.id.buttonScroll)
            buttonScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT)

        }
        override fun onDrawFrame(unused: GL10) {

            // Log.d("RENDER", "DRAW FRAME")

            // render to texture on ACTION_UP
            if (renderToTex) {

                isRendering = !(f.settingsConfig.continuousRender()
                        || reaction == Reaction.P1
                        || reaction == Reaction.P2
                        || reaction == Reaction.P3
                        || reaction == Reaction.P4)
                rr.renderToTexture()

                renderToTex = false
                hasTranslated = false
                hasScaled = false
                resetQuadParams()

            }

            // render from texture
            if (saveImage) {
                Log.d("RENDERER", "migrating to bitmap")
                rr.renderFromTexture(
                    fitToScreen = false,
                    yOrient = -1f
                )
                val im = rr.migrateToBitmap()
                saveImage(im)
                saveImage = false
            }

            rr.renderFromTexture()
            isRendering = false


        }
        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {

        }

    }




    val r : FractalRenderer
//    var hasTranslated = false
//    private val h = Handler()
//    private val longPressed = Runnable {
//        Log.d("SURFACE VIEW", "wow u pressed that so long")
//
//        // vibrate
//        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            vib.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
//        }
//        else {
//            //deprecated in API 26
//            vib.vibrate(15)
//        }
//
//        // toggle uiQuick
//        val uiQuick = context.findViewById<LinearLayout>(R.id.uiQuick)
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

    var reaction = Reaction.POSITION
    val numDisplayParams = {
        when (reaction) {
            Reaction.POSITION -> 3
            Reaction.COLOR -> 2
            else -> 3
        }
    }

    private val prevFocus = floatArrayOf(0.0f, 0.0f)
    // private val edgeRightSize = 150
    private var prevFocalLen = 1.0f
    // private val minPixelMove = 5f


    init {


        // setEGLContextClientVersion(2)              // create OpenGL ES 3.0 context
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

                            if (!f.settingsConfig.continuousRender()) {
                                r.setQuadAnchor(focus)
                                r.setQuadFocus(floatArrayOf(0.0f, 0.0f))
                            }

                            return true
                        }
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            // Log.d("POSITION", "POINTER ${e.actionIndex} DOWN -- x: ${e.x}, y: ${e.y}")
                            val focus = e.focus()
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            prevFocalLen = e.focalLength()
                            // Log.d("POSITION", "focalLen: $prevFocalLen")
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

                            // Log.d("POSITION", "MOVE -- dx: $dx, dy: $dy")
                            f.translate(floatArrayOf(dx, dy))
                            if (!f.settingsConfig.continuousRender()) {
                                r.translate(floatArrayOf(dx, dy))
                            }
                            prevFocus[0] = focus[0]
                            prevFocus[1] = focus[1]
                            if (e.pointerCount > 1) {   // MULTI-TOUCH
                                val focalLen = e.focalLength()
                                // Log.d("POSITION", "MOVE -- prevFocalLen: $prevFocalLen, focalLen: $focalLen")
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
                            // Log.d("POSITION", "POINTER UP")
                            f.updatePositionEditTexts()
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

                                    // change quad anchor to remaining pointer
                                    if (!f.settingsConfig.continuousRender()) {
                                        r.setQuadAnchor(floatArrayOf(e.getX(1), e.getY(1)))
                                    }
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
                            f.updateColorParamEditTexts()
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
                                    f.setMapParamSensitivity(reaction.name[1].toString().toInt(), dFocalLen)
                                    prevFocalLen = focalLen
                                }
                            }
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
                            f.updateMapParamEditTexts()
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