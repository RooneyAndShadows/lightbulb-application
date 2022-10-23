package com.github.rooneyandshadows.lightbulb.application.activity.service.configuration

import com.github.rooneyandshadows.lightbulb.application.activity.service.notification.BaseNotificationJobService

class NotificationServiceRegistry(
    val notificationServiceClass: Class<out BaseNotificationJobService>,
    val configuration: Configuration,
) : BaseServiceRegistry<BaseNotificationJobService>(
    notificationServiceClass
) {
    class Configuration(
        val jobServiceId: Int = 1,
        val startOnSystemBoot: Boolean = true,
        val notificationChannelId: String,
        val notificationChannelName: String? = null,
        val notificationChannelDescription: String? = null,
        val notificationListeners: NotificationListeners
    )

    interface NotificationListeners {
        fun onNotificationReceived()
    }
}