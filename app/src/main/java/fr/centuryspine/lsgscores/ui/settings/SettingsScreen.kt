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
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import fr.centuryspine.lsgscores.ui.theme.availableThemes
import fr.centuryspine.lsgscores.viewmodel.GameZoneViewModel
import fr.centuryspine.lsgscores.viewmodel.LanguageOption
import fr.centuryspine.lsgscores.viewmodel.LanguageViewModel
import fr.centuryspine.lsgscores.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    languageViewModel: LanguageViewModel,
    gameZoneViewModel: GameZoneViewModel = hiltViewModel()
) {
    val selectedThemeId by themeViewModel.selectedThemeId.collectAsState()
    val selectedLanguage by languageViewModel.selectedLanguage.collectAsState()
    val availableLanguages = languageViewModel.getAvailableLanguages()
    val gameZones by gameZoneViewModel.gameZones.collectAsState()
    val context = LocalContext.current

    var showAddZoneDialog by remember { mutableStateOf(false) }
    var newZoneName by remember { mutableStateOf("") }
    var editingGameZone by remember { mutableStateOf<GameZone?>(null) }

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

            // Game Zones Section
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.settings_section_game_zones),
                style = MaterialTheme.typography.titleLarge
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.settings_label_manage_game_zones),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { showAddZoneDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_game_zone_content_description))
                }
            }

            if (gameZones.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_game_zones_defined),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gameZones.forEach { gameZone ->
                        GameZoneItem(
                            gameZone = gameZone,
                            onEditClick = { editingGameZone = it }
                        )
                    }
                }
            }
        }
    }

    if (showAddZoneDialog) {
        AlertDialog(
            onDismissRequest = { showAddZoneDialog = false },
            title = { Text(stringResource(R.string.add_game_zone_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = newZoneName,
                    onValueChange = { newZoneName = it },
                    label = { Text(stringResource(R.string.game_zone_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    gameZoneViewModel.addGameZone(newZoneName)
                    newZoneName = ""
                    showAddZoneDialog = false
                }) {
                    Text(stringResource(R.string.add_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newZoneName = ""
                    showAddZoneDialog = false
                }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    editingGameZone?.let { gameZone ->
        var updatedZoneName by remember { mutableStateOf(gameZone.name) }
        AlertDialog(
            onDismissRequest = { editingGameZone = null },
            title = { Text(stringResource(R.string.edit_game_zone_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = updatedZoneName,
                    onValueChange = { updatedZoneName = it },
                    label = { Text(stringResource(R.string.game_zone_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    gameZoneViewModel.updateGameZone(gameZone.copy(name = updatedZoneName))
                    editingGameZone = null
                }) {
                    Text(stringResource(R.string.update_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGameZone = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
private fun GameZoneItem(
    gameZone: GameZone,
    onEditClick: (GameZone) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = gameZone.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onEditClick(gameZone) }) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_game_zone_content_description))
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
