package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
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


@SuppressLint("ViewConstructor")
class FractalSurfaceView(context: Context, val r: FractalRenderer) : GLSurfaceView(context) {

    init {

        preserveEGLContextOnPause = true
        setEGLContextClientVersion(3)              // create OpenGL ES 3.0 context
        //setEGLContextFactory(ContextFactory())
        setRenderer(r)
        renderMode = RENDERMODE_WHEN_DIRTY          // only render on init and explicitly
        Log.d("FSV", "OpenGL ES context: ${context}")

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return r.onTouchEvent(event).also{ requestRender() }
    }


}