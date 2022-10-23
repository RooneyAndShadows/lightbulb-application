package com.github.rooneyandshadows.lightbulb.application.annotations

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

@Retention(RUNTIME)
@Target(CLASS)
annotation class BindLayout(val name: String)
