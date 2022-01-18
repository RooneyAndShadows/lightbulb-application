package com.github.rooneyandshadows.lightbulb.application.activity.service

import android.app.*
import android.os.Build
import android.util.Log
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.java.commons.stomp.StompClient
import com.github.rooneyandshadows.java.commons.stomp.frame.StompFrame
import com.github.rooneyandshadows.java.commons.stomp.listener.StompConnectionListener
import org.java_websocket.drafts.Draft_6455
import java.lang.Exception
import java.net.URI
import java.util.HashMap
import android.app.NotificationManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Configuration
import com.github.rooneyandshadows.java.commons.stomp.StompSubscription


/*
Add below code in manifest
<service
    android:name="{classpath of your implementation}"
    android:exported="true"
    android:permission="android.permission.BIND_JOB_SERVICE"
    android:stopWithTask="false">
    <!--android:process=":notificationServiceProcess"-->
    <intent-filter>
        <action android:name="com.opasnite.pfm.android.service.NotificationService" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</service>
*/

abstract class StompNotificationJobService() : JobService() {
    private val maxExecutionTime = 15 * 60 * 1000
    private var jobStartTime: Long = 0
    private var jobParameters: JobParameters? = null
    private var isConnected = false
    private var jobStompSubscription: StompSubscription? = null
    private lateinit var configuration: StompNotificationJobServiceConfiguration
    private lateinit var jobStompThread: StompThread
    private lateinit var jobStompClient: StompClient

    init {
        val builder = Configuration.Builder()
        builder.setJobSchedulerJobIdRange(0, 1000)
    }

    abstract fun configure(): StompNotificationJobServiceConfiguration

    override fun onCreate() {
        super.onCreate()
        configuration = configure()
    }

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        this.jobParameters = jobParameters
        this.jobStartTime = System.currentTimeMillis()
        this.jobStompClient = initializeStompClient()
        this.jobStompThread = StompThread()
        this.jobStompThread.start()
        return true
    }

    override fun onStopJob(jobParameters: JobParameters?): Boolean {
        jobStompThread.stopThread()
        return true
    }

    private fun initializeStompClient(): StompClient {
        return object :
            StompClient(URI.create(configuration.stompApiUrl), Draft_6455(), null, 3000) {
            override fun onError(ex: Exception) {
                Log.d(
                    this.javaClass.simpleName,
                    "Failed to connect notification service. Retrying..."
                )
            }
        }.apply {
            isReuseAddr = true
        }.apply {
            setStompConnectionListener(object : StompConnectionListener() {
                override fun onConnecting() {
                    super.onConnecting()
                    isConnected = false
                }

                override fun onConnected() {
                    super.onConnected()
                    Log.d(this.javaClass.simpleName, "Notification service connected")
                    isConnected = true
                    if (!configuration.listenUntilCondition()) return
                    if (jobStompSubscription != null)
                        jobStompClient.removeSubscription(jobStompSubscription)
                    jobStompSubscription =
                        subscribe(configuration.stompSubscribeUrl) { stompFrame: StompFrame ->
                            showNotification(configuration.onFrameReceived(stompFrame))
                        }
                    val message = configuration.stompStartPayload?.configureMessage() ?: ""
                    val headers: MutableMap<String, String> = HashMap()
                    configuration.stompStartPayload?.configureHeaders(headers)
                    send(configuration.stompStartListenUrl, message, headers)
                    configuration.onStartListenCallback()
                }

                override fun onDisconnected() {
                    super.onDisconnected()
                    isConnected = false
                    Log.d(this.javaClass.simpleName, "Notification service disconnected")
                }
            })
        }
    }

    private fun stopListenForNotifications(clearShownNotifications: Boolean) {
        val message = configuration.stompStopPayload?.configureMessage() ?: ""
        val headers: MutableMap<String, String> = HashMap()
        configuration.stompStopPayload?.configureHeaders(headers)
        jobStompClient.send(configuration.stompStopListenUrl, message, headers)
        configuration.onStopListenCallback()
        if (jobStompSubscription != null)
            jobStompClient.removeSubscription(jobStompSubscription)
        jobStompClient.closeConnection(0, "")
        if (clearShownNotifications)
            cancelAllNotifications()
    }

    private fun cancelAllNotifications() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    private fun showNotification(notificationToShow: Notification) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(
                BuildConfig.notificationChannelId,
                configuration.notificationChannelName,
                importance
            )
            notificationManager.createNotificationChannel(mChannel)
            notificationManager.notify(
                notificationToShow.hashCode(),
                notificationToShow
            ) // 0 is the request code, it should be unique id
        } else {
            notificationManager.notify(notificationToShow.hashCode(), notificationToShow)
        }
    }

    inner class StompThread : Thread() {
        private var initialConnection = true
        private var interrupt = false

        fun stopThread() {
            interrupt = true
        }

        override fun run() {
            try {
                while (!interrupt) {
                    if (!isInternetAvailable())
                        jobStompClient.closeConnection(0, "")
                    if (!isConnected && configuration.listenUntilCondition()) {
                        if (initialConnection) {
                            jobStompClient.connectBlocking()
                            initialConnection = false
                        } else {
                            jobStompClient.reconnectBlocking()
                        }
                        if (!isConnected) {
                            sleep(5000)
                            continue
                        }
                    }
                    if ((System.currentTimeMillis() - jobStartTime) > maxExecutionTime) {
                        if (isConnected)
                            stopListenForNotifications(false)
                        break
                    }
                    if (isConnected && !configuration.listenUntilCondition())
                        stopListenForNotifications(true)
                    sleep(5000)
                }
                if (interrupt && isConnected)
                    stopListenForNotifications(false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                jobFinished(jobParameters, true)
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

    abstract class StompNotificationJobServiceConfiguration constructor(
        val notificationChannelName: String,
        val stompApiUrl: String,
        val stompStartListenUrl: String,
        val stompStopListenUrl: String,
        val stompSubscribeUrl: String
    ) {
        var stompStartPayload: StompPayload? = null
        var stompStopPayload: StompPayload? = null
        var onStartListenCallback: (() -> Unit) = {}
        var onStopListenCallback: (() -> Unit) = {}
        var listenUntilCondition: (() -> Boolean) = { true }

        abstract fun onFrameReceived(receivedFrame: StompFrame): Notification

        fun withOnStartListeningCallback(onStartListeningCallback: (() -> Unit)): StompNotificationJobServiceConfiguration {
            return apply {
                this.onStartListenCallback = onStartListeningCallback
            }
        }

        fun withOnStopListeningCallback(onStopListeningCallback: (() -> Unit)): StompNotificationJobServiceConfiguration {
            return apply {
                this.onStopListenCallback = onStopListeningCallback
            }
        }

        fun withStartPayload(startPayload: StompPayload): StompNotificationJobServiceConfiguration {
            return apply {
                this.stompStartPayload = startPayload
            }
        }

        fun withStopPayload(stopPayload: StompPayload): StompNotificationJobServiceConfiguration {
            return apply {
                this.stompStopPayload = stopPayload
            }
        }

        fun withListenUntilCondition(condition: (() -> Boolean)): StompNotificationJobServiceConfiguration {
            return apply {
                this.listenUntilCondition = condition
            }
        }

        abstract class StompPayload {

            open fun configureHeaders(stompHeaders: Map<String, String>) {
            }

            open fun configureMessage(): String {
                return ""
            }
        }
    }
}