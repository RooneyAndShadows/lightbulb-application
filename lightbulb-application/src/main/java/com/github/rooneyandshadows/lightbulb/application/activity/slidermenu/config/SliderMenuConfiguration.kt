package com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.config

import android.graphics.Color
import android.view.View
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.items.MenuItem

class SliderMenuConfiguration {
    var headerView: View? = null
    var badgeTextColor = Color.WHITE
    var badgeCircleColor = Color.RED
    val itemsList: ArrayList<MenuItem> = ArrayList()

    fun addMenuItem(item: MenuItem): SliderMenuConfiguration {
        itemsList.add(item)
        return this
    }

    fun withHeaderView(headerView: View?): SliderMenuConfiguration {
        this.headerView = headerView
        return this
    }
}