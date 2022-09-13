package com.github.rooneyandshadows.lightbulb.application.activity.routing

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.github.terrakok.cicerone.*
import com.github.terrakok.cicerone.androidx.AppNavigator
import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.github.rooneyandshadows.lightbulb.application.R
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.activity.routing.BaseApplicationRouter.NavigationCommands.*

@Suppress("CanBePrimaryConstructorProperty")
open class BaseApplicationRouter(contextActivity: BaseActivity, fragmentContainerId: Int) {
    protected val contextActivity: BaseActivity = contextActivity
    private val fragmentContainerId: Int = fragmentContainerId
    private var cicerone: Cicerone<Router> = Cicerone.create()
    private val router: Router
        get() = cicerone.router

    init {
        cicerone.getNavigatorHolder().setNavigator(setupNavigator(contextActivity))
    }

    protected fun navigate(command: NavigationCommands, screen: Screen) {
        when (command) {
            NAVIGATE_TO -> router.navigateTo(convertScreen(screen))
            NAVIGATE_TO_AND_CLEAR_BACKSTACK -> router.newRootChain(convertScreen(screen))
            REPLACE -> router.replaceScreen(convertScreen(screen))
            BACK_TO -> router.backTo(convertScreen(screen))
        }
    }

    fun newChain(vararg screens: Screen) {
        val chain = convertScreens(*screens)
        router.newRootChain(*chain.toTypedArray())
    }

    fun navigateBack() {
        router.exit()
    }

    fun removeNavigator() {
        cicerone.getNavigatorHolder().removeNavigator()
    }

    private fun convertScreen(screen: Screen): FragmentScreen {
        return object : FragmentScreen {
            override val screenKey: String
                get() = screen.getId()

            override fun createFragment(factory: FragmentFactory): Fragment {
                return screen.getFragment();
            }
        }
    }

    private fun convertScreens(vararg screens: Screen): ArrayList<FragmentScreen> {
        val converted = ArrayList<FragmentScreen>()
        screens.iterator().forEach { screen ->
            converted.add(convertScreen(screen))
        }
        return converted
    }

    private fun setupNavigator(contextActivity: BaseActivity): Navigator {
        return object : AppNavigator(contextActivity, fragmentContainerId) {
            override fun forward(command: Forward) {
                val fragmentManager = contextActivity.supportFragmentManager
                val screen = command.screen as FragmentScreen
                val currentFragment = fragmentManager.findFragmentById(fragmentContainerId)
                val requestedFragment = screen.createFragment(fragmentFactory)
                if (currentFragment != null && currentFragment.javaClass == requestedFragment.javaClass)
                    return
                val fragmentTransaction = fragmentManager.beginTransaction()
                setupFragmentTransaction(
                    screen,
                    fragmentTransaction,
                    currentFragment,
                    requestedFragment
                )
                fragmentTransaction
                    .addToBackStack(screen.screenKey)
                    .replace(fragmentContainerId, requestedFragment, screen.screenKey)
                    .commit()
            }

            override fun setupFragmentTransaction(
                screen: FragmentScreen,
                fragmentTransaction: FragmentTransaction,
                currentFragment: Fragment?,
                nextFragment: Fragment
            ) {
                super.setupFragmentTransaction(
                    screen,
                    fragmentTransaction,
                    currentFragment,
                    nextFragment
                )
                val existsInBackstack = fragmentManager.popBackStackImmediate(
                    screen.screenKey,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                if (existsInBackstack)
                    fragmentTransaction.setCustomAnimations(
                        R.anim.enter_from_left,
                        R.anim.exit_to_right,
                        R.anim.enter_from_right,
                        R.anim.exit_to_left
                    )
                else
                    fragmentTransaction.setCustomAnimations(
                        R.anim.enter_from_right,
                        R.anim.exit_to_left,
                        R.anim.enter_from_left,
                        R.anim.exit_to_right
                    )
            }

            override fun applyCommands(commands: Array<out Command>) {
                super.applyCommands(commands)
                contextActivity.supportFragmentManager.executePendingTransactions()
            }
        }
    }

    abstract class Screen() {
        internal fun getId(): String {
            return this::class.java.name
        }

        abstract fun getFragment(): Fragment
    }

    enum class NavigationCommands {
        NAVIGATE_TO,
        NAVIGATE_TO_AND_CLEAR_BACKSTACK,
        REPLACE,
        BACK_TO
    }
}