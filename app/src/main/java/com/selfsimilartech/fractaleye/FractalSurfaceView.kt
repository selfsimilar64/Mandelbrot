package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.media.*
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.Type
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import java.io.File
import java.io.IOException
import java.lang.NullPointerException
import java.nio.ByteBuffer
import java.util.*
import javax.microedition.khronos.egl.*
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

private class CodecSurfaceFactory(val codecSurface: Surface) : GLSurfaceView.EGLWindowSurfaceFactory {

    override fun createWindowSurface(egl: EGL10, display: EGLDisplay?, config: EGLConfig?, nativeWindow: Any?): EGLSurface {
        return egl.eglCreateWindowSurface(display, config, codecSurface, null).also {
            egl.eglMakeCurrent(display, it, it, egl.eglGetCurrentContext())
        }
    }

    override fun destroySurface(egl: EGL10, display: EGLDisplay?, surface: EGLSurface?) {
        codecSurface.release()
        egl.eglDestroySurface(display, surface)
    }

}



class FractalSurfaceView : GLSurfaceView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private val TAG = "SURFACE"

    val f = Fractal.default
    val sc = Settings
    lateinit var r : FractalRenderer
    private lateinit var handler : MainActivity.ActivityHandler

    val video = Video()
    val videoEncoder = VideoEncoder(context, video)

    var tutorialInProgress = false


    init {

        preserveEGLContextOnPause = true
        setEGLContextClientVersion(3)              // create OpenGL ES 3.0 context
        //setRenderer(r)
        //renderMode = RENDERMODE_WHEN_DIRTY          // only render on init and explicitly
        Log.d("FSV", "OpenGL ES context: $context")

    }

    fun initialize(r: FractalRenderer, handler: MainActivity.ActivityHandler) {

        this.r = r
        this.handler = handler

        setRenderer(r)
        renderMode = RENDERMODE_WHEN_DIRTY

    }





    fun renderContinuousDiscrete() {
        // if (r.isRendering) r.interruptRender = true
        r.resetContinuousSize = true
        r.renderProfile = RenderProfile.CONTINUOUS
        r.renderToTex = true
        r.renderFinishedListener = object : RenderFinishedListener {
            override fun onRenderFinished(buffer: ByteArray?) {
                r.renderProfile = RenderProfile.DISCRETE
                r.renderToTex = true
                r.renderFinishedListener = null
                requestRender()
            }
        }
        requestRender()
    }

    fun interruptRender() {
        if (r.isRendering) r.interruptRender = true
    }


    private var isAnimating = false

    private val prevFocus = floatArrayOf(0f, 0f)
    private var prevZoom = 0.0
    private var prevAngle = 0f
    private var prevFocalLen = 1f
    private var initFocalLen = 1f

    private var tapCount = 0
    private var doubleTapStartTime = 0L

    private fun animate(focus: PointF, duration: Double) {

        handler.hideUi()
        isAnimating = true

        val croppedDims = sc.aspectRatio.getDimensions(Resolution.SCREEN)
        val aspectScale = (sc.aspectRatio.r / AspectRatio.RATIO_SCREEN.r).toFloat()
        val screenPos = if (aspectScale > 1f)
            PointF(
                (focus.x / croppedDims.x - 0.5f) / aspectScale,
                0.5f*AspectRatio.RATIO_SCREEN.r.toFloat() - focus.y/Resolution.SCREEN.w
        ) else
            PointF(
                focus.x / Resolution.SCREEN.w - 0.5f,
                0.5f*sc.aspectRatio.r.toFloat() - focus.y/Resolution.SCREEN.w
        )
        Log.d(TAG, "screenPos: ${screenPos.x}, ${screenPos.y}")

        // continuous render
        val xyEnd = f.shape.position.xyOf(screenPos.x, screenPos.y)
        val newPos = Position(
                xyEnd.first,
                xyEnd.second,
                f.shape.position.zoom / 5.0,
                f.shape.position.rotation
        )

        r.isAnimating = true
        if (sc.continuousPosRender) {
            r.renderProfile = RenderProfile.CONTINUOUS
        }
        else {
            r.hasZoomed = true
        }

        val oldPos = f.shape.position.clone()
        val oldQuadScale = r.quadScale
        val oldQuadCoords = PointF(r.quadCoords[0], r.quadCoords[1])
        val newQuadScale = oldQuadScale * 5f

        val fps = sc.targetFramerate.toDouble()
        val totalFrames = (duration*fps).roundToInt()
        val delta = f.shape.position.delta(newPos, totalFrames)

        Log.d(TAG, "delta: (${delta.x}, ${delta.y}, ${delta.zoom}, ${delta.rotation})")

        val interpolator = FastOutSlowInInterpolator()

        val timer = Timer()
        var frameCount = 0
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val t = frameCount.toDouble() / totalFrames
                if (frameCount == totalFrames) {

                    timer.cancel()
                    isAnimating = false

                    if (sc.continuousPosRender) {
                        tapCount = 0
                        doubleTapStartTime = 0L
                    }
                    else {
                        r.renderFinishedListener = object : RenderFinishedListener {
                            override fun onRenderFinished(buffer: ByteArray?) {
                                tapCount = 0
                                doubleTapStartTime = 0L
                                r.renderFinishedListener = null
                            }
                        }
                        r.checkThresholdCross { f.shape.position.setFrom(newPos) }
                    }
                    r.hasTranslated = true
                    r.hasZoomed = true

                    r.renderProfile = RenderProfile.DISCRETE
                    r.renderToTex = true
                    requestRender()

                    handler.updatePositionParam()
                    handler.showUi()

                } else {
                    val q = interpolator.getInterpolation(t.toFloat())
                    if (sc.continuousPosRender) {
                        f.shape.position.x = (1.0 - q) * oldPos.x + q * newPos.x
                        f.shape.position.y = (1.0 - q) * oldPos.y + q * newPos.y
                        r.checkThresholdCross {
                            f.shape.position.zoom = (1.0 - q) * oldPos.zoom + q * newPos.zoom
                        }
                        f.shape.position.rotation += delta.rotation
                    }
                    else {
                        r.quadScale = (1f - q)*oldQuadScale + q*newQuadScale
                        r.quadCoords[0] = -2f * newQuadScale * q * screenPos.x
                        if (sc.aspectRatio.r > AspectRatio.RATIO_SCREEN.r) {
                            r.quadCoords[1] = 2f * newQuadScale * q * (focus.y / croppedDims.y - 0.5f)
                        }
                        else {
                            r.quadCoords[1] = 2f * newQuadScale * q * (focus.y / croppedDims.y - 0.5f) * aspectScale
                        }
                        // Log.d(TAG, "scale: ${r.quadScale}, coords: (${r.quadCoords[0]}, ${r.quadCoords[1]})")
                    }
                    requestRender()
                    frameCount++

                }
            }
        }, 0L, (1000.0/sc.targetFramerate).toLong())

    }



    fun renderColorframes(onComplete: () -> Unit) {

        Log.v(TAG, "rendering colorframes...")

        var framesRendered = 0
        var t : Double

        handler.sendMessage(MessageType.MSG_SET_UI_STATE, obj = UiState.VIDEO_PROGRESS)

        video.colorframes = List(video.duration.toInt()*COLORFRAMES_PER_SEC + 1) { PointF() }

        r.isPreprocessingVideo = true
        r.renderProfile = RenderProfile.VIDEO
        r.continuousSize = 0.25
        r.updateContinuousRes()

        val prevSpan = PointF(r.textureSpan.min.x, r.textureSpan.max.x)
        f.color.unfitFromSpan(r.textureSpan)

        video.setVideoPosition(f, r, 0.0)

        r.renderFinishedListener = object : RenderFinishedListener {
            override fun onRenderFinished(buffer: ByteArray?) {

                Log.v(TAG, "colorframe $framesRendered: [${r.textureSpan.min.x}, ${r.textureSpan.max.x}]")

                video.colorframes[framesRendered].run {
                    x = r.textureSpan.min.x
                    y = r.textureSpan.max.x
                }
                framesRendered++

                t = framesRendered.toDouble()/video.colorframes.size
                video.setVideoPosition(f, r, t)
                handler.sendMessage(MessageType.MSG_SET_VIDEO_PROGRESS, obj = t)

                if (framesRendered == video.colorframes.size || r.interruptRender) {

                    r.renderFinishedListener = null
                    r.isPreprocessingVideo = false
                    framesRendered = 0

                    if (r.interruptRender) {
                        r.interruptRender = false
                        handler.sendMessage(MessageType.MSG_SET_UI_STATE, obj = UiState.VIDEO_CONFIG)
                        r.renderToTex = true
                        requestRender()
                    } else {
                        video.hasValidColorframes = true
                        video.colorframes.run {

                            r.setTextureSpan(first().x, first().y)
                            f.color.fitToSpan(TextureSpan().apply {
                                min.x = last().x
                                max.x = last().y
                            })

                        }
                        Log.v(TAG, "colorframes rendered!")
                        onComplete()
                    }

                }

                r.renderToTex = true
                requestRender()

            }
        }

        r.renderToTex = true
        requestRender()

    }

    fun previewVideo() {

        handler.sendMessage(MessageType.MSG_HIDE_UI)

        r.renderProfile = RenderProfile.CONTINUOUS
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            var t = 0.0
            override fun run() {
                if (t > 1.0) {
                    timer.cancel()
                    handler.sendMessage(MessageType.MSG_SET_UI_STATE, obj = UiState.VIDEO_CONFIG)
                    handler.sendMessage(MessageType.MSG_SHOW_UI)
                    r.renderProfile = RenderProfile.DISCRETE
                    r.renderToTex = true
                    requestRender()
                } else {

                    video.setVideoPosition(f, r, t)
                    r.renderToTex = true
                    requestRender()
                    t += 1.0/(video.duration*video.framerate.value.toDouble())

                }
            }
        }, 0L, (1000.0/video.framerate.value).toLong())

    }

    fun saveVideo() {

        videoEncoder.setOnEncoderEventListener(object : VideoEncoder.OnEncoderEventListener {

            lateinit var lastValidPosition : Position

            override fun onStart() {
                video.setVideoPosition(f, r, 0.0)
                lastValidPosition = f.shape.position.clone()
            }

            override fun onFrameRendered(t: Double) {

                handler.sendMessage(MessageType.MSG_SET_VIDEO_PROGRESS, obj = t)

                video.setVideoPosition(f, r, t)

                if (lastValidPosition.zoom/f.shape.position.zoom < sqrt(Math.E)) {
                    r.resetQuad()
                    r.zoom((lastValidPosition.zoom/f.shape.position.zoom).toFloat())
                    r.rotate((f.shape.position.rotation - lastValidPosition.rotation).toFloat())
                } else {
                    lastValidPosition.setFrom(f.shape.position)
                    r.renderToTex = true
                }

                requestRender()

            }

            override fun onStop() {
                r.isRenderingVideo = false
                sc.resolution = video.outResolution
                r.fgResolutionChanged = true
                handler.sendMessage(MessageType.MSG_SET_UI_STATE, obj = UiState.HOME)
            }

        })

//        val maxRotation = (zoomInc - 1.0)/Resolution.SCREEN.run { h.toDouble()/w }

        r.renderFinishedListener = object : RenderFinishedListener {
            override fun onRenderFinished(yuvFull: ByteArray?) {
                videoEncoder.onRenderFinished(yuvFull)
            }
        }

        sc.resolution = video.renderResolution
        sc.renderBackground = false

        r.isRenderingVideo = true
        r.fgResolutionChanged = true
        r.renderProfile = RenderProfile.VIDEO

        videoEncoder.start()

        r.renderToTex = true
        requestRender()

    }





    fun recordTapEvent(e: MotionEvent?) {
        when (e?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                when (tapCount) {
                    0 -> doubleTapStartTime = currentTimeMs()
                    1 -> {
                        if (currentTimeMs() - doubleTapStartTime >= DOUBLE_TAP_TIMEOUT) {
                            tapCount = 0
                            doubleTapStartTime = currentTimeMs()
                        }
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {  // multi-touch gesture -- not a double tap
                tapCount = 0
                doubleTapStartTime = 0L
            }
            MotionEvent.ACTION_UP -> {
                if (currentTimeMs() - doubleTapStartTime < DOUBLE_TAP_TIMEOUT) tapCount++
                else {
                    tapCount = 0
                    doubleTapStartTime = 0L
                }
            }
        }
    }

    fun resetDoubleTap() {
        tapCount = 0
        doubleTapStartTime = 0L
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?) : Boolean {

//        if (act.uiState == UiState.PARAM_MENU) {
//            act.uiState = UiState.MINIMIZED
//        }

        if (tutorialInProgress) {
//            handler.passTouchToHighlightWindow(e)
            handler.queryTutorialTaskComplete()
        }


//        if (!isRendering) {

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
        
        r.run {

            if (this@FractalSurfaceView.isAnimating) return false
            if (isRenderingVideo) return false


            recordTapEvent(e)
            if (renderProfile != RenderProfile.CONTINUOUS && isRendering) {

                when (sc.editMode) {
                    EditMode.POSITION -> {
                        if (sc.continuousPosRender || sc.resolution.w <= Resolution.SCREEN.w) interruptRender = true else return false
                    }
                    EditMode.COLOR -> return false
                    EditMode.SHAPE, EditMode.TEXTURE -> {
                        interruptRender = true
                    }
                }

//                if (sc.editMode == EditMode.POSITION && !sc.continuousPosRender && !validAuxiliary) return false
//                if (sc.editMode == EditMode.COLOR) return false
//                if (sc.resolution.w <= Resolution.SCREEN.w) interruptRender = true
//                else return false

            }


            when (e?.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    if (sc.editMode != EditMode.POSITION) handler.hideUi()

                    val focus = e.focus()
                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]

                    when (sc.editMode) {
                        EditMode.POSITION -> {
                            if (sc.continuousPosRender) {
                                continuousFrame = 0
                                renderFinishedListener = null
                                renderProfile = RenderProfile.CONTINUOUS
                                renderToTex = true
                            } else {
                                if (isRendering) interruptRender = true
                                setQuadFocus(focus)
                            }
                        }
                        EditMode.COLOR -> {}
                        EditMode.SHAPE, EditMode.TEXTURE -> {
                            continuousFrame = 0
                            renderProfile = RenderProfile.CONTINUOUS
                            renderToTex = true
                        }
                        EditMode.NONE -> {
                        }
                    }

                    requestRender()
                    return true

                }
                MotionEvent.ACTION_POINTER_DOWN -> {

                    val focus = e.focus()

                    if (sc.editMode == EditMode.POSITION) {
                        prevZoom = f.shape.position.zoom
                        prevAngle = atan2(e.getY(0) - e.getY(1), e.getX(1) - e.getX(0))
                        if (!sc.continuousPosRender) setQuadFocus(focus)
                    }

                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]
                    prevFocalLen = e.focalLength()
                    initFocalLen = prevFocalLen

                    requestRender()
                    return true

                }
                MotionEvent.ACTION_MOVE -> {

                    val focus = e.focus()
                    val dx: Float = focus[0] - prevFocus[0]
                    val dy: Float = focus[1] - prevFocus[1]
                    val focalLen = e.focalLength()
                    val dFocalLen = focalLen / prevFocalLen

                    if (dx*dx + dy*dy > 20) {
                        handler.hideUi()
                        resetDoubleTap()
                    }

                    when (sc.editMode) {
                        EditMode.POSITION -> {

                            f.shape.position.translate(dx / Resolution.SCREEN.w, dy / Resolution.SCREEN.w)
                            if (!sc.continuousPosRender) translate(floatArrayOf(dx, dy))

                            if (e.pointerCount > 1) {

                                checkThresholdCross {
                                    f.shape.position.zoom(
                                        dFocalLen, doubleArrayOf(
                                            focus[0].toDouble() / Resolution.SCREEN.w.toDouble() - 0.5,
                                            -(focus[1].toDouble() / Resolution.SCREEN.w.toDouble() - 0.5 * aspectRatio)
                                        )
                                    )
                                }

                                if (!sc.continuousPosRender) zoom(dFocalLen)

                                val angle = atan2(e.getY(0) - e.getY(1), e.getX(1) - e.getX(0))
                                val dtheta = angle - prevAngle
                                f.shape.position.rotate(dtheta, doubleArrayOf(
                                        focus[0].toDouble() / Resolution.SCREEN.w.toDouble() - 0.5,
                                        -(focus[1].toDouble() / Resolution.SCREEN.w.toDouble() - 0.5 * aspectRatio)
                                ))
                                if (!sc.continuousPosRender) rotate(dtheta)
                                prevAngle = angle

                            }

                            if (sc.continuousPosRender) {
                                renderToTex = true
                                handler.updatePositionParam()
                            }

                        }
                        EditMode.COLOR -> {
                            f.color.run {
                                when (e.pointerCount) {
                                    1 -> {
                                        phase += dx / Resolution.SCREEN.w
                                    }
                                    2 -> {
                                        if (frequency == 0.0) frequency = 0.001
                                        // frequency *= (dFocalLen - 1.0)/2.0 + 1.0
                                        frequency *= dFocalLen
                                        // frequency += (dFocalLen - 1.0)
                                        prevFocalLen = focalLen
                                    }
                                }
                                handler.updateColorParam()
                            }
                        }
                        EditMode.SHAPE, EditMode.TEXTURE -> {

                            val param = if (sc.editMode == EditMode.SHAPE) f.shape.params.active else f.texture.params.active

                            when (e.pointerCount) {
                                1 -> {
                                    if (param is ComplexParam) {
                                        param.u += param.sensitivityFactor * dx / Resolution.SCREEN.w
                                        param.v -= param.sensitivityFactor * dy / Resolution.SCREEN.h
                                    } else {
                                        param.u += param.sensitivityFactor * dx / Resolution.SCREEN.w
                                    }
                                    renderToTex = true
                                }
                                2 -> {
                                    param.sensitivity += (dFocalLen - 1.0)*10.0
                                    prevFocalLen = focalLen
                                }
                            }

                            validAuxiliary = false
                            handler.updateShapeTextureParam()

                        }
                        EditMode.NONE -> {
                        }
                    }

                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]
                    prevFocalLen = focalLen

                    requestRender()
                    return true

                }
                MotionEvent.ACTION_POINTER_UP -> {

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
                    if (sc.editMode == EditMode.COLOR) prevFocalLen = 1f

                    requestRender()
                    return true

                }
                MotionEvent.ACTION_UP -> {

                    handler.showUi()

                    when (sc.editMode) {
                        EditMode.POSITION -> {
                            if (tapCount == 2) queueEvent { animate(PointF(e.x, e.y), 1.0) }
                            else {
                                handler.updatePositionParam()
                                if (sc.continuousPosRender) {
                                    renderFinishedListener = object : RenderFinishedListener {
                                        override fun onRenderFinished(buffer: ByteArray?) {
                                            renderFinishedListener = null
                                            renderProfile = RenderProfile.DISCRETE
                                            requestRender()
                                        }
                                    }
                                }
                                if (tapCount == 0 || sc.continuousPosRender) renderToTex = true
                            }
                        }
                        EditMode.COLOR -> {
                            if (renderProfile == RenderProfile.COLOR_THUMB) renderAllThumbnails = true
                            handler.updateColorParam()
                            prevFocalLen = 1f
                        }
                        EditMode.SHAPE, EditMode.TEXTURE -> {

                            handler.updateShapeTextureParam()

                            renderProfile = RenderProfile.DISCRETE
                            renderToTex = true

                        }
                        EditMode.NONE -> {}
                    }

                    // Log.d("FSV", "Config(${f.shape.position}, ${f.shape.params.toConstructorString()}\n)")

                    requestRender()
                    return true

                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.showUi()
                }

            }


            return false

        }

    }

}