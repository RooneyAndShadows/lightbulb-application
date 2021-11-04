package com.github.rooneyandshadows.lightbulb.application.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration.BaseFragmentConfiguration

@Suppress("MemberVisibilityCanBePrivate", "UNUSED_PARAMETER", "unused")
abstract class BaseFragment : Fragment() {
    lateinit var fragmentConfiguration: BaseFragmentConfiguration
        private set
    lateinit var contextActivity: BaseActivity
        private set
    private var actionBarUtils: ActionBarHelper? = null
    private val isReusedKey = "IS_FRAGMENT_PREVIOUSLY_CREATED"
    protected val isSafe: Boolean
        get() = !(this.isRemoving || this.activity == null || this.isDetached || !this.isAdded || this.view == null)
    protected var isCreated: Boolean = false
        private set
    protected var isRestarted: Boolean = false
        private set
    protected var isReused: Boolean = false
        private set

    protected abstract fun configureFragment(): BaseFragmentConfiguration


    /**
     * Method is used to handle fragment arguments.
     */
    protected open fun handleArguments(arguments: Bundle?) {
    }

    protected open fun create(savedInstanceState: Bundle?) {
    }

    /**
     * Method is used to select views from the fragment layout.
     */
    protected open fun selectViews() {
    }

    protected open fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return null
    }

    protected open fun viewCreated(fragmentView: View, savedInstanceState: Bundle?) {
    }

    /**
     * Called when the transition animation for the fragment is finished.
     */
    protected open fun onEnterTransitionFinished() {
    }

    protected open fun saveInstanceState(outState: Bundle) {
    }

    protected open fun attach() {
    }

    protected open fun pause() {
    }

    /**
     * Method is used to handle back press on fragment.
     */
    open fun onBackPressed(): Boolean {
        return false
    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null)
            isReused = savedInstanceState.getBoolean(isReusedKey, false)
        if (!isReused) {
            isRestarted = savedInstanceState != null
            isCreated = !isRestarted
        }
        create(savedInstanceState)
        if (!isRestarted)
            handleArguments(arguments)
        fragmentConfiguration = configureFragment()
    }

    final override fun onPause() {
        super.onPause()
        isCreated = false
        isRestarted = false
        isReused = true
        pause()
    }

    final override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!isFragmentVisible()) //previously created bundle needed only when fragment is not visible
            outState.putBoolean(isReusedKey, isReused)
        saveInstanceState(outState)
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(fragmentConfiguration.hasOptionsMenu)
        return createView(inflater, container, savedInstanceState);
    }

    /**
     * The method is executed before any saved state started.
     *
     * @param fragmentView       main view of the fragment.
     * @param savedInstanceState saved state for the fragment.
     */
    final override fun onViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(fragmentView, savedInstanceState)
        if (fragmentConfiguration.isContentFragment) {
            contextActivity.enableLeftDrawer(fragmentConfiguration.hasLeftDrawer)
            actionBarUtils = ActionBarHelper(this, fragmentConfiguration.actionBarConfiguration)
        }
        selectViews()
        viewCreated(fragmentView, savedInstanceState)
    }

    final override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseActivity)
            contextActivity = context
        attach()
    }

    final override fun onCreateAnimation(
        transit: Int,
        isEnterTransition: Boolean,
        nextAnim: Int
    ): Animation? {
        enableTouch(false)
        return if (fragmentConfiguration.isContentFragment && nextAnim != 0) {
            val anim = AnimationUtils.loadAnimation(contextActivity, nextAnim)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    if (isEnterTransition && isSafe) {
                        enableTouch(true)
                        onEnterTransitionFinished()
                    }
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            anim
        } else {
            enableTouch(true)
            if (isEnterTransition && isEnterTransition) onEnterTransitionFinished()
            super.onCreateAnimation(transit, isEnterTransition, nextAnim)
        }
    }

    fun getFragmentState(): FragmentStates {
        if (isCreated)
            return FragmentStates.CREATED
        if (isRestarted)
            return FragmentStates.RESTARTED
        return FragmentStates.REUSED
    }

    private fun isFragmentVisible(): Boolean {
        return this.view != null
    }

    private fun enableTouch(enabled: Boolean) {
        val activity = activity ?: return
        val window = activity.window
        if (enabled) window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) else window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    enum class FragmentStates(val value: Int) {
        CREATED(0),
        RESTARTED(1),
        REUSED(2)
    }

    class ActionBarHelper(
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
            val toolbar: Toolbar =
                contextFragment.requireView().findViewById(configuration.actionBarId)
            toolbar.contentInsetStartWithNavigation = 0
            contextFragment.contextActivity.setSupportActionBar(toolbar)
            contextFragment.contextActivity.supportActionBar!!.title = configuration.title
            contextFragment.contextActivity.supportActionBar!!.subtitle = configuration.subtitle
            if (configuration.isEnableActions) setupHomeIcon(configuration.homeIcon)
        }

        private fun setupHomeIcon(icon: Drawable?) {
            val actionBar = getActionBar()!!
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(false)
            actionBar.setHomeAsUpIndicator(icon)
        }

        private fun getActionBar(): ActionBar? {
            return contextFragment.contextActivity.supportActionBar
        }
    }
}