package com.github.rooneyandshadows.lightbulb.application.application

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.appcompat.view.ContextThemeWrapper
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.config.SliderMenuConfiguration
import com.github.rooneyandshadows.lightbulb.commons.utils.LocaleHelper

abstract class BaseApplication : Application() {

    companion object {
        @JvmStatic
        lateinit var application: BaseApplication
            private set

        @JvmStatic
        val context: Context
            get() = application.applicationContext
    }

    protected open fun create() {
    }

    @Override
    final override fun onCreate() {
        super.onCreate()
        application = this
        create()
    }

    @Override
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    @Override
    override fun getResources(): Resources {
        return baseContext.resources
    }

    @Override
    override fun getApplicationContext(): Context {
        val localeWrapper = LocaleHelper.wrapContext(super.getApplicationContext())
        return ContextThemeWrapper(localeWrapper, theme)
    }

    @Override
    override fun getBaseContext(): Context {
        return LocaleHelper.wrapContext(super.getBaseContext())
    }
}