package fr.centuryspine.lsgscores.ui.sessions

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.utils.QrCodeUtils
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel

@Composable
fun SessionQrScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel
) {
    val session = sessionViewModel.ongoingSession.collectAsState(initial = null).value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {


        Text(stringResource(R.string.session_qr_title), style = MaterialTheme.typography.titleLarge)

        if (session == null) {
            Text(stringResource(R.string.session_qr_no_session))
        } else {
            val payload = "LSGSESSION:${session.id}"
            val bitmap: Bitmap = QrCodeUtils.generate(payload, size = 1024)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.session_qr_image_description)
            )
            // Display the QR payload as plain text under the QR image
            Text(text = payload, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.session_qr_instruction))
        }
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(stringResource(R.string.session_qr_button_back))
        }
    }
}
