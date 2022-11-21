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
import com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration.ActionBarConfiguration
import com.github.rooneyandshadows.lightbulb.application.fragment.cofiguration.ActionBarManager
import java.lang.reflect.Method

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
    protected var layoutIdentifier: Int = -1
        private set
    private var animationCreated = false
    private var withLeftDrawer: Boolean = false
    private var withOptionsMenu: Boolean = false
    private var bindingClass: Class<*>? = null

    init {
        try {
            bindingClass = Class.forName(
                javaClass.`package`?.name.plus(".")
                    .plus(javaClass.simpleName).plus("Bindings")
            )
        } catch (e: Throwable) {
            //ignored
        }
    }

    protected open fun configureFragment(): Configuration? {
        return null
    }

    protected open fun configureActionBar(): ActionBarConfiguration? {
        return null
    }

    protected open fun handleArguments(arguments: Bundle?) {
    }

    protected open fun selectViews() {
    }

    protected open fun onEnterTransitionFinished() {
    }

    protected open fun getLayoutId(): Int {
        return -1
    }

    open fun onBackPressed(): Boolean {
        return false
    }

    protected open fun doOnCreate(savedInstanceState: Bundle?) {
    }

    protected open fun doOnCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutIdentifier, container, false)
    }

    protected open fun doOnViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
    }

    protected open fun doOnViewStateRestored(savedInstanceState: Bundle?) {
    }

    protected open fun doOnSaveInstanceState(outState: Bundle) {
    }

    protected open fun doOnAttach() {
    }

    protected open fun doOnPause() {
    }

    protected open fun doOnDestroy() {
    }

    protected open fun doOnResume() {
    }

    protected open fun doOnHiddenChanged(hidden: Boolean) {
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
        doOnCreate(savedInstanceState)
        if (!isRestarted) {
            handleInputParameters()
            handleArguments(arguments)
        }
        if (savedInstanceState != null)
            handleRestoreVariables(savedInstanceState)
    }

    @Override
    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(configuration!!.hasOptionsMenu)
        return doOnCreateView(inflater, container, savedInstanceState)
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
        setupLeftDrawerAndActionBar()
        selectViewsFromGeneratedBindings()
        selectViews()
        doOnViewCreated(fragmentView, savedInstanceState)
    }

    @Override
    final override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        doOnViewStateRestored(savedInstanceState)
    }

    @Override
    final override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!isFragmentVisible()) //previously created bundle needed only when fragment is not visible
            outState.putBoolean(isReusedKey, isReused)
        handleSaveVariables(outState)
        doOnSaveInstanceState(outState)
    }

    @Override
    final override fun onDestroy() {
        super.onDestroy()
        doOnDestroy()
    }

    @Override
    final override fun onPause() {
        super.onPause()
        isCreated = false
        isRestarted = false
        isReused = true
        doOnPause()
    }

    @Override
    final override fun onResume() {
        super.onResume()
        if (configuration!!.isMainScreenFragment && isCreated && !animationCreated)
            onEnterTransitionFinished()
        doOnResume()
    }

    @Override
    final override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) setupLeftDrawerAndActionBar()
        else isReused = true
        doOnHiddenChanged(hidden)
    }

    @Override
    final override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseActivity)
            contextActivity = context
        setupFragment()
        doOnAttach()
    }

    private fun setupFragment() {
        configuration = configureFragment()
        if (configuration == null)
            configuration = getConfigFromGeneratedBinding() ?: Configuration()
        val layout = getLayoutId()
        val hasDefinedLayout = layout != -1
        if (hasDefinedLayout) this.layoutIdentifier = layout
        else this.layoutIdentifier = configuration!!.layoutId
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

    private fun handleRestoreVariables(savedInstanceState: Bundle) {
        if (bindingClass == null)
            return
        val methodName = "restoreVariablesState"
        val method: Method = bindingClass!!.getMethod(methodName, Bundle::class.java, javaClass)
        method.invoke(this, savedInstanceState, this)
    }

    private fun handleSaveVariables(outState: Bundle) {
        if (bindingClass == null)
            return
        val methodName = "saveVariablesState"
        val method: Method = bindingClass!!.getMethod(methodName, Bundle::class.java, javaClass)
        method.invoke(this, outState, this)
    }

    private fun handleInputParameters() {
        if (bindingClass == null)
            return
        val methodName = "generateParameters"
        val method: Method = bindingClass!!.getMethod(methodName, javaClass)
        method.invoke(null, this)
    }

    private fun selectViewsFromGeneratedBindings() {
        if (bindingClass == null)
            return
        val methodName = "generateViewBindings"
        val method: Method =
            bindingClass!!.getMethod(methodName, javaClass)
        method.invoke(null, this)
    }

    private fun getConfigFromGeneratedBinding(): Configuration? {
        if (bindingClass == null)
            return null
        val methodName = "generateConfiguration"
        val method: Method =
            bindingClass!!.getMethod(methodName, javaClass)
        return method.invoke(null, this) as Configuration
    }

    enum class FragmentStates(val value: Int) {
        CREATED(0),
        RESTARTED(1),
        REUSED(2)
    }

    class Configuration(
        val layoutId: Int = -1,
        val isMainScreenFragment: Boolean = true,
        val hasLeftDrawer: Boolean = false,
        val hasOptionsMenu: Boolean = false
    )
}