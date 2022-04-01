package com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.items

import android.graphics.drawable.Drawable
import com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.SliderMenu

class SecondaryMenuItem(
    id: Long,
    title: String,
    badge: String?,
    drawable: Drawable?,
    level: Int = 1,
    val onClick: ((slider: SliderMenu) -> Unit)?
) : MenuItem(id, title, badge, drawable, level) {
    constructor(
        title: String,
        badge: String?,
        drawable: Drawable?,
        level: Int = 1,
        onClick: ((slider: SliderMenu) -> Unit)?
    ) : this(-1, title, badge, drawable, level, onClick)
}