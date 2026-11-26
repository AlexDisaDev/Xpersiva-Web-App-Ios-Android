package app.xpersiva.player.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color as ColorCompose

/** Colores de la marca (rama `brand`, `brand/INSTALL_FOR_CLAUDE.md`). Iguales en `ios/v1`. */
object Colores {
    val Acento = ColorCompose(0xFF6D4AFF)
    val Cian = ColorCompose(0xFF42E3F2)
    val Noche = ColorCompose(0xFF090B1E)
    val Fondo = ColorCompose(0xFFF5F6FA)
    val Texto = ColorCompose(0xFF111318)
    val TextoSuave = ColorCompose(0xFF5B6170)
    val Borde = ColorCompose(0xFFE3E5EC)
    val AcentoSuave = ColorCompose(0xFFECE8FF)
    val Ahora = ColorCompose(0xFF1F9D55)
    val Malo = ColorCompose(0xFFD1434A)
}

@Composable
fun TemaXpersiva(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Colores.Acento,
            onPrimary = ColorCompose.White,
            primaryContainer = Colores.AcentoSuave,
            onPrimaryContainer = Colores.Acento,
            secondary = Colores.Cian,
            onSecondary = Colores.Noche,
            background = Colores.Fondo,
            onBackground = Colores.Texto,
            surface = ColorCompose.White,
            onSurface = Colores.Texto,
            surfaceVariant = ColorCompose(0xFFE9EBF2), // pedales sin pulsar: se tienen que ver sobre el fondo
            onSurfaceVariant = Colores.TextoSuave,
            outline = Colores.Borde,
            outlineVariant = Colores.Borde,
            error = Colores.Malo,
        ),
        content = contenido,
    )
}
