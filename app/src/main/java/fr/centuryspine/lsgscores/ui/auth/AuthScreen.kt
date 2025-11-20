package fr.centuryspine.lsgscores.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
                text = stringResource(R.string.auth_welcome_text),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    Log.d("AuthScreen", "Google button clicked")
                    onGoogle()
                },

                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),

                ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.auth_sign_in_google),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
            if (onFacebook != null) {
                Spacer(modifier = Modifier.height(12.dp))
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
