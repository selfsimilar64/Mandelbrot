package com.selfsimilartech.fractaleye

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.selfsimilartech.fractaleye.databinding.KeyframeListItemBinding
import com.selfsimilartech.fractaleye.databinding.TransitionListItemBinding
import java.text.NumberFormat
import java.text.ParseException

class VideoAdapter(

    val act: MainActivity,
    val fsv: FractalSurfaceView,
    val video: Video

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_KEYFRAME = 0
        const val TYPE_TRANSITION = 1
    }

    private var selectedItemPos = -1

    fun selectItem(newPosition: Int) {

        video.keyframes[selectedItemPos].isSelected = false
        notifyItemChanged(selectedItemPos)

        selectedItemPos = newPosition
        video.keyframes[selectedItemPos].isSelected = true
        notifyItemChanged(selectedItemPos)

    }

    class KeyframeHolder(val b: KeyframeListItemBinding) : RecyclerView.ViewHolder(b.root)

    class TransitionHolder(val b: TransitionListItemBinding) : RecyclerView.ViewHolder(b.root)

    fun String.formatToDouble(showMsg: Boolean = true) : Double? {
        val nf = NumberFormat.getInstance()
        var d : Double? = null
        try { d = nf.parse(this)?.toDouble() }
        catch (e: ParseException) {
            if (showMsg) {
                act.showMessage(act.resources.getString(R.string.msg_invalid_format))
            }
        }
        return d
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_KEYFRAME -> KeyframeHolder(KeyframeListItemBinding.inflate(LayoutInflater.from(parent.context)))
            TYPE_TRANSITION -> TransitionHolder(TransitionListItemBinding.inflate(LayoutInflater.from(parent.context)))
            else -> throw Error()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_KEYFRAME -> {
                holder as KeyframeHolder
                holder.b.keyframeImage.setImageBitmap((video.items[position] as Video.Keyframe).f.thumbnail)
            }
            TYPE_TRANSITION -> {
                holder as TransitionHolder
                holder.b.imageButton.setOnClickListener {
                    // show dialog with transition options
                }
            }
        }
    }

    override fun getItemCount() : Int {
        return video.keyframes.size
    }

    override fun getItemViewType(position: Int): Int {
        return video.items[position].type
    }

}