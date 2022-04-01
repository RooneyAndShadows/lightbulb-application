package com.github.rooneyandshadows.lightbulb.application.activity.slidermenu

import android.graphics.Color
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
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.config.SliderMenuConfiguration
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.items.*
import com.github.rooneyandshadows.lightbulb.commons.utils.KeyboardUtils
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.ColorHolder
import com.mikepenz.materialdrawer.util.*

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class SliderMenu(
    private val contextActivity: BaseActivity,
    private val drawerLayout: DrawerLayout,
    private val sliderLayout: MaterialDrawerSliderView,
    private val sliderSavedState: Bundle?,
    private var sliderSettings: SliderMenuConfiguration?
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

    fun setItemTitle(id: Long, title: String) {
        sliderLayout.updateName(id, StringHolder(title))
    }

    fun setItemBadge(id: Long, badge: String) {
        sliderLayout.updateBadge(id, StringHolder(badge))
    }

    fun setItemIcon(id: Long, icon: Drawable) {
        sliderLayout.updateIcon(id, ImageHolder(icon))
    }

    fun openSlider() {
        drawerLayout.open()
    }

    fun closeSlider() {
        drawerLayout.close()
    }

    fun enableSlider() {
        isSliderEnabled = true
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    fun disableSlider() {
        isSliderEnabled = false
        drawerLayout.close()
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun isDrawerOpen(): Boolean {
        return drawerLayout.isDrawerOpen(GravityCompat.START)
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
                is PrimaryMenuItem -> result.add(
                    mapPrimaryItem(
                        itemToAdd,
                        sliderSettings!!.badgeTextColor,
                        sliderSettings!!.badgeCircleColor
                    )
                )
                is SecondaryMenuItem -> result.add(
                    mapSecondaryItem(
                        itemToAdd,
                        sliderSettings!!.badgeTextColor,
                        sliderSettings!!.badgeCircleColor
                    )
                )
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

    private fun mapPrimaryItem(
        item: PrimaryMenuItem,
        badgeTextColor: Int,
        badgeBackgroundColor: Int
    ): PrimaryDrawerItem {
        return PrimaryDrawerItem().apply {
            identifier = item.id
            icon = ImageHolder(item.drawable)
            name = StringHolder(item.title)
            badge = StringHolder(item.badge)
            badgeStyle = BadgeStyle().apply {
                textColor = ColorHolder.fromColor(badgeTextColor)
                color = ColorHolder.fromColor(badgeBackgroundColor)
            }
            level = item.level
            onDrawerItemClickListener = { view, drawerItem, position ->
                item.onClick?.invoke(this@SliderMenu)
                onSliderClosed = {
                }
                true
            }
        }
    }

    private fun mapSecondaryItem(
        item: SecondaryMenuItem,
        badgeTextColor: Int,
        badgeBackgroundColor: Int
    ): SecondaryDrawerItem {
        return SecondaryDrawerItem().apply {
            identifier = item.id
            icon = ImageHolder(item.drawable)
            name = StringHolder(item.title)
            badge = StringHolder(item.badge)
            badgeStyle = BadgeStyle().apply {
                textColor = ColorHolder.fromColor(badgeTextColor)
                color = ColorHolder.fromColor(badgeBackgroundColor)
            }
            level = item.level
            onDrawerItemClickListener = { view, drawerItem, position ->
                item.onClick?.invoke(this@SliderMenu)
                onSliderClosed = {
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
}