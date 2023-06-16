package com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.config

import android.graphics.Color.*
import android.view.View
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.items.MenuItem

class SliderMenuConfiguration @JvmOverloads constructor(
    val headerConfiguration: HeaderConfiguration? = null,
    val itemsList: MutableList<MenuItem> = mutableListOf()
) {
    var badgeTextColor = WHITE
    var badgeCircleColor = RED

    class HeaderConfiguration(
        val headerView: Int,
        val headerInflateListener: HeaderInflateListener? = null
    ) {

        fun interface HeaderInflateListener {
            fun onInflated(headerView: View)
        }
    }
}