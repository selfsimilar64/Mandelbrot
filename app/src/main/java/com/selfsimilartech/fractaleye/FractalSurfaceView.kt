package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.media.*
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
import java.lang.NullPointerException
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
    val sc = SettingsConfig
    lateinit var r : FractalRenderer
    private lateinit var handler : MainActivity.ActivityHandler

    private var muxer : MediaMuxer? = null
    private var muxerStarted = false
    private var trackIndex = -1

    private var framesRendered = 0
    private var framesMuxed = 0

    private var FRAMERATE = 60
    private var startZoom = 1.0
    private var startRotation = 0.0
    private var zoomInc = 1.0
    private var rotationInc = 0.0
    private var totalFrames = 0
    val video = Video()

    val highlightMatrix = Matrix()


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


    fun saveVideo() {

        val startTime = currentTimeMs()
        var startRenderTime = 0L
        var totalRenderTime = 0L
        var startCodecTime = 0L
        var totalCodecTime = 0L
        var startMuxTime = 0L
        var totalMuxTime = 0L


        val outputResolution = sc.resolution
        val renderResolution = outputResolution.videoCompanions!!.first


//        val videoProgressDialog = AlertDialog.Builder(context, R.style.AlertDialogCustom)
//                .setView(R.layout.alert_dialog_progress)
//                .setTitle(R.string.rendering_video)
//                .setIcon(R.drawable.hourglass)
//                .setNegativeButton(android.R.string.cancel) { _, _ ->
//                    // cancel video render
//                }
//                .show()
//        videoProgressDialog.setCanceledOnTouchOutside(false)

//        val videoProgress = videoProgressDialog.findViewById<ProgressBar>(R.id.alertProgress)
//        val videoProgressText = videoProgressDialog.findViewById<TextView>(R.id.alertProgressText)

        val durationUs = video.duration*1e6
        totalFrames = (video.duration*FRAMERATE).toInt()
        val bitrateMbps = 160

//        zoomInc = 10.0.pow(zoomAmount/totalFrames)
//        val maxRotation = (zoomInc - 1.0)/Resolution.SCREEN.run { h.toDouble()/w }
//        rotationInc = min(rotationAmount/totalFrames, maxRotation)
//        Log.e("SURFACE", "maxRotation: $maxRotation, rotationInc: $rotationInc")

        var t : Double

        setVideoPosition(0.0)
        val lastValidPosition = f.shape.position.clone()



        // get current date and time
        val cal = GregorianCalendar(TimeZone.getDefault())
        // Log.d("RENDERER", "${c[Calendar.YEAR]}, ${c[Calendar.MONTH]}, ${c[Calendar.DAY_OF_MONTH]}")
        val year = cal[Calendar.YEAR]
        val month = cal[Calendar.MONTH]
        val day = cal[Calendar.DAY_OF_MONTH]
        val hour = cal[Calendar.HOUR_OF_DAY]
        val minute = cal[Calendar.MINUTE]
        val second = cal[Calendar.SECOND]

        val appNameAbbrev = resources.getString(R.string.fe_abbrev)
        val subDirectory = Environment.DIRECTORY_MOVIES + "/" + resources.getString(R.string.app_name)
        val videoName = "${appNameAbbrev}_%4d%02d%02d_%02d%02d%02d".format(year, month + 1, day, hour, minute, second)

        val resolver = context.contentResolver
        val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val videoDetails = ContentValues().apply {
            put(MediaStore.Video.VideoColumns.DISPLAY_NAME, videoName)
            put(MediaStore.Video.VideoColumns.RELATIVE_PATH, subDirectory)
        }
        val contentUri = resolver.insert(videoCollection, videoDetails)

        val fd = resolver.openFileDescriptor(contentUri!!, "w", null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            muxer = MediaMuxer(fd!!.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }


        // val height16x9 = outputResolution.w*16/9
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC

        val format = MediaFormat.createVideoFormat(mimeType, outputResolution.w, outputResolution.h)
        format.apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateMbps*1000000)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAMERATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0)
            // setInteger(MediaFormat.KEY_DURATION, durationUs.toInt())
        }

        val compatCodecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->
            info.isEncoder && info.supportedTypes.isNotEmpty() && info.supportedTypes[0].equals(mimeType, true)
        }
//        compatCodecs.forEach { info ->
//            Log.e(TAG, "${info.name} -- ${info.getCapabilitiesForType(info.supportedTypes[0]).videoCapabilities.let { it.supportedWidths.toString() + it.supportedHeights.toString() + it.bitrateRange + it.isSizeSupported(1440, 3120) }}")
//        }
        val codec = MediaCodec.createByCodecName(compatCodecs[0].name)
//        val codec = MediaCodec.createByCodecName(MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format))
//        val codec = MediaCodec.createEncoderByType(mimeType)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        Log.d(TAG, "codec -- ${codec.name}")


        r.rgbAllocation = Allocation.createTyped(r.rs, Type.createXY(r.rs, Element.RGBA_8888(r.rs), outputResolution.w, outputResolution.h))
        r.yuvAllocation = Allocation.createTyped(r.rs, Type.createX(r.rs, Element.U8(r.rs), outputResolution.n*3/2))


        r.renderFinishedListener = object : RenderFinishedListener {
            override fun onRenderFinished(yuvFull: ByteArray?) {

                if (r.isPreprocessingVideo) {

                    r.videoTextureSpans?.get(framesRendered)?.apply {
                        x = r.textureSpan.min()
                        y = r.textureSpan.max()
                    }
                    framesRendered++

                    t = framesRendered.toDouble()/totalFrames
                    setVideoPosition(t)

                    if (framesRendered == totalFrames) {

                        framesRendered = 0
                        r.checkThresholdCross { f.shape.position.zoom = startZoom }
                        r.videoTextureSpans?.let {
                            r.setTextureSpan(it[0].x, it[0].y)
                        }

                        r.isPreprocessingVideo = false
                        r.isRenderingVideo = true
                        sc.resolution = renderResolution
                        r.fgResolutionChanged = true

                    }

                    r.renderToTex = true
                    requestRender()

                } else if (r.isRenderingVideo) {

                    totalRenderTime += currentTimeMs() - startRenderTime

                    if (yuvFull == null) throw NullPointerException("array is null")

                    startMuxTime = currentTimeMs()
                    drainEncoder(codec)
                    Log.e("SURFACE", "encoder drained !!")
                    totalMuxTime += currentTimeMs() - startMuxTime

                    val inputBufferIndex = codec.dequeueInputBuffer(10000L)
                    Log.e("SURFACE", "input buffer dequeued !!")
                    if (inputBufferIndex >= 0) {

                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        Log.e("SURFACE", "input buffer gotten !!")

                        if (framesRendered == totalFrames) {

                            framesRendered = 0
                            inputBuffer?.clear()
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            // Log.d(TAG, "end of stream buffer sent!")
                            drainEncoder(codec)
                            r.isRenderingVideo = false
                            codec.stop()
                            codec.release()
                            muxer?.stop()
                            muxer?.release()
                            muxer = null
                            muxerStarted = false
                            fd?.close()

                            val totalTime = currentTimeMs() - startTime
                            Log.d(TAG, "video render took ${totalTime/1000.0} sec -- render: ${totalRenderTime/1000.0} sec (${100.0*totalRenderTime/totalTime}%), codec: ${totalCodecTime/1000.0} sec (${100.0*totalCodecTime/totalTime}%), muxer: ${totalMuxTime/1000.0} sec (${100.0*totalMuxTime/totalTime}%)")

//                            videoProgressDialog.dismiss()
                            sc.resolution = outputResolution
                            r.fgResolutionChanged = true

                        }
                        else {

                            startCodecTime = currentTimeMs()

                            val totalPixels = outputResolution.n
                            val yuvCompact = ByteArray(totalPixels*3/2)

                            r.rgbAllocation?.copyFrom(yuvFull)
                            r.compactYUVScript.apply {
                                _gOut = r.yuvAllocation
                                _width = outputResolution.w
                                _height = outputResolution.h
                                // _frameSize = totalPixels
                                forEach_compact(r.rgbAllocation)
                            }
                            r.yuvAllocation?.copyTo(yuvCompact)

                            Log.e("SURFACE", "yuv conversion completed !!")

                            inputBuffer?.clear()
                            inputBuffer?.put(yuvCompact)
                            codec.queueInputBuffer(
                                    inputBufferIndex, 0,
                                    yuvCompact.size,
                                    (framesRendered * 1e6 / FRAMERATE).toLong(),
                                    0
                            )
                            Log.e("SURFACE", "input buffer queued !!")
                            framesRendered++
                            t = framesRendered.toDouble()/totalFrames
                            handler.updateProgress(t)
//                            videoProgress?.progress = (t*100).toInt()
//                            videoProgressText?.text = "${(t*100).toInt()}%"

                            setVideoPosition(t)
                            r.videoTextureSpans?.let {
                                r.setTextureSpan(it[framesRendered - 1].x, it[framesRendered - 1].y)
                            }

                            if (lastValidPosition.zoom/f.shape.position.zoom < sqrt(Math.E)) {
                                r.resetQuad()
                                r.zoom((lastValidPosition.zoom/f.shape.position.zoom).toFloat())
                                r.rotate((f.shape.position.rotation - lastValidPosition.rotation).toFloat())
                            } else {
                                lastValidPosition.setFrom(f.shape.position)
                                r.resetQuad()
                                r.renderToTex = true
                            }

                            totalCodecTime += currentTimeMs() - startCodecTime
                            startRenderTime = currentTimeMs()
                            requestRender()

                        }

                    }
                    else Log.e(TAG, "!! input buffer index -1 !!")

                }

            }
        }


        if (sc.autofitColorRange) {

            r.isPreprocessingVideo = true
            r.videoTextureSpans = List(totalFrames) { PointF() }
            sc.resolution = outputResolution.videoCompanions!!.second

        } else {

            sc.resolution = renderResolution
            r.isRenderingVideo = true

        }

        r.fgResolutionChanged = true
        r.renderProfile = RenderProfile.VIDEO
        sc.renderBackground = false
        codec.start()

        startRenderTime = currentTimeMs()
        r.renderToTex = true
        requestRender()

    }


    private fun drainEncoder(codec: MediaCodec) {

        val TIMEOUT_USEC = 1000L

        while (true) {

            val bufferInfo = MediaCodec.BufferInfo()
            val encoderStatus = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.d(TAG, "no output available")
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    Log.d(TAG, "spinning to await EOS")
                }
                else break
            }
            else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                // should happen before receiving buffers, and should only happen once
                if (muxerStarted) throw RuntimeException("format changed twice")
                val newFormat = codec.outputFormat
                Log.d(TAG, "encoder output format changed: $newFormat")

                // now that we have the Magic Goodies, start the muxer
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Log.d(TAG, "codec.outputFormat -- (${newFormat.features.joinToString(", ")})")
                }
                muxer?.run { trackIndex = addTrack(newFormat) }
                muxer?.start()
                muxerStarted = true

            }
            else if (encoderStatus < 0) Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
            else {

                val encodedData = codec.getOutputBuffer(encoderStatus)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG")
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0) {
                    if (!muxerStarted) throw RuntimeException("muxer hasn't started")

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData!!.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer?.writeSampleData(trackIndex, encodedData, bufferInfo)
                    Log.d(TAG, "sent " + bufferInfo.size.toString() + " bytes to muxer")
                    framesMuxed++
                    // Log.e(TAG, "frames muxed: $framesMuxed")
                }
                codec.releaseOutputBuffer(encoderStatus, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    Log.d(TAG, "EOS reached")
                    break
                }

            }

        }

    }

    fun setVideoPosition(t: Double) {

        val i = video.getKeyframeIndexFromPosition(t)
        val subPosition = video.getSubPositionFromPosition(t)
        video.keyframes[i].isSelected = true
        video.keyframes[i + 1].isSelected = true
        video.transitions[i].isSelected = true

        r.checkThresholdCross {
            f.interpolate(
                video.keyframes[i].f,
                video.keyframes[i + 1].f,
                video.transitions[i],
                subPosition
            )
        }

//        val c = 0.2
//        val q = when {
//            t < c         -> t*t/(2.0*c*(1.0 - c))
//            t <= 1.0 - c  -> (t - 0.5*c)/(1.0 - c)
//            t <= 1.0      -> (t*(1.0 - 0.5*t) - 0.5)/(c*(1.0 - c)) + 1.0
//            else          -> 0.0
//        }
//
//        r.checkThresholdCross(showMsg = false) {
//            f.shape.position.zoom = startZoom/zoomInc.pow(q*totalFrames)
//        }
//        f.shape.position.rotation = (1.0 - q)*startRotation + q*totalFrames*rotationInc
//        f.phase = (t*30.0).toFloat()

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

            if (renderProfile != RenderProfile.CONTINUOUS && isRendering) {
                if (!sc.continuousPosRender && sc.editMode == EditMode.POSITION && !validAuxiliary) return false
                if (sc.editMode == EditMode.COLOR) return false
                if (sc.resolution.w <= Resolution.SCREEN.w) interruptRender = true
                else return false
            }


            when (e?.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    val focus = e.focus()
                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]

                    when (tapCount) {
                        0 -> {
                            doubleTapStartTime = currentTimeMs()
                        }
                        1 -> {
                            if (currentTimeMs() - doubleTapStartTime >= DOUBLE_TAP_TIMEOUT) {
                                tapCount = 0
                                doubleTapStartTime = currentTimeMs()
                            }
                        }
                    }

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

                    // multi-touch gesture -- not a double tap
                    tapCount = 0
                    doubleTapStartTime = 0L

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
                        tapCount = 0
                        doubleTapStartTime = 0L
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


                    if (currentTimeMs() - doubleTapStartTime < DOUBLE_TAP_TIMEOUT) tapCount++
                    else {
                        tapCount = 0
                        doubleTapStartTime = 0L
                    }

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
                                renderToTex = true
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

            }


            return false

        }

    }

}