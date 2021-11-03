package com.github.rooneyandshadows.lightbulb.application.activity.broadcasters

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.activity.LightBulbActivity

public class InternetConnectionStateBroadcaster : BroadcastReceiver() {
    var contextActivity: LightBulbActivity? = null
    override fun onReceive(context: Context, intent: Intent) {
        if (contextActivity == null)
            return
        val actionEnabled = BuildConfig.internetConnectionBroadcasterActionEnabled;
        contextActivity?.onInternetConnectionStatusChanged(intent.action.equals(actionEnabled))
    }
}