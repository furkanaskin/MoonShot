package com.haroldadmin.moonshotRepository.rocket

import com.haroldadmin.cnradapter.executeWithRetry
import com.haroldadmin.moonshot.core.Resource
import com.haroldadmin.moonshot.database.rocket.RocketsDao
import com.haroldadmin.moonshot.models.rocket.RocketMinimal
import com.haroldadmin.moonshotRepository.networkBoundFlow
import com.haroldadmin.spacex_api_wrapper.rocket.RocketsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GetRocketDetailsUseCase(rocketsDao: RocketsDao, rocketsService: RocketsService) :
    RocketsUseCase(rocketsDao, rocketsService) {

    private suspend fun getCached(rocketId: String) = withContext(Dispatchers.IO) {
        rocketsDao.getRocket(rocketId)
    }

    private suspend fun getFromApi(rocketId: String) = withContext(Dispatchers.IO) {
        executeWithRetry {
            rocketsService.getRocket(rocketId).await()
        }
    }

    suspend fun getRocketDetails(rocketId: String): Flow<Resource<RocketMinimal>> {
        return networkBoundFlow(
            dbFetcher = { getCached(rocketId) },
            cacheValidator = { cached -> cached != null },
            apiFetcher = { getFromApi(rocketId) },
            dataPersister = this::persistApiRocket
        )
    }
}