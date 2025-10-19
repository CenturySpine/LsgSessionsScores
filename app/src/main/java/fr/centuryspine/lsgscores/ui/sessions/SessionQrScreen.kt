package fr.centuryspine.lsgscores.ui.sessions

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
        Text("QR code de la session", style = MaterialTheme.typography.titleLarge)
        if (session == null) {
            Text("Aucune session en cours")
            Button(onClick = { navController.popBackStack() }) {
                Text("Retour")
            }
        } else {
            val payload = "LSGSESSION:${session.id}"
            val bitmap: Bitmap = QrCodeUtils.generate(payload, size = 1024)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR de session"
            )
            Text("Scannez ce code pour rejoindre la session")
        }
    }
}
