package io.jasperapps.appusagernd.worker

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import io.jasperapps.appusagernd.database.DataUploader
import io.jasperapps.appusagernd.health.HealthDatasource
import io.jasperapps.appusagernd.health.HealthDatasourceImpl
import io.jasperapps.appusagernd.utils.Constants
import java.time.LocalDateTime

class DataUploadWorker(
    context: Context,
    parameters: WorkerParameters,
) :
    CoroutineWorker(context, parameters) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val dataUploader = DataUploader(applicationContext)
    private val healthDatasource: HealthDatasource =
        HealthDatasourceImpl(HealthConnectClient.getOrCreate(context))

    override suspend fun doWork(): Result {
        val result = auth.currentUser?.let { dataUploader.upload(it.uid) }
        val steps = healthDatasource.readStepsByRange(
            LocalDateTime.now().minusHours(Constants.SELF_REMINDER_HOUR.toLong()),
            LocalDateTime.now()
        )
        val heartRate = healthDatasource.readHeartRate(
            LocalDateTime.now().minusHours(Constants.SELF_REMINDER_HOUR.toLong()),
            LocalDateTime.now()
        )
        val distance = healthDatasource.readDistanceTotal(
            LocalDateTime.now().minusHours(Constants.SELF_REMINDER_HOUR.toLong()),
            LocalDateTime.now()
        )
        auth.currentUser?.let { dataUploader.saveHeartRate(it.uid, heartRate) }
        if (steps != null) {
            auth.currentUser?.let { dataUploader.saveSteps(it.uid, steps) }
        }
        auth.currentUser?.let { dataUploader.saveDistance(it.uid, distance) }
        return if (result == true) {
            Result.success()
        } else Result.failure()
    }
}
