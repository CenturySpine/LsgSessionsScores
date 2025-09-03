package fr.centuryspine.lsgscores.data.city

import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentCityManager @Inject constructor(
    private val appPreferences: AppPreferences
) {
    fun hasSelectedCity(): Boolean {
        return appPreferences.getSelectedCityId() != null
    }

    fun getCurrentCityId(): Long? = appPreferences.getSelectedCityId()
}