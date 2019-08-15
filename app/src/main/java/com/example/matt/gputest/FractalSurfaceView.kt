package com.example.matt.gputest

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.ceil
import kotlin.math.sqrt


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

            private val renderProgram = GLES32.glCreateProgram()
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

            private val sampleProgram = GLES32.glCreateProgram()
            private val viewCoordsSampleHandle : Int
            private val quadCoordsSampleHandle : Int
            private val textureSampleHandle    : Int

            private val colorProgram = GLES32.glCreateProgram()
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

            private val interpolation = { GLES32.GL_NEAREST }

            private val textures = arrayOf(
                    Texture(intArrayOf(bgTexWidth(), bgTexHeight()), GLES32.GL_NEAREST, GLES32.GL_RGBA16F, 0),
                    Texture(f.texRes(), interpolation(), GLES32.GL_RGBA16F, 1),
                    Texture(f.texRes(), interpolation(), GLES32.GL_RGBA16F, 2)
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


                // create and compile shaders
                vRenderShader = loadShader(GLES32.GL_VERTEX_SHADER, vRenderCode)
                vSampleShader = loadShader(GLES32.GL_VERTEX_SHADER, vSampleCode)

                fRenderShader = loadShader(GLES32.GL_FRAGMENT_SHADER, f.renderShader())
                fSampleShader = loadShader(GLES32.GL_FRAGMENT_SHADER, fSampleCode)
                fColorShader  = loadShader(GLES32.GL_FRAGMENT_SHADER, f.colorShader())


                GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

                // generate texture and framebuffer objects
                GLES32.glGenFramebuffers(1, fboIDs)



                // attach shaders and create renderProgram executables
                GLES32.glAttachShader(renderProgram, vRenderShader)
                GLES32.glAttachShader(renderProgram, fRenderShader)
                GLES32.glLinkProgram(renderProgram)

                viewCoordsHandle     =  GLES32.glGetAttribLocation(   renderProgram, "viewCoords"  )
                iterHandle           =  GLES32.glGetUniformLocation(  renderProgram, "maxIter"     )
                bailoutHandle        =  GLES32.glGetUniformLocation(  renderProgram, "R"           )
                xInitHandle          =  GLES32.glGetUniformLocation(  renderProgram, "x0"          )
                yInitHandle          =  GLES32.glGetUniformLocation(  renderProgram, "y0"          )
                xScaleHandle         =  GLES32.glGetUniformLocation(  renderProgram, "xScale"      )
                yScaleHandle         =  GLES32.glGetUniformLocation(  renderProgram, "yScale"      )
                xOffsetHandle        =  GLES32.glGetUniformLocation(  renderProgram, "xOffset"     )
                yOffsetHandle        =  GLES32.glGetUniformLocation(  renderProgram, "yOffset"     )
                bgScaleHandle        =  GLES32.glGetUniformLocation(  renderProgram, "bgScale"     )
                mapParamHandles      =  IntArray(NUM_MAP_PARAMS) { i: Int ->
                    GLES32.glGetUniformLocation(renderProgram, "P${i+1}")
                }
                textureParamHandles  =  IntArray(NUM_TEXTURE_PARAMS) { i: Int ->
                    GLES32.glGetUniformLocation(renderProgram, "Q${i+1}")
                }


                GLES32.glAttachShader(sampleProgram, vSampleShader)
                GLES32.glAttachShader(sampleProgram, fSampleShader)
                GLES32.glLinkProgram(sampleProgram)

                viewCoordsSampleHandle = GLES32.glGetAttribLocation(  sampleProgram, "viewCoords"  )
                quadCoordsSampleHandle = GLES32.glGetAttribLocation(  sampleProgram, "quadCoords"  )
                textureSampleHandle    = GLES32.glGetUniformLocation( sampleProgram, "tex"         )

                GLES32.glAttachShader(colorProgram, vSampleShader)
                GLES32.glAttachShader(colorProgram, fColorShader)
                GLES32.glLinkProgram(colorProgram)

                viewCoordsColorHandle = GLES32.glGetAttribLocation(   colorProgram, "viewCoords"   )
                quadCoordsColorHandle = GLES32.glGetAttribLocation(   colorProgram, "quadCoords"   )
                textureColorHandle    = GLES32.glGetUniformLocation(  colorProgram, "tex"          )
                numColorsHandle       = GLES32.glGetUniformLocation(  colorProgram, "numColors"    )
                paletteHandle         = GLES32.glGetUniformLocation(  colorProgram, "palette"      )
                frequencyHandle       = GLES32.glGetUniformLocation(  colorProgram, "frequency"    )
                phaseHandle           = GLES32.glGetUniformLocation(  colorProgram, "phase"        )

            }

            private fun loadShader(type: Int, shaderCode: String): Int {

                // create a vertex shader type (GL.GL_VERTEX_SHADER)
                // or a fragment shader type (GL.GL_FRAGMENT_SHADER)
                val shader = GLES32.glCreateShader(type)

                // add the source code to the shader and compile it
                GLES32.glShaderSource(shader, shaderCode)
                GLES32.glCompileShader(shader)

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
                    GLES32.glDetachShader(renderProgram, fRenderShader)
                    fRenderShader = loadShader(GLES32.GL_FRAGMENT_SHADER, f.renderShader())
                    GLES32.glAttachShader(renderProgram, fRenderShader)
                    GLES32.glLinkProgram(renderProgram)
                    f.renderShaderChanged = false
                }
                if (f.resolutionChanged) {
                    val texRes = f.texRes()
                    textures[1].delete()
                    textures[1] = Texture(texRes, interpolation(), GLES32.GL_RGBA16F, 1)
                    textures[2].delete()
                    textures[2] = Texture(texRes, interpolation(), GLES32.GL_RGBA16F, 2)
                    f.resolutionChanged = false
                }
                if (f.renderProfileChanged) {
                    textures[0].delete()
                    textures[0] = Texture(intArrayOf(bgTexWidth(), bgTexHeight()), GLES32.GL_NEAREST, GLES32.GL_RGBA16F, 0)
                    f.renderProfileChanged = false
                }

                context.findViewById<ProgressBar>(R.id.progressBar).progress = 0

                GLES32.glUseProgram(renderProgram)
                GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer

                for (i in 0 until NUM_MAP_PARAMS) {
                    val p = floatArrayOf(
                            (f.fractalConfig.params["p${i + 1}"] as DoubleArray)[0].toFloat(),
                            (f.fractalConfig.params["p${i + 1}"] as DoubleArray)[1].toFloat())
                    // Log.d("RENDER ROUTINE", "passing p${i + 1} in as (${p[0]}, ${p[1]})")
                    GLES32.glUniform2fv(mapParamHandles[i], 1, p, 0)
                }
                for (i in 0 until NUM_TEXTURE_PARAMS) {
                    Log.d("RENDER ROUTINE", "passing q${i + 1} in as ${f.fractalConfig.params["q${i + 1}"]}")
                    GLES32.glUniform1fv(textureParamHandles[i], 1, floatArrayOf((f.fractalConfig.params["q${i + 1}"] as Double).toFloat()), 0)
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

                        GLES32.glUniform2fv(xScaleHandle,  1,  floatArrayOf(xScaleSF, 0.0f),   0)
                        GLES32.glUniform2fv(yScaleHandle,  1,  floatArrayOf(yScaleSF, 0.0f),   0)
                        GLES32.glUniform2fv(xOffsetHandle, 1,  floatArrayOf(xOffsetSF, 0.0f),  0)
                        GLES32.glUniform2fv(yOffsetHandle, 1,  floatArrayOf(yOffsetSF, 0.0f),  0)

                    }
                    Precision.DUAL -> {

                        val xScaleDF = splitSD(xScaleSD)
                        val yScaleDF = splitSD(yScaleSD)
                        val xOffsetDF = splitSD(xOffsetSD)
                        val yOffsetDF = splitSD(yOffsetSD)

                        GLES32.glUniform2fv(xScaleHandle,  1,  xScaleDF,   0)
                        GLES32.glUniform2fv(yScaleHandle,  1,  yScaleDF,   0)
                        GLES32.glUniform2fv(xOffsetHandle, 1,  xOffsetDF,  0)
                        GLES32.glUniform2fv(yOffsetHandle, 1,  yOffsetDF,  0)

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

                GLES32.glEnableVertexAttribArray(viewCoordsHandle)
                GLES32.glUniform1i(iterHandle, f.fractalConfig.maxIter())
                GLES32.glUniform1fv(bailoutHandle, 1, floatArrayOf(f.fractalConfig.bailoutRadius()), 0)
                GLES32.glUniform1fv(xInitHandle, 1, x0, 0)
                GLES32.glUniform1fv(yInitHandle, 1, y0, 0)




                //======================================================================================
                // RENDER LOW-RES
                //======================================================================================

                GLES32.glViewport(0, 0, textures[0].res[0], textures[0].res[1])
                GLES32.glUniform1fv(bgScaleHandle, 1, bgScaleFloat, 0)
                GLES32.glVertexAttribPointer(
                        viewCoordsHandle,       // index
                        3,                      // coordinates per vertex
                        GLES32.GL_FLOAT,            // type
                        false,                  // normalized
                        12,                     // coordinates per vertex * bytes per float
                        viewBuffer              // coordinates
                )
                GLES32.glFramebufferTexture2D(
                        GLES32.GL_FRAMEBUFFER,              // target
                        GLES32.GL_COLOR_ATTACHMENT0,        // attachment
                        GLES32.GL_TEXTURE_2D,               // texture target
                        textures[0].id,                 // texture
                        0                               // level
                )

                GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT)
                GLES32.glDrawElements(GLES32.GL_TRIANGLES, drawOrder.size, GLES32.GL_UNSIGNED_SHORT, drawListBuffer)




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

                    GLES32.glViewport(0, 0, textures[intIndex].res[0], textures[intIndex].res[1])
                    GLES32.glUniform1fv(bgScaleHandle, 1, floatArrayOf(1.0f), 0)
                    GLES32.glVertexAttribPointer(
                            viewCoordsHandle,           // index
                            3,                          // coordinates per vertex
                            GLES32.GL_FLOAT,                // type
                            false,                      // normalized
                            12,                         // coordinates per vertex * bytes per float
                            viewChunkBuffer             // coordinates
                    )
                    GLES32.glFramebufferTexture2D(
                            GLES32.GL_FRAMEBUFFER,              // target
                            GLES32.GL_COLOR_ATTACHMENT0,        // attachment
                            GLES32.GL_TEXTURE_2D,               // texture target
                            textures[intIndex].id,          // texture
                            0                               // level
                    )
                    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT)

                    val chunksA = splitCoords(xComplementViewCoordsA, yComplementViewCoordsA)
                    val chunksB = splitCoords(xComplementViewCoordsB, yComplementViewCoordsB)
                    val totalChunks = chunksA.size + chunksB.size
                    var chunksRendered = 0
                    for (complementViewChunkCoordsA in chunksA) {

                        viewChunkBuffer.put(complementViewChunkCoordsA).position(0)
                        GLES32.glDrawElements(GLES32.GL_TRIANGLES, drawOrder.size, GLES32.GL_UNSIGNED_SHORT, drawListBuffer)
                        GLES32.glFinish()
                        chunksRendered++
                        if (!f.settingsConfig.continuousRender()) {
                            context.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }
                    for (complementViewChunkCoordsB in chunksB) {

                        viewChunkBuffer.put(complementViewChunkCoordsB).position(0)
                        GLES32.glDrawElements(GLES32.GL_TRIANGLES, drawOrder.size, GLES32.GL_UNSIGNED_SHORT, drawListBuffer)
                        GLES32.glFinish()
                        chunksRendered++
                        if (!f.settingsConfig.continuousRender()) {
                            context.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }





                    //===================================================================================
                    // SAMPLE -- TRANSLATION INTERSECTION
                    //===================================================================================

                    GLES32.glUseProgram(sampleProgram)
                    GLES32.glViewport(0, 0, textures[intIndex].res[0], textures[intIndex].res[1])

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


                    GLES32.glEnableVertexAttribArray(viewCoordsSampleHandle)
                    GLES32.glEnableVertexAttribArray(quadCoordsSampleHandle)
                    GLES32.glUniform1i(textureSampleHandle, currIndex)
                    GLES32.glVertexAttribPointer(
                            viewCoordsSampleHandle,        // index
                            3,                          // coordinates per vertex
                            GLES32.GL_FLOAT,                // type
                            false,                      // normalized
                            12,                         // coordinates per vertex * bytes per float
                            viewChunkBuffer             // coordinates
                    )
                    GLES32.glVertexAttribPointer(
                            quadCoordsSampleHandle,        // index
                            3,                          // coordinates per vertex
                            GLES32.GL_FLOAT,                // type
                            false,                      // normalized
                            12,                         // coordinates per vertex * bytes per float
                            quadBuffer                  // coordinates
                    )


                    GLES32.glFramebufferTexture2D(
                            GLES32.GL_FRAMEBUFFER,              // target
                            GLES32.GL_COLOR_ATTACHMENT0,        // attachment
                            GLES32.GL_TEXTURE_2D,               // texture target
                            textures[intIndex].id,          // texture
                            0                               // level
                    )

                    GLES32.glDrawElements(GLES32.GL_TRIANGLES, drawOrder.size, GLES32.GL_UNSIGNED_SHORT, drawListBuffer)

                    GLES32.glDisableVertexAttribArray(viewCoordsSampleHandle)
                    GLES32.glDisableVertexAttribArray(quadCoordsSampleHandle)


                    // swap intermediate and current texture indices
                    val temp = intIndex
                    intIndex = currIndex
                    currIndex = temp


                }
                else {

                    //===================================================================================
                    // NOVEL RENDER -- ENTIRE TEXTURE
                    //===================================================================================

                    GLES32.glViewport(0, 0, textures[currIndex].res[0], textures[currIndex].res[1])
                    GLES32.glUniform1fv(bgScaleHandle, 1, floatArrayOf(1.0f), 0)
                    GLES32.glFramebufferTexture2D(
                            GLES32.GL_FRAMEBUFFER,              // target
                            GLES32.GL_COLOR_ATTACHMENT0,        // attachment
                            GLES32.GL_TEXTURE_2D,               // texture target
                            textures[currIndex].id,         // texture
                            0                               // level
                    )

                    // check framebuffer status
                    val status = GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER)
                    if (status != GLES32.GL_FRAMEBUFFER_COMPLETE) {
                        Log.d("FRAMEBUFFER", "$status")
                    }

                    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT)


                    val chunks = splitCoords(floatArrayOf(-1.0f, 1.0f), floatArrayOf(-1.0f, 1.0f))
                    val totalChunks = chunks.size
                    var chunksRendered = 0
                    for (viewChunkCoords in chunks) {

                        viewChunkBuffer.put(viewChunkCoords)
                        viewChunkBuffer.position(0)

                        GLES32.glVertexAttribPointer(
                                viewCoordsHandle,           // index
                                3,                          // coordinates per vertex
                                GLES32.GL_FLOAT,                // type
                                false,                      // normalized
                                12,                         // coordinates per vertex * bytes per float
                                viewChunkBuffer             // coordinates
                        )

                        GLES32.glDrawElements(GLES32.GL_TRIANGLES, drawOrder.size, GLES32.GL_UNSIGNED_SHORT, drawListBuffer)
                        GLES32.glFinish()   // force chunk to finish rendering before continuing
                        chunksRendered++
                        if(!f.settingsConfig.continuousRender()) {
                            context.findViewById<ProgressBar>(R.id.progressBar).progress =
                                    (chunksRendered.toFloat() / totalChunks.toFloat() * 100.0f).toInt()
                        }

                    }

                    GLES32.glDisableVertexAttribArray(viewCoordsHandle)

                }




            }
            fun renderFromTexture(fitToScreen: Boolean) {

//        Log.d("RENDER", "render from texture -- start")

                if (f.colorShaderChanged) {
                    Log.d("RENDER ROUTINE", "color shader changed")
                    GLES32.glDetachShader(colorProgram, fColorShader)
                    fColorShader = loadShader(GLES32.GL_FRAGMENT_SHADER, f.colorShader())
                    GLES32.glAttachShader(colorProgram, fColorShader)
                    GLES32.glLinkProgram(colorProgram)
                    f.colorShaderChanged = false
                }

                //======================================================================================
                // PRE-RENDER PROCESSING
                //======================================================================================

                GLES32.glUseProgram(colorProgram)

                GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0)
                if (fitToScreen) {
                    GLES32.glViewport(0, 0, f.screenRes[0], f.screenRes[1])
                }
                else {
                    GLES32.glViewport(0, 0, textures[currIndex].res[0], textures[currIndex].res[1])
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

                GLES32.glUniform1i(numColorsHandle, f.colorConfig.palette().size)
                GLES32.glUniform3fv(paletteHandle, f.colorConfig.palette().size, f.colorConfig.palette().flatPalette, 0)
                GLES32.glUniform1fv(frequencyHandle, 1, floatArrayOf(f.colorConfig.frequency()), 0)
                GLES32.glUniform1fv(phaseHandle, 1, floatArrayOf(f.colorConfig.phase()), 0)

                GLES32.glEnableVertexAttribArray(viewCoordsColorHandle)
                GLES32.glEnableVertexAttribArray(quadCoordsColorHandle)




                //======================================================================================
                // RENDER LOW-RES
                //======================================================================================

                GLES32.glUniform1i(textureColorHandle, 0)    // use GL_TEXTURE0
                GLES32.glVertexAttribPointer(
                        viewCoordsColorHandle,      // index
                        3,                          // coordinates per vertex
                        GLES32.GL_FLOAT,                // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        viewBuffer                  // coordinates
                )
                GLES32.glVertexAttribPointer(
                        quadCoordsColorHandle,      // index
                        3,                          // coordinates per vertex
                        GLES32.GL_FLOAT,                // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        bgQuadBuffer                // coordinates
                )

                GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT)
                GLES32.glDrawElements(GLES32.GL_TRIANGLES, drawOrder.size, GLES32.GL_UNSIGNED_SHORT, drawListBuffer)




                //======================================================================================
                // RENDER HIGH-RES
                //======================================================================================

                GLES32.glUniform1i(textureColorHandle, currIndex)
                GLES32.glVertexAttribPointer(
                        viewCoordsColorHandle,        // index
                        3,                          // coordinates per vertex
                        GLES32.GL_FLOAT,                // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        viewBuffer                  // coordinates
                )
                GLES32.glVertexAttribPointer(
                        quadCoordsColorHandle,        // index
                        3,                          // coordinates per vertex
                        GLES32.GL_FLOAT,                // type
                        false,                      // normalized
                        12,                         // coordinates per vertex * bytes per float
                        quadBuffer                  // coordinates
                )

                GLES32.glDrawElements(GLES32.GL_TRIANGLES, drawOrder.size, GLES32.GL_UNSIGNED_SHORT, drawListBuffer)




                GLES32.glDisableVertexAttribArray(viewCoordsColorHandle)
                GLES32.glDisableVertexAttribArray(quadCoordsColorHandle)

//        Log.d("RENDER", "render from texture -- end")

                context.findViewById<ProgressBar>(R.id.progressBar).progress = 0

            }
            fun migrateToBitmap() : Bitmap {
                val bufferSize = textures[currIndex].res[0]*textures[currIndex].res[1]*4
                val imBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())

                GLES32.glReadnPixels(
                        0, 0,
                        textures[currIndex].res[0],
                        textures[currIndex].res[1],
                        GLES32.GL_RGBA,
                        GLES32.GL_UNSIGNED_BYTE,
                        bufferSize,
                        imBuffer
                )
                val e = GLES32.glGetError()
                if (e != GLES32.GL_NO_ERROR) {
                    val s = when (e) {
                        GLES32.GL_INVALID_ENUM -> "invalid enum"
                        GLES32.GL_INVALID_VALUE -> "invalid value"
                        GLES32.GL_INVALID_OPERATION -> "invalid operation"
                        GLES32.GL_INVALID_FRAMEBUFFER_OPERATION -> "invalid framebuffer operation"
                        GLES32.GL_OUT_OF_MEMORY -> "out of memory"
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
            val file = File(dir, "/FS_%4d%02d%02d_%02d%02d%02d.png".format(year, month+1, day, hour, minute, second))
            file.createNewFile()
            val fos = FileOutputStream(file)
            fos.write(bos.toByteArray())
            fos.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val contentUri = Uri.fromFile(file)
                scanIntent.data = contentUri
                context.sendBroadcast(scanIntent)
            }
            else {
                val intent = Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()))
                context.sendBroadcast(intent)
            }

        }

        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

            // get OpenGL ES version
            Log.d("SURFACE VIEW", "OpenGL ES version: ${unused.glGetString(GL10.GL_VERSION)}")

            // get fragment shader precision
            val a : IntBuffer = IntBuffer.allocate(2)
            val b : IntBuffer = IntBuffer.allocate(1)
            GLES32.glGetShaderPrecisionFormat(GLES32.GL_FRAGMENT_SHADER, GLES32.GL_HIGH_FLOAT, a, b)
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

                isRendering = !(f.settingsConfig.continuousRender() || reaction.name[0].toString() == "P")
                rr.renderToTexture()

                renderToTex = false
                hasTranslated = false
                hasScaled = false
                resetQuadParams()

            }

            // render from texture
            if (saveImage) {
                Log.d("RENDERER", "migrating to bitmap")
                rr.renderFromTexture(false)
                val im = rr.migrateToBitmap()
                saveImage(im)
                saveImage = false
            }

            rr.renderFromTexture(true)
            isRendering = false


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


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {

        if (!r.isRendering) {

            // monitor for long press
            when(e?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val focus = e.focus()
                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]
                    h.postDelayed(longPressed, 1000)
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