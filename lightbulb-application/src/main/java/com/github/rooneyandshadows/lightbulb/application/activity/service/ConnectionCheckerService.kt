package com.github.rooneyandshadows.lightbulb.application.activity.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import java.lang.Exception
import android.os.Binder
import android.os.Bundle

class ConnectionCheckerService : Service() {
    private var connectionManager: ConnectivityManager? = null
    private lateinit var checkerThread: CheckerThread
    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): ConnectionCheckerService = this@ConnectionCheckerService
    }

    override fun onCreate() {
        super.onCreate()
        connectionManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        checkerThread = CheckerThread()
        checkerThread.start()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        checkerThread.stopThread()
    }

    override fun onBind(arg0: Intent): IBinder {
        return binder
    }

    inner class CheckerThread : Thread() {
        private var interrupt: Boolean = false

        fun stopThread() {
            interrupt = true
        }

        override fun run() {
            super.run()
            try {
                while (!interrupt) {
                    sleep(2000)
                    val intent = Intent(BuildConfig.internetConnectionStatusAction)
                    val extras = Bundle()
                    extras.putBoolean("IS_INTERNET_AVAILABLE", isInternetAvailable())
                    intent.putExtras(extras)
                    sendBroadcast(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                //ignored
            }
        }
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