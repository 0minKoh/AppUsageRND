package io.jasperapps.appusagernd

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AlertDialog
import android.provider.Settings
import android.net.Uri
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.health.connect.client.PermissionController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import io.jasperapps.appusagernd.database.DataUploader
import io.jasperapps.appusagernd.database.UserInfoViewModel
import io.jasperapps.appusagernd.helper.NotificationHelper
import io.jasperapps.appusagernd.utils.Constants.HEALTH_PERMISSION
import io.jasperapps.appusagernd.utils.Constants.TIMED_NOTIFICATION_CHANNEL_ID
import io.jasperapps.appusagernd.utils.HealthPermissionNotGranted
import io.jasperapps.appusagernd.utils.HealthUtils
import io.jasperapps.appusagernd.utils.MessageDialogShower
import io.jasperapps.appusagernd.utils.NotificationPermission
import io.jasperapps.appusagernd.utils.NotificationsEnabled
import io.jasperapps.appusagernd.utils.PermissionNotGrantedDialogShower
import io.jasperapps.appusagernd.utils.PreferenceProvider
import io.jasperapps.appusagernd.utils.ShowHealthClientNotAvailable
import io.jasperapps.appusagernd.utils.UsagePermission
import io.jasperapps.appusagernd.worker.NotificationService
import java.util.Locale

@AndroidEntryPoint
class MainFragment : Fragment() {
    private val args: MainFragmentArgs by navArgs() // Navigator
    private val userInfoViewModel: UserInfoViewModel by viewModels() // Model

    // 유틸리티 객체
    private lateinit var usagePermission: UsagePermission // 사용자 동의 여부
    private lateinit var isNotificationsEnabled: NotificationsEnabled // 알림 설정 여부
    private lateinit var notificationHelper: NotificationHelper // 알림 관리 객체
    private lateinit var notificationPermission: NotificationPermission // 알림 권한
    private lateinit var messageDialogShower: MessageDialogShower // 메시지 보여주는 객체
    private lateinit var dataUploader: DataUploader


    // Fragment의 UI를 생성하고 반환할 때 호출
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { // Context를 기반으로 객체들을 초기화합니다.
        usagePermission = UsagePermission(requireContext())
        isNotificationsEnabled = NotificationsEnabled(requireActivity())
        notificationPermission = NotificationPermission(requireContext())
        messageDialogShower = MessageDialogShower(requireContext())
        notificationHelper = NotificationHelper(requireContext())
        dataUploader = DataUploader(requireContext())
        return inflater.inflate(R.layout.fragment_main, container, false) // XML 레이아웃 파일을 inflate하여 Fragment의 뷰를 생성
    }

    // Fragment의 UI를 생성하고 반환할 때 호출
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 유탈리티 객체 초기화
        val permissionDialog = PermissionNotGrantedDialogShower(requireContext())
        val preferenceProvider = PreferenceProvider(requireContext())
        val healthNotification = ShowHealthClientNotAvailable(requireContext())
        val healthUtils = HealthUtils(requireContext())

        // 현재 앱의 언어 설정
        preferenceProvider.saveApplicationLanguage(Locale.getDefault().language)

        // XML 레이아웃에서 텍스트뷰와 버튼을 찾습니다.
        val mainText: TextView = view.findViewById(R.id.mainText) // Text 뷰
        val logout: Button = view.findViewById(R.id.logout) // Logout 버튼

        // 알림 권한 요청을 위한 launcher 설정
        val requestPermissionLauncher =
            registerForActivityResult(
                RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // 알림 권한이 허용되면 알림 채널을 생성합니다.
                    notificationHelper.createNotificationChannel(
                        TIMED_NOTIFICATION_CHANNEL_ID,
                        getString(R.string.timed_notification_channel_name),
                    )
                } else {
                    // 알림 권한이 거부되면 에러 다이얼로그를 보여줍니다.
                    // messageDialogShower.showAlertDialogError(getString(R.string.notification_denied))

                    // 알림 권한이 거부되면 설정 페이지로 이동하도록 안내하는 다이얼로그를 보여줍니다.
                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.notification_denied))
                        .setMessage(getString(R.string.notification_denied_description))
                        .setPositiveButton(getString(R.string.go_to_setting)) { _, _ ->
                            // 설정 페이지로 이동
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", requireContext().packageName, null)
                            }
                            startActivity(intent)
                        }
                        .show()
                }
            }

        // Health Connect(Check) 권한 요청
        // Health Connect 앱이 없는 사용자는 권한 요청 불가
        // Health Connect 권한을 요청하는 런처를 등록합니다.
        val requestPermissionActivityContract =
            PermissionController.createRequestPermissionResultContract()
        val requestHealthPermissions =
            registerForActivityResult(requestPermissionActivityContract) { granted ->
                if (!granted.containsAll(HEALTH_PERMISSION)) {
                    // Health Connect 권한이 거부되면 권한 요청 다이얼로그를 보여줍니다.
                    HealthPermissionNotGranted(requireContext()).showPermissionDialog()
                }
            }

        // Health Connect가 사용 가능한지 확인합니다.
        if (healthUtils.checkIfHealthConnectAvailable()) {
            // 사용 가능하다면, Health 권한을 요청합니다.
            userInfoViewModel.readHealthPermission(requestHealthPermissions)
        } else {
            // 사용 불가능하다면 경고 다이얼로그를 보여줍니다.
            healthNotification.showAlertDialog()
        }

        // args로 전달받은 이메일을 메인 텍스트뷰에 설정합니다.
        mainText.text = getString(R.string.main_text, args.email)

        // 로그아웃 버튼 클릭 리스너를 설정합니다.
        logout.setOnClickListener {
            Firebase.auth.signOut() // Firebase 인증에서 로그아웃합니다.
            userInfoViewModel.stopWorkManager() // WorkManager를 중지합니다.
            Intent(requireContext(), NotificationService::class.java).also { intent ->
                requireActivity().stopService(intent) // 알림 서비스를 중지합니다.
            }
            // RegisterFragment (회원가입?)로 이동하는 네비게이션 액션을 실행합니다.
            val action = MainFragmentDirections.actionMainFragmentToRegisterFragment()
            findNavController().navigate(action)
        }

        // 알림 권한
        // 알림 권한을 요청합니다.
        requestPermissionLauncher.launch(POST_NOTIFICATIONS)

        // 알림이 활성화되었는지 확인하고 로그를 출력합니다.
        val isNotificationEnabledBool = isNotificationsEnabled()
        Log.e("Remote Config", "updated: $isNotificationEnabledBool")

        // 사용자가 권한을 부여했는지 확인합니다.
        if (!usagePermission()) {
            println("앱 사용 데이터 접근 권한이 부여됨")
            // 권한이 부여되지 않았다면 권한 요청 다이얼로그를 보여줍니다.
            permissionDialog.showAlertDialogPermissionNotGranted(getString(R.string.permission_not_granted))
        } else {
            // 권한이 부여되었다면 주기적으로 데이터를 전송하는 작업을 설정합니다.
            // 핵심 소스코드일 가능성?
            println("앱 사용 데이터 접근 권한이 부여됨")
            userInfoViewModel.setPeriodicallySendingData()
        }

        // 테스트 코드
        // testGetListOfUsage()
        dataUploader.testGetListOfUsage()

        // 알림 서비스를 포그라운드(지속적) 서비스로 시작합니다.
        Intent(requireContext(), NotificationService::class.java).also { intent ->
            requireActivity().startForegroundService(intent)
        }
    }

    // Fragment가 사용자와 상호작용을 시작할 때 호출
    override fun onResume() {
        super.onResume()
        // 프래그먼트가 화면에 다시 나타났을 때 사용자가 권한을 부여했는지 확인하고
        if (usagePermission()) {
        // 주기적으로 데이터를 전송하는 작업을 설정합니다.
            userInfoViewModel.setPeriodicallySendingData()
        }
    }
}
