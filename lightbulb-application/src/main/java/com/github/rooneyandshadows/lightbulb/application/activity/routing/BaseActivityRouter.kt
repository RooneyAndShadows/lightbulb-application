package com.github.rooneyandshadows.lightbulb.application.activity.routing

import android.content.ContentValues
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.github.rooneyandshadows.lightbulb.application.R
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import java.util.*

@Suppress("CanBePrimaryConstructorProperty")
open class BaseActivityRouter(contextActivity: BaseActivity, fragmentContainerId: Int) {
    protected val contextActivity: BaseActivity = contextActivity
    private val fragmentContainerId: Int = fragmentContainerId
    private val fragmentManager: FragmentManager
    private val logTag: String

    init {
        fragmentManager = contextActivity.supportFragmentManager
        logTag = "[".plus(javaClass.simpleName).plus("]")
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
        val currentFragment = fragmentManager.findFragmentById(fragmentContainerId)
        val requestedFragment = newScreen.getFragment()
        //if (currentFragment != null && currentFragment.javaClass == requestedFragment.javaClass)
        //    return
        startTransaction().apply {
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
            add(fragmentContainerId, requestedFragment, UUID.randomUUID().toString())
            if (currentFragment != null)
                hide(currentFragment)
            addToBackStack(backStackEntryName)
            commit()
        }
        fragmentManager.executePendingTransactions()
    }

    fun replace(newScreen: FragmentScreen) {
        replace(newScreen, TransitionTypes.ENTER)
    }

    fun replace(newScreen: FragmentScreen, transition: TransitionTypes) {
        //val currentFragment = fragmentManager.findFragmentById(fragmentContainerId)
        val requestedFragment = newScreen.getFragment()
        //if (currentFragment != null && currentFragment.javaClass == requestedFragment.javaClass)
        //    return
        startTransaction().apply {
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
            replace(fragmentContainerId, requestedFragment, UUID.randomUUID().toString())
            commit()
        }
        fragmentManager.executePendingTransactions()
    }

    fun back() {
        fragmentManager.popBackStack()
    }

    fun back(steps: Int) {
        val entriesCount = fragmentManager.backStackEntryCount
        if (steps <= 0 || entriesCount < steps)
            return
        val entryName = fragmentManager.getBackStackEntryAt(entriesCount - steps).name
        fragmentManager.popBackStack(entryName, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    fun backToRoot() {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    fun newRootChain(vararg screens: FragmentScreen) {
        backToRoot()
        screens.forEachIndexed { index, fragmentScreen ->
            if (index == 0)
                replace(fragmentScreen)
            else
                forward(fragmentScreen)
        }
    }

    fun newRootScreen(newRootScreen: FragmentScreen) {
        backToRoot()
        replace(newRootScreen)
    }

    fun customAction(action: CustomRouterAction) {
        action.execute(fragmentManager)
        fragmentManager.executePendingTransactions()
    }

    private fun startTransaction(): FragmentTransaction {
        val transaction = fragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)
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
        fun execute(fragmentManager: FragmentManager)
    }

    abstract class FragmentScreen {
        abstract fun getFragment(): Fragment
    }
}