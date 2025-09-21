package fr.centuryspine.lsgscores.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.centuryspine.lsgscores.R

@Composable
fun AuthScreen(
    onGoogle: () -> Unit,
    onFacebook: (() -> Unit)? = null
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.auth_required_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                Log.d("AuthScreen", "Google button clicked")
                onGoogle()
            }) {
                Text(stringResource(id = R.string.auth_sign_in_google))
            }
            if (onFacebook != null) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    Log.d("AuthScreen", "Facebook button clicked")
                    onFacebook()
                }, enabled = false) {
                    Text(stringResource(id = R.string.auth_sign_in_facebook_coming_soon))
                }
            }
        }
    }
}
