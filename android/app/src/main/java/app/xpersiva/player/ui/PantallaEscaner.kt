package app.xpersiva.player.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.xpersiva.player.nucleo.EnlaceUnion
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Escáner de QR. La primera vez explica para qué es la cámara antes de que
 * salga el aviso del sistema.
 */
@Composable
fun PantallaEscaner(alEncontrar: (EnlaceUnion) -> Unit, alCancelar: () -> Unit) {
    val contexto = LocalContext.current
    var permiso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denegado by remember { mutableStateOf(false) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        permiso = ok
        denegado = !ok
    }

    if (!permiso) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Necesitamos la cámara", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Solo para leer el código QR de la experiencia. No guardamos fotos ni vídeo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (denegado) {
                        contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", contexto.packageName, null)))
                    } else {
                        pedir.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (denegado) "Abrir ajustes" else "Permitir cámara") }
            TextButton(onClick = alCancelar, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        }
        return
    }

    var aviso by remember { mutableStateOf<String?>(null) }
    var hecho by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CamaraQr { texto ->
            if (hecho) return@CamaraQr
            val enlace = EnlaceUnion.desdeTexto(texto)
            if (enlace != null) {
                hecho = true
                alEncontrar(enlace)
            } else {
                aviso = "Este QR no es de una experiencia Xpersiva."
            }
        }
        Box(
            Modifier.align(Alignment.Center).size(240.dp)
                .border(BorderStroke(3.dp, Color.White), RoundedCornerShape(24.dp)),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(aviso ?: "Apunta al QR de la pantalla", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = alCancelar) { Text("Cancelar", color = Color.White) }
        }
    }
}

/** Vista previa de CameraX que pasa cada fotograma por el lector de QR de ML Kit. */
@Composable
private fun CamaraQr(alLeer: (String) -> Unit) {
    val ciclo = LocalLifecycleOwner.current
    val lectorActual by rememberUpdatedState(alLeer)
    val hilo = remember { Executors.newSingleThreadExecutor() }
    val lector = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    DisposableEffect(Unit) {
        onDispose {
            lector.close()
            hilo.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val vista = PreviewView(ctx)
            val futuro = ProcessCameraProvider.getInstance(ctx)
            futuro.addListener({
                val proveedor = futuro.get()
                val vistaPrevia = Preview.Builder().build().also { it.surfaceProvider = vista.surfaceProvider }
                val analisis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analisis.setAnalyzer(hilo) { imagen ->
                    val media = imagen.image
                    if (media == null) { imagen.close(); return@setAnalyzer }
                    lector.process(InputImage.fromMediaImage(media, imagen.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codigos -> codigos.firstNotNullOfOrNull { it.rawValue }?.let { lectorActual(it) } }
                        .addOnCompleteListener { imagen.close() }
                }
                proveedor.unbindAll()
                proveedor.bindToLifecycle(ciclo, CameraSelector.DEFAULT_BACK_CAMERA, vistaPrevia, analisis)
            }, ContextCompat.getMainExecutor(ctx))
            vista
        },
    )
}
