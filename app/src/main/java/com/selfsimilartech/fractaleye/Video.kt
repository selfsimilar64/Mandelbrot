package com.selfsimilartech.fractaleye

import android.graphics.PointF
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlin.math.floor

const val COLORFRAMES_PER_SEC = 2

class Video {

    interface VideoItem {
        val type : Int
        var isSelected : Boolean
    }

    enum class Framerate(val value: Int, val bitrateCoef: Double) { F24(24, 0.4), F30(30, 0.5), F60(60, 1.0) }
    enum class Quality(val bitrateCoef: Double) { LOW(0.1), MEDIUM(0.35), HIGH(0.75), MAX(1.0) }
    enum class FileType(val ext: String, val fileMime: String, val codecMime: String, val muxerFormat: Int) {
        MPEG4(".mp4", "video/mp4", MediaFormat.MIMETYPE_VIDEO_AVC, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4),
        WEBM(".webm", "video/webm", MediaFormat.MIMETYPE_VIDEO_VP8, MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM)
    }

    class Keyframe(
        val f: Fractal,
        var time: Double = 0.0,
    ) : VideoItem {
        override val type = VideoAdapter.TYPE_KEYFRAME
        override var isSelected = false
    }

    inner class Transition(
        var linearity : Double = 0.9,
        var rotations : Int = 0,
        var phaseRotations : Int = 0
    ) : VideoItem {

        override val type = VideoAdapter.TYPE_TRANSITION
        override var isSelected = false

        var duration : Double = 5.0
            set(value) {
                field = value
                updateDuration()
            }


    }


    val items       : ArrayList<VideoItem>  = arrayListOf()
    val keyframes   : ArrayList<Keyframe>   = arrayListOf()
    val transitions : ArrayList<Transition> = arrayListOf()

    var useAutocolor = true
    var colorframes : List<PointF> = listOf()
    var hasValidColorframes = false

    var duration = 0.0

    var fileType = FileType.MPEG4

    var maxBitrate = 0
        set(value) {
            field = value
            estimateFileSize()
        }

    var outResolution = Resolution.R720
        set(value) {
            field = value
            renderResolution = value.videoCompanion!!
            estimateFileSize()
        }

    var renderResolution = Resolution.R480.videoCompanion!!

    var framerate = Framerate.F30
        set(value) {
            field = value
            estimateFileSize()
            updateFrameCount()
        }
    var quality = Quality.MEDIUM
        set(value) {
            field = value
            estimateFileSize()
        }

    var frameCount = 0

    var fileSize = 0.0



    fun setVideoPosition(f: Fractal, r: FractalRenderer, t: Double) {

        val i = getKeyframeIndexFromPosition(t)
        val subPosition = getSubPositionFromPosition(t)
        keyframes[i].isSelected = true
        keyframes[i + 1].isSelected = true
        transitions[i].isSelected = true

        r.checkThresholdCross {
            f.interpolate(
                keyframes[i].f,
                keyframes[i + 1].f,
                transitions[i],
                subPosition
            )
        }

        if (useAutocolor) {
            colorframes.run {
                if (t == 1.0) {
                    r.setTextureSpan(last().x, last().y)
                } else {
                    val q = t * (size - 1)
                    val m = floor(q).toInt()
                    val s = q.rem(1.0)
                    r.setTextureSpan(
                        ((1.0 - s) * get(m).x + s * get(m + 1).x).toFloat(),
                        ((1.0 - s) * get(m).y + s * get(m + 1).y).toFloat()
                    )
                }
            }
        }

//        val phaseRotationsPerSec = 0.35
//        f.color.phase = (1.0 - t)*video.duration*phaseRotationsPerSec

    }



    fun reset() {
        items.removeAll { true }
        keyframes.removeAll { true }
        transitions.removeAll { true }
        hasValidColorframes = false
    }

    private fun estimateFileSize() {
        fileSize = getBitrate()/8e6*duration
    }

    fun getDisplayFileSize() : String {
        return "(%.1f MB)".format(fileSize)
    }

    private fun updateFrameCount() {
        frameCount = (duration*framerate.value).toInt()
    }

    private fun updateDuration() {
        duration = transitions.sumOf { t -> t.duration }
        estimateFileSize()
        updateFrameCount()
    }

    fun getBitrate() : Int {
        return (maxBitrate.toDouble()*quality.bitrateCoef*framerate.bitrateCoef*outResolution.bitrateCoef).toInt()
    }

    fun addKeyframe(k: Keyframe) {
        keyframes.add(k)
        items.add(k)
    }

    fun addTransition() {
        transitions.add(Transition())
        updateDuration()
//        items.add(t)
    }

    fun getPosition(k: Keyframe) : Double = k.time / duration

    private fun getSubPositionFromTime(time: Double) : Double {
        var sum = 0.0
        transitions.forEach {
            sum += it.duration
            if (sum >= time) return (time - (sum - it.duration))/it.duration
        }
        return 0.0
    }

    fun getSubPositionFromPosition(t: Double) : Double {
        return getSubPositionFromTime(t*duration)
    }

    private fun getKeyframeIndexFromTime(time: Double) : Int {
        var sum = 0.0
        transitions.forEachIndexed { i, t ->
            sum += t.duration
            if (sum >= time) return i
        }
        return 0
    }

    fun getKeyframeIndexFromPosition(t: Double) : Int {
        return getKeyframeIndexFromTime(t*duration)
    }

}