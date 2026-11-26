package app.xpersiva.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.xpersiva.player.nucleo.EnlaceUnion
import app.xpersiva.player.nucleo.Perfil

/** Entrada a una experiencia: aquí aterrizan el escáner y los enlaces del QR. */
@Composable
fun PantallaEntrar(enlace: EnlaceUnion, perfil: Perfil, alEntrar: () -> Unit) {
    var nombre by remember { mutableStateOf(perfil.nombre) }
    val foco = remember { FocusRequester() }
    val limpio = nombre.trim()
    val entrar = {
        if (limpio.isNotEmpty()) {
            perfil.nombre = limpio
            alEntrar()
        }
    }
    LaunchedEffect(Unit) { if (nombre.isEmpty()) foco.requestFocus() }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(24.dp)) {
        Spacer(Modifier.weight(1f))
        Text("EXPERIENCIA ${enlace.codigo}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        Text("¿Cómo te llamas?", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it.take(24) },
            placeholder = { Text("Tu nombre en la partida") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { entrar() }),
            modifier = Modifier.fillMaxWidth().focusRequester(foco),
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = entrar, enabled = limpio.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Entrar")
        }
    }
}
