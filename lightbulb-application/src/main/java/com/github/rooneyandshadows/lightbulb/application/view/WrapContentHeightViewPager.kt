package com.github.rooneyandshadows.lightbulb.application.view

import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class WrapContentHeightViewPager : ViewPager {
    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var wMeasureSpec = widthMeasureSpec
        var hMeasureSpec = heightMeasureSpec
        var height = 0
        var width = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(wMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            val h = child.measuredHeight
            val w = child.measuredWidth
            if (h > height) height = h
            if (w > width) width = w
        }
        hMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        wMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        super.onMeasure(wMeasureSpec, hMeasureSpec)
    }
}