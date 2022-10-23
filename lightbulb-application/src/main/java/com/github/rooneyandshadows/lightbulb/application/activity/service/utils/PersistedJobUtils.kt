package com.github.rooneyandshadows.lightbulb.application.activity.service.utils

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.Context
import android.os.PersistableBundle
import com.github.rooneyandshadows.java.commons.json.JsonUtils
import com.github.rooneyandshadows.lightbulb.application.activity.service.PERSISTED_SERVICE_JOBS
import com.github.rooneyandshadows.lightbulb.commons.utils.PreferenceUtils
import com.google.gson.reflect.TypeToken

class PersistedJobUtils {
    companion object {
        fun getPersistedJobs(context: Context): List<PersistedJobServiceInfo> {
            val persisted = PreferenceUtils.getString(context, PERSISTED_SERVICE_JOBS, "")
            if (persisted.isBlank())
                return mutableListOf()
            val type = object : TypeToken<MutableList<PersistedJobServiceInfo>>() {}
            return JsonUtils.fromJson(persisted, type.type)
        }

        fun addPersistedJob(
            context: Context,
            componentName: String,
            jobId: Int,
            extras: PersistableBundle
        ) {
            val persistedJobs = getPersistedJobs(context) as MutableList<PersistedJobServiceInfo>
            val foundJob = persistedJobs.stream()
                .filter { return@filter it.componentName == componentName }
                .findFirst()
                .orElse(null)
            if (foundJob != null)
                return
            persistedJobs.add(PersistedJobServiceInfo(componentName, jobId, extras))
            save(context, persistedJobs)
        }

        fun removePersistedJob(
            context: Context,
            componentName: String,
        ) {
            val persistedJobs = getPersistedJobs(context) as MutableList<PersistedJobServiceInfo>
            persistedJobs.removeIf { it.componentName == componentName }
            save(context, persistedJobs)
        }

        fun checkIfJobServiceScheduled(context: Context, jobId: Int): Boolean {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            var hasBeenScheduled = false
            for (jobInfo in scheduler.allPendingJobs) {
                if (jobInfo.id == jobId) {
                    hasBeenScheduled = true
                    break
                }
            }
            return hasBeenScheduled
        }

        private fun save(context: Context, jobs: List<PersistedJobServiceInfo>) {
            PreferenceUtils.saveString(
                context,
                PERSISTED_SERVICE_JOBS,
                JsonUtils.toJson(jobs)
            )
        }
    }

    class PersistedJobServiceInfo(
        val componentName: String,
        val jobId: Int,
        val extras: PersistableBundle
    )
}