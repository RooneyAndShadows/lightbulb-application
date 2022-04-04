package com.github.rooneyandshadows.lightbulb.application.activity.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.rooneyandshadows.lightbulb.application.BuildConfig

class NotificationBroadcastReceiver : BroadcastReceiver() {
    var onNotificationReceived: (() -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        val receivedString = intent.action
        if (receivedString == BuildConfig.notificationReceivedAction)
            onNotificationReceived?.invoke()
    }
}