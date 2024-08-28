package io.jasperapps.appusagernd

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val navController: NavController by lazy {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.navController
    }
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        inflateGraph()
    }

    private fun inflateGraph() {
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        val startDestination = if (auth.currentUser == null) {
            R.id.greetingFragment
        } else {
            R.id.mainFragment
        }
        val parameters = bundleOf(
            "email" to (auth.currentUser?.email ?: "")
        )
        graph.setStartDestination(startDestination)
        navController.setGraph(graph, parameters)
    }
}
