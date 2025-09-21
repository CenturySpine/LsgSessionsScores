package fr.centuryspine.lsgscores.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.centuryspine.lsgscores.BuildConfig
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import fr.centuryspine.lsgscores.ui.theme.availableThemes
import fr.centuryspine.lsgscores.viewmodel.GameZoneViewModel
import fr.centuryspine.lsgscores.viewmodel.LanguageOption
import fr.centuryspine.lsgscores.viewmodel.LanguageViewModel
import fr.centuryspine.lsgscores.viewmodel.ThemeViewModel
import fr.centuryspine.lsgscores.viewmodel.MigrationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    languageViewModel: LanguageViewModel,

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

// Theme section with grid layout
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                availableThemes.forEach { theme ->
                    CompactThemePreviewCard(
                        theme = theme,
                        isSelected = selectedThemeId == theme.id,
                        onThemeSelected = { themeViewModel.setTheme(theme.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Language section (ajoutez après la section thème)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.settings_label_language),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Language section with grid layout
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                availableLanguages.forEach { language ->
                    CompactLanguageSelectionCard(
                        language = language,
                        isSelected = selectedLanguage == language.code,
                        onLanguageSelected = {
                            languageViewModel.setLanguage(language.code)
                            (context as android.app.Activity).recreate()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Migration (debug)",
                    style = MaterialTheme.typography.titleLarge
                )
                MigrationDebugSection()
            }

            // Sign out section (for testing Google Auth)
            val authViewModel: fr.centuryspine.lsgscores.viewmodel.AuthViewModel = hiltViewModel()
            val currentUser by authViewModel.user.collectAsState()
            if (currentUser != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Compte",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(onClick = { authViewModel.signOut() }) {
                    Text("Se déconnecter")
                }
            }

        }
    }


}


@Composable
private fun CompactThemePreviewCard(
    theme: fr.centuryspine.lsgscores.ui.theme.AppTheme,
    isSelected: Boolean,
    onThemeSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Color preview circles
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ColorCircle(color = theme.lightColors.primary, modifier = Modifier.size(16.dp))
                ColorCircle(color = theme.lightColors.secondary, modifier = Modifier.size(16.dp))
                ColorCircle(color = theme.lightColors.tertiary, modifier = Modifier.size(16.dp))
            }

            Text(
                text = theme.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            RadioButton(
                selected = isSelected,
                onClick = onThemeSelected,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CompactLanguageSelectionCard(
    language: LanguageOption,
    isSelected: Boolean,
    onLanguageSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Flag emoji
            Text(
                text = language.flagEmoji,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = language.displayName,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            RadioButton(
                selected = isSelected,
                onClick = onLanguageSelected,
                modifier = Modifier.size(20.dp)
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

@Composable
private fun MigrationDebugSection(
    viewModel: MigrationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = state.lastMessage ?: "Prêt à migrer vos données locales vers Supabase.")

            state.error?.let { err ->
                Text(text = err, color = Color.Red)
            }

            state.report?.let { r ->
                Text(text = "Résultat:")
                Text(text = "- Villes: ${r.cities}")
                Text(text = "- Zones de jeu: ${r.gameZones}")
                Text(text = "- Modes de score: ${r.scoringModes}")
                Text(text = "- Joueurs: ${r.players}")
                Text(text = "- Trous: ${r.holes}")
                Text(text = "- Sessions: ${r.sessions}")
                Text(text = "- Équipes: ${r.teams}")
                Text(text = "- Trous joués: ${r.playedHoles}")
                Text(text = "- Scores: ${r.playedHoleScores}")
            }

            Button(onClick = { viewModel.runMigration() }, enabled = !state.isRunning) {
                Text(if (state.isRunning) "Migration en cours…" else "Lancer la migration")
            }

        }
    }
}

