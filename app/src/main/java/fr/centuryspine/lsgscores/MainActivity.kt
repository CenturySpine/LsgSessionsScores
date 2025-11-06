package fr.centuryspine.lsgscores

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import fr.centuryspine.lsgscores.ui.MainScreen
import fr.centuryspine.lsgscores.ui.theme.LsgScoresTheme
import fr.centuryspine.lsgscores.utils.LanguageManager
import fr.centuryspine.lsgscores.viewmodel.LanguageViewModel
import fr.centuryspine.lsgscores.viewmodel.ThemeViewModel
import fr.centuryspine.lsgscores.viewmodel.AuthViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var supabase: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate intent.data=${intent?.data}")

        // Important: let Supabase Auth consume OAuth deeplinks when activity is created from a link
        try {
            supabase.handleDeeplinks(intent)
            Log.d("MainActivity", "handleDeeplinks called in onCreate")
        } catch (t: Throwable) {
            Log.w("MainActivity", "handleDeeplinks onCreate failed: ${t.message}")
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val languageViewModel: LanguageViewModel = hiltViewModel()

            LsgScoresTheme(themeViewModel = themeViewModel) {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()

                // Gate the entire app behind authentication
                fr.centuryspine.lsgscores.ui.auth.AuthGate {
                    MainScreen(
                        navController = navController,
                        languageViewModel = languageViewModel,
                        authViewModel = authViewModel,
                        supabase = supabase
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent intent.data=${intent.data}")
        try {
            supabase.handleDeeplinks(intent)
            Log.d("MainActivity", "handleDeeplinks called in onNewIntent")
        } catch (t: Throwable) {
            Log.w("MainActivity", "handleDeeplinks onNewIntent failed: ${t.message}")
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            // Apply saved language preference
            val appPreferences = fr.centuryspine.lsgscores.data.preferences.AppPreferences(newBase)
            val contextWithLanguage =
                LanguageManager.applyLanguage(newBase, appPreferences.selectedLanguage)
            super.attachBaseContext(contextWithLanguage)
        } else {
            super.attachBaseContext(newBase)
        }
    }
}