package app.xpersiva.player.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            // Cabecera azul noche, como el bloque de bienvenida del inicio.
            Surface(color = Colores.Noche, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    e.marca?.let {
                        Text(it.uppercase(), color = Colores.Cian, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(e.nombre, color = Color.White, style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                    if (e.estaEnMarcha()) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.background(Colores.Ahora, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp).background(Color.White, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("EN DIRECTO", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Colores.Borde)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dato("Dónde", listOfNotNull(e.lugar, e.ciudad, distancia).joinToString(" · "))
                    if (fechas.isNotEmpty()) Dato("Cuándo", fechas)
                }
            }
            e.descripcion?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                val uri = Uri.parse("geo:${e.lat},${e.lng}?q=${e.lat},${e.lng}(${Uri.encode(e.nombre)})")
                contexto.startActivity(Intent(Intent.ACTION_VIEW, uri))
            },
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, Colores.Acento),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Default.Directions, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cómo llegar", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        BotonEscanear(alEscanear, texto = "ESTOY ALLÍ · ESCANEAR QR")
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta.uppercase(), style = MaterialTheme.typography.labelSmall, color = Colores.TextoSuave,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
