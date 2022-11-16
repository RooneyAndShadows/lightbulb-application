package com.github.rooneyandshadows.lightbulb.application.activity.routing

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.github.rooneyandshadows.lightbulb.application.R
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import java.util.*

@Suppress("CanBePrimaryConstructorProperty", "unused", "MemberVisibilityCanBePrivate")
open class BaseActivityRouter(contextActivity: BaseActivity, fragmentContainerId: Int) {
    protected val contextActivity: BaseActivity = contextActivity
    private val fragmentContainerId: Int = fragmentContainerId
    private val fragmentManager: FragmentManager
    private val logTag: String
    private var backStack = ActivityRouterBackStack()

    init {
        fragmentManager = contextActivity.supportFragmentManager
        logTag = "[".plus(javaClass.simpleName).plus("]")
    }

    fun saveState(activityBundle: Bundle) {
        activityBundle.putParcelable("ACTIVITY_ROUTER_BACKSTACK", backStack)
    }

    fun restoreState(activitySavedState: Bundle) {
        backStack = activitySavedState.getParcelable("ACTIVITY_ROUTER_BACKSTACK")!!
    }

    fun forward(newScreen: FragmentScreen) {
        forward(newScreen, TransitionTypes.ENTER, UUID.randomUUID().toString())
    }

    fun forward(newScreen: FragmentScreen, backStackEntryName: String) {
        forward(newScreen, TransitionTypes.ENTER, backStackEntryName)
    }

    fun forward(newScreen: FragmentScreen, transition: TransitionTypes) {
        forward(newScreen, transition, UUID.randomUUID().toString())
    }

    fun forward(
        newScreen: FragmentScreen,
        transition: TransitionTypes,
        backStackEntryName: String,
    ) {
        //val currentFragment = fragmentManager.findFragmentById(fragmentContainerId)
        val requestedFragment = newScreen.getFragment()
        val currentFragment = getCurrentFragment()
        startTransaction(transition).apply {
            runOnCommit {
                backStack.add(backStackEntryName)
            }
            add(fragmentContainerId, requestedFragment, backStackEntryName)
            if (currentFragment != null)
                hide(currentFragment)
            commit()
        }
    }

    fun back() {
        if (backStack.getEntriesCount() <= 1) contextActivity.moveTaskToBack(true)
        else startTransaction(TransitionTypes.EXIT).apply {
            val currentFrag = popCurrentFragment()
            val nextFragment = getCurrentFragment()
            if (currentFrag != null)
                remove(currentFrag)
            show(nextFragment!!)
            commit()
        }
    }

    fun backNTimesAndReplace(n: Int, newScreen: FragmentScreen) {
        backNTimesAndReplace(n, newScreen, true)
    }

    fun backNTimesAndReplace(n: Int, newScreen: FragmentScreen, animate: Boolean) {
        startTransaction(null).apply {
            val initialSize = backStack.getEntriesCount()
            while (backStack.getEntriesCount() > initialSize - n) {
                val fragToRemove = popCurrentFragment()
                remove(fragToRemove!!)
            }
            val backStackName = UUID.randomUUID().toString()
            val fragmentToAdd = newScreen.getFragment()
            if (animate)
                setCustomAnimations(
                    0,
                    0,
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
                )
            add(R.id.fragmentContainer, fragmentToAdd, backStackName)
            runOnCommit {
                backStack.add(backStackName)
            }
            commit()
        }
    }

    fun replaceTop(newScreen: FragmentScreen) {
        replaceTop(newScreen, true)
    }

    fun replaceTop(newScreen: FragmentScreen, animate: Boolean) {
        backNTimesAndReplace(1, newScreen, animate)
    }

    fun backToRoot() {
        startTransaction(null).apply {
            while (backStack.getEntriesCount() > 1) {
                val fragToRemove = popCurrentFragment()
                remove(fragToRemove!!)
            }
            show(getCurrentFragment()!!)
            commit()
        }
    }

    fun newRootChain(vararg screens: FragmentScreen) {
        startTransaction(null).apply {
            while (backStack.getEntriesCount() > 0) {
                val fragToRemove = popCurrentFragment()
                remove(fragToRemove!!)
            }
            screens.forEachIndexed { index, fragmentScreen ->
                val isLast = index == screens.size - 1
                val backStackName = UUID.randomUUID().toString()
                val fragmentToAdd = fragmentScreen.getFragment()
                add(R.id.fragmentContainer, fragmentToAdd, backStackName)
                if (!isLast)
                    hide(fragmentToAdd)
                runOnCommit {
                    backStack.add(backStackName)
                }
                commit()
            }
        }
    }

    fun newRootScreen(newRootScreen: FragmentScreen) {
        newRootChain(newRootScreen)
    }

    fun customAction(action: CustomRouterAction) {
        action.execute(fragmentContainerId, fragmentManager, backStack)
    }

    private fun getCurrentFragment(): Fragment? {
        val currentTag = backStack.getCurrent() ?: return null
        return fragmentManager.fragments.find { return@find it.tag == currentTag }
    }

    private fun popCurrentFragment(): Fragment? {
        val currentTag = backStack.pop() ?: return null
        return fragmentManager.fragments.find { return@find it.tag == currentTag }
    }

    private fun startTransaction(transition: TransitionTypes?): FragmentTransaction {
        val transaction = fragmentManager.beginTransaction().apply {
            setReorderingAllowed(true)
            when (transition) {
                TransitionTypes.ENTER -> setCustomAnimations(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left,
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
                )
                TransitionTypes.EXIT -> setCustomAnimations(
                    R.anim.enter_from_left,
                    R.anim.exit_to_right,
                    R.anim.enter_from_right,
                    R.anim.exit_to_left
                )
                else -> {}
            }
        }
        return transaction
    }

    fun printBackStack() {
        Log.i(logTag, "CURRENT BACKSTACK")
        Log.i(logTag, "----------------------------")
        val entriesCount = fragmentManager.backStackEntryCount
        if (entriesCount == 0)
            Log.i(logTag, "Backstack is empty")
        for (entry in 0 until entriesCount)
            Log.i(
                logTag,
                entry.toString().plus(" " + fragmentManager.getBackStackEntryAt(entry).name)
            )
    }

    enum class TransitionTypes(val type: Int) {
        NONE(1),
        ENTER(2),
        EXIT(3)
    }

    interface CustomRouterAction {
        fun execute(
            fragmentContainerId: Int,
            fragmentManager: FragmentManager,
            backStack: ActivityRouterBackStack
        )
    }

    abstract class FragmentScreen {
        abstract fun getFragment(): Fragment
    }
}