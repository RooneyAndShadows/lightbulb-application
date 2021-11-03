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
import androidx.work.Configuration


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
    private var startTime: Long = 0
    private var params: JobParameters? = null
    private var isConnected = false
    private lateinit var configuration: StompNotificationJobServiceConfiguration
    private var stompClient: StompClient? = null

    init {
        val builder = Configuration.Builder()
        builder.setJobSchedulerJobIdRange(0, 1000)
    }

    abstract fun configure(): StompNotificationJobServiceConfiguration

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        params = jobParameters
        startTime = System.currentTimeMillis()
        stompClient = initializeStompClient()
        startStompThread()
        return true
    }

    override fun onStopJob(jobParameters: JobParameters?): Boolean {
        return true
    }

    override fun onCreate() {
        super.onCreate()
        configuration = configure()
    }

    private fun startStompThread() {
        val stompThread: Thread = object : Thread() {
            private var initialConnection = true

            override fun run() {
                try {
                    while (true) {
                        if (!isConnected && configuration.listenUntilCondition()) {
                            if (initialConnection) {
                                stompClient!!.connectBlocking()
                                initialConnection = false
                            } else {
                                stompClient!!.reconnectBlocking()
                            }
                            if (!isConnected) {
                                sleep(5000)
                                continue
                            }
                        }
                        if ((System.currentTimeMillis() - startTime) > maxExecutionTime) {
                            if (isConnected)
                                stopListenForNotifications(false)
                            break
                        }
                        if (isConnected && !configuration.listenUntilCondition())
                            stopListenForNotifications(true)
                        sleep(5000)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    jobFinished(params, true)
                }
            }
        }
        stompThread.start()
    }

    private fun initializeStompClient(): StompClient {
        val stompClient = object :
            StompClient(URI.create(configuration.stompApiUrl), Draft_6455(), null, 3000) {
            override fun onError(ex: Exception) {
                Log.d(
                    this.javaClass.simpleName,
                    "Failed to connect notification service. Retrying..."
                )
            }
        }
        stompClient.setStompConnectionListener(object : StompConnectionListener() {
            override fun onConnecting() {
                super.onConnecting()
                isConnected = false
            }

            override fun onConnected() {
                super.onConnected()
                Log.d(this.javaClass.simpleName, "Notification service connected")
                isConnected = true
                if (!configuration.listenUntilCondition()) return
                stompClient.subscribe(configuration.stompSubscribeUrl) { stompFrame: StompFrame ->
                    showNotification(configuration.onFrameReceived(stompFrame))
                }
                val message = configuration.stompStartPayload?.configureMessage() ?: ""
                val headers: MutableMap<String, String> = HashMap()
                configuration.stompStartPayload?.configureHeaders(headers)
                stompClient.send(configuration.stompStartListenUrl, message, headers)
                configuration.onStartListenCallback()
            }

            override fun onDisconnected() {
                super.onDisconnected()
                isConnected = false
                Log.d(this.javaClass.simpleName, "Notification service disconnected")
            }
        })
        return stompClient
    }

    private fun stopListenForNotifications(clearShownNotifications: Boolean) {
        val message = configuration.stompStopPayload?.configureMessage() ?: ""
        val headers: MutableMap<String, String> = HashMap()
        configuration.stompStopPayload?.configureHeaders(headers);
        stompClient!!.send(configuration.stompStopListenUrl, message, headers)
        configuration.onStopListenCallback()
        stompClient!!.closeConnection(0, "")
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