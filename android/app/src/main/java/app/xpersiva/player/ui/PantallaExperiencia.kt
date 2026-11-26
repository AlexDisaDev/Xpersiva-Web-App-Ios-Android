package app.xpersiva.player.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.xpersiva.player.experiencias.CatalogoViewModel
import app.xpersiva.player.experiencias.Experiencia
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ficha de una experiencia. También se abre con <web>/e/<id>. */
@Composable
fun PantallaExperiencia(id: String, catalogo: CatalogoViewModel, alEscanear: () -> Unit) {
    val estado by catalogo.estado.collectAsStateWithLifecycle()
    val e = estado.todas.firstOrNull { it.id == id }
    val contexto = LocalContext.current

    if (e == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (estado.cargando) CircularProgressIndicator()
            else Text("Esta experiencia ya no está disponible", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val formato = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val fechas = listOfNotNull(e.inicio, e.fin).joinToString(" – ") { it.format(formato) }
    val distancia = estado.aqui?.let { Experiencia.formatoDistancia(e.distanciaDesde(it.lat, it.lng)) }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            e.marca?.let {
                Text(it.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            }
            Text(e.nombre, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(listOfNotNull(e.lugar, e.ciudad, distancia).joinToString(" · "))
            if (fechas.isNotEmpty()) Text(fechas + if (e.estaEnMarcha()) " · ahora" else "")
            e.descripcion?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedButton(
            onClick = {
                val uri = Uri.parse("geo:${e.lat},${e.lng}?q=${e.lat},${e.lng}(${Uri.encode(e.nombre)})")
                contexto.startActivity(Intent(Intent.ACTION_VIEW, uri))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Cómo llegar") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = alEscanear, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Estoy allí · escanear QR")
        }
    }
}
