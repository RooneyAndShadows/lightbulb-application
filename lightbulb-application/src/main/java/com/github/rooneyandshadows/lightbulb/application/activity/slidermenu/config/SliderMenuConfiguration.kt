package com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.config

import android.graphics.Color
import android.view.View
import androidx.annotation.LayoutRes
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.items.MenuItem

class SliderMenuConfiguration @JvmOverloads constructor(
    val headerView: Int? = null,
    val itemsList: MutableList<MenuItem> = mutableListOf()
) {
    var badgeTextColor = Color.WHITE
    var badgeCircleColor = Color.RED
}