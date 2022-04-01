package com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.drawable

import android.content.Context

class NavigateBackDrawable(context: Context) : BaseHomeDrawable(context) {
    init {
        progress = 1F
        setEnabled(false)
    }
}