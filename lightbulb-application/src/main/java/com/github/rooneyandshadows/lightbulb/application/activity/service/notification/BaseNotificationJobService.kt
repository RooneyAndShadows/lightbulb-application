package com.github.rooneyandshadows.lightbulb.application.activity.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.os.Build
import androidx.work.Configuration
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.activity.service.notification.NotificationClient

abstract class BaseNotificationJobService : JobService() {
    private lateinit var notificationClient: NotificationClient
    private lateinit var jobParameters: JobParameters

    init {
        val builder = Configuration.Builder()
        builder.setJobSchedulerJobIdRange(0, 1000)
    }

    protected abstract fun buildNotificationClient(): NotificationClient

    protected abstract fun getNotificationChannelName(): String

    @Override
    override fun onCreate() {
        super.onCreate()
        notificationClient = buildNotificationClient()
        notificationClient.initialize()
        notificationClient.onNotificationReceived.add { notification ->
            showNotification(notification)
            sendBroadcast(Intent(BuildConfig.notificationReceivedAction))
        }
        notificationClient.onClose.add {
            jobFinished(jobParameters, false)
        }
        notificationClient.onNotificationsInvalidated.add {
            cancelAllNotifications()
        }
    }

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        this.jobParameters = jobParameters
        notificationClient.start()
        return false //reschedule
    }

    override fun onStopJob(jobParameters: JobParameters?): Boolean {
        notificationClient.stop()
        return false //reschedule
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
                getNotificationChannelName(),
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
}