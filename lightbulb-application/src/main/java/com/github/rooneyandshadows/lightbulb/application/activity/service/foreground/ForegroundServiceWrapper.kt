package com.github.rooneyandshadows.lightbulb.application.activity.service.foreground

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.activity.receivers.InternetConnectionStatusBroadcastReceiver
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_DESCRIPTION_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_ID_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_NAME_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.configuration.ForegroundServiceRegistry
import com.github.rooneyandshadows.lightbulb.application.activity.service.foreground.ForegroundService.ForegroundServiceBinder
import com.github.rooneyandshadows.lightbulb.application.activity.service.foreground.ForegroundService.ForegroundServiceConfiguration

class ForegroundServiceWrapper(
    private val activity: BaseActivity,
    val serviceRegistry: ForegroundServiceRegistry
) : DefaultLifecycleObserver {
    private var isBound = false
    private var boundService: ForegroundService? = null
    private val foregroundServiceConnection: ServiceConnection = object : ServiceConnection {
        @Override
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundServiceBinder
            this@ForegroundServiceWrapper.boundService = binder.getService()
            isBound = true
        }

        @Override
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    @Override
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        startService()
    }

    private fun startService() {
        val configuration = serviceRegistry.configuration
        val intent = Intent(activity, serviceRegistry.foregroundServiceClass).apply {
            putExtra(
                NOTIFICATION_CHANNEL_ID_KEY,
                configuration.notificationChannelId
            )
            putExtra(
                NOTIFICATION_CHANNEL_NAME_KEY,
                configuration.notificationChannelName
            )
            putExtra(
                NOTIFICATION_CHANNEL_DESCRIPTION_KEY,
                configuration.notificationChannelDescription
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(intent)
        } else {
            activity.startService(intent)
        }
        activity.bindService(intent, foregroundServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopService() {
        activity.unbindService(foregroundServiceConnection)
        activity.stopService(Intent(activity, serviceRegistry.foregroundServiceClass))
    }
}