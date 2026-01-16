package com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration

import android.graphics.drawable.Drawable

class ActionBarConfiguration(val actionBarId: Int) {
    var title: String = "Application title"
        private set
    var subtitle: String = "subtitle"
        private set
    var homeIcon: Drawable? = null
        private set

    fun withTitle(title: String): ActionBarConfiguration {
        return apply { this.title = title }
    }

    fun withSubTitle(subTitle: String): ActionBarConfiguration {
        return apply { subtitle = subTitle }
    }

    fun withHomeIcon(icon: Drawable?): ActionBarConfiguration {
        return apply { homeIcon = icon }
    }
}