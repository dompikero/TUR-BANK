package com.turbank.mobile.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private fun cameraQrText(language: String, tr: String, en: String, de: String): String = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}

/** Full-screen CameraX preview + bundled ML Kit QR decoder. Results stay in the app for review. */
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraQrScannerScreen(
    language: String,
    onCodeRead: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var scanState by remember { mutableStateOf("") }
    val delivered = remember { AtomicBoolean(false) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            setBackgroundColor(android.graphics.Color.rgb(8, 11, 14))
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val scanner: BarcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(cameraProviderFuture) {
        cameraProviderFuture.addListener({
            runCatching { cameraProvider = cameraProviderFuture.get() }
                .onFailure { scanState = cameraQrText(language, "Kamera başlatılamadı. Uygulamayı yeniden açıp dene.", "Could not start the camera. Reopen the scanner and try again.", "Kamera konnte nicht gestartet werden. Öffne den Scanner erneut.") }
        }, mainExecutor)
        onDispose { cameraProvider?.unbindAll() }
    }

    DisposableEffect(cameraProvider, hasPermission, lifecycleOwner, previewView, scanner) {
        val provider = cameraProvider
        var analysis: ImageAnalysis? = null
        if (provider != null && hasPermission) {
            try {
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { useCase ->
                        useCase.setAnalyzer(analyzerExecutor) { proxy ->
                            val mediaImage = proxy.image
                            if (mediaImage == null) {
                                proxy.close()
                            } else {
                                val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                                scanner.process(input)
                                    .addOnSuccessListener { barcodes ->
                                        val raw = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE && !it.rawValue.isNullOrBlank() }?.rawValue
                                        if (raw != null && delivered.compareAndSet(false, true)) {
                                            mainExecutor.execute { onCodeRead(raw) }
                                        }
                                    }
                                    .addOnFailureListener {
                                        // Keep scanning; a blurry frame must not interrupt the camera session.
                                    }
                                    .addOnCompleteListener { proxy.close() }
                            }
                        }
                    }
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                scanState = cameraQrText(language, "QR kodunu çerçevenin içine hizala.", "Align the QR code inside the frame.", "Richte den QR-Code im Rahmen aus.")
            } catch (error: Exception) {
                scanState = cameraQrText(language, "Kamera açılamadı. İzni ve kamera erişimini kontrol et.", "Camera could not start. Check permission and camera access.", "Kamera konnte nicht starten. Prüfe Berechtigung und Kamerazugriff.")
            }
        }
        onDispose {
            analysis?.clearAnalyzer()
            if (provider != null) provider.unbindAll()
            camera = null
        }
    }

    DisposableEffect(scanner, analyzerExecutor) {
        onDispose {
            runCatching { scanner.close() }
            analyzerExecutor.shutdown()
        }
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF080B0E)).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onClose, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.size(5.dp))
                Text(cameraQrText(language, "Kapat", "Close", "Schließen"))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(cameraQrText(language, "QR tarayıcı", "QR scanner", "QR-Scanner"), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White)
                Text(cameraQrText(language, "Kameradan canlı okuma", "Live camera scan", "Live-Kamerascanner"), style = MaterialTheme.typography.bodySmall, color = Color(0xFFA4AFB6))
            }
            Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF19C37D), modifier = Modifier.size(30.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color(0xFF10171B))) {
            if (hasPermission) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier.align(Alignment.Center).size(250.dp)
                        .border(3.dp, Color(0xFF24D18A), RoundedCornerShape(28.dp))
                )
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Color(0xDD080B0E)).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = scanState, label = "scanner-hint") { hint ->
                        Text(hint, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Column(
                    Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF19C37D), modifier = Modifier.size(46.dp))
                    Text(
                        cameraQrText(language, "QR taramak için kamera izni gerekiyor.", "Camera access is required to scan QR codes.", "Zum Scannen von QR-Codes ist Kamerazugriff erforderlich."),
                        color = Color.White
                    )
                    if (permissionDenied) Text(cameraQrText(language, "İzin verilmedi. Tekrar izin isteyebilir veya ayarlardan açabilirsin.", "Permission was denied. Try again or enable it in settings.", "Berechtigung verweigert. Bitte erneut versuchen oder in den Einstellungen aktivieren."), color = Color(0xFFA4AFB6))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text(cameraQrText(language, "Kamera izni ver", "Allow camera", "Kamerazugriff erlauben"))
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    camera?.let { boundCamera ->
                        torchEnabled = !torchEnabled
                        boundCamera.cameraControl.enableTorch(torchEnabled)
                    }
                },
                enabled = hasPermission && camera?.cameraInfo?.hasFlashUnit() == true,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(17.dp)
            ) { Text(if (torchEnabled) cameraQrText(language, "Flaşı kapat", "Turn flash off", "Blitz aus") else cameraQrText(language, "Flaşı aç", "Turn flash on", "Blitz an")) }
            Button(onClick = onClose, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp)) {
                Text(cameraQrText(language, "Geri dön", "Back", "Zurück"))
            }
        }

        Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF11171B))) {
            Text(
                cameraQrText(language, "QR okununca tarama otomatik durur. İşlem kaydedilmeden önce alıcı ve tutarı ekranda kontrol et.", "Scanning stops when a QR is read. Review the recipient and amount before saving any entry.", "Nach dem Lesen stoppt der Scanner. Prüfe Empfänger und Betrag vor dem Speichern."),
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                color = Color(0xFFA4AFB6),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
