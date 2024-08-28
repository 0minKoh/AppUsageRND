package io.jasperapps.appusagernd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import io.jasperapps.appusagernd.utils.MessageDialogShower
import io.jasperapps.appusagernd.utils.ProgressDialogShower
import io.jasperapps.appusagernd.utils.ValidationEmail
import io.jasperapps.appusagernd.utils.ValidationPassword

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private val auth: FirebaseAuth = Firebase.auth
    private val validationEmail = ValidationEmail()
    private val validationPassword = ValidationPassword()
    private lateinit var messageDialogShower: MessageDialogShower
    private lateinit var progressDialogShower: ProgressDialogShower

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        messageDialogShower = MessageDialogShower(requireContext())
        progressDialogShower = ProgressDialogShower(requireContext())
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button: Button = view.findViewById(R.id.submit)
        val emailEditText: EditText = view.findViewById(R.id.input_email)
        val passwordEditText: EditText = view.findViewById(R.id.input_password)
        val notification: TextView = view.findViewById(R.id.notification)

        emailEditText.addTextChangedListener {
            notification.visibility = View.GONE
        }
        passwordEditText.addTextChangedListener {
            notification.visibility = View.GONE
        }

        button.setOnClickListener {
            if (emailEditText.text.isBlank() || passwordEditText.text.isBlank()) {
                notification.visibility = View.VISIBLE
            } else {
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()
                val loading = progressDialogShower.showLoadingProgressDialog()

                if (validationEmail(email) && validationPassword(password)) {
                    loading.show()
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                        if (it.isSuccessful) {
                            loading.dismiss()
                            val action =
                                LoginFragmentDirections.actionLoginFragmentToMainFragment(email)
                            findNavController().navigate(action)
                        } else {
                            loading.dismiss()
                            messageDialogShower.showAlertDialogError(getString(R.string.login_failed_message))
                        }
                    }
                } else {
                    notification.visibility = View.VISIBLE
                }
            }
        }
    }
}
