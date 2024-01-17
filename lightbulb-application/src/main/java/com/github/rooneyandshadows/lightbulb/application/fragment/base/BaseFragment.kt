package com.github.rooneyandshadows.lightbulb.application.fragment.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration.ActionBarConfiguration
import com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration.ActionBarManager

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class BaseFragment : Fragment() {
    private var configuration: Configuration? = null
    lateinit var contextActivity: BaseActivity
        private set
    lateinit var actionBarManager: ActionBarManager
        private set
    private val isReusedKey = "IS_FRAGMENT_PREVIOUSLY_CREATED"
    protected val isSafe: Boolean
        get() = !(this.isRemoving || this.activity == null || this.isDetached || !this.isAdded || this.view == null)
    protected var isCreated: Boolean = false
        private set
    protected var isRestarted: Boolean = false
        private set
    protected var isReused: Boolean = false
        private set
    private var withLeftDrawer: Boolean = false
    private var withOptionsMenu: Boolean = false


    protected open fun configureFragment(): Configuration? {
        return Configuration()
    }

    protected open fun configureActionBar(): ActionBarConfiguration? {
        return null
    }

    protected open fun selectViews() {
    }

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configuration = configureFragment() ?: Configuration()
        if (savedInstanceState != null)
            isReused = savedInstanceState.getBoolean(isReusedKey, false)
        if (!isReused) {
            isRestarted = savedInstanceState != null
            isCreated = !isRestarted
        }
    }

    @Override
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(configuration!!.hasOptionsMenu)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    /**
     * The method is executed before any saved state started.
     *
     * @param fragmentView       main view of the fragment.
     * @param savedInstanceState saved state for the fragment.
     */
    @Override
    override fun onViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(fragmentView, savedInstanceState)
        setupLeftDrawerAndActionBar()
        selectViews()
    }

    @Override
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!isFragmentVisible()) //previously created bundle needed only when fragment is not visible
            outState.putBoolean(isReusedKey, isReused)
    }

    @Override
    override fun onPause() {
        super.onPause()
        isCreated = false
        isRestarted = false
        isReused = true
    }

    @Override
    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) setupLeftDrawerAndActionBar()
        else isReused = true
    }

    @Override
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseActivity)
            contextActivity = context
    }

    fun getFragmentState(): FragmentStates {
        if (isReused) return FragmentStates.REUSED
        if (isRestarted) return FragmentStates.RESTARTED
        return FragmentStates.CREATED
    }


    private fun isFragmentVisible(): Boolean {
        return this.view != null
    }

    private fun setupLeftDrawerAndActionBar() {
        if (configuration!!.isMainScreenFragment) {
            contextActivity.enableLeftDrawer(configuration!!.hasLeftDrawer)
            actionBarManager = ActionBarManager(this, configureActionBar())
        }
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

    class Configuration(
        val isMainScreenFragment: Boolean = true,
        val hasLeftDrawer: Boolean = true,
        val hasOptionsMenu: Boolean = true
    )
}