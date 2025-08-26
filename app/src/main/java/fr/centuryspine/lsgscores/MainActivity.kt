package fr.centuryspine.lsgscores

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import fr.centuryspine.lsgscores.ui.MainScreen
import fr.centuryspine.lsgscores.ui.theme.LsgScoresTheme
import fr.centuryspine.lsgscores.utils.LanguageManager
import fr.centuryspine.lsgscores.viewmodel.GameZoneViewModel
import fr.centuryspine.lsgscores.viewmodel.HoleViewModel
import fr.centuryspine.lsgscores.viewmodel.LanguageViewModel
import fr.centuryspine.lsgscores.viewmodel.PlayerViewModel
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel
import fr.centuryspine.lsgscores.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val languageViewModel: LanguageViewModel = hiltViewModel()

            // Remove the problematic LaunchedEffect that was causing infinite recreation

            LsgScoresTheme(themeViewModel = themeViewModel) {
                val navController = rememberNavController()

                MainScreen(
                    navController = navController,
                    playerViewModel = hiltViewModel<PlayerViewModel>(),
                    holeViewModel = hiltViewModel<HoleViewModel>(),
                    sessionViewModel = hiltViewModel<SessionViewModel>(),
                    gameZoneViewModel = hiltViewModel<GameZoneViewModel>(),
                    languageViewModel = languageViewModel
                )
            }
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