package com.github.rooneyandshadows.lightbulb.application.application

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.franmontiel.localechanger.LocaleChanger
import com.github.rooneyandshadows.lightbulb.application.activity.LightBulbActivity
import java.util.*
import java.util.function.Predicate

abstract class LightBulbApplication : Application() {
    protected abstract val supportedLocales: List<Locale>

    protected open fun create() {
    }

    protected open fun configurationChanged(newConfig: Configuration) {
    }

    companion object {
        @JvmStatic
        lateinit var application: LightBulbApplication
            private set

        @JvmStatic
        val context: Context
            get() = application.applicationContext
    }

    @Override
    final override fun onCreate() {
        super.onCreate()
        LocaleChanger.initialize(applicationContext, supportedLocales)
        application = this
        create()
    }

    @Override
    final override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleChanger.onConfigurationChanged()
        configurationChanged(newConfig)
    }

    fun changeLocale(locale: String) {
        val localeToSet = supportedLocales.stream()
            .filter(Predicate {
                it.language == locale
            }).findFirst()
            .orElse(null);
        if (localeToSet != null)
            LocaleChanger.setLocale(localeToSet)
        else
            Log.w(null, "Locale \"$locale\"  is not supported");
    }

    fun changeLocaleAndRecreateActivity(locale: String, context: LightBulbActivity) {
        val localeToSet = supportedLocales.stream()
            .filter(Predicate {
                it.language == locale
            }).findFirst()
            .orElse(null);
        if (localeToSet != null) {
            LocaleChanger.setLocale(localeToSet)
            context.recreate()
        } else
            Log.w(null, "Locale \"$locale\"  is not supported");
    }
}