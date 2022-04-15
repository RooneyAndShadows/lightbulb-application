package com.github.rooneyandshadows.lightbulb.application.activity.configuration

import com.github.rooneyandshadows.lightbulb.application.activity.service.BaseNotificationJobService

class StompNotificationServiceRegistry(
    val notificationServiceClass: Class<out BaseNotificationJobService>,
    val notificationChannelName: String
)