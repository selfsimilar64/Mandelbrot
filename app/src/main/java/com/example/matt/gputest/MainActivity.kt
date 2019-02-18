package com.example.matt.gputest

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.content.Context
import android.graphics.PointF
import javax.microedition.khronos.egl.EGLConfig
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.opengl.GLES32 as GL
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
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



class Fractal(ctx: Context, private val emulateDouble: Boolean,
              private val screenRes: FloatArray) {

    private val squareCoords = floatArrayOf(
            -1.0f,  1.0f,  0.0f,    // top left
            -1.0f, -1.0f,  0.0f,    // bottom left
            1.0f, -1.0f,  0.0f,     // bottom right
            1.0f,  1.0f,  0.0f )    // top right

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)      // order to draw vertices
    private val program = GL.glCreateProgram()              // create empty OpenGL program
    private val texProgram = GL.glCreateProgram()
    private val coordsPerVertex = 3
    private val vertexStride = coordsPerVertex * 4            // 4 bytes/float
    private val vertexBuffer : FloatBuffer
    private val vertexShader : Int
    private val fragmentShader : Int
    private val vertexTexShader : Int
    private val fragmentTexShader : Int



    private val texWidth = 1440
    private val texHeight = 2392
    private val texRes = floatArrayOf(texWidth.toFloat(), texHeight.toFloat())

    // create variables to store texture and fbo IDs
    private val texID : IntBuffer = IntBuffer.allocate(1)
    private val fboID : IntBuffer = IntBuffer.allocate(1)



    // initialize vertex byte buffer for shape coordinates
    // num coord values * 4 bytes/float
    private val bb : ByteBuffer = allocateDirect(squareCoords.size * 4)

    // initialize byte buffer for the draw list
    // num coord values * 2 bytes/short
    private val dlb : ByteBuffer = allocateDirect(drawOrder.size * 2)


    init {
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(squareCoords)
        vertexBuffer.position(0)

        dlb.order(ByteOrder.nativeOrder())
        val drawListBuffer = dlb.asShortBuffer()
        drawListBuffer.put(drawOrder)
        drawListBuffer.position(0)

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

        // prepare shaders
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


        // genereate texture and fbo
        GL.glGenFramebuffers(1, fboID)
        GL.glGenTextures(1, texID)
        // GLES20.glGenRenderbuffers(1, depthRb, 0); // the depth buffer

        // bind texture and set parameters
        GL.glBindTexture(GL.GL_TEXTURE_2D, texID[0])
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT)
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT)

        val texByteBuf : ByteBuffer = allocateDirect(texWidth * texHeight * 4)
        texByteBuf.order(ByteOrder.nativeOrder())

        GL.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8, texWidth, texHeight, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, texByteBuf)

        // GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRb[0]);
        // GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, texW, texH);





        GL.glAttachShader(program, vertexShader)        // add vertex shader
        GL.glAttachShader(program, fragmentShader)      // add fragment shader
        GL.glLinkProgram(program)                       // create OpenGL program executables

        GL.glAttachShader(texProgram, vertexTexShader)
        GL.glAttachShader(texProgram, fragmentTexShader)
        GL.glLinkProgram(texProgram)

    }

    fun renderToTexture(
            doubleScale:   DoubleArray,
            doubleOffset:  DoubleArray,
            maxIter:       Int,
            lightPos:      FloatArray
    ) {


        GL.glUseProgram(program)        // add program to OpenGL environment


        // split parameter doubles into dual-floats
        val floatScale = floatArrayOf(doubleScale[0].toFloat(), doubleScale[1].toFloat())
        val floatOffset = floatArrayOf(doubleOffset[0].toFloat(), doubleOffset[1].toFloat())
        val xScale = floatArrayOf(floatScale[0], 0.0f)
        val yScale = floatArrayOf(floatScale[1], 0.0f)
        val xOffset = floatArrayOf(floatOffset[0], 0.0f)
        val yOffset = floatArrayOf(floatOffset[1], 0.0f)
        if (emulateDouble) {

            xScale[1] = (doubleScale[0]-floatScale[0].toDouble()).toFloat()
            yScale[1] = (doubleScale[1]-floatScale[1].toDouble()).toFloat()
            xOffset[1] = (doubleOffset[0]-floatOffset[0].toDouble()).toFloat()
            yOffset[1] = (doubleOffset[1]-floatOffset[1].toDouble()).toFloat()

        }


        val posHandle = GL.glGetAttribLocation(program, "vPos")
        val xScaleHandle = GL.glGetUniformLocation(program, "xScale")
        val yScaleHandle = GL.glGetUniformLocation(program, "yScale")
        val xOffsetHandle = GL.glGetUniformLocation(program, "xOffset")
        val yOffsetHandle = GL.glGetUniformLocation(program, "yOffset")
        val texResHandle = GL.glGetUniformLocation(program, "texRes")
        val iterHandle = GL.glGetUniformLocation(program, "maxIter")
        val lightHandle = GL.glGetUniformLocation(program, "lightPos")


        // pass uniform scale, offset, and maxIter to shader
        GL.glUniform2fv(xScaleHandle, 1, xScale, 0)
        GL.glUniform2fv(yScaleHandle, 1, yScale, 0)
        GL.glUniform2fv(xOffsetHandle, 1, xOffset, 0)
        GL.glUniform2fv(yOffsetHandle, 1, yOffset, 0)
        GL.glUniform2fv(texResHandle, 1, texRes, 0)
        GL.glUniform1i(iterHandle, maxIter)
        GL.glUniform2fv(lightHandle, 1, lightPos, 0)

        // add attribute array of vertices
        GL.glEnableVertexAttribArray(posHandle)
        GL.glVertexAttribPointer(posHandle, coordsPerVertex,
                GL.GL_FLOAT, false,
                vertexStride, vertexBuffer)


        GL.glViewport(0, 0, texWidth, texHeight)
        GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboID[0])
        GL.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, texID[0], 0)
        // GL.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, depthRb[0])

        val status = GL.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)
        if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
            Log.d("FRAMEBUFFER", "STATUS == NOT COMPLETE")
        }

        GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GL.glClear(GL.GL_COLOR_BUFFER_BIT)

        // Draw the square
        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, dlb)
        GL.glDisableVertexAttribArray(posHandle)        // disable vertex array

    }

    fun renderFromTexture(
            doubleScale:    DoubleArray,
            doubleOffset:   DoubleArray,
            screenRes:      FloatArray,
            m:              FloatArray
    ) {

        GL.glUseProgram(texProgram)

        // split parameter doubles into dual-floats
        val floatScale = floatArrayOf(doubleScale[0].toFloat(), doubleScale[1].toFloat())
        val floatOffset = floatArrayOf(doubleOffset[0].toFloat(), doubleOffset[1].toFloat())
        val xScale = floatArrayOf(floatScale[0], 0.0f)
        val yScale = floatArrayOf(floatScale[1], 0.0f)
        val xOffset = floatArrayOf(floatOffset[0], 0.0f)
        val yOffset = floatArrayOf(floatOffset[1], 0.0f)
        if (emulateDouble) {

            xScale[1] = (doubleScale[0]-floatScale[0].toDouble()).toFloat()
            yScale[1] = (doubleScale[1]-floatScale[1].toDouble()).toFloat()
            xOffset[1] = (doubleOffset[0]-floatOffset[0].toDouble()).toFloat()
            yOffset[1] = (doubleOffset[1]-floatOffset[1].toDouble()).toFloat()

        }

        val posTexHandle = GL.glGetAttribLocation(texProgram, "vPos")
        val xScaleHandle = GL.glGetUniformLocation(program, "xScale")
        val yScaleHandle = GL.glGetUniformLocation(program, "yScale")
        val xOffsetHandle = GL.glGetUniformLocation(program, "xOffset")
        val yOffsetHandle = GL.glGetUniformLocation(program, "yOffset")
        val texHandle = GL.glGetUniformLocation(texProgram, "tex")
        val resTexHandle = GL.glGetUniformLocation(texProgram, "screenRes")
        val mHandle = GL.glGetUniformLocation(texProgram, "m")

        GL.glUniform1i(texHandle, 0)    // use GL_TEXTURE0 ?
        GL.glUniform2fv(xScaleHandle, 1, xScale, 0)
        GL.glUniform2fv(yScaleHandle, 1, yScale, 0)
        GL.glUniform2fv(xOffsetHandle, 1, xOffset, 0)
        GL.glUniform2fv(yOffsetHandle, 1, yOffset, 0)
        GL.glUniform2fv(resTexHandle, 1, screenRes, 0)
        GL.glUniformMatrix4fv(mHandle, 1, true, m, 0)

        // add attribute array of vertices
        GL.glEnableVertexAttribArray(posTexHandle)
        GL.glVertexAttribPointer(posTexHandle, coordsPerVertex,
                GL.GL_FLOAT, false,
                vertexStride, vertexBuffer)


        // change framebuffer back to screen window
        GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0)
        GL.glViewport(0, 0, screenRes[0].toInt(), screenRes[1].toInt())

        GL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GL.glClear(GL.GL_COLOR_BUFFER_BIT)

        GL.glActiveTexture(GL.GL_TEXTURE0)
        GL.glBindTexture(GL.GL_TEXTURE_2D, texID[0])


        GL.glDrawElements(GL.GL_TRIANGLES, drawOrder.size, GL.GL_UNSIGNED_SHORT, dlb)
        GL.glDisableVertexAttribArray(posTexHandle)        // disable vertex array

    }

}


fun loadShader(type: Int, shaderCode: String): Int {

    // create a vertex shader type (GL.GL_VERTEX_SHADER)
    // or a fragment shader type (GL.GL_FRAGMENT_SHADER)
    val shader = GL.glCreateShader(type)

    // add the source code to the shader and compile it
    GL.glShaderSource(shader, shaderCode)
    GL.glCompileShader(shader)

    return shader
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

        val renderer : FractalRenderer
        var reactionType : Int = 0
        val continuousRender : Boolean
        val emulateDouble : Boolean = true

        private val prevFocus = floatArrayOf(0.0f, 0.0f)
        private var prevFocalLen = 1.0f


        init {
            setEGLContextClientVersion(3)                   // create OpenGL ES 3.0 context
            renderer = FractalRenderer(ctx, emulateDouble)  // create renderer
            setRenderer(renderer)                           // set renderer
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
                        Log.d("DOWN", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                        return true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        val focus = e.focus()
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]
                        prevFocalLen = e.focalLength()
                        Log.d("POINTER DOWN", "focus: $focus, focalLen: $prevFocalLen")
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val focus = e.focus()
                        val dx : Float = focus[0] - prevFocus[0]
                        val dy : Float = focus[1] - prevFocus[1]
                        renderer.translate(floatArrayOf(dx, dy))
                        prevFocus[0] = focus[0]
                        prevFocus[1] = focus[1]
                        if (e.pointerCount > 1) {   // MULTI-TOUCH
                            val focalLen = e.focalLength()
                            val dFocalLen = focalLen / prevFocalLen
                            renderer.scale(dFocalLen)
                            prevFocalLen = focalLen
                            // Log.d("SCALE", "$dScale")
                        }

                        // Log.d("MOVE", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                        Log.d("TRANSLATE", "dx: $dx, dy: $dy")
                        requestRender()

                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.d("UP", "x: ${e.x}, y: ${e.y}, count: ${e.pointerCount}")
                        if (emulateDouble) {
                            renderer.renderToTex()
                        }
                        requestRender()
                        invalidate()
                        return true
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        Log.d("POINTER UP", "x: ${e.x}, y: ${e.y}, count: ${e.pointerCount}")
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
                        Log.d("DOWN", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                        val u : Float = e.x - renderer.screenWidth
                        val v : Float = e.y - renderer.screenHeight
                        val r : Float = sqrt(u*u + v*v)
                        renderer.lightPos = floatArrayOf(u/r, v/r)
                        Log.d("LIGHTPOS", "u: ${u/r}, v: ${v/r}")
                        if (continuousRender) { requestRender() }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.d("UP", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                        if (!continuousRender) {
                            Log.d("LIGHTPOS", "u: ${renderer.lightPos[0]}, v: ${renderer.lightPos[1]}")
                            requestRender()
                            invalidate()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        Log.d("MOVE", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                        val u : Float = e.x - renderer.screenWidth/2.0f
                        val v : Float = e.y - renderer.screenHeight/2.0f
                        val r : Float = sqrt(u.pow(2) + v.pow(2))
                        renderer.lightPos = floatArrayOf(u/r, -v/r)
                        if (continuousRender) {
                            Log.d("LIGHTPOS", "u: ${u/r}, v: ${-v/r}")
                            requestRender()
                        }
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

        private val T : DoubleArray
        private val S : DoubleArray

        var lightPos : FloatArray
        var maxIter : Int = 63

        var renderToTex = false
        private val i = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f,
                                     0.0f, 1.0f, 0.0f, 0.0f,
                                     0.0f, 0.0f, 1.0f, 0.0f,
                                     0.0f, 0.0f, 0.0f, 1.0f)
        private var m = i.clone()

        lateinit var f : Fractal

        init {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenRes = floatArrayOf(screenWidth.toFloat(), screenHeight.toFloat())
            Log.d("WIDTH", "$screenWidth")
            Log.d("HEIGHT", "$screenHeight")
            ratio = screenWidth.toFloat()/screenHeight.toFloat()
            S = doubleArrayOf(1.75, 1.75/ratio)
            T = doubleArrayOf(-0.75, 0.0)
            lightPos = floatArrayOf(1.0f/sqrt(2.0f), 1.0f/sqrt(2.0f))
        }

        fun translate(dPos: FloatArray) {
            T[0] -= (dPos[0] / screenWidth).toDouble() * 2.0 * S[0]
            T[1] += (dPos[1] / screenHeight).toDouble() * 2.0 * S[1]
            m[3] += 2.0f * dPos[0] / screenWidth.toFloat()
            m[7] -= 2.0f * dPos[1] / screenHeight.toFloat()
            // Log.d("TRANSLATE", "dPos: (${dPos.x}, ${dPos.y}),  offset: (${offset[0]}, ${offset[1]})")
        }
        fun scale(dScale: Float) {
            Log.d("SCALE", "$dScale")
            S[0] /= dScale.toDouble()
            S[1] /= dScale.toDouble()
            m[0] *= dScale
            m[5] *= dScale
            // Log.d("SCALE", "dScale: $dScale,  scale: (${scale[0]}, ${scale[1]})")
        }
        fun renderToTex() {
            renderToTex = true
            m = i.clone()
        }

        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

            // set background color to black
            GL.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f )

            // get OpenGL ES version
            Log.d("VERSION", unused.glGetString(GL10.GL_VERSION))

            // get fragment shader precision
            val a : IntBuffer = IntBuffer.allocate(2)
            val b : IntBuffer = IntBuffer.allocate(1)
            GL.glGetShaderPrecisionFormat(GL.GL_FRAGMENT_SHADER, GL.GL_HIGH_FLOAT, a, b)
            Log.d("PRECISION", b[0].toString())

            f = Fractal(ctx, emulateDouble, screenRes)
            f.renderToTexture(S, T, maxIter, lightPos)


        }

        override fun onDrawFrame(unused: GL10) {
            if (renderToTex) {
                f.renderToTexture(S, T, maxIter, lightPos)
                renderToTex = false
            }
            f.renderFromTexture(S, T, screenRes, m)
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            GL.glViewport(0, 0, width, height)
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
                fractalView.renderer.maxIter = ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(10) - 1)*p).toInt()
                if (fractalView.continuousRender) {
                    Log.d("SEEKBAR", i.toString())
                    fractalView.renderer.renderToTex = true
                    fractalView.requestRender()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (!fractalView.continuousRender) {
                    Log.d("SEEKBAR", seekBar.progress.toString())
                    fractalView.renderer.renderToTex = true
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
//            if (fractalView.reactionType == 0) {fractalView.reactionType = 1}
//            else if (fractalView.reactionType == 1) {fractalView.reactionType = 0}
//        }
//        addContentView(lightPosToggle, lightPosToggle.layoutParams)
//        lightPosToggle.bringToFront()

    }


}
