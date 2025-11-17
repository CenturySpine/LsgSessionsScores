package fr.centuryspine.lsgscores

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LsgScoresApplication : Application() {

    @javax.inject.Inject
    lateinit var imageCacheManager: fr.centuryspine.lsgscores.utils.ImageCacheManager

    @javax.inject.Inject
    lateinit var appPreferences: fr.centuryspine.lsgscores.data.preferences.AppPreferences

    private var activityCount = 0

    override fun onCreate() {
        super.onCreate()
        // Warm cache for current city if selected
        appPreferences.getSelectedCityId()?.let { cityId ->
            imageCacheManager.warmAllForCity(cityId)
        }
        // Track app lifecycle to clear cache on normal close (best-effort)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {
                activityCount++
            }

            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {
                activityCount--
                if (activityCount <= 0) {
                    // No more activities, app closed normally
                    imageCacheManager.clearCache()
                }
            }
        })
    }
}