package com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.items

import android.graphics.drawable.Drawable

class ExpandableMenuItem(
    id: Long,
    title: String,
    drawable: Drawable?,
    level: Int = 1,
    vararg children: MenuItem
) : MenuItem(id, title, "", drawable, level) {

    val children: ArrayList<MenuItem> = ArrayList()

    init {
        this.children.addAll(children);
    }

    constructor(
        title: String,
        drawable: Drawable?,
        level: Int = 1,
        vararg children: MenuItem
    ) : this(-1, title, drawable, level, *children)

}