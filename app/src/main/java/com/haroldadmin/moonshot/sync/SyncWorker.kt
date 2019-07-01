package com.haroldadmin.moonshot.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.haroldadmin.moonshot.core.Resource
import com.haroldadmin.moonshotRepository.launch.LaunchesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val launchesRepository: LaunchesRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val NAME = "sync-work"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        when (launchesRepository.syncLaunches()) {
            is Resource.Success -> Result.success()
            else -> Result.retry()
        }
    }
}
