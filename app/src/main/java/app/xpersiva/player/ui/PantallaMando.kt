package app.xpersiva.player.ui

import android.app.Activity
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
    val ventana = (LocalContext.current as? Activity)?.window

    DisposableEffect(conexion) {
        conexion.iniciar()
        ventana?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // que no se apague jugando
        onDispose {
            conexion.detener()
            ventana?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(9.dp).background(if (estado == Estado.DENTRO) Colores.Ahora else Color(0xFFD1434A), CircleShape))
            Text("$nombre · ${textoEstado(estado)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
