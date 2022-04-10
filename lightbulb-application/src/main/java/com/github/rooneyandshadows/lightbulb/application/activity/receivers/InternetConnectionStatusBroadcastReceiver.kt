package com.github.rooneyandshadows.lightbulb.application.activity.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.rooneyandshadows.lightbulb.application.BuildConfig

class InternetConnectionStatusBroadcastReceiver : BroadcastReceiver() {
    var onInternetConnectionStatusReceived: ((hasInternetEnabled: Boolean) -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        val receivedString = intent.action
        if (receivedString == BuildConfig.internetConnectionStatusAction) {
            val connectionAvailable = intent.extras?.getBoolean("IS_INTERNET_AVAILABLE") ?: false
            onInternetConnectionStatusReceived?.invoke(connectionAvailable)
        }
    }
}