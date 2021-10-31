package com.github.rooneyandshadows.lightbulb.application.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import com.franmontiel.localechanger.LocaleChanger
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.service.ConnectionCheckerService
import com.github.rooneyandshadows.lightbulb.application.service.StompNotificationJobService
import java.util.*
import java.util.function.Predicate

abstract class BaseApplication : Application() {
    protected abstract val supportedLocales: List<Locale>

    protected open fun create() {
    }

    protected open fun configurationChanged(newConfig: Configuration) {
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

    fun changeLocaleAndRecreateActivity(locale: String, context: BaseActivity) {
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