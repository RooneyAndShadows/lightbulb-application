package com.github.rooneyandshadows.lightbulb.application.activity.configuration

import com.github.rooneyandshadows.lightbulb.application.activity.service.notification.BaseNotificationJobService

class NotificationServiceRegistry(
    val notificationServiceClass: Class<out BaseNotificationJobService>,
    val notificationChannelName: String,
    val notificationListeners: NotificationListeners?
) {

    interface NotificationListeners {
        fun onNotificationReceived()
    }
}