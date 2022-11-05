package com.github.rooneyandshadows.lightbulb.application.fragment.annotations

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.CLASS

@Retention(SOURCE)
@Target(CLASS)
annotation class FragmentConfiguration(
    val isMainScreenFragment: Boolean = true,
    val hasLeftDrawer: Boolean = false,
    val hasOptionsMenu: Boolean = false
)
