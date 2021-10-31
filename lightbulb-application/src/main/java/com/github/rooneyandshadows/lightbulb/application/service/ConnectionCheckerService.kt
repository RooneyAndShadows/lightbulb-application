package com.github.rooneyandshadows.lightbulb.application.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import java.lang.Exception

class ConnectionCheckerService : Service() {
    private var isRunning = false
    private var connectionManager: ConnectivityManager? = null

    override fun onCreate() {
        super.onCreate()
        connectionManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (isRunning) return START_NOT_STICKY
        startConnectionCheckerThread()
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    private fun startConnectionCheckerThread() {
        val checkerThread: Thread = object : Thread() {
            override fun run() {
                super.run()
                try {
                    synchronized(this) {
                        while (true) {
                            sleep(2000)
                            when {
                                !isInternetAvailable() -> sendBroadcast(Intent(BuildConfig.internetConnectionBroadcasterActionDisabled))
                                else -> sendBroadcast(Intent(BuildConfig.internetConnectionBroadcasterActionEnabled))
                            }
                        }
                    }
                } catch (e: Exception) {
                    //ignored
                }
            }
        }
        checkerThread.start()
    }

    private fun isInternetAvailable(): Boolean {
        val result: Boolean
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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