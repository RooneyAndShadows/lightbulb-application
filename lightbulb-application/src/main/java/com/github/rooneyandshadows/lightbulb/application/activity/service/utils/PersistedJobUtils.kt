package com.github.rooneyandshadows.lightbulb.application.activity.service.utils

import android.app.job.JobScheduler
import android.content.Context
import android.os.PersistableBundle
import com.github.rooneyandshadows.java.commons.json.JsonUtils
import com.github.rooneyandshadows.lightbulb.application.activity.service.PERSISTED_SERVICE_JOBS
import com.github.rooneyandshadows.lightbulb.commons.utils.PreferenceUtils

@Suppress("MemberVisibilityCanBePrivate")
class PersistedJobUtils {
    companion object {
        fun getPersistedJobs(context: Context): PersistedJobs {
            val persisted = PreferenceUtils.getString(context, PERSISTED_SERVICE_JOBS, "")
            if (persisted.isBlank())
                return PersistedJobs()
            return JsonUtils.fromJson(persisted, PersistedJobs::class.java)
        }

        fun addPersistedJob(
            context: Context,
            componentName: String,
            jobId: Int,
            extras: PersistableBundle
        ) {
            val persistedJobs = getPersistedJobs(context)
            persistedJobs.add(componentName, jobId, extras)
            save(context, persistedJobs)
        }

        fun removePersistedJob(
            context: Context,
            componentName: String,
        ) {
            val persistedJobs = getPersistedJobs(context)
            persistedJobs.remove(componentName)
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

        private fun save(context: Context, jobs: PersistedJobs) {
            PreferenceUtils.saveString(
                context,
                PERSISTED_SERVICE_JOBS,
                JsonUtils.toJson(jobs)
            )
        }
    }

    class PersistedJobs(val jobs: MutableList<PersistedJobServiceInfo> = mutableListOf()) {
        fun add(componentName: String, jobId: Int, extras: PersistableBundle) {
            val foundJob = jobs.stream()
                .filter { return@filter it.componentName == componentName }
                .findFirst()
                .orElse(null)
            if (foundJob != null)
                return
            jobs.add(PersistedJobServiceInfo(componentName, jobId, extras))
        }

        fun remove(componentName: String) {
            jobs.removeIf { it.componentName == componentName }
        }

    }

    class PersistedJobServiceInfo(
        val componentName: String,
        val jobId: Int,
        val extras: PersistableBundle
    )
}