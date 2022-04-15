package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateLayoutParams

class EditModeButton(context: Context, attrs: AttributeSet) : ParamButton(context, attrs) {

    val utilityLayoutId : Int
    val paramDisplayLayoutId : Int
    val paramSelectorLayoutId : Int
    val listLayoutId : Int

    var utilityLayout : ViewGroup? = null
    var paramDisplayLayout : ViewGroup? = null
    var paramSelectorLayout : ViewGroup? = null
    var listLayout : ViewGroup? = null

    var viewsFound = false





    private fun findViews() {
        utilityLayout = rootView.findViewById(utilityLayoutId)
        paramDisplayLayout = rootView.findViewById(paramDisplayLayoutId)
        paramSelectorLayout = rootView.findViewById(paramSelectorLayoutId)
        listLayout = rootView.findViewById(listLayoutId)
        viewsFound = true
    }

    override fun setActivated(activated: Boolean) {
        if (!viewsFound) findViews()
        if (activated) {
            Log.e("EDIT MODE BUTTON", "$mode activated")
            updateLayoutParams<LayoutParams> {
                width = LayoutParams.WRAP_CONTENT
                weight = 0f
            }
            utilityLayout?.show()
            paramSelectorLayout?.show()
            paramDisplayLayout?.show()
        } else {
            Log.e("EDIT MODE BUTTON", "$mode deactivated")
            updateLayoutParams<LayoutParams> {
                width = 0
                weight = 1f
            }
            utilityLayout?.hide()
            paramSelectorLayout?.hide()
            paramDisplayLayout?.hide()
        }
        super.setActivated(activated)
    }



    var mode : EditMode = EditMode.POSITION

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.EditModeButton)
        utilityLayoutId        = typedArray.getResourceId( R.styleable.EditModeButton_utility_layout,          R.id.position_utility_buttons )
        paramDisplayLayoutId   = typedArray.getResourceId( R.styleable.EditModeButton_param_display_layout,    R.id.position_param_display   )
        paramSelectorLayoutId  = typedArray.getResourceId( R.styleable.EditModeButton_param_selector_layout,   R.id.position_param_menu      )
        listLayoutId           = typedArray.getResourceId( R.styleable.EditModeButton_list_layout,             R.id.palette_list             )
        typedArray.recycle()
    }

}