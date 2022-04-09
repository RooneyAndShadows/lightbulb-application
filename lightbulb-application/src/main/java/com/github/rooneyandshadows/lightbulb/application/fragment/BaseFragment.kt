package com.github.rooneyandshadows.lightbulb.application.fragment

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration.BaseFragmentConfiguration

@Suppress("MemberVisibilityCanBePrivate", "UNUSED_PARAMETER", "unused")
abstract class BaseFragment : Fragment() {
    lateinit var fragmentConfiguration: BaseFragmentConfiguration
        private set
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
    private var willExecuteAnimation = false

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

    protected open fun viewStateRestored(savedInstanceState: Bundle?) {
    }

    protected open fun onEnterTransitionFinished() {
    }

    protected open fun saveInstanceState(outState: Bundle) {
    }

    protected open fun attach() {
    }

    protected open fun pause() {
    }

    protected open fun destroy() {
    }

    protected open fun resume() {
    }

    /**
     * Method is used to handle back press on fragment.
     */
    @Override
    open fun onBackPressed(): Boolean {
        return false
    }

    @Override
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

    @Override
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
    @Override
    final override fun onViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(fragmentView, savedInstanceState)
        if (fragmentConfiguration.isContentFragment) {
            contextActivity.enableLeftDrawer(fragmentConfiguration.hasLeftDrawer)
            actionBarManager = ActionBarManager(this, fragmentConfiguration.actionBarConfiguration)
        }
        selectViews()
        viewCreated(fragmentView, savedInstanceState)
    }

    @Override
    final override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewStateRestored(savedInstanceState)
    }

    @Override
    final override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!isFragmentVisible()) //previously created bundle needed only when fragment is not visible
            outState.putBoolean(isReusedKey, isReused)
        saveInstanceState(outState)
    }

    @Override
    final override fun onDestroy() {
        super.onDestroy()
        destroy()
    }

    @Override
    final override fun onPause() {
        super.onPause()
        isCreated = false
        isRestarted = false
        isReused = true
        pause()
    }

    @Override
    final override fun onResume() {
        super.onResume()
        if (!willExecuteAnimation && isCreated)
            onEnterTransitionFinished()
        resume()
    }

    @Override
    final override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseActivity)
            contextActivity = context
        attach()
    }

    @Override
    final override fun onCreateAnimation(
        transit: Int,
        isEnterTransition: Boolean,
        nextAnim: Int
    ): Animation? {
        enableTouch(false)
        return if (fragmentConfiguration.isContentFragment && nextAnim != 0) {
            willExecuteAnimation = true
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

    @Override
    final override fun onCreateAnimator(
        transit: Int,
        isEnterTransition: Boolean,
        nextAnim: Int
    ): Animator? {
        enableTouch(false)
        return if (fragmentConfiguration.isContentFragment && nextAnim != 0) {
            willExecuteAnimation = true
            val animator = AnimatorInflater.loadAnimator(contextActivity, nextAnim)
            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    TODO("Not yet implemented")
                }

                override fun onAnimationEnd(animation: Animator?) {
                    if (isEnterTransition && isSafe) {
                        enableTouch(true)
                        onEnterTransitionFinished()
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                    if (isEnterTransition && isSafe) {
                        enableTouch(true)
                        onEnterTransitionFinished()
                    }
                }

                override fun onAnimationRepeat(animation: Animator?) {}
            })
            animator
        } else {
            enableTouch(true)
            if (isEnterTransition && isEnterTransition) onEnterTransitionFinished()
            super.onCreateAnimator(transit, isEnterTransition, nextAnim)
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
}