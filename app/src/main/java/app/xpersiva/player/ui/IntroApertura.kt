package app.xpersiva.player.ui

import android.graphics.Color as ColorAndroid
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.xpersiva.player.R
import kotlinx.coroutines.delay

/**
 * Vídeo de apertura (`res/raw/startup_intro.mp4`, 3,4 s, sin sonido), una vez por
 * arranque. Su primer y último fotograma son del mismo azul noche que el fondo
 * de arranque del sistema, así que no se nota el paso. Con "quitar animaciones"
 * activado en Accesibilidad se muestra solo el símbolo un momento. Tocar lo salta.
 */
@Composable
fun IntroApertura(alTerminar: () -> Unit) {
    val contexto = LocalContext.current
    val terminar by rememberUpdatedState(alTerminar)
    val sinMovimiento = remember {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    // Por si el vídeo no llega a terminar (la app pasa a segundo plano, el códec falla...).
    LaunchedEffect(Unit) {
        delay(if (sinMovimiento) 800 else 6000)
        terminar()
    }

    Box(
        Modifier.fillMaxSize()
            .background(Colores.Noche)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Saltar la introducción",
            ) { terminar() },
        contentAlignment = Alignment.Center,
    ) {
        if (sinMovimiento) {
            Image(painterResource(R.drawable.simbolo_arranque), contentDescription = "Xpersiva", modifier = Modifier.size(160.dp))
        } else {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        // Que no pare la música de quien abre la app: el vídeo no tiene sonido.
                        setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                        // Hasta el primer fotograma, azul noche en vez del negro de la superficie.
                        setBackgroundColor(ColorAndroid.rgb(0x09, 0x0B, 0x1E))
                        setOnInfoListener { _, que, _ ->
                            if (que == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) setBackgroundColor(ColorAndroid.TRANSPARENT)
                            false
                        }
                        setOnCompletionListener { terminar() }
                        setOnErrorListener { _, _, _ -> terminar(); true }
                        setVideoURI(Uri.parse("android.resource://${ctx.packageName}/${R.raw.startup_intro}"))
                        start()
                    }
                },
                onRelease = { it.stopPlayback() },
                modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f),
            )
        }
    }
}
