package fr.centuryspine.lsgscores

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import fr.centuryspine.lsgscores.ui.MainScreen
import fr.centuryspine.lsgscores.ui.theme.LsgScoresTheme
import fr.centuryspine.lsgscores.utils.AppVersionResolver
import fr.centuryspine.lsgscores.utils.LanguageManager
import fr.centuryspine.lsgscores.viewmodel.AuthViewModel
import fr.centuryspine.lsgscores.viewmodel.LanguageViewModel
import fr.centuryspine.lsgscores.viewmodel.ThemeViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.handleDeeplinks
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var supabase: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate intent.data=${intent?.data}")

        // Avant toute autre action, vérifier la version courante côté BDD (Supabase)
        lifecycleScope.launch {
            val versionInfo = fetchCurrentVersionFromSupabaseSafely()

            // Récupère la version installée depuis le PackageManager (source de vérité),
            // avec repli sur BuildConfig.VERSION_NAME si indisponible.
            val localVersionFromPkg = AppVersionResolver.resolveLocalVersionName(this@MainActivity)
            val localVersionFromBuildConfig = BuildConfig.VERSION_NAME
            val localVersion = localVersionFromPkg.ifBlank { localVersionFromBuildConfig }.trim()

            val remoteVersion = versionInfo?.version?.trim()

            Log.d(
                "MainActivity",
                "Local version (PackageInfo)=$localVersionFromPkg, (BuildConfig)=$localVersionFromBuildConfig, Remote version=$remoteVersion"
            )

            if (remoteVersion == null) {
                showErrorDialogAndExit()
            } else if (remoteVersion != localVersion) {
                // Versions différentes: afficher un dialogue bloquant proposant le lien de téléchargement, puis quitter
                showOutdatedDialogAndExit(versionInfo)
            } else {
                // Versions identiques -> on démarre normalement
                startNormalStartup()
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

    // -------------------
    // Helpers
    // -------------------

    @Serializable
    private data class AppVersionRow(
        val version: String? = null,
        val is_current: Boolean = false,
        val download_link: String? = null
    )

    private suspend fun fetchCurrentVersionFromSupabaseSafely(): AppVersionRow? = withContext(Dispatchers.IO) {
        try {
            // Requête directe sur la table app_versions: on récupère la ligne is_current = true
            val result = supabase.postgrest.from("app_versions").select {
                filter { eq("is_current", true) }
            }
            // Décodage en liste et on prend la première si elle existe
            val rows = result.decodeList<AppVersionRow>()

            rows.firstOrNull()
        } catch (t: Throwable) {
            Log.w("MainActivity", "fetchCurrentVersionFromSupabaseSafely failed: ${t.message}")
            null
        }
    }


    private fun startNormalStartup() {
        // Important: laisser Supabase Auth consommer les deeplinks OAuth si l'activité a été créée depuis un lien
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
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }

    private fun showErrorDialogAndExit() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Erreur")
            .setMessage("Impossible d'évaluer la version courante. L'application va se terminer.")
            .setPositiveButton("OK") { d, _ ->
                d.dismiss()
            }
            .create()

        dialog.setOnDismissListener {
            // Quitter immédiatement l'application après fermeture du dialogue
            try {
                finishAffinity()
                finishAndRemoveTask()
            } catch (_: Throwable) {
            }
            exitProcess(0)
        }

        dialog.show()
    }

    private fun showOutdatedDialogAndExit(info: AppVersionRow) {
        val downloadUrl = info.download_link
        val message = buildString {
            append("Votre version de l'application n'est pas à jour.\n\n")
            append("Version attendue: ${info.version ?: "?"}\n")
            append("Version installée: ${AppVersionResolver.resolveLocalVersionName(this@MainActivity)}\n\n")
            if (!downloadUrl.isNullOrBlank()) {
                append("Lien de téléchargement:\n")
                append(downloadUrl)
            } else {
                append("Aucun lien de téléchargement fourni.")
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Mise à jour requise")
            .setMessage(message)
            .setPositiveButton("OK") { d, _ ->
                d.dismiss()
            }
            .apply {
                if (!downloadUrl.isNullOrBlank()) {
                    setNeutralButton("Ouvrir le lien") { _, _ ->
                        kotlin.runCatching {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
                        }.onFailure { e ->
                            Log.w("MainActivity", "Impossible d'ouvrir le lien: ${e.message}")
                        }
                    }
                }
            }
            .create()

        dialog.setOnDismissListener {
            // Quitter immédiatement l'application après fermeture du dialogue
            try {
                finishAffinity()
                finishAndRemoveTask()
            } catch (_: Throwable) {
            }
            exitProcess(0)
        }

        dialog.show()
    }
}