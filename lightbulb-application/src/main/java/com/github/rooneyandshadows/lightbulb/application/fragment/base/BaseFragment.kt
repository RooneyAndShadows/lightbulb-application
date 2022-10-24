package com.github.rooneyandshadows.lightbulb.application.fragment.base

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
import com.github.rooneyandshadows.lightbulb.application.annotations.BindLayout
import com.github.rooneyandshadows.lightbulb.application.annotations.BindView
import com.github.rooneyandshadows.lightbulb.application.fragment.annotations.FragmentConfiguration
import com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration.ActionBarConfiguration
import com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration.ActionBarManager

@Suppress("MemberVisibilityCanBePrivate", "UNUSED_PARAMETER", "unused")
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
    private var layoutId: Int = -1
    private var animationCreated = false
    private var withLeftDrawer: Boolean = false
    private var withOptionsMenu: Boolean = false

    protected open fun configureFragment(): Configuration {
        return Configuration()
    }

    protected open fun configureActionBar(): ActionBarConfiguration? {
        return null
    }

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
        return inflater.inflate(layoutId, container, false)
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

    protected open fun getLayoutId(): Int {
        return -1
    }

    /**
     * Method is used to handle back press on fragment.
     */
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
        configuration = configureFragment()
    }

    @Override
    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(configuration!!.hasOptionsMenu)
        return createView(inflater, container, savedInstanceState)
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
        if (configuration!!.isMainScreenFragment) {
            contextActivity.enableLeftDrawer(configuration!!.hasLeftDrawer)
            actionBarManager = ActionBarManager(this, configureActionBar())
        }
        selectViewsInternally()
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
        if (configuration!!.isMainScreenFragment && isCreated && !animationCreated)
            onEnterTransitionFinished()
        resume()
    }

    @Override
    final override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseActivity)
            contextActivity = context
        setupFragment()
        attach()
    }

    private fun setupFragment() {
        handleClassAnnotations()
        if (configuration == null)
            configuration = configureFragment()
        val layout = getLayoutId()
        if (layout != -1)
            this.layoutId = layout
    }

    @Override
    final override fun onCreateAnimation(
        transit: Int,
        isEnterTransition: Boolean,
        nextAnim: Int
    ): Animation? {
        animationCreated = true
        enableTouch(false)
        return if (configuration!!.isMainScreenFragment && nextAnim != 0) {
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
        animationCreated = true
        enableTouch(false)
        return if (configuration!!.isMainScreenFragment && nextAnim != 0) {
            val animator = AnimatorInflater.loadAnimator(contextActivity, nextAnim)
            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
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


    private fun handleClassAnnotations() {
        for (annotation in javaClass.annotations)
            when (annotation) {
                is FragmentConfiguration -> {
                    configuration = Configuration(
                        annotation.isMainScreenFragment,
                        annotation.hasLeftDrawer,
                        annotation.hasOptionsMenu
                    )
                }
                is BindLayout -> {
                    layoutId = resources.getIdentifier(
                        annotation.name,
                        "layout",
                        requireActivity().packageName
                    )
                }
            }
    }

    private fun selectViewsInternally() {
        for (field in javaClass.declaredFields) {
            val annotations = field.annotations
            if (annotations.isEmpty()) continue
            annotations.forEach { annotation ->
                if (annotation is BindView) {
                    field.isAccessible = true
                    val id = resources.getIdentifier(
                        annotation.name,
                        "id",
                        requireActivity().packageName
                    )
                    field.set(this, requireView().findViewById(id))
                }
            }
        }
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

    class Configuration(
        val isMainScreenFragment: Boolean = true,
        val hasLeftDrawer: Boolean = false,
        val hasOptionsMenu: Boolean = false
    )
}