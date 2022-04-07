package com.github.rooneyandshadows.lightbulb.application.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.drawerlayout.widget.DrawerLayout
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.R
import com.github.rooneyandshadows.lightbulb.application.activity.configuration.StompNotificationServiceRegistry
import com.github.rooneyandshadows.lightbulb.application.activity.receivers.MenuChangedBroadcastReceiver
import com.github.rooneyandshadows.lightbulb.application.activity.receivers.NotificationBroadcastReceiver
import com.github.rooneyandshadows.lightbulb.application.activity.routing.BaseApplicationRouter
import com.github.rooneyandshadows.lightbulb.application.activity.service.ConnectionCheckerService
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.SliderMenu
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.config.SliderMenuConfiguration
import com.github.rooneyandshadows.lightbulb.application.fragment.BaseFragment
import com.github.rooneyandshadows.lightbulb.commons.utils.KeyboardUtils.Companion.hideKeyboard
import com.github.rooneyandshadows.lightbulb.commons.utils.LocaleHelper
import com.github.rooneyandshadows.lightbulb.textinputview.TextInputView
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView

@Suppress(
    "unused",
    "UNUSED_PARAMETER",
    "UNUSED_ANONYMOUS_PARAMETER"
)
abstract class BaseActivity : AppCompatActivity() {
    private val sliderBundleKey = "DRAWER_STATE"
    private val stompNotificationJobId = 1
    private val contentContainerIdentifier = R.id.fragmentContainer
    private var dragged = false
    private var appRouter: BaseApplicationRouter? = null
    private lateinit var sliderMenu: SliderMenu
    private lateinit var notificationBroadcastReceiver: NotificationBroadcastReceiver
    private lateinit var menuConfigurationBroadcastReceiver: MenuChangedBroadcastReceiver
    private lateinit var internetAccessServiceConnection: ServiceConnection
    var onNotificationReceivedListener: Runnable? = null

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

    protected open fun initializeRouter(fragmentContainerId: Int): BaseApplicationRouter? {
        return null
    }

    protected open fun beforeCreate(savedInstanceState: Bundle?) {
    }

    protected open fun create(savedInstanceState: Bundle?) {
    }

    protected open fun resume() {

    }

    protected open fun newIntent(intent: Intent) {
    }

    protected open fun saveInstanceState(outState: Bundle) {
    }

    protected open fun pause() {
    }

    protected open fun destroy() {
    }

    protected open fun registerStompService(): StompNotificationServiceRegistry? {
        return null
    }

    protected open fun onNotificationReceived() {

    }

    open fun onInternetConnectionStatusChanged(hasInternetServiceEnabled: Boolean) {
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
        beforeCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_activity_layout)
        initializeMenuChangedReceiver()
        initializeStompNotificationServiceIfPresented()
        setupActivity(savedInstanceState)
        setUnhandledGlobalExceptionHandler()
        create(savedInstanceState)
    }

    @Override
    final override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(sliderBundleKey, sliderMenu.saveState())
        saveInstanceState(outState)
    }

    @Override
    final override fun onDestroy() {
        super.onDestroy()
        appRouter?.removeNavigator()
        unregisterReceiver(notificationBroadcastReceiver)
        unregisterReceiver(menuConfigurationBroadcastReceiver)
        destroy()
    }

    @Override
    final override fun onPause() {
        super.onPause()
        stopInternetCheckerService()
        pause()
    }

    @Override
    override fun onResume() {
        super.onResume()
        startInternetCheckerService();
        resume()
    }

    @Override
    final override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        newIntent(getIntent())
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
            if (supportFragmentManager.backStackEntryCount == 0) moveTaskToBack(true)
            else appRouter?.navigateBack()
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
                    if (sliderMenu.isDrawerOpen())
                        sliderMenu.closeSlider()
                    else
                        sliderMenu.openSlider()
                }
            }
        }
        return false
    }

    fun reload() {
        val intent: Intent = getIntent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
    }

    fun getSliderMenu(): SliderMenu {
        return sliderMenu
    }

    fun isDrawerEnabled(): Boolean {
        return sliderMenu.isSliderEnabled
    }

    /**
     * Method enables or disables drawer
     */
    fun enableLeftDrawer(enabled: Boolean) {
        if (enabled) sliderMenu.enableSlider()
        else sliderMenu.disableSlider()
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
        appRouter = initializeRouter(contentContainerIdentifier)
    }

    /**
     * Method sets exception handler for uncaught exceptions in the activity context.
     */
    private fun setUnhandledGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { paramThread: Thread?, exception: Throwable ->
            exception.printStackTrace()
            this.runOnUiThread {
                Toast.makeText(
                    this@BaseActivity,
                    "Error occurred.",
                    Toast.LENGTH_LONG
                ).show()
            }
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

    private fun startInternetCheckerService() {
        internetAccessServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                val binder = service as ConnectionCheckerService.LocalBinder
                binder.getService().activity = this@BaseActivity
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
            }
        }
        val intent = Intent(this, ConnectionCheckerService::class.java)
        bindService(intent, internetAccessServiceConnection, Context.BIND_AUTO_CREATE)
        startService(intent)
    }

    private fun stopInternetCheckerService() {
        stopService(Intent(this, ConnectionCheckerService::class.java))
        unbindService(internetAccessServiceConnection)
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

    private fun initializeStompNotificationServiceIfPresented() {
        notificationBroadcastReceiver = NotificationBroadcastReceiver()
        notificationBroadcastReceiver.onNotificationReceived = {
            onNotificationReceivedListener?.run()
            onNotificationReceived()
        }
        registerReceiver(
            notificationBroadcastReceiver,
            IntentFilter(BuildConfig.notificationReceivedAction)
        )
        val stompNotificationServiceRegistry = registerStompService() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                BuildConfig.notificationChannelId,
                stompNotificationServiceRegistry.notificationChannelName,
                importance
            )
            channel.setShowBadge(true)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val name = ComponentName(this, stompNotificationServiceRegistry.notificationServiceClass)
        val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (!checkIfJobServiceScheduled(stompNotificationJobId)) {
            val b = JobInfo.Builder(stompNotificationJobId, name)
                .setPeriodic(900000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
            scheduler.schedule(b.build())
        }
    }

    private fun checkIfJobServiceScheduled(jobServiceId: Int): Boolean {
        val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        var hasBeenScheduled = false
        for (jobInfo in scheduler.allPendingJobs) {
            if (jobInfo.id == stompNotificationJobId) {
                hasBeenScheduled = true
                break
            }
        }
        return hasBeenScheduled
    }
}