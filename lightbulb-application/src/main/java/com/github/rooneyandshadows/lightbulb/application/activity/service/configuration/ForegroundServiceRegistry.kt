package com.github.rooneyandshadows.lightbulb.application.activity.service.configuration

import com.github.rooneyandshadows.lightbulb.application.activity.service.foreground.ForegroundService

class ForegroundServiceRegistry(
    val foregroundServiceClass: Class<out ForegroundService>,
    val configuration: Configuration
) : BaseServiceRegistry<ForegroundService>(
    foregroundServiceClass
) {
    class Configuration(
        val notificationChannelId: String,
        val notificationChannelName: String? = null,
        val notificationChannelDescription: String? = null
    )
}