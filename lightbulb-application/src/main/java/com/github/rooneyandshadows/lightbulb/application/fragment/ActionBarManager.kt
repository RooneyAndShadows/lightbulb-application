package com.github.rooneyandshadows.lightbulb.application.fragment

import android.graphics.drawable.Drawable
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration.BaseFragmentConfiguration

class ActionBarManager(
    private val contextFragment: BaseFragment,
    private val configuration: BaseFragmentConfiguration.ActionBarConfiguration?
) {

    init {
        if (this.configuration != null)
            initializeActionBarForConfiguration(configuration)
    }

    private fun initializeActionBarForConfiguration(configuration: BaseFragmentConfiguration.ActionBarConfiguration?) {
        if (configuration == null)
            return
        val toolbar: Toolbar = contextFragment.requireView().findViewById(configuration.actionBarId)
        toolbar.contentInsetStartWithNavigation = 0
        contextFragment.contextActivity.setSupportActionBar(toolbar)
        contextFragment.contextActivity.supportActionBar!!.title = configuration.title
        contextFragment.contextActivity.supportActionBar!!.subtitle = configuration.subtitle
        if (configuration.isEnableActions)
            setHomeIcon(configuration.homeIcon)
    }

    fun setHomeIcon(icon: Drawable?) {
        if (!configuration!!.isEnableActions)
            return
        val actionBar = getActionBar()!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeButtonEnabled(false)
        actionBar.setHomeAsUpIndicator(icon)
    }

    private fun getActionBar(): ActionBar? {
        return contextFragment.contextActivity.supportActionBar
    }
}