package com.github.rooneyandshadows.lightbulb.application.activity.service.connection

import android.content.*
import android.os.IBinder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.activity.receivers.InternetConnectionStatusBroadcastReceiver
import com.github.rooneyandshadows.lightbulb.application.activity.service.connection.ConnectionCheckerService.ConnectionCheckerServiceBinder

class ConnectionCheckerServiceWrapper(
    private val activity: BaseActivity,
    private val internetConnectionStateListener: InternetConnectionStateListeners
) : DefaultLifecycleObserver {
    private var isBound = false
    private var boundService: ConnectionCheckerService? = null
    private var broadcastReceiver: InternetConnectionStatusBroadcastReceiver? = null
    private val connectionCheckerServiceConnection: ServiceConnection = object : ServiceConnection {
        @Override
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ConnectionCheckerServiceBinder
            this@ConnectionCheckerServiceWrapper.boundService = binder.getService()
            isBound = true
        }

        @Override
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    @Override
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        activity.unregisterReceiver(broadcastReceiver)
    }

    @Override
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        stopService()
    }

    @Override
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        startService()
    }

    private fun startService() {
        broadcastReceiver = InternetConnectionStatusBroadcastReceiver()
        broadcastReceiver!!.onInternetConnectionStatusReceived = { internetAvailable ->
            internetConnectionStateListener.onStateReceived(internetAvailable)
        }
        activity.registerReceiver(
            broadcastReceiver,
            IntentFilter(BuildConfig.internetConnectionStatusAction)
        )
        val intent = Intent(activity, ConnectionCheckerService::class.java)
        activity.startService(intent)
        activity.bindService(intent, connectionCheckerServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopService() {
        activity.unbindService(connectionCheckerServiceConnection)
        activity.stopService(Intent(activity, ConnectionCheckerService::class.java))
    }

    interface InternetConnectionStateListeners {
        fun onStateReceived(isInternetAvailable: Boolean)
    }
}