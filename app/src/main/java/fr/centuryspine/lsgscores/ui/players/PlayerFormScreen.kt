package fr.centuryspine.lsgscores.ui.players

import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.ui.common.CombinedPhotoPicker
import fr.centuryspine.lsgscores.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerFormScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel
) {
    var name by rememberSaveable { mutableStateOf("") }

    // State for the cropped photo URI
    var croppedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }


    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.player_form_label_name)) },
                    modifier = Modifier.weight(1f)
                )

                CombinedPhotoPicker(
                    onImagePicked = { path -> photoPath = path }
                )
            }
            Spacer(Modifier.height(16.dp))

            // Show cropped photo if available
            croppedPhotoUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = stringResource(R.string.player_form_photo_description),
                    modifier = Modifier.size(128.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.player_form_button_cancel))
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            playerViewModel.addPlayer(
                                name = name,
                                photoUri = photoPath
                            ) {
                                navController.popBackStack()
                            }
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.player_form_button_save))
                }
            }
        }
    }
}

