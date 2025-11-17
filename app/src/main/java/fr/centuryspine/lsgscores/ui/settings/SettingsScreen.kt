package fr.centuryspine.lsgscores.ui.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.ui.theme.availableThemes
import fr.centuryspine.lsgscores.viewmodel.LanguageOption
import fr.centuryspine.lsgscores.viewmodel.LanguageViewModel
import fr.centuryspine.lsgscores.viewmodel.ThemeViewModel

@SuppressLint("UseKtx")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    languageViewModel: LanguageViewModel,
    authViewModel: fr.centuryspine.lsgscores.viewmodel.AuthViewModel
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
            // Langage section (ajoutez après la section thème)
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

            // Legal section
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.settings_section_legal),
                style = MaterialTheme.typography.titleLarge
            )
            Button(onClick = {
                val url = context.getString(R.string.settings_privacy_policy_url)
                try {
                    val viewIntent =
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                            addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    val chooser = android.content.Intent.createChooser(
                        viewIntent,
                        context.getString(R.string.settings_privacy_policy)
                    )
                    context.startActivity(chooser)
                } catch (_: Exception) {
                    // Fallback: just try a basic ACTION_VIEW
                    val fallback = android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(fallback)
                }
            }) {
                Text(stringResource(R.string.settings_privacy_policy))
            }


            // Account section: sign out and delete an account
            val currentUser by authViewModel.user.collectAsState()
            val deleteState by authViewModel.deleteAccountState.collectAsState()
            var showDeleteDialog by remember { mutableStateOf(false) }

            // Success toast after deletion and sign-out
            val ctx = context
            LaunchedEffect(deleteState) {
                if (deleteState is fr.centuryspine.lsgscores.viewmodel.DeleteAccountState.Success) {
                    android.widget.Toast.makeText(
                        ctx,
                        ctx.getString(R.string.settings_delete_account_success_toast),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    authViewModel.resetDeleteAccountState()
                }
            }

            if (currentUser != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.settings_account_section_title),
                    style = MaterialTheme.typography.titleLarge
                )

                // Sign out button
                Button(onClick = { authViewModel.signOut() }) {
                    Text(stringResource(R.string.settings_sign_out_button))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Delete account button (destructive)
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    enabled = deleteState !is fr.centuryspine.lsgscores.viewmodel.DeleteAccountState.Loading
                ) {
                    if (deleteState is fr.centuryspine.lsgscores.viewmodel.DeleteAccountState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.settings_delete_account_button))
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text(stringResource(R.string.settings_delete_account_confirm_title)) },
                        text = { Text(stringResource(R.string.settings_delete_account_confirm_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                authViewModel.deleteAccount()
                            }) {
                                Text(stringResource(R.string.settings_delete_account_confirm_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text(stringResource(R.string.settings_delete_account_confirm_cancel))
                            }
                        }
                    )
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


