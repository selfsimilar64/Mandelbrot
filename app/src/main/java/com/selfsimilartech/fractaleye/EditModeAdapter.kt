package com.selfsimilartech.fractaleye

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class EditModeAdapter(

    val fragments : List<MenuFragment>,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle

) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount() : Int {
        return fragments.size
    }

    override fun createFragment(position: Int) : Fragment {
//        return when (position) {
//            0 -> TextureFragment()
//            1 -> ShapeFragment()
//            2 -> ColorFragment()
//            3 -> PositionFragment()
//            else -> SettingsFragment()
//        }
        return fragments[position]
    }

}