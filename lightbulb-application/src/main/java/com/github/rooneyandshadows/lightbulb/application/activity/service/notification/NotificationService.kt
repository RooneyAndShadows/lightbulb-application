package com.github.rooneyandshadows.lightbulb.application.activity.service.notification

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotificationService(val startOnSystemBoot: Boolean)
