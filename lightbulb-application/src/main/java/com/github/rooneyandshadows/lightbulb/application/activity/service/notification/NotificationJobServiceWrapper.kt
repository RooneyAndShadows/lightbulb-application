package com.github.rooneyandshadows.lightbulb.application.activity.service.notification

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.os.PersistableBundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.activity.receivers.NotificationBroadcastReceiver
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_DESCRIPTION_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_ID_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.NOTIFICATION_CHANNEL_NAME_KEY
import com.github.rooneyandshadows.lightbulb.application.activity.service.configuration.NotificationServiceRegistry

class NotificationJobServiceWrapper(
    private val activity: BaseActivity,
    private val notificationServiceRegistry: NotificationServiceRegistry
) : DefaultLifecycleObserver {
    private val stompNotificationJobId = 1
    private lateinit var broadcastReceiver: NotificationBroadcastReceiver

    @Override
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        initializeNotificationService()
    }

    @Override
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        activity.unregisterReceiver(broadcastReceiver)
    }

    @Override
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
    }

    @Override
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
    }

    private fun initializeNotificationService() {
        val configuration = notificationServiceRegistry.configuration
        broadcastReceiver = NotificationBroadcastReceiver()
        broadcastReceiver.onNotificationReceived = {
            configuration.notificationListeners.onNotificationReceived()
        }
        activity.registerReceiver(
            broadcastReceiver,
            IntentFilter(BuildConfig.notificationReceivedAction)
        )
        scheduleTask(
            configuration.notificationChannelId,
            configuration.notificationChannelName,
            configuration.notificationChannelDescription
        )
    }

    private fun scheduleTask(
        notificationChannelId: String,
        notificationChannelName: String?,
        notificationChannelDescription: String?
    ) {
        val name = ComponentName(activity, notificationServiceRegistry.notificationServiceClass)
        val scheduler = activity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (!checkIfJobServiceScheduled()) {
            val jobInfoBundle = PersistableBundle().apply {
                putString(
                    NOTIFICATION_CHANNEL_ID_KEY,
                    notificationChannelId
                )
                putString(
                    NOTIFICATION_CHANNEL_NAME_KEY,
                    notificationChannelName
                )
                putString(
                    NOTIFICATION_CHANNEL_DESCRIPTION_KEY,
                    notificationChannelDescription
                )
            }
            val jobInfoBuilder = JobInfo.Builder(stompNotificationJobId, name)
                .setPeriodic(900000L)
                .setExtras(jobInfoBundle)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
            scheduler.schedule(jobInfoBuilder.build())
        }
    }

    private fun checkIfJobServiceScheduled(): Boolean {
        val scheduler = activity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        var hasBeenScheduled = false
        for (jobInfo in scheduler.allPendingJobs) {
            if (jobInfo.id == stompNotificationJobId) {
                hasBeenScheduled = true
                break
            }
        }
        return hasBeenScheduled
    }
}