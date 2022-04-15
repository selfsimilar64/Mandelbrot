package com.selfsimilartech.fractaleye

import android.content.ContentValues
import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.IOException
import java.lang.NullPointerException
import java.nio.ByteBuffer
import java.util.*

class VideoEncoder(private val ctx: Context, val video: Video) {

    companion object {
        const val TAG = "ENCODER"
    }

    interface OnEncoderEventListener {
        fun onStart()
        fun onFrameRendered(t: Double)
        fun onStop()
    }

    private var listener : OnEncoderEventListener? = null

    private var videoName = ""

    private var codecName = ""
    private var format = MediaFormat()

    private lateinit var codec : MediaCodec
    private lateinit var muxer : MediaMuxer
    private lateinit var fd : ParcelFileDescriptor

    private var muxerStarted = false
    private var trackIndex = -1
    private var stridesLogged = false

    private var t = 0.0
    private var framesRendered = 0
    private var framesMuxed = 0

    var startTime = 0L
    var startRenderTime = 0L
    var totalRenderTime = 0L
    var startCodecTime = 0L
    var totalCodecTime = 0L
    var startMuxTime = 0L
    var totalMuxTime = 0L


    fun updateFormat() {

        val newFormat = MediaFormat.createVideoFormat(video.fileType.codecMime, video.outResolution.w, video.outResolution.h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_FRAME_RATE, video.framerate.value)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0)
        }

        val compatCodecInfos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->
            info.isEncoder && info.supportedTypes.contains(video.fileType.codecMime) && info.getCapabilitiesForType(video.fileType.codecMime).videoCapabilities.run {
                video.outResolution.w in supportedWidths && video.outResolution.h in supportedHeights && video.framerate.value in supportedFrameRates
            }
        }

        if (compatCodecInfos.isEmpty()) Log.w(TAG, "no compatible codecs")
        else {

            var maxBitrate = 1
            var maxBitrateCodecInfo = compatCodecInfos[0]
            compatCodecInfos.forEach { info ->
                info.getCapabilitiesForType(video.fileType.codecMime).videoCapabilities.let { cap ->
                    Log.d(TAG, "%28s - w: %10s, h: %10s, br: %16s, fr: %8s".format(
                        info.name, cap.supportedWidths.toString(), cap.supportedHeights.toString(), cap.bitrateRange.toString(), cap.supportedFrameRates.toString()
                    ))
                    if (cap.bitrateRange.upper > maxBitrate) {
                        maxBitrate = cap.bitrateRange.upper
                        maxBitrateCodecInfo = info
                    }
                    // Log.d(TAG, "${info.name} - w: ${cap.supportedWidths}, h: ${cap.supportedHeights}, br: ${cap.bitrateRange}, fr: ${cap.supportedFrameRates}")
                }
            }

            codecName = maxBitrateCodecInfo.name
            format = newFormat
            video.maxBitrate = maxBitrate
            Log.d(TAG, "new codec: $codecName, max bitrate: $maxBitrate")

        }

    }

    fun start() {

        // get current date and time
        val cal = GregorianCalendar(TimeZone.getDefault())
        // Log.d("RENDERER", "${c[Calendar.YEAR]}, ${c[Calendar.MONTH]}, ${c[Calendar.DAY_OF_MONTH]}")
        val year    =  cal[Calendar.YEAR]
        val month   =  cal[Calendar.MONTH]
        val day     =  cal[Calendar.DAY_OF_MONTH]
        val hour    =  cal[Calendar.HOUR_OF_DAY]
        val minute  =  cal[Calendar.MINUTE]
        val second  =  cal[Calendar.SECOND]

        val appNameAbbrev = ctx.resources.getString(R.string.fe_abbrev)
        val subDirectory = Environment.DIRECTORY_MOVIES + "/" + ctx.resources.getString(R.string.app_name)

        videoName = "${appNameAbbrev}_%4d%02d%02d_%02d%02d%02d".format(year, month + 1, day, hour, minute, second)

        // val height16x9 = outputResolution.w*16/9

        val contentUri : Uri?
        val resolver = ctx.contentResolver

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

            // app external storage directory
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                ctx.resources.getString(R.string.app_name)
            )

            // create directory if not already created
            if (!dir.exists()) {
                Log.v(TAG, "Directory does not exist -- creating...")
                when {
                    dir.mkdir() -> Log.v(TAG, "Directory created")
                    dir.mkdirs() -> Log.v(TAG, "Directories created")
                    else -> {
                        Log.e(TAG, "Directory could not be created")
                        // handler.showMessage(R.string.msg_error)
                        return
                    }
                }
            }

            val file = File(dir, videoName + video.fileType.ext)
            try {
                file.createNewFile()
            } catch (e: IOException) {
                // handler.showMessage(R.string.msg_error)
            }
            if (file.exists()) {
                contentUri = Uri.fromFile(file)
            } else return

        } else {

            val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            // val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val videoDetails = ContentValues().apply {
                put(MediaStore.Video.VideoColumns.MIME_TYPE, video.fileType.fileMime)
                put(MediaStore.Video.VideoColumns.DISPLAY_NAME, videoName)
                put(MediaStore.Video.VideoColumns.RELATIVE_PATH, subDirectory)
            }
            contentUri = resolver.insert(videoCollection, videoDetails)

        }

        fd = resolver.openFileDescriptor(contentUri!!, "w", null)!!

        muxer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            MediaMuxer(contentUri.path!!, video.fileType.muxerFormat)
        } else {
            MediaMuxer(fd.fileDescriptor, video.fileType.muxerFormat)
        }

        format.setInteger(MediaFormat.KEY_BIT_RATE, video.getBitrate())
        Log.d(TAG, "using bitrate: ${format.getInteger(MediaFormat.KEY_BIT_RATE)}")

        //codec = MediaCodec.createByCodecName(MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format))
        codec = MediaCodec.createByCodecName(codecName)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        Log.d(TAG, "codec -- ${codec.name}")
        Log.d(TAG, "video config: [resolution: ${video.outResolution}, fps: ${video.framerate.value}, quality: ${video.quality}, bitrate: ${video.getBitrate()} Mbps]")

        startRenderTime = currentTimeMs()
        startTime = currentTimeMs()
        listener?.onStart()
        codec.start()

    }

    fun onRenderFinished(yuvFull: ByteArray?) {

        totalRenderTime += currentTimeMs() - startRenderTime

        if (yuvFull == null) throw NullPointerException("array is null")

        startMuxTime = currentTimeMs()
        drainEncoder()
        Log.v("SURFACE", "encoder drained !!")
        totalMuxTime += currentTimeMs() - startMuxTime

        val inputBufferIndex = codec.dequeueInputBuffer(10000L)
        Log.v("SURFACE", "input buffer dequeued !!")

        if (inputBufferIndex >= 0) {

            val bufferCapacity = codec.getInputBuffer(inputBufferIndex)?.capacity() ?: 0
            Log.v("SURFACE", "input buffer received -- capacity: $bufferCapacity")

            val inputImage = codec.getInputImage(inputBufferIndex)!!

            val yBuffer        = inputImage.planes[0].buffer!!
            val uBuffer        = inputImage.planes[1].buffer!!
            val vBuffer        = inputImage.planes[2].buffer!!

            val yPixelStride   = inputImage.planes[0].pixelStride
            val yRowStride     = inputImage.planes[0].rowStride
            val uvPixelStride  = inputImage.planes[1].pixelStride
            val uvRowStride    = inputImage.planes[1].rowStride

            if (!stridesLogged) {
                Log.d(TAG, "y pixel stride: $yPixelStride")
                Log.d(TAG, "y row stride: $yRowStride")
                Log.d(TAG, "uv pixel stride: $uvPixelStride")
                Log.d(TAG, "uv row stride: $uvRowStride")
                Log.d(TAG, "y buffer capacity: ${yBuffer.capacity()}")
                Log.d(TAG, "uv buffer capacity: ${uBuffer.capacity()}")
                stridesLogged = true
            }


            if (framesRendered < video.frameCount) {  // rendering...

                startCodecTime = currentTimeMs()

                yuvMap(
                    video.outResolution.w,
                    video.outResolution.h,
                    uvPixelStride,
                    yuvFull, yBuffer, uBuffer, vBuffer
                )

                codec.queueInputBuffer(
                    inputBufferIndex, 0,
                    bufferCapacity, // yuvCompact.size,
                    (framesRendered * 1e6 / video.framerate.value).toLong(),
                    0
                )

                Log.v("SURFACE", "input buffer queued !!")
                framesRendered++
                t = framesRendered.toDouble()/video.frameCount

                listener?.onFrameRendered(t)

                totalCodecTime += currentTimeMs() - startCodecTime
                startRenderTime = currentTimeMs()

            } else {  // video render finished

                codec.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                Log.v(TAG, "end of stream buffer sent!")

                stop()

            }

        }
        else Log.w(TAG, "input buffer index -1")

    }

    fun stop() {

        framesRendered = 0

        drainEncoder()

        codec.stop()
        codec.release()
        muxer.stop()
        muxer.release()
        muxerStarted = false
        fd.close()

        val totalTime = currentTimeMs() - startTime
        Log.d(TAG, "video render took ${totalTime/1000.0} sec -- render: ${totalRenderTime/1000.0} sec (${100.0*totalRenderTime/totalTime}%), codec: ${totalCodecTime/1000.0} sec (${100.0*totalCodecTime/totalTime}%), muxer: ${totalMuxTime/1000.0} sec (${100.0*totalMuxTime/totalTime}%)")

        listener?.onStop()
        video.reset()

    }


    private fun drainEncoder() {

        val TIMEOUT_USEC = 1000L

        while (true) {

            val bufferInfo = MediaCodec.BufferInfo()
            val encoderStatus = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.v(TAG, "no output available")
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    Log.v(TAG, "spinning to await EOS")
                }
                else break
            }
            else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                // should happen before receiving buffers, and should only happen once
                if (muxerStarted) throw RuntimeException("format changed twice")
                val newFormat = codec.outputFormat
                Log.v(TAG, "encoder output format changed: $newFormat")

                // now that we have the Magic Goodies, start the muxer
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Log.v(TAG, "codec.outputFormat -- (${newFormat.features.joinToString(", ")})")
                }
                muxer.run { trackIndex = addTrack(newFormat) }
                muxer.start()
                muxerStarted = true

            }
            else if (encoderStatus < 0) Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
            else {

                val encodedData = codec.getOutputBuffer(encoderStatus)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.v(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG")
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0) {
                    if (!muxerStarted) throw RuntimeException("muxer hasn't started")

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData!!.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer?.writeSampleData(trackIndex, encodedData, bufferInfo)
                    Log.v(TAG, "sent " + bufferInfo.size.toString() + " bytes to muxer")
                    framesMuxed++
                    // Log.e(TAG, "frames muxed: $framesMuxed")
                }
                codec.releaseOutputBuffer(encoderStatus, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    Log.v(TAG, "EOS reached")
                    break
                }

            }

        }

    }

    fun setOnEncoderEventListener(l: OnEncoderEventListener?) {
        listener = l
    }

    private external fun yuvMap(
        width: Int, height: Int, uvPixelStride: Int, inputBuffer: ByteArray, yBuffer: ByteBuffer, uBuffer: ByteBuffer, vBuffer: ByteBuffer
    )


}