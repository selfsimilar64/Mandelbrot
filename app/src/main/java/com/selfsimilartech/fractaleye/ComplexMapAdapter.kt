package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView


class ComplexMapAdapter(context: Context, private var maps: List<ComplexMap>) : BaseAdapter() {

    private var inflter: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return maps.size
    }

    override fun getItem(i: Int): Any? {
        return maps[i]
    }

    override fun getItemId(i: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflter.inflate(R.layout.complex_map_spinner_item, null)
        val mapNameText = view.findViewById<TextView>(R.id.mapNameText)
        val mapPreview = view.findViewById<ImageView>(R.id.mapPreview)
        mapNameText.text = maps[position].name
        mapPreview.setImageResource(maps[position].icon)
        return view
    }

}