package com.example.lsgscores

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.lsgscores.data.preferences.AppPreferences
import com.example.lsgscores.ui.MainScreen
import com.example.lsgscores.ui.theme.LsgScoresTheme
import com.example.lsgscores.util.LanguageManager
import com.example.lsgscores.viewmodel.HoleViewModel
import com.example.lsgscores.viewmodel.LanguageViewModel
import com.example.lsgscores.viewmodel.PlayerViewModel
import com.example.lsgscores.viewmodel.SessionViewModel
import com.example.lsgscores.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val languageViewModel: LanguageViewModel = hiltViewModel()
            val selectedLanguage by languageViewModel.selectedLanguage.collectAsState()

            // Apply language when it changes
            LaunchedEffect(selectedLanguage) {
                if (selectedLanguage != AppPreferences.LANGUAGE_SYSTEM) {
                    // Language will be applied through attachBaseContext for non-system languages
                    recreate()
                }
            }

            LsgScoresTheme(themeViewModel = themeViewModel) {
                val navController = rememberNavController()

                MainScreen(
                    navController = navController,
                    playerViewModel = hiltViewModel<PlayerViewModel>(),
                    holeViewModel = hiltViewModel<HoleViewModel>(),
                    sessionViewModel = hiltViewModel<SessionViewModel>(),
                    languageViewModel = languageViewModel
                )
            }
        }
    }
    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            // Apply saved language preference
            val appPreferences = com.example.lsgscores.data.preferences.AppPreferences(newBase)
            val contextWithLanguage = LanguageManager.applyLanguage(newBase, appPreferences.selectedLanguage)
            super.attachBaseContext(contextWithLanguage)
        } else {
            super.attachBaseContext(newBase)
        }
    }
}