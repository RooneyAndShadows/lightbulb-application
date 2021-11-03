package com.github.rooneyandshadows.lightbulb.application.activity.configuration

import com.github.rooneyandshadows.lightbulb.application.activity.service.StompNotificationJobService

class StompNotificationServiceRegistry(
    val notificationServiceClass: Class<out StompNotificationJobService>,
    val notificationChannelName: String
)