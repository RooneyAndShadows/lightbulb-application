package com.github.rooneyandshadows.lightbulb.application.activity.service.notification

import android.app.Notification
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

abstract class NotificationClient {
    val onNotificationReceived: MutableList<((notification: Notification) -> Unit)> =
        mutableListOf()
    val onNotificationsInvalidated: MutableList<(() -> Unit)> = mutableListOf()
    val onClose: MutableList<(() -> Unit)> = mutableListOf()

    abstract fun initialize()
    abstract fun start()
    abstract fun stop()

    protected fun isInternetAvailable(connectivityManager: ConnectivityManager): Boolean {
        val result: Boolean
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        result = when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        return result
    }
}