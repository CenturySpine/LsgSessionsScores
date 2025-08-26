package fr.centuryspine.lsgscores.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class AppTheme(
    val id: String,
    val name: String,
    val lightColors: ColorScheme,
    val darkColors: ColorScheme
)

// Ocean theme colors
private val oceanLight = lightColorScheme(
    primary = Color(0xFF006A6B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF6FF7F8),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6363),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E7),
    onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCE5FF),
    onTertiaryContainer = Color(0xFF001E31),
    background = Color(0xFFF4FFFE),
    surface = Color(0xFFF4FFFE),
    onBackground = Color(0xFF161D1D),
    onSurface = Color(0xFF161D1D)
)

private val oceanDark = darkColorScheme(
    primary = Color(0xFF4DDBDC),
    onPrimary = Color(0xFF003738),
    primaryContainer = Color(0xFF004F51),
    onPrimaryContainer = Color(0xFF6FF7F8),
    secondary = Color(0xFFB1CCCB),
    onSecondary = Color(0xFF1C3535),
    secondaryContainer = Color(0xFF324B4B),
    onSecondaryContainer = Color(0xFFCCE8E7),
    tertiary = Color(0xFFADC9E0),
    onTertiary = Color(0xFF143448),
    tertiaryContainer = Color(0xFF2C4A60),
    onTertiaryContainer = Color(0xFFCCE5FF),
    background = Color(0xFF0E1415),
    surface = Color(0xFF0E1415),
    onBackground = Color(0xFFDDE4E3),
    onSurface = Color(0xFFDDE4E3)
)

// Sunset theme colors
private val sunsetLight = lightColorScheme(
    primary = Color(0xFF8B4513),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4CC),
    onPrimaryContainer = Color(0xFF2F1400),
    secondary = Color(0xFF765848),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF2B160A),
    tertiary = Color(0xFF646133),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEAE6AC),
    onTertiaryContainer = Color(0xFF1F1D00),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5),
    onBackground = Color(0xFF201A17),
    onSurface = Color(0xFF201A17)
)

private val sunsetDark = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF693C00),
    onPrimaryContainer = Color(0xFFFFE4CC),
    secondary = Color(0xFFE7C0A6),
    onSecondary = Color(0xFF422B1D),
    secondaryContainer = Color(0xFF5B4132),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = Color(0xFFCDCA91),
    onTertiary = Color(0xFF343209),
    tertiaryContainer = Color(0xFF4B491E),
    onTertiaryContainer = Color(0xFFEAE6AC),
    background = Color(0xFF181210),
    surface = Color(0xFF181210),
    onBackground = Color(0xFFEDE0DB),
    onSurface = Color(0xFFEDE0DB)
)

val availableThemes = listOf(
    AppTheme(
        id = "default",
        name = "Default (System)",
        lightColors = lightColorScheme(), // Will use dynamic colors when available
        darkColors = darkColorScheme()
    ),
    AppTheme(
        id = "material_io",
        name = "LSG Brand",
        lightColors = lightColorScheme(
            primary = primaryLight,
            onPrimary = onPrimaryLight,
            primaryContainer = primaryContainerLight,
            onPrimaryContainer = onPrimaryContainerLight,
            secondary = secondaryLight,
            onSecondary = onSecondaryLight,
            secondaryContainer = secondaryContainerLight,
            onSecondaryContainer = onSecondaryContainerLight,
            tertiary = tertiaryLight,
            onTertiary = onTertiaryLight,
            tertiaryContainer = tertiaryContainerLight,
            onTertiaryContainer = onTertiaryContainerLight,
            error = errorLight,
            onError = onErrorLight,
            errorContainer = errorContainerLight,
            onErrorContainer = onErrorContainerLight,
            background = backgroundLight,
            onBackground = onBackgroundLight,
            surface = surfaceLight,
            onSurface = onSurfaceLight,
            surfaceVariant = surfaceVariantLight,
            onSurfaceVariant = onSurfaceVariantLight,
            outline = outlineLight,
            outlineVariant = outlineVariantLight,
            scrim = scrimLight,
            inverseSurface = inverseSurfaceLight,
            inverseOnSurface = inverseOnSurfaceLight,
            inversePrimary = inversePrimaryLight
        ),
        darkColors = darkColorScheme(
            primary = primaryDark,
            onPrimary = onPrimaryDark,
            primaryContainer = primaryContainerDark,
            onPrimaryContainer = onPrimaryContainerDark,
            secondary = secondaryDark,
            onSecondary = onSecondaryDark,
            secondaryContainer = secondaryContainerDark,
            onSecondaryContainer = onSecondaryContainerDark,
            tertiary = tertiaryDark,
            onTertiary = onTertiaryDark,
            tertiaryContainer = tertiaryContainerDark,
            onTertiaryContainer = onTertiaryContainerDark,
            error = errorDark,
            onError = onErrorDark,
            errorContainer = errorContainerDark,
            onErrorContainer = onErrorContainerDark,
            background = backgroundDark,
            onBackground = onBackgroundDark,
            surface = surfaceDark,
            onSurface = onSurfaceDark,
            surfaceVariant = surfaceVariantDark,
            onSurfaceVariant = onSurfaceVariantDark,
            outline = outlineDark,
            outlineVariant = outlineVariantDark,
            scrim = scrimDark,
            inverseSurface = inverseSurfaceDark,
            inverseOnSurface = inverseOnSurfaceDark,
            inversePrimary = inversePrimaryDark
        )
    ),
    AppTheme(
        id = "ocean",
        name = "Ocean",
        lightColors = oceanLight,
        darkColors = oceanDark
    ),
    AppTheme(
        id = "sunset",
        name = "Sunset",
        lightColors = sunsetLight,
        darkColors = sunsetDark
    )
)