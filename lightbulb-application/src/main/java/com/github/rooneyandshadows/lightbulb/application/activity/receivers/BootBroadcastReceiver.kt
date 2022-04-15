package com.github.rooneyandshadows.lightbulb.application.activity.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.rooneyandshadows.lightbulb.application.activity.receivers.BootBroadcastReceiver

/*
add in manifest
<receiver
    android:name=".activity.receivers.BootBroadcastReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
 */
class BootBroadcastReceiver : BroadcastReceiver() {
    private val ACTION = "android.intent.action.BOOT_COMPLETED"
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION) {
        }
    }
}