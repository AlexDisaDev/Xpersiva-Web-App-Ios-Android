package app.xpersiva.player.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color as ColorCompose

/** Tema claro y neutro: la app es generalista, cada experiencia pone su marca. */
object Colores {
    val Acento = ColorCompose(0xFF6D4AFF)
    val Ahora = ColorCompose(0xFF1F9D55)
}

@Composable
fun TemaXpersiva(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Colores.Acento, background = ColorCompose.White, surface = ColorCompose.White),
        content = contenido,
    )
}
