package io.jasperapps.appusagernd.module

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.jasperapps.appusagernd.health.HealthDatasource
import io.jasperapps.appusagernd.health.HealthDatasourceImpl

@InstallIn(SingletonComponent::class)
@Module
class HealthModule {

    // HealthClient 생성
    @Provides
    fun providesHealthClient(@ApplicationContext appContext: Context): HealthConnectClient {
        return HealthConnectClient.getOrCreate(context = appContext)
    }

    // Health 데이터 적용
    @Provides
    fun providesHealthDateSource(healthConnectClient: HealthConnectClient): HealthDatasource {
        return HealthDatasourceImpl(healthConnectClient = healthConnectClient)
    }
}
