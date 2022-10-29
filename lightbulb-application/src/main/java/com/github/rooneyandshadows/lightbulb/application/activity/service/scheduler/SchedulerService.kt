package com.github.rooneyandshadows.lightbulb.application.activity.service.scheduler

import android.app.job.JobService
import android.app.job.JobParameters
import androidx.work.Configuration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SchedulerService : JobService() {

    init {
        val builder = Configuration.Builder()
        builder.setJobSchedulerJobIdRange(0, 1000)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.scheduleAtFixedRate({

        }, 0, 1000, TimeUnit.MILLISECONDS)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }
}