package app.xpersiva.player.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.xpersiva.player.R
import app.xpersiva.player.experiencias.CatalogoViewModel
import app.xpersiva.player.experiencias.Experiencia

/**
 * Inicio: logo arriba sobre fondo claro, bloque de bienvenida azul noche con el
 * buscador, filtros ("Cerca de mí" pide la ubicación solo al pulsarlo, "En
 * directo" deja las que están en marcha) y las experiencias de la web en
 * tarjetas. El botón "Escanear QR" queda siempre a mano abajo.
 */
@Composable
fun PantallaInicio(
    catalogo: CatalogoViewModel,
    alAbrirExperiencia: (String) -> Unit,
    alEscanear: () -> Unit,
) {
    val estado by catalogo.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var busqueda by rememberSaveable { mutableStateOf("") }
    var soloEnDirecto by rememberSaveable { mutableStateOf(false) }

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

    val lista = remember(estado, busqueda, soloEnDirecto) {
        estado.visibles(busqueda).let { l -> if (soloEnDirecto) l.filter { it.estaEnMarcha() } else l }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = Color.White) {
                Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Image(
                        painterResource(R.drawable.logo_cabecera),
                        contentDescription = "Xpersiva",
                        modifier = Modifier.height(30.dp),
                    )
                }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                BotonEscanear(alEscanear, Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp))
            }
        },
    ) { margen ->
        LazyColumn(
            Modifier.fillMaxSize().padding(margen),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "bienvenida") { Bienvenida(busqueda) { busqueda = it } }

            item(key = "filtros") {
                Column(Modifier.padding(top = 12.dp)) {
                    Text(
                        "Explora experiencias",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Filtro(
                            texto = if (denegado && !permiso) "Activar ubicación" else "Cerca de mí",
                            activo = permiso,
                            icono = { Icon(Icons.Default.LocationOn, contentDescription = null, Modifier.size(18.dp)) },
                        ) {
                            when {
                                permiso -> catalogo.localizar()
                                denegado -> contexto.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
                                )
                                else -> pedirUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        }
                        Filtro(
                            texto = "En directo",
                            activo = soloEnDirecto,
                            icono = { PuntoEnDirecto(if (soloEnDirecto) Color.White else Colores.Ahora) },
                        ) { soloEnDirecto = !soloEnDirecto }
                    }
                    if (denegado) {
                        Text(
                            "Sin ubicación las verás igual, ordenadas por fecha.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            estado.error?.let { error ->
                item(key = "error") {
                    Column {
                        Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = catalogo::cargar) { Text("Reintentar") }
                    }
                }
            }

            if (estado.cargando && estado.todas.isEmpty()) {
                item(key = "cargando") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }

            items(lista, key = { it.id }) { e ->
                TarjetaExperiencia(e, estado.aqui?.let { Experiencia.formatoDistancia(e.distanciaDesde(it.lat, it.lng)) }) {
                    alAbrirExperiencia(e.id)
                }
            }

            if (lista.isEmpty() && !estado.cargando && estado.error == null) {
                item(key = "vacio") {
                    Text(
                        when {
                            busqueda.isNotBlank() -> "Ninguna experiencia coincide con tu búsqueda."
                            soloEnDirecto -> "Ahora mismo no hay ninguna experiencia en directo."
                            else -> "Aún no hay experiencias publicadas."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
        }
    }
}

/** Bloque azul noche: frase de marca y buscador. */
@Composable
private fun Bienvenida(busqueda: String, alCambiar: (String) -> Unit) {
    Surface(color = Colores.Noche, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                "EXPERIENCIAS INMERSIVAS",
                color = Colores.Cian,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "El evento empieza contigo.",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Escanea el QR del evento y tu móvil se convierte en el mando.",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = busqueda,
                onValueChange = alCambiar,
                placeholder = { Text("Busca una experiencia o ciudad") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Colores.Cian,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Colores.Texto,
                    unfocusedTextColor = Colores.Texto,
                    focusedPlaceholderColor = Colores.TextoSuave,
                    unfocusedPlaceholderColor = Colores.TextoSuave,
                    focusedLeadingIconColor = Colores.Acento,
                    unfocusedLeadingIconColor = Colores.TextoSuave,
                    cursorColor = Colores.Acento,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Píldora de filtro: violeta lleno si está activo, violeta suave si no. */
@Composable
private fun Filtro(texto: String, activo: Boolean, icono: @Composable () -> Unit, alPulsar: () -> Unit) {
    FilterChip(
        selected = activo,
        onClick = alPulsar,
        label = { Text(texto, fontWeight = FontWeight.SemiBold) },
        leadingIcon = icono,
        shape = CircleShape,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Colores.AcentoSuave,
            labelColor = Colores.Acento,
            iconColor = Colores.Acento,
            selectedContainerColor = Colores.Acento,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White,
        ),
        modifier = Modifier.heightIn(min = 40.dp),
    )
}

@Composable
private fun PuntoEnDirecto(color: Color) {
    Box(Modifier.size(8.dp).background(color, CircleShape))
}

@Composable
private fun TarjetaExperiencia(e: Experiencia, distancia: String?, alPulsar: () -> Unit) {
    Surface(
        onClick = alPulsar,
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Colores.Borde),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Inicial de la marca (o del nombre) en un cuadro violeta suave: da ritmo a la lista sin inventar imágenes.
            Box(
                Modifier.size(48.dp).background(Colores.AcentoSuave, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (e.marca ?: e.nombre).take(1).uppercase(),
                    color = Colores.Acento,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (e.estaEnMarcha()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PuntoEnDirecto(Colores.Ahora)
                        Spacer(Modifier.width(6.dp))
                        Text("EN DIRECTO", color = Colores.Ahora, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Text(e.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(e.subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                distancia?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.labelLarge, color = Colores.Acento, fontWeight = FontWeight.Bold)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Colores.TextoSuave)
        }
    }
}

/** Botón principal de la app, el mismo abajo del inicio y en la ficha. */
@Composable
fun BotonEscanear(alPulsar: () -> Unit, modifier: Modifier = Modifier, texto: String = "ESCANEAR QR") {
    Button(
        onClick = alPulsar,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.fillMaxWidth().height(60.dp),
    ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(texto, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}
