package com.github.rooneyandshadows.lightbulb.application.application

import android.app.Application
import android.content.Context
import android.content.res.Configuration

abstract class BaseApplication : Application() {

    protected open fun create() {
    }

    companion object {
        @JvmStatic
        lateinit var application: BaseApplication
            private set

        @JvmStatic
        val context: Context
            get() = application.applicationContext
    }

    @Override
    final override fun onCreate() {
        super.onCreate()
        application = this
        create()
    }
}