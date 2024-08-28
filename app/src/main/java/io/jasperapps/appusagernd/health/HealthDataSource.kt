package io.jasperapps.appusagernd.health

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.jasperapps.appusagernd.utils.Constants
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

interface HealthDatasource {
    suspend fun readStepsByRange(startTime: LocalDateTime, endTime: LocalDateTime): Long?
    suspend fun readHeartRate(startTime: LocalDateTime, endTime: LocalDateTime): Map<String, Long?>
    suspend fun readDistanceTotal(startTime: LocalDateTime, endTime: LocalDateTime): Long
    suspend fun insertSteps(startTime: Instant, endTime: Instant)
}

class HealthDatasourceImpl(private val healthConnectClient: HealthConnectClient) :
    HealthDatasource {

        // 발걸음 데이터 GET
    override suspend fun readStepsByRange(startTime: LocalDateTime, endTime: LocalDateTime): Long? {
        try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            // The result may be null if no data is available in the time range
            return response[StepsRecord.COUNT_TOTAL]
        } catch (e: Exception) {
            Log.e("Error", "Unable to read steps")
        }
        return 0
    }

    // 심장 박동수 데이터 GET
    override suspend fun readHeartRate(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Map<String, Long?> {
        val heartRate = HashMap<String, Long?>()
        try {
            val response =
                healthConnectClient.aggregate(
                    AggregateRequest(
                        setOf(HeartRateRecord.BPM_MAX, HeartRateRecord.BPM_MIN),
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )

            // The result may be null if no data is available in the time range
            heartRate[Constants.MAX_HEART_RATE] = response[HeartRateRecord.BPM_MAX]
            heartRate[Constants.MIN_HEART_RATE] = response[HeartRateRecord.BPM_MIN]
        } catch (e: Exception) {
            Log.e("Error", "Unable to read heart rate")
        }
        return heartRate
    }

    // 총 이동 거리 데이터 GET
    override suspend fun readDistanceTotal(startTime: LocalDateTime, endTime: LocalDateTime): Long {
        try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            // The result may be null if no data is available in the time range
            return response[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.toLong() ?: 0L
        } catch (e: Exception) {
            Log.e("Error", "Unable to read distance")
        }
        return 0
    }

    // 발걸음 데이터 추가
    override suspend fun insertSteps(startTime: Instant, endTime: Instant) {
        try {
            val stepsRecord = StepsRecord(
                count = 120,
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC
            )
            healthConnectClient.insertRecords(listOf(stepsRecord))
        } catch (e: Exception) {
            Log.e("Error", "ErrorStep")
        }
    }
}
