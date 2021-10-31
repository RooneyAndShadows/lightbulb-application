package com.github.rooneyandshadows.lightbulb.application.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.java.commons.stomp.StompClient
import com.github.rooneyandshadows.java.commons.stomp.frame.StompFrame
import com.github.rooneyandshadows.java.commons.stomp.listener.StompConnectionListener
import com.github.rooneyandshadows.lightbulb.application.R
import org.java_websocket.drafts.Draft_6455
import java.lang.Exception
import java.net.URI
import java.util.HashMap
import android.app.NotificationManager
import android.app.PendingIntent


/*
Add below code in manifest
<service
    android:name="{classpath of your implementation}"
    android:exported="true"
    android:stopWithTask="false">
    <!--android:process=":NotificationServiceProcess"-->
    <intent-filter>
        <action android:name="com.opasnite.pfm.android.service.NotificationService" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</service>
 */

private abstract class StompNotificationForegroundService : Service() {
    private var currentAuthCode: String? = null
    private var isRunning = false;
    private lateinit var notification: Notification
    private lateinit var configuration: StompNotificationServiceConfiguration

    abstract fun configure(): StompNotificationServiceConfiguration

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        configuration = configure()
        createForegroundNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (isRunning)
            return START_NOT_STICKY
        startForeground(1, notification)
        StartStompThread()
        isRunning = true;
        return START_STICKY
    }

    private fun createForegroundNotification() {
        val notificationIntent = Intent(this, StompNotificationForegroundService::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val v = RemoteViews(packageName, R.layout.hidden_notification)
        notification = NotificationCompat.Builder(
            this,
            BuildConfig.notificationChannelId
        )
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setSmallIcon(0)
            .setContent(v)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .build()
    }


    private fun StartStompThread() {
        val stompThread: Thread = object : Thread() {
            private lateinit var stompClient: StompClient
            private var connected = false
            private var connecting = false
            override fun run() {
                try {
                    initializeStompClient()
                    stompClient.connectBlocking()
                    while (true) {
                        sleep(5000)
                        if (connected && !configuration.listenUntil()) {
                            stopListenForNotifications()
                            continue
                        }
                        if (connected || !configuration.listenUntil())
                            continue
                        stompClient.reconnectBlocking()
                    }
                } catch (e: Exception) {
                    //ignored
                }
            }

            private fun stopListenForNotifications() {
                configuration.onStopListening();
                val message = configuration.stompStopListeners?.configureMessage() ?: ""
                val headers: MutableMap<String, String> = HashMap()
                configuration.stompStopListeners?.configureHeaders(headers);
                stompClient.send(configuration.stompStopListenUrl, message, headers)
                currentAuthCode = null
                connected = false
                connecting = false
                Log.i(this.javaClass.simpleName, "Notification service disconnected")
                cancelAll()
            }

            private fun cancelAll() {
                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancelAll()
            }

            private fun initializeStompClient() {
                stompClient = object :
                    StompClient(URI.create(configuration.stompApiUrl), Draft_6455(), null, 3000) {
                    override fun onError(ex: Exception) {
                        Log.w(
                            this.javaClass.simpleName,
                            "Failed to connect notification service. Retrying..."
                        )
                    }
                }
                stompClient.setStompConnectionListener(object : StompConnectionListener() {
                    override fun onConnecting() {
                        super.onConnecting()
                        connecting = true
                    }

                    override fun onConnected() {
                        super.onConnected()
                        Log.w(this.javaClass.simpleName, "Notification service connected")
                        connected = true
                        connecting = false
                        configuration.onStartListening();
                        stompClient.subscribe(configuration.stompSubscribeUrl) { stompFrame: StompFrame ->
                            showNotification(configuration.onNotificationFrameReceived(stompFrame))
                        }
                        val message = configuration.stompStartListeners?.configureMessage() ?: ""
                        val headers: MutableMap<String, String> = HashMap()
                        configuration.stompStartListeners?.configureHeaders(headers)
                        stompClient.send(configuration.stompStartListenUrl, message, headers)
                    }

                    override fun onDisconnected() {
                        super.onDisconnected()
                        connected = false
                        connecting = false
                    }
                })
            }
        }
        stompThread.start()
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

    abstract class StompNotificationServiceConfiguration constructor(
        val notificationChannelName: String,
        val stompApiUrl: String,
        val stompStartListenUrl: String,
        val stompStopListenUrl: String,
        val stompSubscribeUrl: String
    ) {
        var stompStartListeners: StompPayloadCallback? = null
        var stompStopListeners: StompPayloadCallback? = null

        fun withStartPayload(startListener: StompPayloadCallback): StompNotificationServiceConfiguration {
            return apply {
                this.stompStartListeners = startListener
            }
        }

        fun withStopPayload(stopListener: StompPayloadCallback): StompNotificationServiceConfiguration {
            return apply {
                this.stompStopListeners = stopListener
            }
        }

        abstract fun onStartListening()

        abstract fun onStopListening()

        abstract fun listenUntil(): Boolean

        abstract fun onNotificationFrameReceived(receivedFrame: StompFrame): Notification

        abstract class StompPayloadCallback {

            open fun configureHeaders(stompHeaders: Map<String, String>) {

            }

            open fun configureMessage(): String {
                return ""
            }
        }
    }
}