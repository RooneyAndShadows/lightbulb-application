package com.github.rooneyandshadows.lightbulb.application.activity.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.os.Build
import androidx.work.Configuration
import com.github.rooneyandshadows.java.commons.string.StringUtils
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_DESCRIPTION_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_ID_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_NAME_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.notification.client.BaseNotificationClient

/*
Add below code in manifest
<service
    android:name="{classpath of your implementation}"
    android:exported="true"
    android:label="@string/system_app_name_phrase"
    android:permission="android.permission.BIND_JOB_SERVICE"
    android:stopWithTask="false">
    <!--android:process=":notificationServiceProcess"-->
    <intent-filter>
        <action android:name="ServiceNotification" />

        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</service>
 */
abstract class BaseNotificationJobService : JobService() {
    private var notificationClient: BaseNotificationClient? = null
    private lateinit var jobParameters: JobParameters
    private lateinit var channelId: String
    private lateinit var channelName: String
    private lateinit var channelDescription: String

    init {
        val builder = Configuration.Builder()
        builder.setJobSchedulerJobIdRange(0, 1000)
    }

    protected abstract fun buildNotificationClient(notificationChannelId: String): BaseNotificationClient

    @Override
    override fun onCreate() {
        super.onCreate()

    }

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        this.jobParameters = jobParameters
        readParameters()
        setupNotificationChannel()
        initializeNotificationClient()
        notificationClient!!.start()
        return true
    }

    override fun onStopJob(jobParameters: JobParameters?): Boolean {
        notificationClient?.stop()
        return false //reschedule
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationClient?.stop()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        notificationClient?.stop()
    }

    private fun readParameters() {
        val extras = jobParameters.extras
        channelId = extras.getString(NOTIFICATION_CHANNEL_ID_KEY)
            ?: NOTIFICATION_CHANNEL_ID_KEY
        val name = extras.getString(NOTIFICATION_CHANNEL_NAME_KEY)
        val description = extras.getString(NOTIFICATION_CHANNEL_DESCRIPTION_KEY)
        channelName = if (StringUtils.isNullOrEmptyString(name)) channelId
        else name!!
        channelDescription = if (StringUtils.isNullOrEmptyString(description)) channelId
        else description!!
    }

    private fun initializeNotificationClient() {
        if (notificationClient != null)
            return
        notificationClient = buildNotificationClient(channelId).apply {
            initialize()
            onNotificationReceived.add { notification ->
                showNotification(notification)
                sendBroadcast(Intent(BuildConfig.notificationReceivedAction))
            }
            onClose.add {
                jobFinished(jobParameters, false)
            }
            onNotificationsInvalidated.add {
                cancelAllNotifications()
            }
        }
    }

    private fun cancelAllNotifications() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    private fun showNotification(notificationToShow: Notification) {
        setupNotificationChannel()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            notificationToShow.hashCode(),
            notificationToShow
        )
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                channelId,
                channelName,
                importance
            )
            channel.description = channelDescription
            channel.setShowBadge(true)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}