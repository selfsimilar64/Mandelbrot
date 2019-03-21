package com.example.matt.gputest

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.content.Context
import javax.microedition.khronos.egl.EGLConfig
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.opengl.GLES32 as GL
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.FrameLayout.LayoutParams as LP
import android.widget.LinearLayout.LayoutParams as LP2
import android.widget.FrameLayout
import android.widget.SeekBar
import java.nio.ByteOrder
import java.nio.ByteBuffer.allocateDirect
import java.util.*
import javax.microedition.khronos.opengles.GL10
import java.nio.IntBuffer
import kotlin.math.*
const val SPLIT = 8193.0


fun loadShader(type: Int, shaderCode: String): Int {

    // create a vertex shader type (GL.GL_VERTEX_SHADER)
    // or a fragment shader type (GL.GL_FRAGMENT_SHADER)
    val shader = GL.glCreateShader(type)

    // add the source code to the shader and compile it
    GL.glShaderSource(shader, shaderCode)
    GL.glCompileShader(shader)

    return shader
}



class Fractal(
        ctx: Context,
        private val screenWidth : Int,
        private val screenHeight : Int
) {
    
    val emulateDouble = false
    val emulateQuad = false

    private val numChunks = 10
    private val chunkInc : Float = 2.0f / numChunks

    // coordinates of default view boundaries
    private val viewCoords = floatArrayOf(
            -1.0f,   1.0f,   0.0f,     // top left
            -1.0f,  -1.0f,   0.0f,     // bottom left
             1.0f,  -1.0f,   0.0f,     // bottom right
             1.0f,   1.0f,   0.0f )    // top right
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

    private val program = GL.glCreateProgram()
    private val viewCoordsHandle : Int
    private val iterHandle       : Int
    private val xScaleHandle     : Int
    private val yScaleHandle     : Int
    private val xOffsetHandle    : Int
    private val yOffsetHandle    : Int
    private val xTouchHandle     : Int
    private val yTouchHandle     : Int
    private val bgScaleHandle    : Int

    private val texProgram = GL.glCreateProgram()
    private val viewCoordsTexHandle : Int
    private val quadCoordsTexHandle : Int
    private val textureTexHandle    : Int

    private val vertexShader      : Int
    private val fragmentShader    : Int
    private val vertexTexShader   : Int
    private val fragmentTexShader : Int

    // define texture resolutions
    private val texWidth = screenWidth/4
    private val texHeight = screenHeight/4
    private val bgTexWidth = screenWidth/16
    private val bgTexHeight = screenHeight/16

    // allocate memory for textures
    private val texLowResBuffer =
            allocateDirect(bgTexWidth * bgTexHeight * 4)
            .order(ByteOrder.nativeOrder())
    private val texHighResBuffer1 =
            allocateDirect(texWidth * texHeight * 4)
            .order(ByteOrder.nativeOrder())
    private val texHighResBuffer2 =
            allocateDirect(texWidth * texHeight * 4)
            .order(ByteOrder.nativeOrder())
    private val quadBuffer =
            allocateDirect(viewCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    private val bgQuadBuffer =
            allocateDirect(viewCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

    // create variables to store texture and fbo IDs
    private val texIDs : IntBuffer = IntBuffer.allocate(3)
    private val fboIDs : IntBuffer = IntBuffer.allocate(1)
    private var currHighResIndex = 1      // current high-res texture ID index
    private var intHighResIndex = 2       // intermediate high-res texture ID index

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
        val vertStream = ctx.resources.openRawResource(R.raw.vert)
        val vertShaderCode = Scanner(vertStream).useDelimiter("\\Z").next()
        vertStream.close()

        val fragSFStream = ctx.resources.openRawResource(R.raw.frag)
        val fragSFShaderCode = Scanner(fragSFStream).useDelimiter("\\Z").next()
        fragSFStream.close()

        val fragDFStream = ctx.resources.openRawResource(R.raw.frag_df64)
        val fragDFShaderCode = Scanner(fragDFStream).useDelimiter("\\Z").next()
        fragDFStream.close()

        val fragQFStream = ctx.resources.openRawResource(R.raw.frag_qf128)
        val fragQFShaderCode = Scanner(fragQFStream).useDelimiter("\\Z").next()
        fragQFStream.close()

        val vertTexStream = ctx.resources.openRawResource(R.raw.vert_tex)
        val vertTexShaderCode = Scanner(vertTexStream).useDelimiter("\\Z").next()
        vertTexStream.close()

        val fragTexStream = ctx.resources.openRawResource(R.raw.frag_tex)
        val fragTexShaderCode = Scanner(fragTexStream).useDelimiter("\\Z").next()
        fragTexStream.close()


        // create and compile shaders
        vertexShader = loadShader(GL.GL_VERTEX_SHADER, vertShaderCode)
        fragmentShader = if (emulateQuad) {
            loadShader(GL.GL_FRAGMENT_SHADER, fragQFShaderCode)
        }
        else if (emulateDouble) {
            loadShader(GL.GL_FRAGMENT_SHADER, fragDFShaderCode)
        }
        else {
            loadShader(GL.GL_FRAGMENT_SHADER, fragSFShaderCode)
        }
        vertexTexShader = loadShader(GL.GL_VERTEX_SHADER, vertTexShaderCode)
        fragmentTexShader = loadShader(GL.GL_FRAGMENT_SHADER, fragTexShaderCode)


        GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // generate texture and framebuffer objects
        GL.glGenFramebuffers(1, fboIDs)
        GL.glGenTextures(3, texIDs)


        
        //======================================================================================
        // INITIALIZE LOW-RES TEXTURE
        //======================================================================================

        // bind and set texture parameters
        GL.glActiveTexture(GL.GL_TEXTURE0)
        GL.glBindTexture(GL.GL_TEXTURE_2D, texIDs[0])
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)

        // define texture specs
        GL.glTexImage2D(
                GL.GL_TEXTURE_2D,           // target
                0,                          // mipmap level
                GL.GL_RGBA8,                // internal format
                bgTexWidth, bgTexHeight,    // texture resolution
                0,                          // border
                GL.GL_RGBA,                 // format
                GL.GL_UNSIGNED_BYTE,        // type
                texLowResBuffer             // texture
        )



        //======================================================================================
        // INITIALIZE HIGH-RES TEXTURE 1
        //======================================================================================

        // bind and set texture parameters
        GL.glActiveTexture(GL.GL_TEXTURE1)
        GL.glBindTexture(GL.GL_TEXTURE_2D, texIDs[1])
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)

        // define texture specs
        GL.glTexImage2D(
                GL.GL_TEXTURE_2D,           // target
                0,                          // mipmap level
                GL.GL_RGBA8,                // internal format
                texWidth, texHeight,        // texture resolution
                0,                          // border
                GL.GL_RGBA,                 // format
                GL.GL_UNSIGNED_BYTE,        // type
                texHighResBuffer1           // texture
        )



        //======================================================================================
        // INITIALIZE HIGH-RES TEXTURE 2
        //======================================================================================

        // bind and set texture parameters
        GL.glActiveTexture(GL.GL_TEXTURE2)
        GL.glBindTexture(GL.GL_TEXTURE_2D, texIDs[2])
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)

        // define texture specs
        GL.glTexImage2D(
                GL.GL_TEXTURE_2D,           // target
                0,                          // mipmap level
                GL.GL_RGBA8,                // internal format
                texWidth, texHeight,        // texture resolution
                0,                          // border
                GL.GL_RGBA,                 // format
                GL.GL_UNSIGNED_BYTE,        // type
                texHighResBuffer2           // texture
        )




        // attach shaders and create program executables
        // render to texture program
        GL.glAttachShader(program, vertexShader)
        GL.glAttachShader(program, fragmentShader)
        GL.glLinkProgram(program)

        viewCoordsHandle =  GL.glGetAttribLocation(   program, "viewCoords"  )
        iterHandle       =  GL.glGetUniformLocation(  program, "maxIter"     )
        xScaleHandle     =  GL.glGetUniformLocation(  program, "xScale"      )
        yScaleHandle     =  GL.glGetUniformLocation(  program, "yScale"      )
        xOffsetHandle    =  GL.glGetUniformLocation(  program, "xOffset"     )
        yOffsetHandle    =  GL.glGetUniformLocation(  program, "yOffset"     )
        xTouchHandle     =  GL.glGetUniformLocation(  program, "xTouchPos"   )
        yTouchHandle     =  GL.glGetUniformLocation(  program, "yTouchPos"   )
        bgScaleHandle    =  GL.glGetUniformLocation(  program, "bgScale"     )

        // render from texture program
        GL.glAttachShader(texProgram, vertexTexShader)
        GL.glAttachShader(texProgram, fragmentTexShader)
        GL.glLinkProgram(texProgram)

        viewCoordsTexHandle = GL.glGetAttribLocation(  texProgram, "viewCoords"  )
        quadCoordsTexHandle = GL.glGetAttribLocation(  texProgram, "quadCoords"  )
        textureTexHandle    = GL.glGetUniformLocation( texProgram, "tex"         )

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

    fun renderToTexture(
            strictTranslate:    Boolean,
            xCoords:            DoubleArray,
            yCoords:            DoubleArray,
            xCoordsDD:          Array<DualDouble>,
            yCoordsDD:          Array<DualDouble>,
            xQuadCoords:        DoubleArray,
            yQuadCoords:        DoubleArray,
            maxIter:            Int,
            touchPos:           DoubleArray,
            bgScale:            FloatArray
    ) {

//        Log.d("RENDER", "render to texture -- start")


        //======================================================================================
        // PRE-RENDER PROCESSING
        //======================================================================================

        GL.glUseProgram(program)
        GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer
        
        val floatTouchPos = floatArrayOf( touchPos[0].toFloat(),     touchPos[1].toFloat()     )
        val xTouchPos = floatArrayOf( floatTouchPos[0], 0.0f )
        val yTouchPos = floatArrayOf( floatTouchPos[1], 0.0f )

        if (emulateQuad) {
            
            val xScaleDD = (xCoordsDD[1] - xCoordsDD[0]) * DualDouble(0.5, 0.0)
            val yScaleDD = (yCoordsDD[1] - yCoordsDD[0]) * DualDouble(0.5, 0.0)
            val xOffsetDD = xCoordsDD[1] - xScaleDD
            val yOffsetDD = yCoordsDD[1] - yScaleDD

            Log.d("TEST", "xScaleDD : $xScaleDD")
//            Log.d("TEST", "$yScaleDD")
//            Log.d("TEST", "$xOffsetDD")
//            Log.d("TEST", "$yOffsetDD")
            
            val xScaleQF = splitDD(xScaleDD)
            val yScaleQF = splitDD(yScaleDD)
            val xOffsetQF = splitDD(xOffsetDD)
            val yOffsetQF = splitDD(yOffsetDD)

            Log.d("TEST", "xScaleQF : (${xScaleQF[0]}, ${xScaleQF[1]}, ${xScaleQF[2]}, ${xScaleQF[3]})")
            Log.d("TEST", "${Float.MIN_VALUE}")
//            Log.d("TEST", "${yScaleQF[0]}")
//            Log.d("TEST", "${xOffsetQF[0]}")
//            Log.d("TEST", "${yOffsetQF[0]}")

            GL.glUniform4fv(xScaleHandle,  1,  xScaleQF,   0)
            GL.glUniform4fv(yScaleHandle,  1,  yScaleQF,   0)
            GL.glUniform4fv(xOffsetHandle, 1,  xOffsetQF,  0)
            GL.glUniform4fv(yOffsetHandle, 1,  yOffsetQF,  0)
            
        }
        else {

            val xScaleSD = (xCoords[1] - xCoords[0]) / 2.0
            val yScaleSD = (yCoords[1] - yCoords[0]) / 2.0
            val xOffsetSD = xCoords[1] - xScaleSD
            val yOffsetSD = yCoords[1] - yScaleSD

            if (emulateDouble) {
                
                val xScaleDF = splitSD(xScaleSD)
                val yScaleDF = splitSD(yScaleSD)
                val xOffsetDF = splitSD(xOffsetSD)
                val yOffsetDF = splitSD(yOffsetSD)

                Log.d("TEST", "xScaleDF: (${xScaleDF[0]}, ${xScaleDF[1]})")

                GL.glUniform2fv(xScaleHandle,  1,  xScaleDF,   0)
                GL.glUniform2fv(yScaleHandle,  1,  yScaleDF,   0)
                GL.glUniform2fv(xOffsetHandle, 1,  xOffsetDF,  0)
                GL.glUniform2fv(yOffsetHandle, 1,  yOffsetDF,  0)
                
            }
            else {
                
                val xScaleSF = floatArrayOf(xScaleSD.toFloat())
                val yScaleSF = floatArrayOf(yScaleSD.toFloat())
                val xOffsetSF = floatArrayOf(xOffsetSD.toFloat())
                val yOffsetSF = floatArrayOf(yOffsetSD.toFloat())

                Log.d("TEST", "xScaleSF: ${xScaleSF[0]}")

                GL.glUniform1fv(xScaleHandle,  1,  xScaleSF,   0)
                GL.glUniform1fv(yScaleHandle,  1,  yScaleSF,   0)
                GL.glUniform1fv(xOffsetHandle, 1,  xOffsetSF,  0)
                GL.glUniform1fv(yOffsetHandle, 1,  yOffsetSF,  0)
                
            }

        }

        GL.glEnableVertexAttribArray(viewCoordsHandle)
        GL.glUniform1i(iterHandle, maxIter)
        GL.glUniform2fv(xTouchHandle,  1,  xTouchPos, 0)
        GL.glUniform2fv(yTouchHandle,  1,  yTouchPos, 0)




        //======================================================================================
        // RENDER LOW-RES
        //======================================================================================

        GL.glViewport(0, 0, bgTexWidth, bgTexHeight)
        GL.glUniform1fv(bgScaleHandle, 1, bgScale, 0)
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
                texIDs[0],                      // texture
                0                               // level
        )

        val status2 = GL.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)
        if (status2 != GL.GL_FRAMEBUFFER_COMPLETE) {
            Log.d("FRAMEBUFFER", "$status2")
        }

        GL.glClear(GL.GL_COLOR_BUFFER_BIT)
        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)




        //======================================================================================
        // RENDER HIGH-RES
        //======================================================================================

        if (strictTranslate) {

            //===================================================================================
            // PRE-RENDER PROCESSING
            //===================================================================================

            val xIntersectQuadCoords : DoubleArray
            val yIntersectQuadCoords : DoubleArray
            val xIntersectViewCoords : DoubleArray
            val yIntersectViewCoords : DoubleArray

            val xComplementViewCoordsA : DoubleArray
            val yComplementViewCoordsA : DoubleArray

            val xComplementViewCoordsB = doubleArrayOf(-1.0, 1.0)
            val yComplementViewCoordsB : DoubleArray


            if (xQuadCoords[0] > -1.0) {
                xIntersectQuadCoords = doubleArrayOf(xQuadCoords[0], 1.0)
                xIntersectViewCoords = doubleArrayOf(-1.0, -xQuadCoords[0])
                xComplementViewCoordsA = doubleArrayOf(-1.0, xQuadCoords[0])
//                Log.d("QUADCOORDS", "xA: ${xComplementViewCoordsA[0]}, ${xComplementViewCoordsA[1]}")
            }
            else {
                xIntersectQuadCoords = doubleArrayOf(-1.0, xQuadCoords[1])
                xIntersectViewCoords = doubleArrayOf(-xQuadCoords[1], 1.0)
                xComplementViewCoordsA = doubleArrayOf(xQuadCoords[1], 1.0)
//                Log.d("QUADCOORDS", "xA: ${xComplementViewCoordsA[0]}, ${xComplementViewCoordsA[1]}")
            }


            if (yQuadCoords[0] > -1.0) {
                yIntersectQuadCoords = doubleArrayOf(yQuadCoords[0], 1.0)
                yIntersectViewCoords = doubleArrayOf(-1.0, -yQuadCoords[0])
                yComplementViewCoordsA = doubleArrayOf(yQuadCoords[0], 1.0)
                yComplementViewCoordsB = doubleArrayOf(-1.0, yQuadCoords[0])
//                Log.d("QUADCOORDS", "yA: ${yComplementViewCoordsA[0]}, ${yComplementViewCoordsA[1]}")
//                Log.d("QUADCOORDS", "yB: ${yComplementViewCoordsB[0]}, ${yComplementViewCoordsB[1]}")
            }
            else {
                yIntersectQuadCoords = doubleArrayOf(-1.0, yQuadCoords[1])
                yIntersectViewCoords = doubleArrayOf(-yQuadCoords[1], 1.0)
                yComplementViewCoordsA = doubleArrayOf(-1.0, yQuadCoords[1])
                yComplementViewCoordsB = doubleArrayOf(yQuadCoords[1], 1.0)
//                Log.d("QUADCOORDS", "yA: ${yComplementViewCoordsA[0]}, ${yComplementViewCoordsA[1]}")
//                Log.d("QUADCOORDS", "yB: ${yComplementViewCoordsB[0]}, ${yComplementViewCoordsB[1]}")
            }

            val complementViewCoordsA = floatArrayOf(
                    xComplementViewCoordsA[0].toFloat(),  yComplementViewCoordsA[1].toFloat(),  0.0f,     // top left
                    xComplementViewCoordsA[0].toFloat(),  yComplementViewCoordsA[0].toFloat(),  0.0f,     // bottom left
                    xComplementViewCoordsA[1].toFloat(),  yComplementViewCoordsA[0].toFloat(),  0.0f,     // bottom right
                    xComplementViewCoordsA[1].toFloat(),  yComplementViewCoordsA[1].toFloat(),  0.0f )    // top right
            val complementViewCoordsB = floatArrayOf(
                    xComplementViewCoordsB[0].toFloat(),  yComplementViewCoordsB[1].toFloat(),  0.0f,     // top left
                    xComplementViewCoordsB[0].toFloat(),  yComplementViewCoordsB[0].toFloat(),  0.0f,     // bottom left
                    xComplementViewCoordsB[1].toFloat(),  yComplementViewCoordsB[0].toFloat(),  0.0f,     // bottom right
                    xComplementViewCoordsB[1].toFloat(),  yComplementViewCoordsB[1].toFloat(),  0.0f )    // top right




            //===================================================================================
            // NOVEL RENDER -- TRANSLATION COMPLEMENT
            //===================================================================================

            GL.glViewport(0, 0, texWidth, texHeight)
            GL.glUniform1fv(bgScaleHandle, 1, floatArrayOf(1.0f), 0)
            viewChunkBuffer
                    .put(complementViewCoordsA)
                    .position(0)
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
                    texIDs[intHighResIndex],        // texture
                    0                               // level
            )

            GL.glClear(GL.GL_COLOR_BUFFER_BIT)
            GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)

            viewChunkBuffer
                    .put(complementViewCoordsB)
                    .position(0)

            GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)





            //===================================================================================
            // SAMPLE -- TRANSLATION INTERSECTION
            //===================================================================================

            GL.glUseProgram(texProgram)
            GL.glViewport(0, 0, texWidth, texHeight)

            val intersectQuadCoords = floatArrayOf(
                    xIntersectQuadCoords[0].toFloat(),  yIntersectQuadCoords[1].toFloat(),  0.0f,     // top left
                    xIntersectQuadCoords[0].toFloat(),  yIntersectQuadCoords[0].toFloat(),  0.0f,     // bottom left
                    xIntersectQuadCoords[1].toFloat(),  yIntersectQuadCoords[0].toFloat(),  0.0f,     // bottom right
                    xIntersectQuadCoords[1].toFloat(),  yIntersectQuadCoords[1].toFloat(),  0.0f )    // top right
            quadBuffer
                    .put(intersectQuadCoords)
                    .position(0)

            val intersectViewCoords = floatArrayOf(
                    xIntersectViewCoords[0].toFloat(),  yIntersectViewCoords[1].toFloat(),  0.0f,     // top left
                    xIntersectViewCoords[0].toFloat(),  yIntersectViewCoords[0].toFloat(),  0.0f,     // bottom left
                    xIntersectViewCoords[1].toFloat(),  yIntersectViewCoords[0].toFloat(),  0.0f,     // bottom right
                    xIntersectViewCoords[1].toFloat(),  yIntersectViewCoords[1].toFloat(),  0.0f )    // top right
            viewChunkBuffer
                    .put(intersectViewCoords)
                    .position(0)


            GL.glEnableVertexAttribArray(viewCoordsTexHandle)
            GL.glEnableVertexAttribArray(quadCoordsTexHandle)
            GL.glUniform1i(textureTexHandle, currHighResIndex)
            GL.glVertexAttribPointer(
                    viewCoordsTexHandle,        // index
                    3,                          // coordinates per vertex
                    GL.GL_FLOAT,                // type
                    false,                      // normalized
                    12,                         // coordinates per vertex * bytes per float
                    viewChunkBuffer             // coordinates
            )
            GL.glVertexAttribPointer(
                    quadCoordsTexHandle,        // index
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
                    texIDs[intHighResIndex],        // texture
                    0                               // level
            )

            GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)

            GL.glDisableVertexAttribArray(viewCoordsTexHandle)
            GL.glDisableVertexAttribArray(quadCoordsTexHandle)


            // swap intermediate and current texture indices
            val temp = intHighResIndex
            intHighResIndex = currHighResIndex
            currHighResIndex = temp


        }
        else {

            //===================================================================================
            // NOVEL RENDER -- ENTIRE TEXTURE
            //===================================================================================

            GL.glViewport(0, 0, texWidth, texHeight)
            GL.glUniform1fv(bgScaleHandle, 1, floatArrayOf(1.0f), 0)
            GL.glFramebufferTexture2D(
                    GL.GL_FRAMEBUFFER,              // target
                    GL.GL_COLOR_ATTACHMENT0,        // attachment
                    GL.GL_TEXTURE_2D,               // texture target
                    texIDs[currHighResIndex],       // texture
                    0                               // level
            )

            // check framebuffer status
            val status = GL.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)
            if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
                Log.d("FRAMEBUFFER", "$status")
            }

            GL.glClear(GL.GL_COLOR_BUFFER_BIT)

            for (i in 0..(numChunks - 1)) {

                val viewChunkCoords = floatArrayOf(
                        -1.0f + i * chunkInc, 1.0f, 0.0f,    // top left
                        -1.0f + i * chunkInc, -1.0f, 0.0f,    // bottom left
                        -1.0f + (i + 1) * chunkInc, -1.0f, 0.0f,    // bottom right
                        -1.0f + (i + 1) * chunkInc, 1.0f, 0.0f     // top right
                )
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


//        Log.d("RENDER", "render to texture -- end")


    }

    fun renderFromTexture(
            xQuadCoords:    DoubleArray,
            yQuadCoords:    DoubleArray,
            xQuadCoords2:   DoubleArray,
            yQuadCoords2:   DoubleArray
    ) {

//        Log.d("RENDER", "render from texture -- start")


        //======================================================================================
        // PRE-RENDER PROCESSING
        //======================================================================================

        GL.glUseProgram(texProgram)
        GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0)
        GL.glViewport(0, 0, screenWidth, screenHeight)

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

        GL.glEnableVertexAttribArray(viewCoordsTexHandle)
        GL.glEnableVertexAttribArray(quadCoordsTexHandle)




        //======================================================================================
        // RENDER LOW-RES
        //======================================================================================

        GL.glUniform1i(textureTexHandle, 0)    // use GL_TEXTURE0
        GL.glVertexAttribPointer(
                viewCoordsTexHandle,        // index
                3,                          // coordinates per vertex
                GL.GL_FLOAT,                // type
                false,                      // normalized
                12,                         // coordinates per vertex * bytes per float
                viewBuffer                  // coordinates
        )
        GL.glVertexAttribPointer(
                quadCoordsTexHandle,        // index
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

        GL.glUniform1i(textureTexHandle, currHighResIndex)
        GL.glVertexAttribPointer(
                viewCoordsTexHandle,        // index
                3,                          // coordinates per vertex
                GL.GL_FLOAT,                // type
                false,                      // normalized
                12,                         // coordinates per vertex * bytes per float
                viewBuffer                  // coordinates
        )
        GL.glVertexAttribPointer(
                quadCoordsTexHandle,        // index
                3,                          // coordinates per vertex
                GL.GL_FLOAT,                // type
                false,                      // normalized
                12,                         // coordinates per vertex * bytes per float
                quadBuffer                  // coordinates
        )

        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)




        GL.glDisableVertexAttribArray(viewCoordsTexHandle)
        GL.glDisableVertexAttribArray(quadCoordsTexHandle)

//        Log.d("RENDER", "render from texture -- end")

    }

}



data class DualDouble(
        var hi : Double,
        var lo : Double
) {

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

    operator fun times(b: DualDouble): DualDouble {
        var p = twoProd(hi, b.hi)
        p.lo += hi * b.lo
        p.lo += lo * b.hi
        p = quickTwoSum(p.hi, p.lo)
        return p
    }

}


class MainActivity : AppCompatActivity() {



    /* MotionEvent extension functions ------ */

    fun MotionEvent.focalLength() : Float {
        val f = focus()
        val pos = floatArrayOf(x, y)
        val dist = floatArrayOf(pos[0] - f[0], pos[1] - f[1])
        return Math.sqrt(Math.pow(dist[0].toDouble(), 2.0) +
                Math.pow(dist[1].toDouble(), 2.0)).toFloat()
    }

    fun MotionEvent.focus() : FloatArray {
        return if (pointerCount == 1) floatArrayOf(x, y)
        else {
            var sumX = 0.0f
            var sumY = 0.0f
            for (i in 0 until pointerCount) {
                sumX += getX(i)
                sumY += getY(i)
            }
            floatArrayOf(sumX/pointerCount, sumY/pointerCount)
        }
    }

    /* -------------------------------------- */



    inner class FractalSurfaceView(ctx : Context) : GLSurfaceView(ctx) {

        val r : FractalRenderer
        var reactionType : Int = 0
        val continuousRender = false

        private val prevFocus = floatArrayOf(0.0f, 0.0f)
        private var prevFocalLen = 1.0f


        init {
            setEGLContextClientVersion(3)                   // create OpenGL ES 3.0 context
            r = FractalRenderer(ctx)         // create renderer
            setRenderer(r)                                  // set renderer
            renderMode = RENDERMODE_WHEN_DIRTY              // only render on init and explicitly
//            continuousRender = !emulateDouble
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(e: MotionEvent?): Boolean {

            if (reactionType == 0) {
                // actions change fractal
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
                        r.newQuadFocus = true
                        //// Log.d("POINTER DOWN", "focus: $focus, focalLen: $prevFocalLen")
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val focus = e.focus()
                        val dx : Float = focus[0] - prevFocus[0]
                        val dy : Float = focus[1] - prevFocus[1]
                        r.translate(floatArrayOf(dx, dy))
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]
                        if (e.pointerCount > 1) {   // MULTI-TOUCH
                            val focalLen = e.focalLength()
                            val dFocalLen = focalLen / prevFocalLen
                            r.scale(dFocalLen, focus)
                            prevFocalLen = focalLen
                            // // Log.d("SCALE", "$dScale")
                        }

                        //// Log.d("MOVE", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                        //// Log.d("TRANSLATE", "dx: $dx, dy: $dy")
                        if (continuousRender) { r.renderToTex = true }
                        requestRender()

                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        //// Log.d("UP", "x: ${e.x}, y: ${e.y}, count: ${e.pointerCount}")
                        r.renderToTex = true
                        // r.renderFromTex = true
                        requestRender()
                        return true
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        //// Log.d("POINTER UP", "x: ${e.x}, y: ${e.y}, count: ${e.pointerCount}")
                        if (e.getPointerId(e.actionIndex) == 0) {
                            prevFocus[0] = e.getX(1)
                            prevFocus[1] = e.getY(1)
                        }
                        else if (e.getPointerId(e.actionIndex) == 1) {
                            prevFocus[0] = e.getX(0)
                            prevFocus[1] = e.getY(0)
                        }
                        return true
                    }

                }
            }
            else if (reactionType == 1) {
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
                                e.x.toDouble()/r.screenWidth.toDouble(),
                                e.y.toDouble()/r.screenHeight.toDouble()
                        )
                        r.setTouchPos(screenPos)
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
                                e.x.toDouble()/r.screenWidth.toDouble(),
                                e.y.toDouble()/r.screenHeight.toDouble()
                        )
                        r.setTouchPos(screenPos)
                        r.renderToTex = true

                        requestRender()
                        return true

                    }

                }
            }

            return false

        }

    }



    inner class FractalRenderer(private val ctx: Context) : GLSurfaceView.Renderer {

        val screenWidth : Int
        val screenHeight : Int
        private val ratio : Float
        private val screenRes : FloatArray

        var maxIter : Int = 63
        var renderToTex = false
        // var renderFromTex = true

        private var hasTranslated = false
        private var hasScaled = false

        // complex coordinate arrays for 32/64-bit precision
        private val xCoords : DoubleArray
        private val yCoords : DoubleArray

        // complex coordinate arrays for 128-bit precision
        private val xCoordsDD : Array<DualDouble>
        private val yCoordsDD : Array<DualDouble>

        private val xQuadCoords = doubleArrayOf(-1.0, 1.0)
        private val yQuadCoords = doubleArrayOf(-1.0, 1.0)

        private val bgScaleFloat = floatArrayOf(5.0f)
        private val bgScaleDouble = 5.0

        private val xQuadCoords2 = doubleArrayOf(-bgScaleDouble, bgScaleDouble)
        private val yQuadCoords2 = doubleArrayOf(-bgScaleDouble, bgScaleDouble)

        private var quadFocus = doubleArrayOf(0.0, 0.0)
        private val touchPos = doubleArrayOf(0.0, 0.0)

        var newQuadFocus = false

        private lateinit var f : Fractal

        init {

            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenRes = floatArrayOf(screenWidth.toFloat(), screenHeight.toFloat())
            // Log.d("WIDTH", "$screenWidth")
            // Log.d("HEIGHT", "$screenHeight")
            ratio = screenWidth.toFloat()/screenHeight.toFloat()

            xCoords = doubleArrayOf(-2.5, 1.0)
            yCoords = doubleArrayOf(-1.75/ratio, 1.75/ratio)
            xCoordsDD = arrayOf(DualDouble(-2.5, 0.0), DualDouble(1.0, 0.0))
            yCoordsDD = arrayOf(DualDouble(-1.75/ratio, 0.0), DualDouble(1.75/ratio, 0.0))
//            xCoords = doubleArrayOf(-1.75, 1.75)

        }

        fun translate(dScreenPos: FloatArray) {

            // update complex coordinates
            var dPos : DoubleArray

            if (f.emulateQuad) {
                dPos = doubleArrayOf(
                        (dScreenPos[0] / screenRes[0]).toDouble() * (xCoordsDD[1].hi - xCoordsDD[0].hi),
                        (dScreenPos[1] / screenRes[1]).toDouble() * (yCoordsDD[1].hi - yCoordsDD[0].hi)
                )
                val dPosDD = arrayOf(DualDouble(dPos[0], 0.0), DualDouble(dPos[1], 0.0))
                xCoordsDD[0] -= dPosDD[0]
                xCoordsDD[1] -= dPosDD[0]
                yCoordsDD[0] += dPosDD[1]
                yCoordsDD[1] += dPosDD[1]
            }
            else {
                dPos = doubleArrayOf(
                        (dScreenPos[0] / screenRes[0]).toDouble() * (xCoords[1] - xCoords[0]),
                        (dScreenPos[1] / screenRes[1]).toDouble() * (yCoords[1] - yCoords[0])
                )
                xCoords[0] -= dPos[0]
                xCoords[1] -= dPos[0]
                yCoords[0] += dPos[1]
                yCoords[1] += dPos[1]
            }


            // update texture quad coordinates
            dPos = doubleArrayOf(
                    (dScreenPos[0] / screenRes[0]).toDouble() * 2.0,
                    (dScreenPos[1] / screenRes[1]).toDouble() * 2.0
            )

            xQuadCoords[0] += dPos[0]
            xQuadCoords[1] += dPos[0]
            yQuadCoords[0] -= dPos[1]
            yQuadCoords[1] -= dPos[1]

            xQuadCoords2[0] += dPos[0]
            xQuadCoords2[1] += dPos[0]
            yQuadCoords2[0] -= dPos[1]
            yQuadCoords2[1] -= dPos[1]

            // magic
            quadFocus[0] += dPos[0]
            quadFocus[1] -= dPos[1]

            hasTranslated = true


            //// Log.d("TRANSLATE", "dPos: (${dPos[0]}, ${dPos[1]})")
            //// Log.d("COORDS", "xCoords: (${xCoords[0]}, ${xCoords[1]}), yCoords: (${yCoords[0]}, ${yCoords[1]})")
            // Log.d("COORDS", "xQuadCoords: (${xQuadCoords[0]}, ${xQuadCoords[1]}), yQuadCoords: (${yQuadCoords[0]}, ${yQuadCoords[1]})")

        }

        fun scale(dScale: Float, screenFocus: FloatArray) {

            // update complex coordinates
            // convert focus coordinates from screen space to complex space
            val prop = doubleArrayOf(
                    (screenFocus[0] / screenRes[0]).toDouble(),
                    (screenFocus[1] / screenRes[1]).toDouble()
            )
            val focus : DoubleArray

            //// Log.d("SCALE", "dScale: $dScale,  focus: (${focus[0]}, ${focus[1]})")

            if (f.emulateQuad) {
                focus = doubleArrayOf(
                        xCoordsDD[0].hi*(1 - prop[0]) + prop[0]*xCoordsDD[1].hi,
                        yCoordsDD[1].hi*(1 - prop[1]) + prop[1]*yCoordsDD[0].hi
                )
                val focusDD = arrayOf(DualDouble(focus[0], 0.0), DualDouble(focus[1], 0.0))
                val dScaleDD = DualDouble(1.0/dScale, 0.0)
                
                // translate focus to origin in complex coordinates
                xCoordsDD[0] -= focusDD[0]
                xCoordsDD[1] -= focusDD[0]
                yCoordsDD[0] -= focusDD[1]
                yCoordsDD[1] -= focusDD[1]

                // scale complex coordinates
                xCoordsDD[0] *= dScaleDD
                xCoordsDD[1] *= dScaleDD
                yCoordsDD[0] *= dScaleDD
                yCoordsDD[1] *= dScaleDD

                // translate origin back to focusDD in complex coordinates
                xCoordsDD[0] += focusDD[0]
                xCoordsDD[1] += focusDD[0]
                yCoordsDD[0] += focusDD[1]
                yCoordsDD[1] += focusDD[1]
            }
            else {
                focus = doubleArrayOf(
                        xCoords[0]*(1 - prop[0]) + prop[0]*xCoords[1],
                        yCoords[1]*(1 - prop[1]) + prop[1]*yCoords[0]
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


            // update texture quad coordinates
            // convert focus coordinates from screen space to quad space
            if (newQuadFocus) {
                val quadProp = doubleArrayOf(
                    (screenFocus[0] / screenRes[0]).toDouble(),
                    (screenFocus[1] / screenRes[1]).toDouble()
                )

                // half magic
                quadFocus[0] = (xQuadCoords[0] - quadFocus[0])*(1 - quadProp[0]) + quadProp[0]*(xQuadCoords[1] - quadFocus[0])
                quadFocus[1] = (yQuadCoords[1] - quadFocus[1])*(1 - quadProp[1]) + quadProp[1]*(yQuadCoords[0] - quadFocus[1])
                newQuadFocus = false
            }


            // Log.d("SCALE", "dScale: $dScale,  quadFocus: (${quadFocus[0]}, ${quadFocus[1]})")

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

            //// Log.d("COORDS", "xCoords: (${xCoords[0]}, ${xCoords[1]}), yCoords: (${yCoords[0]}, ${yCoords[1]})")
            // Log.d("COORDS", "xQuadCoords: (${xQuadCoords[0]}, ${xQuadCoords[1]}), yQuadCoords: (${yQuadCoords[0]}, ${yQuadCoords[1]})")

        }

        fun setTouchPos(screenPos : DoubleArray) {

            touchPos[0] = xCoords[0] + screenPos[0]*(xCoords[1] - xCoords[0])
            touchPos[1] = yCoords[1] - screenPos[1]*(yCoords[1] - yCoords[0])

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

            f = Fractal(ctx, screenWidth, screenHeight)

            f.renderToTexture(
                    false,
                    xCoords, yCoords,
                    xCoordsDD, yCoordsDD,
                    xQuadCoords, yQuadCoords,
                    maxIter,
                    touchPos,
                    bgScaleFloat
            )

        }

        override fun onDrawFrame(unused: GL10) {

            // Log.d("RENDER", "DRAW FRAME")

            // render to texture on ACTION_UP
            if (renderToTex) {

                val strictTranslate = hasTranslated && !hasScaled
//                Log.d("HEY", "hasTranslated == $hasTranslated")
//                Log.d("HEY", "hasScaled == $hasScaled")
//                Log.d("HEY", "$strictTranslate")

                f.renderToTexture(
                        strictTranslate,
                        xCoords, yCoords,
                        xCoordsDD, yCoordsDD,
                        xQuadCoords, yQuadCoords,
                        maxIter,
                        touchPos,
                        bgScaleFloat
                )

                renderToTex = false
                hasTranslated = false
                hasScaled = false
                resetQuadCoords()

            }

            // render from texture
            f.renderFromTexture(xQuadCoords, yQuadCoords, xQuadCoords2, yQuadCoords2)

        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {

        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // set up frame layout
        val frame = FrameLayout(this)
        frame.layoutParams = LP(LP(LP.MATCH_PARENT, LP.MATCH_PARENT))
        addContentView(frame, frame.layoutParams)

        // create GLSurfaceView and add to frame
        val fractalView = FractalSurfaceView(this)
        frame.addView(fractalView)

        // create SeekBar and add to frame
        val maxIterBar = SeekBar(this)
        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val p: Float = i.toFloat() / 100.0f
                fractalView.r.maxIter = ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(11) - 1)*p).toInt()
                if (fractalView.continuousRender) {
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (!fractalView.continuousRender) {
                    // Log.d("SEEKBAR", seekBar.progress.toString())
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
            }

        })
        maxIterBar.layoutParams = LP(LP.MATCH_PARENT, LP.WRAP_CONTENT, Gravity.BOTTOM)
        addContentView(maxIterBar, maxIterBar.layoutParams)
        maxIterBar.bringToFront()

        // create Button and add to frame
//        val lightPosToggle = Button(this)
//        lightPosToggle.layoutParams = LP(LP.WRAP_CONTENT, LP.WRAP_CONTENT, Gravity.BOTTOM)
//        lightPosToggle.setOnClickListener {
            // fractalView.r.renderFromTex = true
            // fractalView.requestRender()
//            if (fractalView.reactionType == 0) {fractalView.reactionType = 1}
//            else if (fractalView.reactionType == 1) {fractalView.reactionType = 0}
//        }
//        addContentView(lightPosToggle, lightPosToggle.layoutParams)
//        lightPosToggle.bringToFront()

    }


}
