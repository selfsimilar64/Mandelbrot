package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

class MaxHeightScrollView : ScrollView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var newSpec = heightMeasureSpec
        newSpec = MeasureSpec.makeMeasureSpec(200.dp(context), MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, newSpec)
    }

}