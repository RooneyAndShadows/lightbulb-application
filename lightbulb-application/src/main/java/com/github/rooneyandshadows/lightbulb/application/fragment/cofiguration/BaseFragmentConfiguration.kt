package com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration

import android.graphics.drawable.Drawable

class BaseFragmentConfiguration {
    val isContentFragment: Boolean
    var hasLeftDrawer: Boolean = false
        private set
    var hasOptionsMenu: Boolean = false
        private set
    var actionBarConfiguration: ActionBarConfiguration? = null
        private set

    constructor() {
        isContentFragment = true
    }

    constructor(isContentFragment: Boolean) {
        this.isContentFragment = isContentFragment
    }

    fun withLeftDrawer(hasLeftDrawer: Boolean): BaseFragmentConfiguration {
        return apply { this.hasLeftDrawer = hasLeftDrawer }
    }

    fun withOptionsMenu(hasOptionsMenu: Boolean): BaseFragmentConfiguration {
        return apply { this.hasOptionsMenu = hasOptionsMenu }
    }

    fun withActionBarConfiguration(actionBarConfiguration: ActionBarConfiguration): BaseFragmentConfiguration {
        return apply { this.actionBarConfiguration = actionBarConfiguration; }
    }

    class ActionBarConfiguration(val actionBarId: Int) {
        var isEnableActions: Boolean = true
            private set
        var isAttachToDrawer: Boolean = true
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

        fun withActionButtons(state: Boolean): ActionBarConfiguration {
            return apply { isEnableActions = state }
        }

        fun attachToDrawer(state: Boolean): ActionBarConfiguration {
            return apply { isAttachToDrawer = state }
        }

        fun withHomeIcon(icon: Drawable?): ActionBarConfiguration {
            return apply { homeIcon = icon }
        }
    }
}