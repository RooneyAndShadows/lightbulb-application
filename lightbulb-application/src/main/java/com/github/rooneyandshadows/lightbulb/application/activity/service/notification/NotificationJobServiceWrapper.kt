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
import com.github.rooneyandshadows.lightbulb.application.activity.service.*
import com.github.rooneyandshadows.lightbulb.application.activity.service.configuration.NotificationServiceRegistry
import com.github.rooneyandshadows.lightbulb.application.activity.service.configuration.NotificationServiceRegistry.Configuration
import com.github.rooneyandshadows.lightbulb.application.activity.service.utils.PersistedJobUtils

class NotificationJobServiceWrapper(
    private val activity: BaseActivity,
    private val notificationServiceRegistry: NotificationServiceRegistry
) : DefaultLifecycleObserver {
    private lateinit var configuration: Configuration
    private lateinit var broadcastReceiver: NotificationBroadcastReceiver

    @Override
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        configuration = notificationServiceRegistry.configuration
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
        val componentName = notificationServiceRegistry.notificationServiceClass.name
        val name = ComponentName(activity, componentName)
        val scheduler = activity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val args = mutableMapOf<String, String>().apply {
            put(NOTIFICATION_CHANNEL_ID_KEY, notificationChannelId)
            if (notificationChannelName != null)
                put(NOTIFICATION_CHANNEL_NAME_KEY, notificationChannelName)
            if (notificationChannelDescription != null)
                put(NOTIFICATION_CHANNEL_DESCRIPTION_KEY, notificationChannelDescription)
        }
        if (!PersistedJobUtils.checkIfJobServiceScheduled(
                activity,
                configuration.jobServiceId
            )
        ) {
            val jobInfoBundle = PersistableBundle().apply {
                args.forEach { (key, value) ->
                    putString(key, value)
                }
            }
            val jobInfoBuilder = JobInfo.Builder(configuration.jobServiceId, name)
                .setPeriodic(900000L)
                .setExtras(jobInfoBundle)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            if (configuration.startOnSystemBoot)
                jobInfoBuilder.setPersisted(true)
            scheduler.schedule(jobInfoBuilder.build().apply {
                PersistedJobUtils.apply {
                    if (configuration.startOnSystemBoot)
                        addPersistedJob(
                            activity,
                            componentName,
                            configuration.jobServiceId,
                            args
                        )
                    else {
                        removePersistedJob(
                            activity,
                            componentName
                        )
                    }
                }
            })
        }
    }
}