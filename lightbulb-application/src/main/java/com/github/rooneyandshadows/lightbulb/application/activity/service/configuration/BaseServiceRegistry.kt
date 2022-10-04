package com.github.rooneyandshadows.lightbulb.application.activity.service.configuration

import android.app.Service

open class BaseServiceRegistry<T : Service>(
    val serviceClass: Class<out T>
)