package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.media.*
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import java.lang.NullPointerException
import java.nio.ByteBuffer
import java.util.*
import javax.microedition.khronos.egl.*
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.roundToInt


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

fun clamp(d: Int, low: Int, high: Int) : Int {
    return maxOf(minOf(d, high), low)
}



class FractalSurfaceView : GLSurfaceView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private val TAG = "SURFACE"

    val f = Fractal.default
    val sc = SettingsConfig
    lateinit var r : FractalRenderer
    private lateinit var act : MainActivity

    private var muxer : MediaMuxer? = null
    private var muxerStarted = false
    private var trackIndex = -1

    private var framesRendered = 0
    private var framesMuxed = 0

    private val FRAMERATE = 30


    init {

        preserveEGLContextOnPause = true
        setEGLContextClientVersion(3)              // create OpenGL ES 3.0 context
        //setRenderer(r)
        //renderMode = RENDERMODE_WHEN_DIRTY          // only render on init and explicitly
        Log.d("FSV", "OpenGL ES context: $context")

    }

    fun initialize(r: FractalRenderer, act: MainActivity) {

        this.r = r
        this.act = act

        setRenderer(r)
        renderMode = RENDERMODE_WHEN_DIRTY

    }
    

    private var isAnimating = false

    private val prevFocus = floatArrayOf(0f, 0f)
    private var prevZoom = 0.0
    private var prevAngle = 0f
    private var prevFocalLen = 1.0f

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

        val fps = if (sc.continuousPosRender) 30.0 else 45.0
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
                            override fun onRenderFinished(buffer: ByteBuffer?) {
                                // Log.e(TAG, "onRenderFinished")
                                tapCount = 0
                                doubleTapStartTime = 0L
                                r.renderFinishedListener = null
                            }
                        }
                        f.shape.position.setFrom(newPos)
                    }
                    r.hasTranslated = true
                    r.hasZoomed = true

                    r.checkThresholdCross(oldPos.zoom)

                    r.renderProfile = RenderProfile.DISCRETE
                    r.renderToTex = true
                    requestRender()

                } else {
                    val q = interpolator.getInterpolation(t.toFloat())
                    if (sc.continuousPosRender) {
                        f.shape.position.x = (1.0 - q) * oldPos.x + q * newPos.x
                        f.shape.position.y = (1.0 - q) * oldPos.y + q * newPos.y
                        f.shape.position.zoom = (1.0 - q) * oldPos.zoom + q * newPos.zoom
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

        }, 0L, if (sc.continuousPosRender) 33L else 22L)

    }
    
    fun saveVideo(duration: Double) {

        val startTime = now()
        var startRenderTime = 0L
        var totalRenderTime = 0L
        var startCodecTime = 0L
        var totalCodecTime = 0L
        var startMuxTime = 0L
        var totalMuxTime = 0L

        val videoProgressDialog = AlertDialog.Builder(act, R.style.AlertDialogCustom)
                .setView(R.layout.alert_dialog_progress)
                .setTitle(R.string.rendering_video)
                .setIcon(R.drawable.hourglass)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    // cancel video render
                }
                .show()
        videoProgressDialog.setCanceledOnTouchOutside(false)

        val videoProgress = videoProgressDialog.findViewById<ProgressBar>(R.id.alertProgress)
        val videoProgressText = videoProgressDialog.findViewById<TextView>(R.id.alertProgressText)

        val durationUs = duration*1000000.0
        val totalFrames = (duration*FRAMERATE).toInt()
        val bitrateMbps = 160

        val zoomAmount = 11.0
        val zoomInc = 10.0.pow(zoomAmount/totalFrames)

        var t: Double

        val startZoom = f.shape.position.zoom
        val startRotation = f.shape.position.rotation





        // get current date and time
        val c = GregorianCalendar(TimeZone.getDefault())
        // Log.d("RENDERER", "${c[Calendar.YEAR]}, ${c[Calendar.MONTH]}, ${c[Calendar.DAY_OF_MONTH]}")
        val year = c[Calendar.YEAR]
        val month = c[Calendar.MONTH]
        val day = c[Calendar.DAY_OF_MONTH]
        val hour = c[Calendar.HOUR_OF_DAY]
        val minute = c[Calendar.MINUTE]
        val second = c[Calendar.SECOND]

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


        val height16x9 = sc.resolution.w*16/9
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC

        val format = MediaFormat.createVideoFormat(mimeType, sc.resolution.w, height16x9)
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

        r.renderProfile = RenderProfile.DISCRETE
        sc.renderBackground = false

        r.renderFinishedListener = object : RenderFinishedListener {
            override fun onRenderFinished(buffer: ByteBuffer?) {

                totalRenderTime += now() - startRenderTime

                if (buffer == null) throw NullPointerException("buffer is null")

                startMuxTime = now()
                drainEncoder(codec)
                totalMuxTime += now() - startMuxTime

                val inputBufferIndex = codec.dequeueInputBuffer(10000L)
                if (inputBufferIndex >= 0) {

                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)

                    if (framesRendered == totalFrames) {

                        inputBuffer?.clear()
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        // Log.d(TAG, "end of stream buffer sent!")
                        drainEncoder(codec)
                        r.isSavingVideo = false
                        codec.stop()
                        codec.release()
                        muxer?.stop()
                        muxer?.release()
                        muxer = null
                        fd?.close()

                        val totalTime = now() - startTime
                        Log.d(TAG, "video render took ${totalTime/1000.0} sec -- render: ${totalRenderTime/1000.0} sec (${100.0*totalRenderTime/totalTime}%), codec: ${totalCodecTime/1000.0} sec (${100.0*totalCodecTime/totalTime}%), muxer: ${totalMuxTime/1000.0} sec (${100.0*totalMuxTime/totalTime}%)")

                        videoProgressDialog.dismiss()

                    }
                    else {

                        startCodecTime = now()
                        val totalPixels = sc.resolution.w*height16x9

                        val yuv = ByteArray(totalPixels*3/2)
                        var yIndex = 0
//                        var uIndex = totalPixels
//                        var vIndex = uIndex*5/4
                        var uvIndex = totalPixels
                        var k = 0
                        val skipRows = (sc.resolution.h - height16x9)/2
                        buffer.position(skipRows*sc.resolution.w*4)

                        for (j in skipRows until sc.resolution.h - skipRows) {
                            for (i in 0 until sc.resolution.w) {

                                val p = buffer.int
                                val y = Color.alpha(p)
                                // if (i == sc.resolution.w/2) Log.e(TAG, "p: (${Color.red(p)}, ${Color.green(p)}, ${Color.blue(p)}, ${Color.alpha(p)})")
                                // val y = Color.alpha(p)
                                yuv[yIndex] = y.toByte()
                                yIndex++
                                if (j % 2 == 0 && k % 2 == 0) {
                                    val u = Color.red(p)
                                    val v = Color.green(p)
                                    yuv[uvIndex] = u.toByte()
                                    yuv[uvIndex + 1] = v.toByte()
                                    uvIndex += 2
//                                     yuv[uIndex] = u.toByte()
//                                     yuv[vIndex] = v.toByte()
//                                     uIndex++
//                                     vIndex++
                                }
                                k++
                            }
                        }

                        inputBuffer?.clear()
                        inputBuffer?.put(yuv)
                        codec.queueInputBuffer(
                                inputBufferIndex, 0,
                                yuv.size,
                                (framesRendered * 1e6 / FRAMERATE).toLong(),
                                0
                        )
                        framesRendered++
                        t = framesRendered.toDouble()/totalFrames

                        videoProgress?.progress = (t*100).toInt()
                        videoProgressText?.text = "${(t*100).toInt()}%"


                        val q = AccelerateDecelerateInterpolator().getInterpolation(t.toFloat())

                        val lastZoom = f.shape.position.zoom
                        f.shape.position.zoom = startZoom/zoomInc.pow(q.toDouble()*totalFrames)
                        r.checkThresholdCross(lastZoom)
                        f.shape.position.rotation = (1.0 - q)*startRotation + q*(startRotation + 0.5*Math.PI)
                        r.renderToTex = true

                        totalCodecTime += now() - startCodecTime
                        startRenderTime = now()
                        requestRender()

                    }

                }
                else {
                    Log.e(TAG, "!! input buffer index -1 !!")
                }
            }
        }

        r.isSavingVideo = true
        codec.start()

        startRenderTime = now()
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


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {


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
        
        r.apply {

            if (this@FractalSurfaceView.isAnimating) return false

            if (renderProfile != RenderProfile.CONTINUOUS && isRendering) {
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
                            doubleTapStartTime = now()
                        }
                        1 -> {
                            if (now() - doubleTapStartTime >= DOUBLE_TAP_TIMEOUT) {
                                tapCount = 0
                                doubleTapStartTime = now()
                            }
                        }
                    }

                    when (reaction) {
                        Reaction.POSITION -> {
                            if (sc.continuousPosRender) {
                                // beginContinuousRender = true
                                renderProfile = RenderProfile.CONTINUOUS
                                renderToTex = true
                            } else {
                                setQuadFocus(focus)
                            }
                        }
                        Reaction.COLOR -> {
                        }
                        Reaction.SHAPE, Reaction.TEXTURE -> {
                            // beginContinuousRender = true
                            val param = if (reaction == Reaction.SHAPE) f.shape.params.active else f.texture.activeParam
                            if (!param.isRateParam) {
                                renderProfile = RenderProfile.CONTINUOUS
                                renderToTex = true
                            }
                        }
                        Reaction.NONE -> {
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

                    if (reaction == Reaction.POSITION) {
                        prevZoom = f.shape.position.zoom
                        prevAngle = atan2(e.getY(0) - e.getY(1), e.getX(1) - e.getX(0))
                        if (!sc.continuousPosRender) setQuadFocus(focus)
                    }

                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]
                    prevFocalLen = e.focalLength()

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

                    when (reaction) {
                        Reaction.POSITION -> {

                            f.shape.position.translate(dx / Resolution.SCREEN.w, dy / Resolution.SCREEN.w)
                            if (!sc.continuousPosRender) translate(floatArrayOf(dx, dy))

                            if (e.pointerCount > 1) {

                                f.shape.position.zoom(dFocalLen, doubleArrayOf(
                                        focus[0].toDouble() / Resolution.SCREEN.w.toDouble() - 0.5,
                                        -(focus[1].toDouble() / Resolution.SCREEN.w.toDouble() - 0.5 * aspectRatio)
                                ))

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

                            if (sc.continuousPosRender) renderToTex = true
                            act.updateDisplayParams()

                        }
                        Reaction.COLOR -> {

                            when (e.pointerCount) {
                                1 -> {
                                    f.phase += dx / Resolution.SCREEN.w
                                }
                                2 -> {
                                    if (f.frequency == 0f) f.frequency = 0.0001f
                                    f.frequency *= dFocalLen
                                    prevFocalLen = focalLen
                                }
                            }
                            act.updateDisplayParams()

                        }
                        Reaction.SHAPE, Reaction.TEXTURE -> {

                            val param = if (reaction == Reaction.SHAPE) f.shape.params.active else f.texture.activeParam

                            when (e.pointerCount) {
                                1 -> {
                                    if (!param.isRateParam) {
                                        param.u += param.sensitivity!!.u * dx / Resolution.SCREEN.w
                                        if (param is ComplexParam) {
                                            param.v -= param.sensitivity.u * dy / Resolution.SCREEN.h
                                        }
                                        renderToTex = true
                                    }
                                }
                                2 -> {
                                    if (!param.isRateParam) param.sensitivity!!.u *= dFocalLen
                                    prevFocalLen = focalLen
                                }
                            }
                            act.updateDisplayParams()

                        }
                        Reaction.NONE -> {
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
                    if (reaction == Reaction.COLOR) prevFocalLen = 1f

                    requestRender()
                    return true

                }
                MotionEvent.ACTION_UP -> {


                    if (now() - doubleTapStartTime < DOUBLE_TAP_TIMEOUT) tapCount++
                    else {
                        tapCount = 0
                        doubleTapStartTime = 0L
                    }

                    when (reaction) {
                        Reaction.POSITION -> {
                            if (tapCount == 2) {

                                queueEvent { animate(PointF(e.x, e.y), 0.75) }

                            } else {
                                act.updatePositionEditTexts()
                                act.updateDisplayParams()
                                if (sc.continuousPosRender) renderProfile = RenderProfile.DISCRETE
                                checkThresholdCross(prevZoom)
                                renderToTex = true
                            }
                        }
                        Reaction.COLOR -> {
                            if (renderProfile == RenderProfile.COLOR_THUMB) renderThumbnails = true
                            act.updateColorEditTexts()
                            prevFocalLen = 1f
                        }
                        Reaction.SHAPE, Reaction.TEXTURE -> {

                            if (reaction == Reaction.SHAPE) act.updateShapeEditTexts()
                            else act.updateTextureEditTexts()

                            act.updateDisplayParams()
                            val param = if (reaction == Reaction.SHAPE) f.shape.params.active else f.texture.activeParam
                            if (!param.isRateParam) {
                                renderProfile = RenderProfile.DISCRETE
                                renderToTex = true
                            }

                        }
                        Reaction.NONE -> {
                        }
                    }

                    requestRender()
                    return true

                }

            }


            return false

        }

    }

}