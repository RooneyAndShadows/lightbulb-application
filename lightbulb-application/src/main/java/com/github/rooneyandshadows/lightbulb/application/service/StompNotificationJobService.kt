package com.github.rooneyandshadows.lightbulb.application.service

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
    private lateinit var configuration: StompNotificationJobServiceConfiguration

    init {
        val builder = Configuration.Builder()
        builder.setJobSchedulerJobIdRange(0, 1000)
    }

    abstract fun configure(): StompNotificationJobServiceConfiguration

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        params = jobParameters
        startTime = System.currentTimeMillis()
        startStompThread()
        return true
    }

    override fun onStopJob(jobParameters: JobParameters?): Boolean {
        return false
    }

    override fun onCreate() {
        super.onCreate()
        configuration = configure()
    }

    private fun startStompThread() {
        val stompThread: Thread = object : Thread() {
            private lateinit var stompClient: StompClient
            override fun run() {
                try {
                    initializeStompClient()
                    if (!establishConnection())
                        return
                    startListeningForNotifications()
                    waitForNotifications()
                } catch (e: Exception) {
                    //ignored
                } finally {
                    jobFinished(params, true)
                }
            }

            private fun waitForNotifications() {
                while (true) {
                    if ((System.currentTimeMillis() - startTime) > maxExecutionTime) {
                        stopListenForNotifications()
                        break
                    }
                    if (!configuration.listenUntilCondition()) {
                        stopListenForNotifications()
                        cancelAllNotifications()
                    }
                    sleep(5000)
                }
            }

            private fun establishConnection(): Boolean {
                stompClient.connectBlocking()
                sleep(1000)
                if (!stompClient.isStompConnected)
                    for (attempt in 1..10) {
                        stompClient.reconnectBlocking()
                        sleep(1000)
                        if (stompClient.isStompConnected)
                            break
                    }
                return stompClient.isStompConnected
            }

            private fun stopListenForNotifications() {
                val message = configuration.stompStopPayload?.configureMessage() ?: ""
                val headers: MutableMap<String, String> = HashMap()
                configuration.stompStopPayload?.configureHeaders(headers);
                stompClient.send(configuration.stompStopListenUrl, message, headers)
                configuration.onStopListenCallback();
                stompClient.closeConnection(0, "")
            }

            private fun startListeningForNotifications() {
                if (!configuration.listenUntilCondition()) return
                stompClient.subscribe(configuration.stompSubscribeUrl) { stompFrame: StompFrame ->
                    showNotification(configuration.onFrameReceived(stompFrame))
                }
                val message = configuration.stompStartPayload?.configureMessage() ?: ""
                val headers: MutableMap<String, String> = HashMap()
                configuration.stompStartPayload?.configureHeaders(headers)
                stompClient.send(configuration.stompStartListenUrl, message, headers)
            }

            private fun initializeStompClient() {
                stompClient = object :
                    StompClient(URI.create(configuration.stompApiUrl), Draft_6455(), null, 3000) {
                    override fun onError(ex: Exception) {
                        if (BuildConfig.DEBUG)
                            Log.d(
                                this.javaClass.simpleName,
                                "Failed to connect notification service. Retrying..."
                            )
                    }
                }
                stompClient.setStompConnectionListener(object : StompConnectionListener() {
                    override fun onConnecting() {
                        super.onConnecting()
                    }

                    override fun onConnected() {
                        super.onConnected()
                        if (BuildConfig.DEBUG)
                            Log.d(this.javaClass.simpleName, "Notification service connected")
                        configuration.onStartListenCallback();

                    }

                    override fun onDisconnected() {
                        super.onDisconnected()
                        if (BuildConfig.DEBUG)
                            Log.d(this.javaClass.simpleName, "Notification service disconnected")
                    }
                })
            }
        }
        stompThread.start()
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