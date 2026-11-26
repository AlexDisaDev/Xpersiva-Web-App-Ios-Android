package app.xpersiva.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.xpersiva.player.experiencias.CatalogoViewModel
import app.xpersiva.player.nucleo.EnlaceUnion
import app.xpersiva.player.nucleo.Perfil
import app.xpersiva.player.ui.*

class MainActivity : ComponentActivity() {
    /** Último enlace recibido (QR o web) que hay que abrir. */
    private val enlacePendiente = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enlacePendiente.value = intent?.data
        val perfil = Perfil(applicationContext)

        setContent {
            TemaXpersiva {
                val nav = rememberNavController()
                val catalogo: CatalogoViewModel = viewModel()
                var enlaceActual by remember { mutableStateOf<EnlaceUnion?>(null) }

                fun irAEntrar(enlace: EnlaceUnion) {
                    enlaceActual = enlace
                    nav.navigate("entrar") { popUpTo("inicio") }
                }

                // Enlaces del QR (https://<web>/j/…) y de la web (/e/<id>) con la app instalada.
                val pendiente by enlacePendiente
                LaunchedEffect(pendiente) {
                    val uri = pendiente ?: return@LaunchedEffect
                    enlacePendiente.value = null
                    abrirEnlace(uri, nav, ::irAEntrar)
                }

                NavHost(nav, startDestination = "inicio") {
                    composable("inicio") {
                        PantallaInicio(
                            catalogo,
                            alAbrirExperiencia = { nav.navigate("experiencia/$it") },
                            alEscanear = { nav.navigate("escaner") },
                        )
                    }
                    composable("experiencia/{id}") { entrada ->
                        PantallaExperiencia(entrada.arguments?.getString("id").orEmpty(), catalogo) { nav.navigate("escaner") }
                    }
                    composable("escaner") {
                        PantallaEscaner(alEncontrar = ::irAEntrar, alCancelar = { nav.popBackStack() })
                    }
                    composable("entrar") {
                        enlaceActual?.let { PantallaEntrar(it, perfil) { nav.navigate("mando") } }
                    }
                    composable("mando") {
                        enlaceActual?.let { PantallaMando(it, perfil.nombre) { nav.popBackStack("inicio", false) } }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        enlacePendiente.value = intent.data
    }

    private fun abrirEnlace(uri: Uri, nav: NavHostController, irAEntrar: (EnlaceUnion) -> Unit) {
        EnlaceUnion.desdeTexto(uri.toString())?.let { irAEntrar(it); return }
        val trozos = uri.pathSegments
        if (trozos.size >= 2 && trozos[0] == "e") nav.navigate("experiencia/${Uri.encode(trozos[1])}")
    }
}
