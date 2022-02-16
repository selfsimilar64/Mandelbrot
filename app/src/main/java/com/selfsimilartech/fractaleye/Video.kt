package com.selfsimilartech.fractaleye

class Video {

    interface VideoItem {
        val type : Int
        var isSelected : Boolean
    }

    class Keyframe(
        val f: Fractal,
        var time: Double = 0.0,
    ) : VideoItem {
        override val type = VideoAdapter.TYPE_KEYFRAME
        override var isSelected = false
    }

    class Transition(
        var duration : Double = 30.0,
        var linearity : Double = 0.25,
        var rotations : Int = 0,
        var phaseRotations : Int = 0
    ) : VideoItem {
        override val type = VideoAdapter.TYPE_TRANSITION
        override var isSelected = false
    }

    val items       : ArrayList<VideoItem>  = arrayListOf()
    val keyframes   : ArrayList<Keyframe>   = arrayListOf()
    val transitions : ArrayList<Transition> = arrayListOf()

    val duration : Double
        get() = transitions.sumOf { it.duration }

    fun addKeyframe(k: Keyframe) {
        keyframes.add(k)
        items.add(k)
    }

    fun addTransition(t: Transition) {
        transitions.add(t)
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