package com.github.rooneyandshadows.lightbulb.application.activity.helpers

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.util.addItems
import com.mikepenz.materialdrawer.util.removeAllItems
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.commons.utils.KeyboardUtils

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class SliderHelper(
    private val contextActivity: BaseActivity,
    private val drawerLayout: DrawerLayout,
    private val sliderLayout: MaterialDrawerSliderView,
    private val sliderSavedState: Bundle?,
    private var sliderSettings: SliderSetup?
) {
    private val sliderStateKey = "SLIDER_STATE_KEY"
    private val drawerEnabledKey = "DRAWER_ENABLED_KEY"
    private var onSliderClosed: (() -> Unit)? = null
    var isSliderEnabled: Boolean = true
        private set


    init {
        setupDrawer()
        setupSlider()
    }

    fun isDrawerOpen(): Boolean {
        return drawerLayout.isDrawerOpen(GravityCompat.START)
    }

    fun reInitialize(settings: SliderSetup?) {
        sliderSettings = settings
        setupSlider()
    }

    fun disableSlider() {
        isSliderEnabled = false
        drawerLayout.close()
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun enableSlider() {
        isSliderEnabled = true
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    fun closeSlider() {
        drawerLayout.close()
    }

    fun openSlider() {
        drawerLayout.open()
    }

    private fun setupDrawer() {
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                KeyboardUtils.hideKeyboard(contextActivity)
            }

            override fun onDrawerOpened(drawerView: View) {
                KeyboardUtils.hideKeyboard(contextActivity)
            }

            override fun onDrawerClosed(drawerView: View) {
                KeyboardUtils.hideKeyboard(contextActivity)
                onSliderClosed?.invoke()
                onSliderClosed = null
            }

            override fun onDrawerStateChanged(newState: Int) {

            }
        })
    }

    private fun setupSlider() {
        val mappedItems = ArrayList<IDrawerItem<*>>(mapItems(sliderSettings?.itemsList))
        sliderLayout.removeAllItems()
        sliderLayout.addItems(*mappedItems.toTypedArray())
        sliderLayout.headerView = sliderSettings?.headerView
        if (sliderSavedState != null) {
            sliderLayout.setSavedInstance(sliderSavedState.getBundle(sliderStateKey))
            isSliderEnabled = sliderSavedState.getBoolean(drawerEnabledKey)
        }
    }

    private fun mapItems(items: ArrayList<MenuItem>?): MutableList<IDrawerItem<*>> {
        val result = ArrayList<IDrawerItem<*>>()
        if (items == null)
            return result
        for (itemToAdd in items) {
            when (itemToAdd) {
                is DividerMenuItem -> result.add(mapDividerItem(itemToAdd))
                is PrimaryMenuItem -> result.add(mapPrimaryItem(itemToAdd))
                is SecondaryMenuItem -> result.add(mapSecondaryItem(itemToAdd))
                is ExpandableMenuItem -> result.add(mapExpandableItem(itemToAdd))
            }
        }
        return result
    }

    private fun mapDividerItem(item: DividerMenuItem): DividerDrawerItem {
        return DividerDrawerItem().apply {
            identifier = item.hashCode().toLong()
        }
    }

    private fun mapPrimaryItem(item: PrimaryMenuItem): PrimaryDrawerItem {
        return PrimaryDrawerItem().apply {
            identifier = item.hashCode().toLong()
            icon = ImageHolder(item.drawable)
            name = StringHolder(item.title)
            level = item.level
            onDrawerItemClickListener = { view, drawerItem, position ->
                closeSlider()
                onSliderClosed = {
                    item.onClick?.invoke(this@SliderHelper)
                }
                true
            }
        }
    }

    private fun mapSecondaryItem(item: SecondaryMenuItem): SecondaryDrawerItem {
        return SecondaryDrawerItem().apply {
            identifier = item.hashCode().toLong()
            icon = ImageHolder(item.drawable)
            name = StringHolder(item.title)
            level = item.level
            onDrawerItemClickListener = { view, drawerItem, position ->
                closeSlider()
                onSliderClosed = {
                    item.onClick?.invoke(this@SliderHelper)
                }
                true
            }
        }
    }

    private fun mapExpandableItem(item: ExpandableMenuItem): ExpandableDrawerItem {
        return ExpandableDrawerItem().apply {
            identifier = item.hashCode().toLong()
            icon = ImageHolder(item.drawable)
            level = item.level
            name = StringHolder(item.title)
            subItems = mapItems(item.children).toMutableList()
        }
    }

    fun saveState(): Bundle {
        val stateBundle = Bundle()
        stateBundle.putBundle(sliderStateKey, Bundle())
        stateBundle.putBoolean(drawerEnabledKey, isSliderEnabled)
        return stateBundle
    }

    class SliderSetup {
        var headerView: View? = null
        val itemsList: ArrayList<MenuItem> = ArrayList()

        fun addMenuItem(item: MenuItem): SliderSetup {
            itemsList.add(item)
            return this
        }

        fun withHeaderView(headerView: View?): SliderSetup {
            this.headerView = headerView
            return this
        }
    }

    class DividerMenuItem() : MenuItem()

    class PrimaryMenuItem(
        title: String,
        drawable: Drawable?,
        level: Int = 1,
        val onClick: ((slider: SliderHelper) -> Unit)?
    ) :
        MenuItem(title, drawable, level)

    class SecondaryMenuItem(
        title: String,
        drawable: Drawable?,
        level: Int = 1,
        val onClick: ((slider: SliderHelper) -> Unit)?
    ) :
        MenuItem(title, drawable, level)

    class ExpandableMenuItem(
        title: String,
        drawable: Drawable?,
        level: Int = 1,
        vararg children: MenuItem
    ) :
        MenuItem(title, drawable, level) {
        val children: ArrayList<MenuItem> = ArrayList()

        init {
            this.children.addAll(children);
        }
    }

    abstract class MenuItem protected constructor(
        val title: String,
        val drawable: Drawable?,
        val level: Int
    ) {
        constructor() : this("", null, 0)
    }

    interface SliderConfiguration {
        fun configure(): SliderSetup
    }

}