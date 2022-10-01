package com.github.rooneyandshadows.lightbulb.application.activity.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.rooneyandshadows.lightbulb.application.BuildConfig
import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity
import com.github.rooneyandshadows.lightbulb.application.activity.configuration.NotificationServiceRegistry
import com.github.rooneyandshadows.lightbulb.application.activity.receivers.NotificationBroadcastReceiver

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
        broadcastReceiver = NotificationBroadcastReceiver()
        broadcastReceiver.onNotificationReceived = {
            notificationServiceRegistry.notificationListeners?.onNotificationReceived()
        }
        activity.registerReceiver(
            broadcastReceiver,
            IntentFilter(BuildConfig.notificationReceivedAction)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                BuildConfig.notificationChannelId,
                notificationServiceRegistry.notificationChannelName,
                importance
            )
            channel.setShowBadge(true)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = activity.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val name = ComponentName(activity, notificationServiceRegistry.notificationServiceClass)
        val scheduler = activity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (!checkIfJobServiceScheduled(stompNotificationJobId)) {
            val b = JobInfo.Builder(stompNotificationJobId, name)
                .setPeriodic(900000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
            scheduler.schedule(b.build())
        }
    }

    private fun checkIfJobServiceScheduled(jobServiceId: Int): Boolean {
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