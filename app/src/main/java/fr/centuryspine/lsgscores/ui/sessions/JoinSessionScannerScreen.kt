@file:OptIn(ExperimentalGetImage::class)
@file:Suppress("UnsafeOptInUsageError")

package fr.centuryspine.lsgscores.ui.sessions

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalGetImage::class)
@Composable
fun JoinSessionScannerScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var error by remember { mutableStateOf<String?>(null) }
    var detected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Runtime CAMERA permission handling
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCamPermission = granted
        if (!granted) {
            error = "Autorisez l'accès à la caméra ou utilisez l'analyse d'une photo."
        } else {
            error = null
        }
    }

    // Gallery picker fallback to analyze a still image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && !detected) {
            val scanner = BarcodeScanning.getClient()
            try {
                val image = InputImage.fromFilePath(context, uri)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val raw = barcode.rawValue ?: continue
                            val id = parseSessionId(raw)
                            if (id != null && !detected) {
                                detected = true
                                scope.launch { navController.navigate("join_session_pick_team/$id") }
                                break
                            }
                        }
                        if (!detected) {
                            error = "Aucun QR de session reconnu dans cette image."
                        }
                    }
                    .addOnFailureListener { e ->
                        error = e.message
                    }
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    // Auto-request permission once when entering the screen
    LaunchedEffect(Unit) {
        if (!hasCamPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Scanner le QR code de la session", style = MaterialTheme.typography.titleLarge)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (hasCamPermission) {
            AndroidView(factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    // Use COMPATIBLE to avoid black screens on some devices/surface configs
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val scanner = BarcodeScanning.getClient()
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy: ImageProxy ->
                                if (detected) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val raw = barcode.rawValue ?: continue
                                                val id = parseSessionId(raw)
                                                if (id != null && !detected) {
                                                    detected = true
                                                    scope.launch {
                                                        navController.navigate("join_session_pick_team/$id")
                                                    }
                                                    break
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            error = e.message
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (e: Exception) {
                        error = e.message
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            })
        } else {
            Text("La caméra n'est pas autorisée. Autorisez-la ou analysez une photo.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Autoriser la caméra")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { pickImageLauncher.launch("image/*") }) {
                Text("Analyser une photo")
            }
            Button(onClick = { navController.popBackStack() }) {
                Text("Annuler")
            }
        }
    }
}

private fun parseSessionId(raw: String): Long? {
    // Expected format: LSGSESSION:<id>
    return if (raw.startsWith("LSGSESSION:")) raw.substringAfter(":").toLongOrNull() else null
}
