package io.jasperapps.appusagernd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import io.jasperapps.appusagernd.database.UserInfoViewModel
import io.jasperapps.appusagernd.enums.Gender
import io.jasperapps.appusagernd.model.AppUsage
import io.jasperapps.appusagernd.model.UserInfo
import io.jasperapps.appusagernd.utils.HealthUtils
import io.jasperapps.appusagernd.utils.MessageDialogShower
import io.jasperapps.appusagernd.utils.ProgressDialogShower
import io.jasperapps.appusagernd.utils.ShowHealthClientNotAvailable
import io.jasperapps.appusagernd.utils.ValidationAge
import io.jasperapps.appusagernd.utils.ValidationEmail
import io.jasperapps.appusagernd.utils.ValidationPassword

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private val auth: FirebaseAuth = Firebase.auth
    private val validationEmail = ValidationEmail()
    private val validationPassword = ValidationPassword()
    private val validationAge = ValidationAge()
    private lateinit var messageDialogShower: MessageDialogShower
    private lateinit var progressDialogShower: ProgressDialogShower
    private val userInfoViewModel: UserInfoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        messageDialogShower = MessageDialogShower(requireContext())
        progressDialogShower = ProgressDialogShower(requireContext())
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button: Button = view.findViewById(R.id.submit)
        val emailEditText: EditText = view.findViewById(R.id.input_email)
        val passwordEditText: EditText = view.findViewById(R.id.input_password)
        val ageEditText: EditText = view.findViewById(R.id.input_age)
        val radioGroups: RadioGroup = view.findViewById(R.id.gender)
        val notification: TextView = view.findViewById(R.id.notification)
        val logInButton: TextView = view.findViewById(R.id.log_in)

        val healthUtils = HealthUtils(requireContext())
        val healthNotification = ShowHealthClientNotAvailable(requireContext())
        if (!healthUtils.checkIfHealthConnectAvailable()) {
            healthNotification.showAlertDialog()
        }

        emailEditText.addTextChangedListener {
            notification.visibility = View.GONE
        }
        passwordEditText.addTextChangedListener {
            notification.visibility = View.GONE
        }

        ageEditText.addTextChangedListener {
            notification.visibility = View.GONE
        }

        logInButton.setOnClickListener {
            val action = RegisterFragmentDirections.actionRegisterFragmentToLoginFragment()
            findNavController().navigate(action)
        }

        button.setOnClickListener {
            if (emailEditText.text.isBlank() || passwordEditText.text.isBlank() || ageEditText.text.isBlank()) {
                notification.visibility = View.VISIBLE
            } else {
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()
                val age = ageEditText.text.toString().toInt()
                var gender = Gender.MALE.toString()

                when (radioGroups.checkedRadioButtonId) {
                    R.id.radio_female -> {
                        gender = Gender.FEMALE.toString()
                    }
                    R.id.radio_male -> {
                        gender = Gender.MALE.toString()
                    }
                }
                val loading = progressDialogShower.showLoadingProgressDialog()

                if (validationEmail(email) && validationPassword(password) && validationAge(
                        age
                    )
                ) {
                    if (healthUtils.checkIfHealthConnectAvailable()) {
                        loading.show()
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    loading.dismiss()
                                    val list = HashMap<String, List<AppUsage>>()
                                    val userInfo = UserInfo(email, age, gender, list)
                                    auth.currentUser?.let { it1 ->
                                        userInfoViewModel.saveToDatabase(
                                            it1.uid,
                                            userInfo
                                        )
                                    }
                                    val action =
                                        RegisterFragmentDirections.actionRegisterFragmentToMainFragment(
                                            email
                                        )
                                    findNavController().navigate(action)
                                } else {
                                    loading.dismiss()
                                    messageDialogShower.showAlertDialogError(getString(R.string.registration_failed_message))
                                }
                            }
                    } else {
                        healthNotification.showAlertDialog()
                    }
                } else {
                    notification.visibility = View.VISIBLE
                }
            }
        }
    }
}
