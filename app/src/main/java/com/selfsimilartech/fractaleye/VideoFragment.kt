package com.selfsimilartech.fractaleye

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.selfsimilartech.fractaleye.databinding.FragmentVideoBinding
import java.util.*


class VideoFragment : Fragment() {

    lateinit var b : FragmentVideoBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        b = FragmentVideoBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val act = activity as MainActivity
        val f = Fractal.default
        val fsv = act.fsv
        val sc = SettingsConfig

        b.keyframeRecycler.adapter = VideoAdapter(act, fsv, fsv.video)

        var videoIsPlaying = false
        var timer = Timer()

        b.videoPlayButton.setOnClickListener {
            if (videoIsPlaying) {
                timer.cancel()
                timer = Timer()
                b.videoPlayButton.setImageResource(R.drawable.video_play)
                videoIsPlaying = false
            } else {
                fsv.r.renderProfile = RenderProfile.CONTINUOUS
                timer.scheduleAtFixedRate(object : TimerTask() {
                    var t = 0.0
                    override fun run() {
                        if (t > 1.0) {
                            timer.cancel()
                            timer = Timer()
                            b.videoPlayButton.setImageResource(R.drawable.video_play)
                            videoIsPlaying = false
                            b.videoScrubber.progress = 0
                            fsv.r.renderProfile = RenderProfile.DISCRETE
                            fsv.r.renderToTex = true
                            fsv.requestRender()
                        } else {
                            fsv.setVideoPosition(t)
                            fsv.r.renderToTex = true
                            fsv.requestRender()
                            b.videoScrubber.progress = (t*b.videoScrubber.max).toInt()
                            t += 1.0/(fsv.video.duration*sc.targetFramerate.toDouble())
                        }
                    }
                }, 0L, (1000.0/sc.targetFramerate).toLong())
                b.videoPlayButton.setImageResource(R.drawable.video_pause)
                videoIsPlaying = true
            }
        }

        b.videoScrubber.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    fsv.setVideoPosition(progress.toDouble() / b.videoScrubber.max.toDouble())
                    fsv.r.renderToTex = true
                    fsv.requestRender()
                }
//                b.durationValue.setText(fsv.video.)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                fsv.r.renderProfile = RenderProfile.CONTINUOUS
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                fsv.r.renderProfile = RenderProfile.DISCRETE
                fsv.r.renderToTex = true
                fsv.requestRender()
            }

        })

    }

}