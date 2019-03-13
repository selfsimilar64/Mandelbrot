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
        private val screenWidth : Int,
        private val screenHeight : Int,
        private val emulateDouble : Boolean
) {

    private val numChunks = 6
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
    private val iterHandle : Int
    private val xScaleHandle : Int
    private val yScaleHandle : Int
    private val xOffsetHandle : Int
    private val yOffsetHandle : Int
    private val xTouchHandle : Int
    private val yTouchHandle : Int
    private val bgScaleHandle : Int

    private val texProgram = GL.glCreateProgram()
    private val viewCoordsTexHandle : Int
    private val quadCoordsTexHandle : Int
    private val textureTexHandle : Int

    private val vertexShader : Int
    private val fragmentShader : Int
    private val vertexTexShader : Int
    private val fragmentTexShader : Int

    // define texture resolutions
    private val texWidth = screenWidth
    private val texHeight = screenHeight
    private val bgTexWidth = screenWidth/8
    private val bgTexHeight = screenHeight/8

    // allocate memory for textures
    private val texHighResBuffer = 
            allocateDirect(texWidth * texHeight * 4)
            .order(ByteOrder.nativeOrder())
    private val texLowResBuffer =
            allocateDirect(bgTexWidth * bgTexHeight * 4)
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
    private val texIDs : IntBuffer = IntBuffer.allocate(2)
    private val fboIDs : IntBuffer = IntBuffer.allocate(1)

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


        GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // generate texture and framebuffer objects
        GL.glGenFramebuffers(1, fboIDs)
        GL.glGenTextures(2, texIDs)



        // initialize high-res texture
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
                texWidth, texHeight,        // texture resolution
                0,                          // border
                GL.GL_RGBA,                 // format
                GL.GL_UNSIGNED_BYTE,        // type
                texHighResBuffer            // texture
        )


        // initialize low-res texture
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
                bgTexWidth, bgTexHeight,    // texture resolution
                0,                          // border
                GL.GL_RGBA,                 // format
                GL.GL_UNSIGNED_BYTE,        // type
                texLowResBuffer             // texture
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



    fun renderToTexture(
            xCoords:    DoubleArray,
            yCoords:    DoubleArray,
            maxIter:    Int,
            touchPos:   DoubleArray,
            bgScale:    FloatArray
    ) {

        Log.d("RENDER", "render to texture -- start")
        GL.glUseProgram(program)
        GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboIDs[0])      // use external framebuffer


        // derive scale and offset from complex space coordinates
        val doubleScale = doubleArrayOf(
                (xCoords[1] - xCoords[0]) / 2.0,
                (yCoords[1] - yCoords[0]) / 2.0
        )
        val doubleOffset = doubleArrayOf(
                (xCoords[1] / doubleScale[0] - 1.0) * doubleScale[0],
                (yCoords[1] / doubleScale[1] - 1.0) * doubleScale[1]
        )

        // create parameter floats from doubles
        val floatScale = floatArrayOf(doubleScale[0].toFloat(), doubleScale[1].toFloat())
        val floatOffset = floatArrayOf(doubleOffset[0].toFloat(), doubleOffset[1].toFloat())
        val floatTouchPos = floatArrayOf(touchPos[0].toFloat(), touchPos[1].toFloat())

        // split parameters into dual-floats
        val xScale = floatArrayOf(floatScale[0], 0.0f)
        val yScale = floatArrayOf(floatScale[1], 0.0f)
        val xOffset = floatArrayOf(floatOffset[0], 0.0f)
        val yOffset = floatArrayOf(floatOffset[1], 0.0f)
        val xTouchPos = floatArrayOf(floatTouchPos[0], 0.0f)
        val yTouchPos = floatArrayOf(floatTouchPos[1], 0.0f)

        if (emulateDouble) {
            // generate low component of dual-floats
            xScale[1] = (doubleScale[0]-floatScale[0].toDouble()).toFloat()
            yScale[1] = (doubleScale[1]-floatScale[1].toDouble()).toFloat()
            xOffset[1] = (doubleOffset[0]-floatOffset[0].toDouble()).toFloat()
            yOffset[1] = (doubleOffset[1]-floatOffset[1].toDouble()).toFloat()
            xTouchPos[1] = (touchPos[0]-floatTouchPos[0].toDouble()).toFloat()
            yTouchPos[1] = (touchPos[1]-floatTouchPos[1].toDouble()).toFloat()
        }

        // pass parameters to shaders
        GL.glUniform1i( iterHandle, maxIter )
        GL.glUniform2fv( xScaleHandle,  1,  xScale,    0 )
        GL.glUniform2fv( yScaleHandle,  1,  yScale,    0 )
        GL.glUniform2fv( xOffsetHandle, 1,  xOffset,   0 )
        GL.glUniform2fv( yOffsetHandle, 1,  yOffset,   0 )
        GL.glUniform2fv( xTouchHandle,  1,  xTouchPos, 0 )
        GL.glUniform2fv( yTouchHandle,  1,  yTouchPos, 0 )

        // pass view coordinates to shaders
        GL.glEnableVertexAttribArray(viewCoordsHandle)
        GL.glVertexAttribPointer(
                viewCoordsHandle,       // index
                3,                      // coordinates per vertex
                GL.GL_FLOAT,            // type
                false,                  // normalized
                12,                     // coordinates per vertex * bytes per float
                viewBuffer              // coordinates
        )





        // RENDER LOW-RES

        GL.glViewport(0, 0, bgTexWidth, bgTexHeight)
        GL.glUniform1fv(bgScaleHandle, 1, bgScale, 0)
        GL.glFramebufferTexture2D(
                GL.GL_FRAMEBUFFER,              // target
                GL.GL_COLOR_ATTACHMENT0,        // attachment
                GL.GL_TEXTURE_2D,               // texture target
                texIDs[1],                      // texture
                0                               // level
        )

        // check framebuffer status
        val status2 = GL.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)
        if (status2 != GL.GL_FRAMEBUFFER_COMPLETE) { Log.d("FRAMEBUFFER", "$status2") }

        GL.glClear(GL.GL_COLOR_BUFFER_BIT)
        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)





        // RENDER HIGH-RES

        GL.glViewport(0, 0, texWidth, texHeight)
        GL.glUniform1fv(bgScaleHandle, 1, floatArrayOf(1.0f), 0)
        GL.glFramebufferTexture2D(
                GL.GL_FRAMEBUFFER,              // target
                GL.GL_COLOR_ATTACHMENT0,        // attachment
                GL.GL_TEXTURE_2D,               // texture target
                texIDs[0],                      // texture
                0                               // level
        )

        // check framebuffer status
        val status = GL.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)
        if (status != GL.GL_FRAMEBUFFER_COMPLETE) { Log.d("FRAMEBUFFER", "$status") }

        GL.glClear(GL.GL_COLOR_BUFFER_BIT)

        for (i in 0..(numChunks-1)) {

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

            GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, drawListBuffer)
            GL.glFinish()   // force chunk to finish rendering before continuing

        }




        GL.glDisableVertexAttribArray(viewCoordsHandle)
        Log.d("RENDER", "render to texture -- end")


    }

    fun renderFromTexture(
            xQuadCoords:    DoubleArray,
            yQuadCoords:    DoubleArray,
            xQuadCoords2:   DoubleArray,
            yQuadCoords2:   DoubleArray
    ) {

        Log.d("RENDER", "render from texture -- start")
        GL.glUseProgram(texProgram)
        GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0)
        GL.glViewport(0, 0, screenWidth, screenHeight)

        // create float array of quad coordinates
        val quadCoords = floatArrayOf(
                xQuadCoords[0].toFloat(),  yQuadCoords[1].toFloat(),  0.0f,     // top left
                xQuadCoords[0].toFloat(),  yQuadCoords[0].toFloat(),  0.0f,     // bottom left
                xQuadCoords[1].toFloat(),  yQuadCoords[0].toFloat(),  0.0f,     // bottom right
                xQuadCoords[1].toFloat(),  yQuadCoords[1].toFloat(),  0.0f )    // top right

        // create float array of background quad coordinates
        val bgQuadCoords = floatArrayOf(
                xQuadCoords2[0].toFloat(),  yQuadCoords2[1].toFloat(),  0.0f,     // top left
                xQuadCoords2[0].toFloat(),  yQuadCoords2[0].toFloat(),  0.0f,     // bottom left
                xQuadCoords2[1].toFloat(),  yQuadCoords2[0].toFloat(),  0.0f,     // bottom right
                xQuadCoords2[1].toFloat(),  yQuadCoords2[1].toFloat(),  0.0f )    // top right

        quadBuffer.put(quadCoords).position(0)
        bgQuadBuffer.put(bgQuadCoords).position(0)

        GL.glEnableVertexAttribArray(viewCoordsTexHandle)
        GL.glEnableVertexAttribArray(quadCoordsTexHandle)





        // RENDER LOW-RES

        GL.glUniform1i(textureTexHandle, 1)    // use GL_TEXTURE1
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




        // RENDER HIGH-RES

        GL.glUniform1i(textureTexHandle, 0)     // use GL_TEXTURE0
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
        val continuousRender = false
        val emulateDouble = true

        private val prevFocus = floatArrayOf(0.0f, 0.0f)
        private var prevFocalLen = 1.0f


        init {
            setEGLContextClientVersion(3)                   // create OpenGL ES 3.0 context
            r = FractalRenderer(ctx, emulateDouble)         // create renderer
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
