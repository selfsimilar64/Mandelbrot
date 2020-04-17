package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.opengl.GLES20
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*
import kotlinx.coroutines.*
import android.renderscript.*
import org.apfloat.*
import java.math.MathContext
import java.nio.*


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
        val res                     : Point,
        private val interpolation   : Int,
        private val internalFormat  : Int,
        val index                   : Int
) {

    val id : Int
    private val numComponents : Int
    private val bytesPerComponent : Int
    private val bytesPerTexel : Int
    private val type : Int
    private val format : Int
    val buffer: FloatBuffer

    init {

        // create texture id
        val b = IntBuffer.allocate(1)
        glGenTextures(1, b)
        id = b[0]

        // allocate texture memory
        numComponents = when(internalFormat) {
            GL_RGBA8 ->    4
            GL_RGBA16F ->  4
            GL_RGBA32F ->  4
            GL_RG16F ->    2
            GL_RG32F ->    2
            GL_RG16UI ->   2
            else -> 0
        }
        bytesPerComponent = when (internalFormat) {
            GL_RGBA8 ->    1
            GL_RGBA16F ->  2
            GL_RGBA32F ->  4
            GL_RG16F ->    2
            GL_RG32F ->    4
            GL_RG16UI ->   2
            else -> 0
        }
        bytesPerTexel = numComponents*bytesPerComponent
        val internalFormatStr = when(internalFormat) {
            GL_RG16UI -> "GL_RG16UI"
            GL_RG32F -> "GL_RG32F"
            else -> "not what u wanted"
        }
        Log.d("RENDER ROUTINE", "res: (${res.x}, ${res.y}), internalFormat: $internalFormatStr, index: $index, bytesPerTexel: $bytesPerTexel, totalBytes: ${res.x*res.y*bytesPerTexel}")

        // bind and set texture parameters
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, id)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, interpolation)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, interpolation)

        type = when(internalFormat) {
            GL_RGBA8 -> GL_UNSIGNED_BYTE
            GL_RGBA16F -> GL_HALF_FLOAT
            GL_RG16F -> GL_HALF_FLOAT
            GL_RGBA32F -> GL_FLOAT
            GL_RG32F -> GL_FLOAT
            GL_RG16UI -> GL_UNSIGNED_SHORT
//            GL_RGBA -> GL_UNSIGNED_BYTE
            else -> 0
        }
        format = when(internalFormat) {
            GL_RG16F, GL_RG32F -> GL_RG
            GL_RG16UI -> GL_RG_INTEGER
            else -> GL_RGBA
        }

        buffer = ByteBuffer.allocateDirect(res.x*res.y*bytesPerTexel).order(ByteOrder.nativeOrder()).asFloatBuffer()
        //Log.e("FSV", "buffer capacity: ${buffer.capacity()}")
        glTexImage2D(
                GL_TEXTURE_2D,              // target
                0,                          // mipmap level
                internalFormat,             // internal format
                res.x, res.y,             // texture resolution
                0,                          // border
                format,                     // internalFormat
                type,                       // type
                buffer                      // memory pointer
        )

    }


    fun get(i: Int, j: Int, k: Int) : Float = buffer.get(numComponents*(j*res.x + i) + k)
    fun set(i: Int, j: Int, k: Int, value: Float) {
        buffer.put(numComponents*(j*res.x + i) + k, value)
    }
    fun put(array: FloatArray) {
        buffer.position(0)
        buffer.put(array)
        buffer.position(0)
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, id)
        glTexImage2D(
                GL_TEXTURE_2D,              // target
                0,                          // mipmap level
                internalFormat,             // internal format
                res.x, res.y,             // texture resolution
                0,                          // border
                format,                     // internalFormat
                type,                       // type
                buffer                      // memory pointer
        )
    }
    fun update() {
        buffer.position(0)
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, id)
        glTexImage2D(
                GL_TEXTURE_2D,              // target
                0,                          // mipmap level
                internalFormat,             // internal format
                res.x, res.y,             // texture resolution
                0,                          // border
                format,                     // internalFormat
                type,                       // type
                buffer                      // memory pointer
        )
    }
    fun delete() {
        glDeleteTextures(1, intArrayOf(id), 0)
    }

}


@SuppressLint("ViewConstructor")
class FractalSurfaceView(context: Context, val r: FractalRenderer) : GLSurfaceView(context) {

    init {

        preserveEGLContextOnPause = true
        setEGLContextClientVersion(3)              // create OpenGL ES 3.0 context
        //setEGLContextFactory(ContextFactory())
        setRenderer(r)
        renderMode = RENDERMODE_WHEN_DIRTY          // only render on init and explicitly

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return r.onTouchEvent(event).also{ requestRender() }
    }


}