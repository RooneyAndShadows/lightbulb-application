package com.github.rooneyandshadows.lightbulb.application.activity.service.foreground

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import java.lang.Exception
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.os.Binder
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat.Builder
import com.github.rooneyandshadows.java.commons.string.StringUtils
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_DESCRIPTION_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_ID_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_NAME_KEY


/*
Add below code in manifest
 <service android:name="classpath of service"
          android:enabled="true"
          android:exported="true" />
 */
abstract class ForegroundService : Service() {
    private var isRunning = false
    private lateinit var workThread: WorkThread
    private lateinit var notification: Notification
    private lateinit var configuration: ForegroundServiceConfiguration
    private var regularView: RemoteViews? = null
    private var expandedView: RemoteViews? = null
    private val binder: IBinder = ForegroundServiceBinder()
    private lateinit var channelId: String
    private lateinit var channelName: String
    private lateinit var channelDescription: String

    inner class ForegroundServiceBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): ForegroundService = this@ForegroundService
    }

    abstract fun configure(): ForegroundServiceConfiguration

    override fun onBind(arg0: Intent): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        configuration = configure()
        workThread = WorkThread()
    }

    private fun initializeNotificationViews() {
        regularView = RemoteViews(
            packageName,
            configuration.regularNotificationLayout.getLayoutId()
        ).apply {
            configuration.regularNotificationLayout.onInflated(this)
        }
        if (configuration.expandedNotificationLayout != null) {
            expandedView = RemoteViews(
                packageName,
                configuration.expandedNotificationLayout!!.getLayoutId()
            ).apply {
                configuration.expandedNotificationLayout!!.onInflated(this)
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (isRunning)
            return START_NOT_STICKY
        readParameters(intent.extras)
        initializeNotificationViews()
        createNotificationChannel()
        createForegroundNotification()
        workThread.start()
        startForeground(1, notification)
        isRunning = true
        return START_NOT_STICKY
    }

    private fun readParameters(extras: Bundle?) {
        channelId = extras?.getString(NOTIFICATION_CHANNEL_ID_KEY)
            ?: NOTIFICATION_CHANNEL_ID_KEY
        val name = extras?.getString(NOTIFICATION_CHANNEL_NAME_KEY)
        val description = extras?.getString(NOTIFICATION_CHANNEL_DESCRIPTION_KEY)
        channelName = if (StringUtils.isNullOrEmptyString(name)) channelId
        else name!!
        channelDescription = if (StringUtils.isNullOrEmptyString(description)) channelId
        else description!!
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                channelId,
                channelName,
                importance
            )
            channel.description = channelDescription
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification() {
        val notificationIntent = Intent(this, this.javaClass)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, FLAG_IMMUTABLE)
        val notificationBuilder = Builder(this, channelId)
            .setContent(regularView)
            .setCustomContentView(regularView)
            //.setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSmallIcon(configuration.notificationIcon)
            .setContentIntent(pendingIntent)
        if (expandedView != null)
            notificationBuilder.setCustomBigContentView(expandedView)
        notification = notificationBuilder.build()
    }

    inner class WorkThread : Thread() {
        private var interrupt: Boolean = false

        fun stopThread() {
            interrupt = true
        }

        override fun run() {
            super.run()
            try {
                while (!interrupt) {
                    sleep(500)
                    configuration.serviceWork.doWork(regularView!!, expandedView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                //ignored
            }
        }
    }

    class ForegroundServiceConfiguration constructor(
        val notificationIcon: Int,
        val regularNotificationLayout: ForegroundServiceNotificationLayout,
        val expandedNotificationLayout: ForegroundServiceNotificationLayout?,
        val serviceWork: ForegroundServiceWork
    )

    interface ForegroundServiceNotificationLayout {
        fun getLayoutId(): Int
        fun onInflated(view: RemoteViews)
    }

    interface ForegroundServiceWork {
        fun doWork(regularView: RemoteViews, expandedView: RemoteViews?)
    }
}