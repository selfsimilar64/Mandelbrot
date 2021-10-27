package com.selfsimilartech.fractaleye

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class EditModeAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() : Int {
        return 4
    }

    override fun createFragment(position: Int) : Fragment {
        return when (position) {
            0 -> TextureFragment()
            1 -> ShapeFragment()
            2 -> ColorFragment()
            3 -> PositionFragment()
            else -> SettingsFragment()
        }
    }

}