package com.github.rooneyandshadows.lightbulb.application.activity

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import com.github.rooneyandshadows.lightbulb.application.R
import com.github.rooneyandshadows.lightbulb.application.activity.service.ConnectionCheckerServiceWrapper
import com.github.rooneyandshadows.lightbulb.application.activity.service.ConnectionCheckerServiceWrapper.InternetConnectionStateListeners
import com.github.rooneyandshadows.lightbulb.commons.utils.KeyboardUtils.Companion.hideKeyboard
import com.github.rooneyandshadows.lightbulb.commons.utils.LocaleHelper
import com.github.rooneyandshadows.lightbulb.textinputview.TextInputView

abstract class BaseActivity : AppCompatActivity() {
    private var dragged = false

    protected open fun doBeforeCreate(savedInstanceState: Bundle?) {}


    protected open fun registerInternetConnectionStateListeners(): InternetConnectionStateListeners? {
        return null
    }

    open fun onUnhandledException(paramThread: Thread?, exception: Throwable) {
    }

    @Override
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
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

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        setUnhandledGlobalExceptionHandler()
        doBeforeCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_activity_layout)
        initializeInternetCheckerService()
    }

    @Override
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    @Override
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_MOVE) dragged = true
        if (event.action == MotionEvent.ACTION_UP && !dragged) {
            val touchedView: View? =
                findViewAtPosition(window.decorView, event.rawX.toInt(), event.rawY.toInt())
            if (touchedView !is TextInputView && touchedView !is EditText) hideKeyboard(this)
        }
        if (event.action == MotionEvent.ACTION_UP) dragged = false
        return super.dispatchTouchEvent(event)
    }

    private fun initializeInternetCheckerService() {
        val internetConnectionListeners = registerInternetConnectionStateListeners()
        if (internetConnectionListeners != null) lifecycle.addObserver(
            ConnectionCheckerServiceWrapper(
                this,
                internetConnectionListeners
            )
        )
    }

    /**
     * Method sets exception handler for uncaught exceptions in the activity context.
     */
    private fun setUnhandledGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { paramThread: Thread?, exception: Throwable ->
            onUnhandledException(paramThread, exception)
        }
    }

    private fun findViewAtPosition(parent: View, x: Int, y: Int): View? {
        return if (parent is ViewGroup) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val viewAtPosition = findViewAtPosition(child, x, y)
                if (viewAtPosition != null) {
                    return viewAtPosition
                }
            }
            null
        } else {
            val rect = Rect()
            parent.getGlobalVisibleRect(rect)
            if (rect.contains(x, y)) {
                parent
            } else {
                null
            }
        }
    }
}