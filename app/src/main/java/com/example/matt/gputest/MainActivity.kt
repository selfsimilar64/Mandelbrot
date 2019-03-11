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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ByteBuffer.allocateDirect
import java.util.*
import javax.microedition.khronos.opengles.GL10
import java.nio.IntBuffer
import kotlin.math.*



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
        screenWidth : Int,
        screenHeight : Int,
        private val emulateDouble : Boolean
) {

    private val numRootChunks = 4   // total chunks == numRootChunks**2
    private val chunkInc : Float = 2.0f / numRootChunks

    // coordinates of default view boundaries
    private val viewCoords = floatArrayOf(
            -1.0f,  1.0f,  0.0f,    // top left
            -1.0f, -1.0f,  0.0f,    // bottom left
            1.0f, -1.0f,  0.0f,     // bottom right
            1.0f,  1.0f,  0.0f )    // top right


    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)     // order to draw vertices

    private val program = GL.glCreateProgram()                 // program for rendering to texture
    private val texProgram = GL.glCreateProgram()              // program for rendering from texture

    private val viewBuffer : FloatBuffer
    private val viewChunkBuffer : FloatBuffer

    private val texByteBuf1 : ByteBuffer
    private val texByteBuf2 : ByteBuffer


    private val vertexShader : Int
    private val fragmentShader : Int
    private val vertexTexShader : Int
    private val fragmentTexShader : Int

    // define texture resolution
    private val texWidth = screenWidth
    private val texHeight = screenHeight
    private val bgTexWidth = screenWidth/8
    private val bgTexHeight = screenHeight/8
    private val bgTexRes = floatArrayOf(bgTexWidth.toFloat(), bgTexHeight.toFloat())

    // create variables to store texture and fbo IDs
    private val texIDs : IntBuffer = IntBuffer.allocate(2)
    private val fboIDs : IntBuffer = IntBuffer.allocate(1)


    // initialize byte buffer for view coordinates
    // num coord values * 4 bytes/float
    private val bbView : ByteBuffer = allocateDirect(viewCoords.size * 4)
    private val bbChunkView : ByteBuffer = allocateDirect(viewCoords.size * 4)

    // initialize byte buffer for the draw list
    // num coord values * 2 bytes/short
    private val dlb : ByteBuffer = allocateDirect(drawOrder.size * 2)


    init {

        // create float buffer of view coordinates
        bbView.order(ByteOrder.nativeOrder())
        viewBuffer = bbView.asFloatBuffer()
        viewBuffer.put(viewCoords)
        viewBuffer.position(0)

        bbChunkView.order(ByteOrder.nativeOrder())
        viewChunkBuffer = bbChunkView.asFloatBuffer()

        // create short buffer of draw order
        dlb.order(ByteOrder.nativeOrder())
        val drawListBuffer = dlb.asShortBuffer()
        drawListBuffer.put(drawOrder)
        drawListBuffer.position(0)

        // load all vertex and fragment shader code
        val vertFloatStream = ctx.resources.openRawResource(R.raw.vert)
        val vertFloatShaderCode = Scanner(vertFloatStream).useDelimiter("\\Z").next()
        vertFloatStream.close()

        val vertDoubleStream = ctx.resources.openRawResource(R.raw.vert_df64)
        val vertDoubleShaderCode = Scanner(vertDoubleStream).useDelimiter("\\Z").next()
        vertDoubleStream.close()

        val fragFloatStream = ctx.resources.openRawResource(R.raw.frag)
        val fragFloatShaderCode = Scanner(fragFloatStream).useDelimiter("\\Z").next()
        fragFloatStream.close()

        val fragDoubleStream = ctx.resources.openRawResource(R.raw.frag_df64)
        val fragDoubleShaderCode = Scanner(fragDoubleStream).useDelimiter("\\Z").next()
        fragDoubleStream.close()

        val fragTexStream = ctx.resources.openRawResource(R.raw.frag_tex)
        val fragTexShaderCode = Scanner(fragTexStream).useDelimiter("\\Z").next()
        fragTexStream.close()

        val vertTexStream = ctx.resources.openRawResource(R.raw.vert_tex)
        val vertTexShaderCode = Scanner(vertTexStream).useDelimiter("\\Z").next()
        vertTexStream.close()


        // create and compile shaders
        if (emulateDouble) {
            vertexShader = loadShader(GL.GL_VERTEX_SHADER, vertDoubleShaderCode)
            fragmentShader = loadShader(GL.GL_FRAGMENT_SHADER, fragDoubleShaderCode)
        }
        else {
            vertexShader = loadShader(GL.GL_VERTEX_SHADER, vertFloatShaderCode)
            fragmentShader = loadShader(GL.GL_FRAGMENT_SHADER, fragFloatShaderCode)
        }

        vertexTexShader = loadShader(GL.GL_VERTEX_SHADER, vertTexShaderCode)
        fragmentTexShader = loadShader(GL.GL_FRAGMENT_SHADER, fragTexShaderCode)


        // generate texture and framebuffer objects
        GL.glGenFramebuffers(1, fboIDs)
        GL.glGenTextures(2, texIDs)


        // allocate memory for textures
        texByteBuf1 = allocateDirect(texWidth * texHeight * 4)
        texByteBuf1.order(ByteOrder.nativeOrder())

        texByteBuf2 = allocateDirect(bgTexWidth * bgTexHeight * 4)
        texByteBuf2.order(ByteOrder.nativeOrder())


        // attach shaders and create program executables
        // render to texture program
        GL.glAttachShader(program, vertexShader)
        GL.glAttachShader(program, fragmentShader)
        GL.glLinkProgram(program)

        // render from texture program
        GL.glAttachShader(texProgram, vertexTexShader)
        GL.glAttachShader(texProgram, fragmentTexShader)
        GL.glLinkProgram(texProgram)

    }



    fun renderToTexture(
            xCoords:    DoubleArray,
            yCoords:    DoubleArray,
            maxIter:    Int,
            touchPos:   DoubleArray,
            bgScale:    FloatArray
    ) {

        Log.d("RENDER", "render to texture -- start")


        GL.glUseProgram(program)


        // derive scale and offset from complex space coordinates
        val doubleScale = doubleArrayOf(
                (xCoords[1] - xCoords[0]) / 2.0,
                (yCoords[1] - yCoords[0]) / 2.0
        )
        val doubleOffset = doubleArrayOf(
                (xCoords[1] / doubleScale[0] - 1.0) * doubleScale[0],
                (yCoords[1] / doubleScale[1] - 1.0) * doubleScale[1]
        )

        // split parameter doubles into dual-floats
        val floatScale = floatArrayOf(doubleScale[0].toFloat(), doubleScale[1].toFloat())
        val floatOffset = floatArrayOf(doubleOffset[0].toFloat(), doubleOffset[1].toFloat())
        val floatTouchPos = floatArrayOf(touchPos[0].toFloat(), touchPos[1].toFloat())
        
        val xScale = floatArrayOf(floatScale[0], 0.0f)
        val yScale = floatArrayOf(floatScale[1], 0.0f)
        val xOffset = floatArrayOf(floatOffset[0], 0.0f)
        val yOffset = floatArrayOf(floatOffset[1], 0.0f)
        val xTouchPos = floatArrayOf(floatTouchPos[0], 0.0f)
        val yTouchPos = floatArrayOf(floatTouchPos[1], 0.0f)
        
        if (emulateDouble) {
            xScale[1] = (doubleScale[0]-floatScale[0].toDouble()).toFloat()
            yScale[1] = (doubleScale[1]-floatScale[1].toDouble()).toFloat()
            xOffset[1] = (doubleOffset[0]-floatOffset[0].toDouble()).toFloat()
            yOffset[1] = (doubleOffset[1]-floatOffset[1].toDouble()).toFloat()
            xTouchPos[1] = (touchPos[0]-floatTouchPos[0].toDouble()).toFloat()
            yTouchPos[1] = (touchPos[1]-floatTouchPos[1].toDouble()).toFloat()
        }


        // create handles for shader uniforms
        val viewCoordsHandle =  GL.glGetAttribLocation(   program, "viewCoords"  )
        val xScaleHandle     =  GL.glGetUniformLocation(  program, "xScale"      )
        val yScaleHandle     =  GL.glGetUniformLocation(  program, "yScale"      )
        val xOffsetHandle    =  GL.glGetUniformLocation(  program, "xOffset"     )
        val yOffsetHandle    =  GL.glGetUniformLocation(  program, "yOffset"     )
        val texResHandle     =  GL.glGetUniformLocation(  program, "texRes"      )
        val iterHandle       =  GL.glGetUniformLocation(  program, "maxIter"     )
        val xTouchHandle     =  GL.glGetUniformLocation(  program, "xTouchPos"   )
        val yTouchHandle     =  GL.glGetUniformLocation(  program, "yTouchPos"   )
        val bgScaleHandle    =  GL.glGetUniformLocation(  program, "bgScale"     )


        // pass values to shaders
        GL.glUniform2fv(xScaleHandle, 1, xScale, 0)
        GL.glUniform2fv(yScaleHandle, 1, yScale, 0)
        GL.glUniform2fv(xOffsetHandle, 1, xOffset, 0)
        GL.glUniform2fv(yOffsetHandle, 1, yOffset, 0)
        GL.glUniform1i(iterHandle, maxIter)
        GL.glUniform2fv(xTouchHandle, 1, xTouchPos, 0)
        GL.glUniform2fv(yTouchHandle, 1, yTouchPos, 0)





        // RENDER LOW-RES

        GL.glBindTexture(GL.GL_TEXTURE_2D, texIDs[1])
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)

        GL.glTexImage2D(
                GL.GL_TEXTURE_2D,           // target
                0,                          // mipmap level
                GL.GL_RGBA8,                // internal format
                bgTexWidth, bgTexHeight,    // texture resolution
                0,                          // border
                GL.GL_RGBA,                 // format
                GL.GL_UNSIGNED_BYTE,        // type
                texByteBuf2                 // texture
        )

        GL.glEnableVertexAttribArray(viewCoordsHandle)
        GL.glVertexAttribPointer(
                viewCoordsHandle,       // index
                3,                      // coordinates per vertex
                GL.GL_FLOAT,            // type
                false,                  // normalized
                12,                     // coordinates per vertex * bytes per float
                viewBuffer              // coordinates
        )

        GL.glUniform1fv(bgScaleHandle, 1, bgScale, 0)
        GL.glUniform2fv(texResHandle, 1, bgTexRes, 0)

        // set viewport to texture resolution
        GL.glViewport(0, 0, bgTexWidth, bgTexHeight)

        // use external framebuffer
        GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboIDs[0])
        GL.glFramebufferTexture2D(
                GL.GL_FRAMEBUFFER,              // target
                GL.GL_COLOR_ATTACHMENT0,        // attachment
                GL.GL_TEXTURE_2D,               // texture target
                texIDs[1],                      // texture
                0                               // level
        )

        // check framebuffer status
        val status2 = GL.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)
        if (status2 != GL.GL_FRAMEBUFFER_COMPLETE) {
            // Log.d("FRAMEBUFFER", "STATUS == NOT COMPLETE")
        }

        // set background color to black
        GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GL.glClear(GL.GL_COLOR_BUFFER_BIT)

        // render to texture
        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, dlb)

        // disable view coordinates array
        GL.glDisableVertexAttribArray(viewCoordsHandle)





        // RENDER HIGH-RES

        // set viewport to texture resolution
        GL.glViewport(0, 0, texWidth, texHeight)

        // bind texture and set parameters
        GL.glBindTexture(GL.GL_TEXTURE_2D, texIDs[0])
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)

        GL.glTexImage2D(
                GL.GL_TEXTURE_2D,           // target
                0,                          // mipmap level
                GL.GL_RGBA8,                // internal format
                texWidth, texHeight,        // texture resolution
                0,                          // border
                GL.GL_RGBA,                 // format
                GL.GL_UNSIGNED_BYTE,        // type
                texByteBuf1                 // texture
        )

        GL.glUniform1fv(bgScaleHandle, 1, floatArrayOf(1.0f), 0)


        // attach texture0 to current framebuffer
        GL.glFramebufferTexture2D(
                GL.GL_FRAMEBUFFER,              // target
                GL.GL_COLOR_ATTACHMENT0,        // attachment
                GL.GL_TEXTURE_2D,               // texture target
                texIDs[0],                      // texture
                0                               // level
        )

        // check framebuffer status
        val status = GL.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)
        if (status == GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
            Log.d("FRAMEBUFFER", "$status")
        }

        // set background color to black
        GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GL.glClear(GL.GL_COLOR_BUFFER_BIT)

        GL.glEnableVertexAttribArray(viewCoordsHandle)

//        for (i in 0..(numRootChunks-1)) {
//            for (j in 0..(numRootChunks-1)) {
//
//                val viewChunkCoords = floatArrayOf(
//                        -1.0f + i*chunkInc,     -1.0f + (j+1)*chunkInc,  0.0f,    // top left
//                        -1.0f + i*chunkInc,     -1.0f + j*chunkInc,      0.0f,    // bottom left
//                        -1.0f + (i+1)*chunkInc, -1.0f + j*chunkInc,      0.0f,    // bottom right
//                        -1.0f + (i+1)*chunkInc, -1.0f + (j+1)*chunkInc,  0.0f     // top right
//                )
//                viewChunkBuffer.put(viewChunkCoords)
//                viewChunkBuffer.position(0)
//
//                GL.glVertexAttribPointer(
//                        viewCoordsHandle,           // index
//                        3,                          // coordinates per vertex
//                        GL.GL_FLOAT,                // type
//                        false,                      // normalized
//                        12,                         // coordinates per vertex * bytes per float
//                        viewChunkBuffer             // coordinates
//                )
//
//                // render to texture
//                GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, dlb)
//
//                // wait for chunk to finish rendering before rendering next chunk
//                GL.glFinish()
//
//            }
//        }

        for (i in 0..(numRootChunks-1)) {

            val viewChunkCoords = floatArrayOf(
                    -1.0f + i*chunkInc,      1.0f,  0.0f,    // top left
                    -1.0f + i*chunkInc,     -1.0f,  0.0f,    // bottom left
                    -1.0f + (i+1)*chunkInc, -1.0f,  0.0f,    // bottom right
                    -1.0f + (i+1)*chunkInc,  1.0f,  0.0f     // top right
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

            // render to texture
            GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, dlb)

            // wait for chunk to finish rendering before rendering next chunk
            GL.glFinish()

        }

        // disable view coordinates array
        GL.glDisableVertexAttribArray(viewCoordsHandle)


        Log.d("RENDER", "render to texture -- end")
        

    }

    fun renderFromTexture(
            xQuadCoords:    DoubleArray,
            yQuadCoords:    DoubleArray,
            xQuadCoords2:   DoubleArray,
            yQuadCoords2:   DoubleArray,
            screenRes:      FloatArray
    ) {

        Log.d("RENDER", "render from texture -- start")
        GL.glUseProgram(texProgram)



        // create float array of quad coordinates
        val quadCoords = floatArrayOf(
                xQuadCoords[0].toFloat(),  yQuadCoords[1].toFloat(),  0.0f,     // top left
                xQuadCoords[0].toFloat(),  yQuadCoords[0].toFloat(),  0.0f,     // bottom left
                xQuadCoords[1].toFloat(),  yQuadCoords[0].toFloat(),  0.0f,     // bottom right
                xQuadCoords[1].toFloat(),  yQuadCoords[1].toFloat(),  0.0f )    // top right

        // create float array of background quad coordinates
        val quadCoords2 = floatArrayOf(
                xQuadCoords2[0].toFloat(),  yQuadCoords2[1].toFloat(),  0.0f,     // top left
                xQuadCoords2[0].toFloat(),  yQuadCoords2[0].toFloat(),  0.0f,     // bottom left
                xQuadCoords2[1].toFloat(),  yQuadCoords2[0].toFloat(),  0.0f,     // bottom right
                xQuadCoords2[1].toFloat(),  yQuadCoords2[1].toFloat(),  0.0f )    // top right

        // initialize byte buffer for quad coordinates
        // 4 bytes per float
        val bbQuad : ByteBuffer = allocateDirect(quadCoords.size * 4)
        val bbQuad2 : ByteBuffer = allocateDirect(quadCoords2.size * 4)

        // create float buffer of quad coordinates
        bbQuad.order(ByteOrder.nativeOrder())
        val quadBuffer = bbQuad.asFloatBuffer()
        quadBuffer.put(quadCoords)
        quadBuffer.position(0)

        bbQuad2.order(ByteOrder.nativeOrder())
        val quadBuffer2 = bbQuad2.asFloatBuffer()
        quadBuffer2.put(quadCoords2)
        quadBuffer2.position(0)

        // create handles for shader uniforms
        val viewCoordsTexHandle = GL.glGetAttribLocation(texProgram, "viewCoords")
        val quadCoordsTexHandle = GL.glGetAttribLocation(texProgram, "quadCoords")
        val textureTexHandle = GL.glGetUniformLocation(texProgram, "tex")





        // RENDER LOW-RES

        GL.glUniform1i(textureTexHandle, 1)    // use GL_TEXTURE1

        // add view coordinates array
        GL.glEnableVertexAttribArray(viewCoordsTexHandle)
        GL.glVertexAttribPointer(
                viewCoordsTexHandle,        // index
                3,                          // coordinates per vertex
                GL.GL_FLOAT,                // type
                false,                      // normalized
                12,                         // coordinates per vertex * bytes per float
                viewBuffer                  // coordinates
        )

        // add quad coordinates array
        GL.glEnableVertexAttribArray(quadCoordsTexHandle)
        GL.glVertexAttribPointer(
                quadCoordsTexHandle,        // index
                3,                          // coordinates per vertex
                GL.GL_FLOAT,                // type
                false,                      // normalized
                12,                         // coordinates per vertex * bytes per float
                quadBuffer2                 // coordinates
        )

        // change framebuffer back to screen window
        GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0)

        // set viewport size to screen resolution
        GL.glViewport(0, 0, screenRes[0].toInt(), screenRes[1].toInt())

        GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)         // set background color to black
        GL.glClear(GL.GL_COLOR_BUFFER_BIT)              // clear screen

        GL.glActiveTexture(GL.GL_TEXTURE1)              // set active texture
        GL.glBindTexture(GL.GL_TEXTURE_2D, texIDs[1])   // bind texture

        // render from texture
        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, dlb)

        GL.glDisableVertexAttribArray(viewCoordsTexHandle)      // disable view coordinates array
        GL.glDisableVertexAttribArray(quadCoordsTexHandle)      // disable quad coordinates array




        // RENDER HIGH-RES

        GL.glUniform1i(textureTexHandle, 0)                 // use GL_TEXTURE0

        // add view coordinates array
        GL.glEnableVertexAttribArray(viewCoordsTexHandle)
        GL.glVertexAttribPointer(
                viewCoordsTexHandle,        // index
                3,                          // coordinates per vertex
                GL.GL_FLOAT,                // type
                false,                      // normalized
                12,                         // coordinates per vertex * bytes per float
                viewBuffer                  // coordinates
        )

        // add quad coordinates array
        GL.glEnableVertexAttribArray(quadCoordsTexHandle)
        GL.glVertexAttribPointer(
                quadCoordsTexHandle,        // index
                3,                          // coordinates per vertex
                GL.GL_FLOAT,                // type
                false,                      // normalized
                12,                         // coordinates per vertex * bytes per float
                quadBuffer                  // coordinates
        )


        // change framebuffer back to screen window
//        GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0)

        // set viewport size to screen resolution
        GL.glViewport(0, 0, screenRes[0].toInt(), screenRes[1].toInt())

//        GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)         // set background color to black
//        GL.glClear(GL.GL_COLOR_BUFFER_BIT)              // clear screen

        GL.glActiveTexture(GL.GL_TEXTURE0)              // set active texture
        GL.glBindTexture(GL.GL_TEXTURE_2D, texIDs[0])   // bind texture

        // render from texture
        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, dlb)

        GL.glDisableVertexAttribArray(viewCoordsTexHandle)      // disable view coordinates array
        GL.glDisableVertexAttribArray(quadCoordsTexHandle)      // disable quad coordinates array
        

        Log.d("RENDER", "render from texture -- end")

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
        val continuousRender : Boolean
        val emulateDouble : Boolean = true

        private val prevFocus = floatArrayOf(0.0f, 0.0f)
        private var prevFocalLen = 1.0f


        init {
            setEGLContextClientVersion(3)                   // create OpenGL ES 3.0 context
            r = FractalRenderer(ctx, emulateDouble)         // create renderer
            setRenderer(r)                                  // set renderer
            renderMode = RENDERMODE_WHEN_DIRTY              // only render on init and explicitly
            continuousRender = !emulateDouble
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
                        requestRender()

                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        //// Log.d("UP", "x: ${e.x}, y: ${e.y}, count: ${e.pointerCount}")
                        if (emulateDouble) {
                            r.renderToTex = true
                            // r.renderFromTex = false

                        }
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
                        r.touchPos[0] = r.xCoords[0] + screenPos[0]*(r.xCoords[1] - r.xCoords[0])
                        r.touchPos[1] = r.yCoords[1] - screenPos[1]*(r.yCoords[1] - r.yCoords[0])
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
                        r.touchPos[0] = r.xCoords[0] + screenPos[0]*(r.xCoords[1] - r.xCoords[0])
                        r.touchPos[1] = r.yCoords[1] - screenPos[1]*(r.yCoords[1] - r.yCoords[0])
                        r.renderToTex = true

                        requestRender()
                        return true

                    }

                }
            }

            return false

        }

    }

    inner class FractalRenderer(private val ctx: Context,
                                private val emulateDouble: Boolean) : GLSurfaceView.Renderer {

        val screenWidth : Int
        val screenHeight : Int
        private val ratio : Float
        private val screenRes : FloatArray

        val touchPos = doubleArrayOf(0.0, 0.0)
        var maxIter : Int = 63

        var renderToTex = false
        // var renderFromTex = true

        val xCoords : DoubleArray
        val yCoords : DoubleArray

        private val xQuadCoords = doubleArrayOf(-1.0, 1.0)
        private val yQuadCoords = doubleArrayOf(-1.0, 1.0)

        private val bgScaleFloat = floatArrayOf(5.0f)
        private val bgScaleDouble = 5.0

        private val xQuadCoords2 = doubleArrayOf(-bgScaleDouble, bgScaleDouble)
        private val yQuadCoords2 = doubleArrayOf(-bgScaleDouble, bgScaleDouble)

        private var quadFocus = doubleArrayOf(0.0, 0.0)

        var newQuadFocus = false

        lateinit var f : Fractal

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
//            xCoords = doubleArrayOf(-1.75, 1.75)
            yCoords = doubleArrayOf(-1.75/ratio, 1.75/ratio)

//            lightPos = floatArrayOf(1.0f/sqrt(2.0f), 1.0f/sqrt(2.0f))
            
        }

        fun translate(dScreenPos: FloatArray) {

            // update complex coordinates
            var dPos = doubleArrayOf(
                    (dScreenPos[0] / screenRes[0]).toDouble() * (xCoords[1] - xCoords[0]),
                    (dScreenPos[1] / screenRes[1]).toDouble() * (yCoords[1] - yCoords[0])
            )
            xCoords[0] -= dPos[0]
            xCoords[1] -= dPos[0]
            yCoords[0] += dPos[1]
            yCoords[1] += dPos[1]


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
            val focus = doubleArrayOf(
                    xCoords[0]*(1 - prop[0]) + prop[0]*xCoords[1],
                    yCoords[1]*(1 - prop[1]) + prop[1]*yCoords[0]
            )

            //// Log.d("SCALE", "dScale: $dScale,  focus: (${focus[0]}, ${focus[1]})")

            // translate focus to origin in complex coordinates
            xCoords[0] -= focus[0]
            xCoords[1] -= focus[0]
            yCoords[0] -= focus[1]
            yCoords[1] -= focus[1]

            // scale complex coordinates
            xCoords[0] = xCoords[0]/dScale
            xCoords[1] = xCoords[1]/dScale
            yCoords[0] = yCoords[0]/dScale
            yCoords[1] = yCoords[1]/dScale

            // translate origin back to focus in complex coordinates
            xCoords[0] += focus[0]
            xCoords[1] += focus[0]
            yCoords[0] += focus[1]
            yCoords[1] += focus[1]


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
            

            //// Log.d("COORDS", "xCoords: (${xCoords[0]}, ${xCoords[1]}), yCoords: (${yCoords[0]}, ${yCoords[1]})")
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

            // set background color to black
            GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

            // get OpenGL ES version
            Log.d("OPENGL ES", "VERSION == ${unused.glGetString(GL10.GL_VERSION)}")

            // get fragment shader precision
            val a : IntBuffer = IntBuffer.allocate(2)
            val b : IntBuffer = IntBuffer.allocate(1)
            GL.glGetShaderPrecisionFormat(GL.GL_FRAGMENT_SHADER, GL.GL_HIGH_FLOAT, a, b)
            Log.d("OPENGL ES", "FLOAT PRECISION == ${b[0]}")

            f = Fractal(ctx, screenWidth, screenHeight, emulateDouble)
            f.renderToTexture(xCoords, yCoords, maxIter, touchPos, bgScaleFloat)

        }

        override fun onDrawFrame(unused: GL10) {

            // Log.d("RENDER", "DRAW FRAME")

            // render to texture on ACTION_UP
            if (renderToTex) {

                f.renderToTexture(xCoords, yCoords, maxIter, touchPos, bgScaleFloat)
                renderToTex = false
                resetQuadCoords()

            }

            // render from texture
            f.renderFromTexture(xQuadCoords, yQuadCoords, xQuadCoords2, yQuadCoords2, screenRes)

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
