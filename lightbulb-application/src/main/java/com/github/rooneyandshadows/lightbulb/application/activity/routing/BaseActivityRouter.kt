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

    fun saveState(): Bundle {
        return Bundle().apply {
            putParcelable("ACTIVITY_ROUTER_BACKSTACK", backStack)
        }
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

    private fun getCurrentFragment(): Fragment? {
        val currentTag = backStack.getCurrent() ?: return null
        return fragmentManager.fragments.find { return@find it.tag == currentTag }
    }

    private fun popCurrentFragment(): Fragment? {
        val currentTag = backStack.pop() ?: return null
        return fragmentManager.fragments.find { return@find it.tag == currentTag }
    }

    fun back() {
        if (backStack.getEntriesCount() <= 1)
            contextActivity.moveTaskToBack(true)
        else
            startTransaction(TransitionTypes.EXIT).apply {
                val currentFrag = popCurrentFragment()
                val nextFragment = getCurrentFragment()
                if (currentFrag != null)
                    remove(currentFrag)
                show(nextFragment!!)
                commit()
                fragmentManager.executePendingTransactions()
            }
    }

    fun forward(
        newScreen: FragmentScreen,
        transition: TransitionTypes,
        backStackEntryName: String,
    ) {
        fragmentManager.fragments

        //val currentFragment = fragmentManager.findFragmentById(fragmentContainerId)
        val requestedFragment = newScreen.getFragment()
        //if (currentFragment != null && currentFragment.javaClass == requestedFragment.javaClass)
        //    return
        //fragmentManager.popBackStack(backStackEntryName,FragmentManager.POP_BACK_STACK_INCLUSIVE)
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
        fragmentManager.executePendingTransactions()
    }

    fun replaceTop(
        newScreen: FragmentScreen,
        animate: Boolean = true
    ) {
        val requestedFragment = newScreen.getFragment()
        startTransaction(null).apply {
            val backStackName = UUID.randomUUID().toString()
            val currentFrag = popCurrentFragment()
            runOnCommit {
                backStack.add(backStackName)
            }
            setCustomAnimations(
                0,
                0,
                R.anim.enter_from_left,
                R.anim.exit_to_right
            )
            remove(currentFrag!!)
            add(R.id.fragmentContainer, requestedFragment, backStackName)
            commit()
        }
        fragmentManager.executePendingTransactions()
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
                backStack.add(backStackName)
                commit()
            }
        }
    }

    fun newRootScreen(newRootScreen: FragmentScreen) {
        newRootChain(newRootScreen)
    }

    fun customAction(action: CustomRouterAction) {
        action.execute(fragmentContainerId, fragmentManager)
        fragmentManager.executePendingTransactions()
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
        fun execute(fragmentContainerId: Int, fragmentManager: FragmentManager)
    }

    abstract class FragmentScreen {
        abstract fun getFragment(): Fragment
    }
}