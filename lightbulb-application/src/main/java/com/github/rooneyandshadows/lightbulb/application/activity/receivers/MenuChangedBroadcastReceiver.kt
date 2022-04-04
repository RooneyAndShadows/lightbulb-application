package com.github.rooneyandshadows.lightbulb.application.activity.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.rooneyandshadows.lightbulb.application.BuildConfig

class MenuChangedBroadcastReceiver : BroadcastReceiver() {
    var onMenuConfigurationChanged: ((currentActivity: String?) -> Unit)? = null

    @Override
    override fun onReceive(context: Context, intent: Intent) {
        val receivedString = intent.action
        val targetActivity = intent.extras?.getString("TARGET_ACTIVITY");
        if (receivedString == BuildConfig.menuConfigChangedAction)
            onMenuConfigurationChanged?.invoke(targetActivity)
    }
}