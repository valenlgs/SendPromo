package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.SendPromoRepository
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ComposerScreen
import com.example.ui.screens.DatabaseScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SuccessScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioBlack
import com.example.util.configureAppCheck
import com.example.viewmodel.AppScreen
import com.example.viewmodel.SendPromoViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioBlack
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        // Unauthenticated session: Render sign-in UI only
        AuthScreen()
    } else {
        // Authenticated session: Keyed by user.uid
        val viewModel: SendPromoViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) as Application
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    SendPromoViewModel(SendPromoRepository(db), user.uid)
                }
            }
        )

        val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

        when (currentScreen) {
            AppScreen.HOME -> HomeScreen(viewModel = viewModel)
            AppScreen.COMPOSER -> ComposerScreen(viewModel = viewModel)
            AppScreen.DATABASE -> DatabaseScreen(viewModel = viewModel)
            AppScreen.ACTIVITY -> ActivityScreen(viewModel = viewModel)
            AppScreen.SUCCESS -> SuccessScreen(viewModel = viewModel)
        }
    }
}
