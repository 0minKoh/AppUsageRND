package io.jasperapps.appusagernd

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// 가장 첫 화면: 인사
class GreetingFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_greeting, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val firebaseAuth = Firebase.auth
        val submitButton: Button = view.findViewById(R.id.confirm)
        val text: TextView = view.findViewById(R.id.greeting_text)
        text.text = Html.fromHtml(getString(R.string.greeting), Html.FROM_HTML_MODE_COMPACT)

        submitButton.setOnClickListener {
            if (firebaseAuth.currentUser != null) {
                val email = firebaseAuth.currentUser?.email ?: ""
                val action = GreetingFragmentDirections.actionGreetingFragmentToMainFragment(email)
                findNavController().navigate(action)
            } else {
                findNavController().navigate(GreetingFragmentDirections.actionGreetingFragmentToRegisterFragment())
            }
        }
    }
}
