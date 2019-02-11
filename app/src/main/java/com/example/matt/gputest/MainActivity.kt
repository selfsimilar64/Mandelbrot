package com.example.matt.gputest

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.content.Context
import android.graphics.PointF
import javax.microedition.khronos.egl.EGLConfig
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.opengl.GLES32
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


class Fractal(ctx: Context, private val emulateDouble: Boolean) {

    private val squareCoords = floatArrayOf(
            -1.0f,  1.0f,  0.0f,    // top left
            -1.0f, -1.0f,  0.0f,    // bottom left
            1.0f, -1.0f,  0.0f,     // bottom right
            1.0f,  1.0f,  0.0f )    // top right

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)      // order to draw vertices
    private val program = GLES32.glCreateProgram()              // create empty OpenGL program
    private val coordsPerVertex = 3
    private val vertexStride = coordsPerVertex * 4            // 4 bytes/float
    private val vertexBuffer : FloatBuffer
    private val vertexShader : Int
    private val fragmentShader : Int

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

        // prepare shaders
        if (emulateDouble) {
            vertexShader = loadShader(GLES32.GL_VERTEX_SHADER, vertDoubleShaderCode)
            fragmentShader = loadShader(GLES32.GL_FRAGMENT_SHADER, fragDoubleShaderCode)
        }
        else {
            vertexShader = loadShader(GLES32.GL_VERTEX_SHADER, vertFloatShaderCode)
            fragmentShader = loadShader(GLES32.GL_FRAGMENT_SHADER, fragFloatShaderCode)
        }

        GLES32.glAttachShader(program, vertexShader)       // add vertex shader
        GLES32.glAttachShader(program, fragmentShader)     // add fragment shader
        GLES32.glLinkProgram(program)                      // create OpenGL program executables
    }

    fun draw(doubleScale: DoubleArray, doubleOffset: DoubleArray, maxIter: Int, lightPos: FloatArray,
             screenRes: FloatArray) {

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

        GLES32.glUseProgram(program)        // add program to OpenGL environment

        val posHandle = GLES32.glGetAttribLocation(program, "vPos")
        val xScaleHandle = GLES32.glGetUniformLocation(program, "xScale")
        val yScaleHandle = GLES32.glGetUniformLocation(program, "yScale")
        val xOffsetHandle = GLES32.glGetUniformLocation(program, "xOffset")
        val yOffsetHandle = GLES32.glGetUniformLocation(program, "yOffset")
        val emulateHandle = GLES32.glGetUniformLocation(program, "emulateDouble")
        val resHandle = GLES32.glGetUniformLocation(program, "screenRes")
        val iterHandle = GLES32.glGetUniformLocation(program, "maxIter")
        val lightHandle = GLES32.glGetUniformLocation(program, "lightPos")

        // pass uniform scale, offset, and maxIter to shader
        GLES32.glUniform2fv(xScaleHandle, 1, xScale, 0)
        GLES32.glUniform2fv(yScaleHandle, 1, yScale, 0)
        GLES32.glUniform2fv(xOffsetHandle, 1, xOffset, 0)
        GLES32.glUniform2fv(yOffsetHandle, 1, yOffset, 0)
        GLES32.glUniform2fv(resHandle, 1, screenRes, 0)
        GLES32.glUniform1i(iterHandle, maxIter)
        GLES32.glUniform2fv(lightHandle, 1, lightPos, 0)

        // add attribute array of vertices
        GLES32.glEnableVertexAttribArray(posHandle)
        GLES32.glVertexAttribPointer(posHandle, coordsPerVertex,
                                     GLES32.GL_FLOAT, false,
                                     vertexStride, vertexBuffer)

        // Draw the square
        GLES32.glDrawElements(GLES32.GL_TRIANGLES, drawOrder.size, GLES32.GL_UNSIGNED_SHORT, dlb)

        GLES32.glDisableVertexAttribArray(posHandle)        // disable vertex array
    }

}


fun loadShader(type: Int, shaderCode: String): Int {

    // create a vertex shader type (GLES32.GL_VERTEX_SHADER)
    // or a fragment shader type (GLES32.GL_FRAGMENT_SHADER)
    val shader = GLES32.glCreateShader(type)

    // add the source code to the shader and compile it
    GLES32.glShaderSource(shader, shaderCode)
    GLES32.glCompileShader(shader)

    return shader
}


class MainActivity : AppCompatActivity() {



    /* MotionEvent extension functions ------ */
    fun MotionEvent.dPointerPos(i: Int) : PointF {
        return PointF(if (historySize > 0) getX(i) - getHistoricalX(i,0) else 0.0f,
                if (historySize > 0) getY(i) - getHistoricalY(i,0) else 0.0f)
    }

    fun MotionEvent.dFocus() : PointF {
        if (pointerCount == 1) return dPointerPos(0)
        else {
            var sumX = 0.0f
            var sumY = 0.0f
            var dPos: PointF
            for (i in 0 until pointerCount) {
                dPos = dPointerPos(i)
                sumX += dPos.x
                sumY += dPos.y
            }
            return PointF(sumX / pointerCount, sumY / pointerCount)
        }
    }

    fun MotionEvent.focalLength() : Float {
        val f = focus()
        val pos = PointF(x,y)
        val dist = Pair(pos.x - f.x,pos.y - f.y)
        return Math.sqrt(Math.pow(dist.first.toDouble(),2.0) +
                Math.pow(dist.second.toDouble(),2.0)).toFloat()
    }

    fun MotionEvent.focus() : PointF {
        return if (pointerCount == 1) PointF(x, y)
        else {
            var sumX = 0.0f
            var sumY = 0.0f
            for (i in 0 until pointerCount) {
                sumX += getX(i)
                sumY += getY(i)
            }
            PointF(sumX / pointerCount, sumY / pointerCount)
        }
    }
    /* -------------------------------------- */




    inner class FractalSurfaceView(ctx : Context) : GLSurfaceView(ctx) {

        val renderer : FractalRenderer
        var reactionType : Int = 0
        val continuousRender : Boolean
        val emulateDouble : Boolean = false
        private var prevFocalLen : Float


        init {
            setEGLContextClientVersion(3)                   // create OpenGL ES 3.0 context
            renderer = FractalRenderer(ctx, emulateDouble)  // create renderer
            setRenderer(renderer)                           // set renderer
            renderMode = RENDERMODE_WHEN_DIRTY              // only render on init and explicitly
            continuousRender = !emulateDouble
            prevFocalLen = 1.0f
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(e: MotionEvent?): Boolean {

            if (reactionType == 0) {
                // actions change fractal
                when (e?.actionMasked) {

                    MotionEvent.ACTION_DOWN -> {
                        Log.d("DOWN", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.d("UP", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                        if (!continuousRender) {
                            requestRender()
                            invalidate()
                        }
                        return true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        val focus = e.focus()
                        prevFocalLen = e.focalLength()
                        Log.d("POINTER DOWN", "focus: $focus, focalLen: $prevFocalLen")
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {

                        val dPos = e.dFocus()
                        renderer.translate(dPos)

                        if (e.pointerCount > 1) {   // MULTI-TOUCH
                            val focalLen = e.focalLength()
                            val dScale = focalLen / prevFocalLen
                            prevFocalLen = focalLen
                            renderer.scale(dScale)
                            Log.d("SCALE", "$dScale")
                        }

                        if (continuousRender) {
                            Log.d("MOVE", "x: ${e.x}, y: ${e.y}, rawX: ${e.rawX}, rawY: ${e.rawY}")
                            Log.d("TRANSLATE", "dPos: $dPos")
                            requestRender()
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
        val screenRes : FloatArray

        private val offset : DoubleArray
        private val scale : DoubleArray

        private val imWidth : Int
        private val imHeight : Int
        private val ratio : Float

        var lightPos : FloatArray
        var maxIter : Int = 63

        lateinit var f : Fractal

        init {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenRes = floatArrayOf(screenWidth.toFloat(), screenHeight.toFloat())
            Log.d("WIDTH", "$screenWidth")
            Log.d("HEIGHT", "$screenHeight")
            imWidth = Math.round(screenWidth/1.0f)
            imHeight = Math.round(screenHeight/1.0f)
            ratio = imWidth/imHeight.toFloat()
            scale = doubleArrayOf(1.75, 1.75/ratio)
            offset = doubleArrayOf(-0.75, 0.0)
            lightPos = floatArrayOf(1.0f/sqrt(2.0f), 1.0f/sqrt(2.0f))
        }

        fun translate(dPos: PointF) {
            offset[0] -= dPos.x / screenWidth * 2.0 * scale[0]
            offset[1] += dPos.y / screenHeight * 2.0 * scale[1]
            Log.d("TRANSLATE", "dPos: (${dPos.x}, ${dPos.y}),  offset: (${offset[0]}, ${offset[1]})")
        }
        fun scale(dScale: Float) {
            scale[0] /= dScale.toDouble()
            scale[1] /= dScale.toDouble()
            Log.d("SCALE", "dScale: $dScale,  scale: (${scale[0]}, ${scale[1]})")
        }

        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

            // set background color to black
            GLES32.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f )

            // get OpenGL ES version
            Log.d("VERSION", unused.glGetString(GL10.GL_VERSION))

            // get fragment shader precision
            val a : IntBuffer = IntBuffer.allocate(2)
            val b : IntBuffer = IntBuffer.allocate(1)
            GLES32.glGetShaderPrecisionFormat(GLES32.GL_FRAGMENT_SHADER, GLES32.GL_HIGH_FLOAT, a, b)
            Log.d("PRECISION", b[0].toString())

            f = Fractal(ctx, emulateDouble)


        }

        override fun onDrawFrame(unused: GL10) {
            GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT)      // redraw background color
            f.draw(scale, offset, maxIter, lightPos, screenRes)
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            GLES32.glViewport(0, 0, width, height)
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
                    fractalView.requestRender()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (!fractalView.continuousRender) {
                    Log.d("SEEKBAR", seekBar.progress.toString())
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
