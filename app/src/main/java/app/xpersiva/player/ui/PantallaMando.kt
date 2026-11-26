package app.xpersiva.player.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.xpersiva.player.juego.ConexionJuego
import app.xpersiva.player.juego.ConexionJuego.Estado
import app.xpersiva.player.juego.VolanteGiroscopio
import app.xpersiva.player.nucleo.EnlaceUnion
import app.xpersiva.player.nucleo.TipoControl

/**
 * El mando. Cada zona tiene su propio pointerInput, así que Compose deja usar
 * varias a la vez: girar con un dedo y acelerar con otro.
 */
@Composable
fun PantallaMando(enlace: EnlaceUnion, nombre: String, alSalir: () -> Unit) {
    val conexion = remember(enlace) { ConexionJuego(enlace, nombre) }
    val estado by conexion.estado.collectAsStateWithLifecycle()
    val actividad = LocalContext.current as? Activity
    val esGiroscopio = enlace.control == TipoControl.GIROSCOPIO

    DisposableEffect(conexion) {
        conexion.iniciar()
        actividad?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // que no se apague jugando
        onDispose {
            conexion.detener()
            actividad?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // La app es de retrato; solo el mando por giroscopio necesita horizontal,
    // y solo mientras esta pantalla está abierta.
    var giroVisual by remember { mutableFloatStateOf(0f) }
    if (esGiroscopio) {
        DisposableEffect(actividad) {
            val giro = actividad?.let(::VolanteGiroscopio)
            actividad?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            giro?.iniciar {
                conexion.volante = it
                giroVisual = it // conexion.volante es @Volatile, no recompone: el indicador lee este sí
            }
            onDispose {
                giro?.detener()
                actividad?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    val cabecera: @Composable (mostrarSalir: Boolean) -> Unit = { mostrarSalir ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(9.dp).background(if (estado == Estado.DENTRO) Colores.Ahora else Colores.Malo, CircleShape))
            Text("$nombre · ${textoEstado(estado)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (mostrarSalir) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = alSalir) { Text("Salir") }
            }
        }
    }

    if (esGiroscopio) {
        // El giro ya controla el volante: aquí solo hace falta el indicador
        // y gas/freno, uno a cada lado para no taparse con los pulgares.
        Row(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pedal("FRENO", Modifier.weight(0.3f)) { conexion.freno = it }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cabecera(true)
                IndicadorGiroscopio(Modifier.weight(1f), giroVisual)
            }
            Pedal("GAS", Modifier.weight(0.3f)) { conexion.acelerador = it }
        }
        return
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cabecera(false)
        when (enlace.control) {
            TipoControl.VOLANTE -> {
                Volante(Modifier.weight(1f)) { conexion.volante = it }
                Row(Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pedal("FRENO", Modifier.weight(1f)) { conexion.freno = it }
                    Pedal("GAS", Modifier.weight(1f)) { conexion.acelerador = it }
                }
            }
            // Experiencias que solo necesitan "pulsa para jugar": reusa `acelerador`
            // (el `throttle` del protocolo) como señal de pulsado/soltado.
            TipoControl.BOTON -> Pedal("PULSA", Modifier.weight(1f)) { conexion.acelerador = it }
            TipoControl.GIROSCOPIO -> Unit // rama aparte arriba, nunca llega aquí.
        }
        OutlinedButton(onClick = alSalir, modifier = Modifier.fillMaxWidth()) { Text("Salir") }
    }
}

private fun textoEstado(e: Estado) = when (e) {
    Estado.CONECTANDO -> "conectando…"
    Estado.CONECTADO -> "conectado, esperando a la experiencia…"
    Estado.DENTRO -> "dentro"
    Estado.REINTENTANDO -> "sin conexión, reintentando…"
}

@Composable
private fun Volante(modifier: Modifier, alCambiar: (Float) -> Unit) {
    var ancho by remember { mutableIntStateOf(1) }
    var valor by remember { mutableFloatStateOf(0f) }
    fun fijar(x: Float) {
        valor = ((x / ancho) * 2f - 1f).coerceIn(-1f, 1f)
        alCambiar(valor)
    }

    BoxWithConstraints(
        modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .onSizeChanged { ancho = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val dedo = awaitFirstDown(requireUnconsumed = false)
                    fijar(dedo.position.x)
                    do {
                        val evento = awaitPointerEvent()
                        val cambio = evento.changes.firstOrNull { it.id == dedo.id } ?: break
                        fijar(cambio.position.x)
                    } while (cambio.pressed)
                    valor = 0f
                    alCambiar(0f)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("arrastra para girar", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            Modifier.align(Alignment.CenterStart)
                .offset(x = maxWidth * ((valor + 1f) / 2f) - 2.dp)
                .width(4.dp).fillMaxHeight().padding(vertical = 10.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
    }
}

/** Solo lectura: el giro lo manda [VolanteGiroscopio], aquí no hay gesto de arrastre. */
@Composable
private fun IndicadorGiroscopio(modifier: Modifier, valor: Float) {
    BoxWithConstraints(
        modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("inclina el móvil para girar", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            Modifier.align(Alignment.CenterStart)
                .offset(x = maxWidth * ((valor + 1f) / 2f) - 2.dp)
                .width(4.dp).fillMaxHeight().padding(vertical = 10.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun Pedal(titulo: String, modifier: Modifier, alCambiar: (Boolean) -> Unit) {
    var pulsado by remember { mutableStateOf(false) }
    val vista = LocalView.current
    Box(
        modifier.fillMaxHeight()
            .background(
                if (pulsado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp),
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val dedo = awaitFirstDown(requireUnconsumed = false)
                    pulsado = true
                    alCambiar(true)
                    vista.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    do {
                        val evento = awaitPointerEvent()
                        val cambio = evento.changes.firstOrNull { it.id == dedo.id } ?: break
                    } while (cambio.pressed)
                    pulsado = false
                    alCambiar(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(titulo, fontWeight = FontWeight.Bold, color = if (pulsado) Color.White else MaterialTheme.colorScheme.onSurface)
    }
}
