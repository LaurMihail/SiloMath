package com.silomath.core.data.network.client

import com.silomath.core.data.network.api.CampaignApi
import com.silomath.core.data.network.config.ApiEnvironmentConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignApiFactory @Inject constructor(
    private val retrofitFactory: RetrofitFactory
) {

    fun create(
        environmentConfig: ApiEnvironmentConfig
    ): CampaignApi {
        return retrofitFactory
            .create(environmentConfig)
            .create(CampaignApi::class.java)
    }
}
