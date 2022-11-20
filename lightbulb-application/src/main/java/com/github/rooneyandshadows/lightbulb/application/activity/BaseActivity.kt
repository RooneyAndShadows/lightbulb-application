@file:Suppress("unused")

package com.github.rooneyandshadows.lightbulb.application.activity

import android.content.*
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.R
import com.github.rooneyandshadows.lightbulb.application.activity.service.configuration.NotificationServiceRegistry
import com.github.rooneyandshadows.lightbulb.application.activity.receivers.MenuChangedBroadcastReceiver
import com.github.rooneyandshadows.lightbulb.application.activity.routing.BaseActivityRouter
import com.github.rooneyandshadows.lightbulb.application.activity.service.configuration.ForegroundServiceRegistry
import com.github.rooneyandshadows.lightbulb.application.activity.service.connection.ConnectionCheckerServiceWrapper
import com.github.rooneyandshadows.lightbulb.application.activity.service.connection.ConnectionCheckerServiceWrapper.InternetConnectionStateListeners
import com.github.rooneyandshadows.lightbulb.application.activity.service.foreground.ForegroundServiceWrapper
import com.github.rooneyandshadows.lightbulb.application.activity.service.notification.NotificationJobServiceWrapper
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.SliderMenu
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.config.SliderMenuConfiguration
import com.github.rooneyandshadows.lightbulb.application.fragment.base.BaseFragment
import com.github.rooneyandshadows.lightbulb.commons.utils.KeyboardUtils.Companion.hideKeyboard
import com.github.rooneyandshadows.lightbulb.commons.utils.LocaleHelper
import com.github.rooneyandshadows.lightbulb.textinputview.TextInputView
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView

abstract class BaseActivity : AppCompatActivity(), LifecycleOwner {
    private val sliderBundleKey = "DRAWER_STATE"
    private val fragmentContainerIdentifier = R.id.fragmentContainer
    private var dragged = false
    private var router: BaseActivityRouter? = null
    private lateinit var sliderMenu: SliderMenu
    private lateinit var fragmentContainerWrapper: RelativeLayout
    private lateinit var menuConfigurationBroadcastReceiver: MenuChangedBroadcastReceiver
    private val lifecycleObservers = mutableListOf<LifecycleObserver>()
    var onNotificationReceivedListener: Runnable? = null
    private var navigatorClass: Class<*>? = null

    init {
        try {
            navigatorClass = Class.forName(
                javaClass.`package`?.name.plus(".")
                    .plus(javaClass.simpleName).plus("Navigator")
            )
        } catch (e: Throwable) {
            //ignored
        }
    }

    companion object {
        @JvmStatic
        private val menuConfigurations: MutableMap<Class<out BaseActivity>, ((targetActivity: BaseActivity) -> SliderMenuConfiguration)> =
            hashMapOf()

        @JvmStatic
        fun updateMenuConfiguration(
            context: Context,
            target: Class<out BaseActivity>,
            configuration: ((targetActivity: BaseActivity) -> SliderMenuConfiguration)
        ) {
            menuConfigurations[target] = configuration
            val intent = Intent(BuildConfig.menuConfigChangedAction)
            val extras = Bundle()
            extras.putString("TARGET_ACTIVITY", target.name)
            intent.putExtras(extras)
            context.sendBroadcast(intent)
        }

        @JvmStatic
        fun getMenuConfiguration(
            targetActivity: Class<out BaseActivity>
        ): ((targetActivity: BaseActivity) -> SliderMenuConfiguration)? {
            return menuConfigurations[targetActivity]
        }
    }

    protected open fun initializeRouter(fragmentContainerId: Int): BaseActivityRouter? {
        return null;
    }

    protected open fun doBeforeCreate(savedInstanceState: Bundle?) {
    }

    protected open fun doOnCreate(savedInstanceState: Bundle?) {
    }

    protected open fun doOnResume() {
    }

    protected open fun doOnNewIntent(intent: Intent) {
    }

    protected open fun doOnSaveInstanceState(outState: Bundle) {
    }

    protected open fun doOnSaveInstanceState(
        outState: Bundle,
        outPersistentState: PersistableBundle
    ) {
    }

    protected open fun doOnRestoreInstanceState(savedInstanceState: Bundle) {
    }

    protected open fun doOnRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
    }

    protected open fun doOnPause() {
    }

    protected open fun doOnDestroy() {
    }

    protected open fun registerNotificationService(): NotificationServiceRegistry? {
        return null
    }

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
    final override fun onCreate(savedInstanceState: Bundle?) {
        setUnhandledGlobalExceptionHandler()
        doBeforeCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_activity_layout)
        initializeMenuChangedReceiver()
        initializeNotificationService()
        initializeInternetCheckerService()
        setupActivity(savedInstanceState)
        doOnCreate(savedInstanceState)
    }

    @Override
    final override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(sliderBundleKey, sliderMenu.saveState())
        router?.saveState(outState)
        doOnSaveInstanceState(outState)
    }

    @Override
    final override fun onSaveInstanceState(
        outState: Bundle,
        outPersistentState: PersistableBundle
    ) {
        doOnSaveInstanceState(outState, outPersistentState)
    }

    @Override
    final override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        doOnRestoreInstanceState(savedInstanceState)
    }

    @Override
    final override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
        doOnRestoreInstanceState(savedInstanceState, persistentState)
    }

    @Override
    final override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(menuConfigurationBroadcastReceiver)
        unBindGeneratedRouterForActivity()
        doOnDestroy()
    }

    @Override
    final override fun onPause() {
        super.onPause()
        doOnPause()
    }

    @Override
    override fun onResume() {
        super.onResume()
        doOnResume()
    }

    @Override
    final override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        doOnNewIntent(getIntent())
    }

    @Override
    final override fun onBackPressed() {
        val fragmentList = supportFragmentManager.fragments
        var handled = false
        for (f in fragmentList)
            if (f is BaseFragment) {
                handled = f.onBackPressed()
                if (handled) break
            }
        if (!handled)
            router?.back()
    }

    @Override
    final override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_MOVE) dragged = true
        if (event.action == MotionEvent.ACTION_UP && !dragged) {
            val touchedView: View? =
                findViewAtPosition(window.decorView, event.rawX.toInt(), event.rawY.toInt())
            if (touchedView !is TextInputView && touchedView !is EditText) hideKeyboard(this)
        }
        if (event.action == MotionEvent.ACTION_UP) dragged = false
        return super.dispatchTouchEvent(event)
    }

    @Override
    final override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (!sliderMenu.isSliderEnabled) {
                    onBackPressed()
                } else {
                    if (sliderMenu.isDrawerOpen()) sliderMenu.closeSlider()
                    else sliderMenu.openSlider()
                }
            }
        }
        return false
    }

    fun relaunch() {
        val intent: Intent = intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
    }

    fun enableLeftDrawer(enabled: Boolean) {
        if (enabled) sliderMenu.enableSlider()
        else sliderMenu.disableSlider()
    }

    fun getFragmentContainerWrapper(): RelativeLayout {
        return fragmentContainerWrapper
    }

    fun getSliderMenu(): SliderMenu {
        return sliderMenu
    }

    fun isDrawerEnabled(): Boolean {
        return sliderMenu.isSliderEnabled
    }

    fun registerForegroundService(registry: ForegroundServiceRegistry) {
        lifecycleObservers.removeIf {
            return@removeIf it is ForegroundServiceWrapper &&
                    it.serviceRegistry.serviceClass == registry.foregroundServiceClass
        }
        lifecycle.addObserver(
            ForegroundServiceWrapper(this, registry).apply {
                lifecycleObservers.add(this)
            }
        )
    }

    private fun initializeNotificationService() {
        val serviceRegistry = registerNotificationService()
        if (serviceRegistry != null) {
            lifecycle.addObserver(
                NotificationJobServiceWrapper(
                    this,
                    serviceRegistry
                )
            )
        }
    }

    private fun initializeInternetCheckerService() {
        val internetConnectionListeners = registerInternetConnectionStateListeners()
        if (internetConnectionListeners != null)
            lifecycle.addObserver(
                ConnectionCheckerServiceWrapper(
                    this,
                    internetConnectionListeners
                )
            )
    }

    /**
     * Method setup up the activity view and main components.
     *
     * @param activityState saved state for the activity.
     */
    private fun setupActivity(activityState: Bundle?) {
        val drawerState = activityState?.getBundle(sliderBundleKey)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val sliderView = findViewById<MaterialDrawerSliderView>(R.id.sliderView)
        val menuConfiguration =
            getMenuConfiguration(this.javaClass)?.invoke(this) ?: SliderMenuConfiguration()
        sliderMenu = SliderMenu(
            this,
            drawerLayout,
            sliderView,
            drawerState,
            menuConfiguration
        )
        router = initializeRouter(fragmentContainerIdentifier)
        if (router == null) router = bindGeneratedRouterForActivity()
        if (activityState != null) router?.restoreState(activityState)
        fragmentContainerWrapper = findViewById(R.id.fragmentContainerWrapper)
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

    private fun initializeMenuChangedReceiver() {
        menuConfigurationBroadcastReceiver = MenuChangedBroadcastReceiver()
        menuConfigurationBroadcastReceiver.onMenuConfigurationChanged = { targetActivity ->
            val currentActivity = javaClass.name
            if (targetActivity == currentActivity) {
                val newMenuConfiguration = getMenuConfiguration(javaClass)?.invoke(this)
                sliderMenu.setConfiguration(newMenuConfiguration ?: SliderMenuConfiguration())
            }
        }
        registerReceiver(
            menuConfigurationBroadcastReceiver,
            IntentFilter(BuildConfig.menuConfigChangedAction)
        )
    }

    private fun bindGeneratedRouterForActivity(): BaseActivityRouter? {
        if (navigatorClass == null) return null
        val getInstanceMethod = navigatorClass!!.getMethod("getInstance")
        getInstanceMethod.invoke(null, this)
        val initRouterMethod = navigatorClass!!.getMethod("initializeRouter")
        return initRouterMethod.invoke(
            this,
            fragmentContainerIdentifier,
            this
        ) as BaseActivityRouter
    }

    private fun unBindGeneratedRouterForActivity() {
        if (navigatorClass == null) return
        val getInstanceMethod = navigatorClass!!.getMethod("getInstance")
        getInstanceMethod.invoke(null, this)
        val unBindRouterMethod = navigatorClass!!.getMethod("unBind")
        unBindRouterMethod.invoke(null, this)
    }
}