package com.example.lsgscores.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lsgscores.R
import com.example.lsgscores.ui.theme.availableThemes
import com.example.lsgscores.viewmodel.LanguageViewModel
import com.example.lsgscores.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel,
    languageViewModel: LanguageViewModel
) {
    val selectedThemeId by themeViewModel.selectedThemeId.collectAsState()
    val selectedLanguage by languageViewModel.selectedLanguage.collectAsState()
    val availableLanguages = languageViewModel.getAvailableLanguages()
    val context = LocalContext.current

    LaunchedEffect(selectedThemeId) {
        println("DEBUG: Theme changed to: $selectedThemeId")
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme section
            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = stringResource(R.string.settings_label_theme),
                style = MaterialTheme.typography.titleMedium
            )

            availableThemes.forEach { theme ->
                ThemePreviewCard(
                    theme = theme,
                    isSelected = selectedThemeId == theme.id,
                    onThemeSelected = { themeViewModel.setTheme(theme.id) }
                )
            }
// Language section (ajoutez après la section thème)
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.settings_label_language),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            availableLanguages.forEach { language ->
                LanguageSelectionCard(
                    language = language,
                    isSelected = selectedLanguage == language.code,
                    onLanguageSelected = {
                        languageViewModel.setLanguage(language.code)
                        // Recreate activity to apply language change
                        (context as android.app.Activity).recreate()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
@Composable
private fun LanguageSelectionCard(
    language: com.example.lsgscores.viewmodel.LanguageOption,
    isSelected: Boolean,
    onLanguageSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLanguageSelected() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flag emoji
                Text(
                    text = language.flagEmoji,
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onLanguageSelected
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: com.example.lsgscores.ui.theme.AppTheme,
    isSelected: Boolean,
    onThemeSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onThemeSelected() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color preview circles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ColorCircle(color = theme.lightColors.primary)
                    ColorCircle(color = theme.lightColors.secondary)
                    ColorCircle(color = theme.lightColors.tertiary)
                }

                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onThemeSelected
            )
        }
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}