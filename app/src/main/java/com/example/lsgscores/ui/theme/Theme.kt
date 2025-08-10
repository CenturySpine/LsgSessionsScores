package com.example.lsgscores.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lsgscores.viewmodel.ThemeViewModel

@Composable
fun LsgScoresTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeViewModel: ThemeViewModel, // Obligatoire maintenant
    content: @Composable () -> Unit
) {
    val selectedThemeId by themeViewModel.selectedThemeId.collectAsState()
    val selectedTheme = availableThemes.find { it.id == selectedThemeId }
        ?: availableThemes.first()

    val colorScheme = when {
        selectedThemeId == "default" && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> selectedTheme.darkColors
        else -> selectedTheme.lightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}