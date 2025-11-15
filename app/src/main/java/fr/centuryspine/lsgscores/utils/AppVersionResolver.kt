package fr.centuryspine.lsgscores.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import fr.centuryspine.lsgscores.BuildConfig

/**
 * Utilitaire pour résoudre de manière fiable la version locale installée de l'application.
 * Utilise le PackageManager (source de vérité) avec repli sur BuildConfig.VERSION_NAME.
 */
object AppVersionResolver {

    fun resolveLocalVersionName(context: Context): String {
        return try {
            val pm = context.packageManager
            val pkgName = context.packageName
            val versionName = if (Build.VERSION.SDK_INT >= 33) {
                val info = pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
                info.versionName
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(pkgName, 0)
                info.versionName
            }
            (versionName ?: BuildConfig.VERSION_NAME).trim()
        } catch (t: Throwable) {
            Log.w("AppVersionResolver", "resolveLocalVersionName failed: ${t.message}")
            BuildConfig.VERSION_NAME
        }
    }
}
