package io.jasperapps.appusagernd.database

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jasperapps.appusagernd.health.HealthDatasource
import io.jasperapps.appusagernd.model.UserInfo
import io.jasperapps.appusagernd.utils.Constants
import io.jasperapps.appusagernd.utils.Constants.HEALTH_PERMISSION
import io.jasperapps.appusagernd.worker.DataUploadWorker
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.Duration

// Hilt를 사용하여 ViewModel을 주입받는다는 것을 나타냅니다.
@HiltViewModel
class UserInfoViewModel @Inject constructor(
    private val context: Application, // 애플리케이션 컨텍스트
    private val healthDatasource: HealthDatasource, // HealthDatasource 객체
    private val healthConnectClient: HealthConnectClient // HealthConnectClient 객체
) : AndroidViewModel(context) { // AndroidViewModel을 상속받아 애플리케이션 컨텍스트를 사용할 수 있습니다.
    private val dataUploader = DataUploader(context)  // 데이터 업로드를 처리하는 객체를 초기화합니다.
    private val workManager = WorkManager.getInstance(context) // WorkManager 인스턴스를 가져옵니다.

    // 데이터베이스에 사용자 정보를 저장하는 함수입니다.
    fun saveToDatabase(userId: String, userInfo: UserInfo) {
        // UserId 및 UserInfo를 콘솔에 출력
        println("유저 등록 완료!")
        println("UserId: $userId")
        println("UserInfo: $userInfo \n")
        dataUploader.saveToDatabase(userId, userInfo)
    }

    // 특정 기간 동안의 걸음 수 데이터를 읽어오는 함수입니다.
    fun readSteps(startTime: LocalDateTime, endTime: LocalDateTime) {
        viewModelScope.launch {
            // HealthDatasource를 통해 걸음 수 데이터를 읽어옵니다.
            healthDatasource.readStepsByRange(startTime = startTime, endTime = endTime)
        }
    }

    // Health Connect 권한을 읽어오고, 필요한 경우 권한을 요청하는 함수입니다.
    fun readHealthPermission(requestPermissions: ActivityResultLauncher<Set<String>>) {
        viewModelScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            // 필요한 권한이 모두 부여되지 않았다면 권한 요청을 실행합니다.
            if (!granted.containsAll(HEALTH_PERMISSION)) {
                requestPermissions.launch(HEALTH_PERMISSION)
            }
        }
    }

    // 주기적으로 데이터를 전송하는 작업을 설정하는 함수입니다.
    fun setPeriodicallySendingData() {
        // 다음날 0시에 작업이 수행되도록 설정
        val delayTime = Duration(
            DateTime.now(),
            DateTime.now().withTimeAtStartOfDay().plusHours(Constants.SELF_REMINDER_HOUR)
        ).standardMinutes

        // 네트워크 연결 상태를 요구 조건으로 설정합니다.
        val constraintUpload = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 주기적으로 데이터를 업로드하는 작업을 설정합니다.
        val uploadDatabase = PeriodicWorkRequestBuilder<DataUploadWorker>(
            Constants.SELF_REMINDER_HOUR.toLong(),
            TimeUnit.HOURS
            // 10,
            // TimeUnit.SECONDS
        )
            .setConstraints(constraintUpload)
            .setInitialDelay(delayTime, TimeUnit.MINUTES)
            .addTag(Constants.SEND_REMIND_TAG)
            .build()

        // 고유한 주기적 작업을 WorkManager에 등록합니다.
        workManager.enqueueUniquePeriodicWork(
            Constants.SEND_REMIND_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            uploadDatabase
        )
    }

    // WorkManager의 모든 작업을 취소하는 함수입니다.
    fun stopWorkManager() {
        workManager.cancelAllWorkByTag(Constants.SEND_REMIND_TAG)
    }
}
