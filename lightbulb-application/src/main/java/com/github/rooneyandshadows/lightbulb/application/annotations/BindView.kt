package com.github.rooneyandshadows.lightbulb.application.annotations

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

@Retention(RUNTIME)
@Target(FIELD)
annotation class BindView(val name: String)
