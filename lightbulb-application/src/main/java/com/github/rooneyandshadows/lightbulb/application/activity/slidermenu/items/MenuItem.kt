package com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.items

import android.graphics.drawable.Drawable

abstract class MenuItem protected constructor(
    val id: Long,
    val title: String,
    val badge: String?,
    val drawable: Drawable?,
    val level: Int
) {
    constructor() : this(-1, "", null, null, 0)
}