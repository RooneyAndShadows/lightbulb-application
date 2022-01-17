package com.github.rooneyandshadows.lightbulb.application.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.graphics.Rect
import android.net.NetworkRequest
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.LocaleChangerAppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import com.github.rooneyandshadows.lightbulb.application.activity.helpers.SliderHelper
import com.github.rooneyandshadows.lightbulb.application.activity.helpers.SliderHelper.*
import com.github.rooneyandshadows.lightbulb.application.activity.routing.BaseApplicationRouter
import com.github.rooneyandshadows.lightbulb.application.fragment.BaseFragment
import com.github.rooneyandshadows.lightbulb.commons.utils.KeyboardUtils.Companion.hideKeyboard
import com.github.rooneyandshadows.lightbulb.textinputview.TextInputView
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.R
import com.github.rooneyandshadows.lightbulb.application.activity.configuration.StompNotificationServiceRegistry
import com.github.rooneyandshadows.lightbulb.application.activity.service.ConnectionCheckerService

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
    private lateinit var sliderUtils: SliderHelper
    private lateinit var localeChangerAppCompatDelegate: LocaleChangerAppCompatDelegate
    protected abstract val drawerConfiguration: SliderConfiguration
    private val internetAccessServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as ConnectionCheckerService.LocalBinder
            binder.getService().activity = this@BaseActivity
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
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

    open fun onInternetConnectionStatusChanged(hasInternetServiceEnabled: Boolean) {
    }

    @Override
    final override fun onCreate(savedInstanceState: Bundle?) {
        beforeCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_activity_layout)
        initializeStompNotificationServiceIfPresented();
        setupActivity(savedInstanceState)
        setUnhandledGlobalExceptionHandler()
        create(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        startInternetCheckerService();
        resume()
    }

    @Override
    final override fun onPause() {
        super.onPause()
        stopInternetCheckerService()
        pause()
    }

    @Override
    final override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(sliderBundleKey, sliderUtils.saveState())
        saveInstanceState(outState)
    }

    @Override
    final override fun onDestroy() {
        super.onDestroy()
        appRouter?.removeNavigator()
        destroy()
    }

    @Override
    final override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        newIntent(getIntent())
    }

    @Override
    final override fun getDelegate(): AppCompatDelegate {
        localeChangerAppCompatDelegate = LocaleChangerAppCompatDelegate(super.getDelegate())
        return localeChangerAppCompatDelegate
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
                if (!sliderUtils.isSliderEnabled) {
                    onBackPressed()
                } else {
                    if (sliderUtils.isDrawerOpen())
                        sliderUtils.closeSlider();
                    else
                        sliderUtils.openSlider();
                }
            }
        }
        return false
    }

    fun isDrawerEnabled(): Boolean {
        return sliderUtils.isSliderEnabled
    }

    /**
     * Method enables or disables drawer
     */
    fun enableLeftDrawer(enabled: Boolean) {
        if (enabled) sliderUtils.enableSlider()
        else sliderUtils.disableSlider()
    }

    fun reinitializeLeftDrawer() {
        sliderUtils.reInitialize(drawerConfiguration.configure());
    }

    /**
     * Method setup up the activity view and main components.
     *
     * @param activityState saved state for the activity.
     */
    private fun setupActivity(activityState: Bundle?) {
        val drawerState = activityState?.getBundle(sliderBundleKey)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout);
        val sliderView = findViewById<MaterialDrawerSliderView>(R.id.sliderView)
        sliderUtils = SliderHelper(
            this,
            drawerLayout,
            sliderView,
            drawerState,
            drawerConfiguration.configure()
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
        Intent(this, ConnectionCheckerService::class.java).also { intent ->
            bindService(intent, internetAccessServiceConnection, Context.BIND_AUTO_CREATE)
        }
        startService(Intent(this, ConnectionCheckerService::class.java))
    }

    private fun stopInternetCheckerService() {
        unbindService(internetAccessServiceConnection)
    }

    private fun initializeStompNotificationServiceIfPresented() {
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