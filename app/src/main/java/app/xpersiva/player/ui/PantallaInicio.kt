package app.xpersiva.player.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.xpersiva.player.experiencias.CatalogoViewModel
import app.xpersiva.player.experiencias.Experiencia

/** Inicio: buscador de experiencias, de más cerca a más lejos. */
@Composable
fun PantallaInicio(
    catalogo: CatalogoViewModel,
    alAbrirExperiencia: (String) -> Unit,
    alEscanear: () -> Unit,
) {
    val estado by catalogo.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var busqueda by rememberSaveable { mutableStateOf("") }

    fun tienePermiso() = ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    var permiso by remember { mutableStateOf(tienePermiso()) }
    var denegado by remember { mutableStateOf(false) }
    val pedirUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        permiso = ok
        denegado = !ok
        if (ok) catalogo.localizar()
    }
    LaunchedEffect(Unit) { if (permiso) catalogo.localizar() }

    val lista = remember(estado, busqueda) { estado.visibles(busqueda) }

    Scaffold(
        bottomBar = {
            Button(
                onClick = alEscanear,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Escanear QR")
            }
        },
    ) { margen ->
        Column(Modifier.padding(margen).padding(horizontal = 16.dp)) {
            Text("xpersiva", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Experiencia, marca o ciudad") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (!permiso) {
                AssistChip(
                    onClick = {
                        if (denegado) {
                            contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", contexto.packageName, null)))
                        } else {
                            pedirUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = {
                        Text(if (denegado) "Activa la ubicación en Ajustes" else "Ver primero lo que tengo cerca")
                    },
                )
            }
            estado.error?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                TextButton(onClick = catalogo::cargar) { Text("Reintentar") }
            }
            if (estado.cargando && estado.todas.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(lista, key = { it.id }) { e ->
                    FilaExperiencia(e, estado.aqui?.let { Experiencia.formatoDistancia(e.distanciaDesde(it.lat, it.lng)) }) {
                        alAbrirExperiencia(e.id)
                    }
                    HorizontalDivider()
                }
                if (lista.isEmpty() && !estado.cargando && estado.error == null) {
                    item {
                        Text(
                            if (busqueda.isBlank()) "Aún no hay experiencias publicadas." else "Ninguna experiencia coincide con tu búsqueda.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaExperiencia(e: Experiencia, distancia: String?, alPulsar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = alPulsar).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(e.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(e.subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            distancia?.let { Text(it, fontWeight = FontWeight.Bold) }
            if (e.estaEnMarcha()) Text("AHORA", color = Colores.Ahora, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
        }
    }
}
