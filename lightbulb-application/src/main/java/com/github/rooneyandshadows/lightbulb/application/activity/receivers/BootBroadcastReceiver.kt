package com.github.rooneyandshadows.lightbulb.application.activity.receivers

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import com.github.rooneyandshadows.lightbulb.application.activity.service.utils.PersistedJobUtils

/*
add in manifest
<receiver
    android:name=".activity.receivers.BootBroadcastReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.QUICKBOOT_POWERON" />
    </intent-filter>
</receiver>
 */
class BootBroadcastReceiver : BroadcastReceiver() {
    private val BOOT_ACTION = "android.intent.action.BOOT_COMPLETED"
    private val REBOOT_ACTION = "android.intent.action.QUICKBOOT_POWERON"
    private val logTag: String = "[".plus(javaClass.simpleName).plus("]")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BOOT_ACTION && intent.action != REBOOT_ACTION)
            return
        Log.i(logTag, "BOOT DETECTED")
        schedulePersistedJobs(context)
    }

    private fun schedulePersistedJobs(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        PersistedJobUtils.getPersistedJobs(context).jobs.forEach {
            val name = ComponentName(context, it.componentName)
            if (!PersistedJobUtils.checkIfJobServiceScheduled(context, it.jobId)) {
                val jobInfoBuilder = JobInfo.Builder(it.jobId, name)
                    .setPeriodic(900000L)
                    .setExtras(PersistableBundle().apply {
                        it.arguments.forEach { (key, value) ->
                            putString(key, value)
                        }
                    })
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                scheduler.schedule(jobInfoBuilder.build())
                Log.i(logTag, "Scheduled job service:".plus(it.componentName))
            } else {
                Log.i(
                    logTag,
                    "Job schedule ignored, because it's already scheduled:".plus(it.componentName)
                )
            }
        }
    }
}