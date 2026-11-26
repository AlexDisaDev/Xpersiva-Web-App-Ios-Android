package app.xpersiva.player.juego

import app.xpersiva.player.nucleo.EnlaceUnion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Conexión con el plugin `websocket` del juego.
 *
 * Protocolo (el mismo que el mando web):
 *   app → juego  {"type":"hello","name","code"}
 *   juego → app  {"type":"joined","id"}
 *   app → juego  {"type":"input","steer","throttle","brake"}  hasta 30 veces por segundo
 */
class ConexionJuego(private val enlace: EnlaceUnion, private val nombre: String) {
    enum class Estado { CONECTANDO, CONECTADO, DENTRO, REINTENTANDO }

    private val _estado = MutableStateFlow(Estado.CONECTANDO)
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    @Volatile var volante = 0f
    @Volatile var acelerador = false
    @Volatile var freno = false

    private val http = OkHttpClient.Builder().pingInterval(15, TimeUnit.SECONDS).build()
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var socket: WebSocket? = null
    private var reintento: Job? = null
    private var ultimoEnviado = ""

    fun iniciar() {
        conectar()
        // Solo mandamos cuando algo cambió, igual que el mando web.
        ambito.launch {
            while (isActive) {
                delay(33)
                val s = socket ?: continue
                if (_estado.value != Estado.CONECTADO && _estado.value != Estado.DENTRO) continue
                val carga = JSONObject()
                    .put("type", "input")
                    .put("steer", (volante * 1000).roundToInt() / 1000.0)
                    .put("throttle", if (acelerador) 1 else 0)
                    .put("brake", if (freno) 1 else 0)
                    .toString()
                if (carga != ultimoEnviado && s.send(carga)) ultimoEnviado = carga
            }
        }
    }

    fun detener() {
        ambito.cancel()
        socket?.close(1001, "salir")
        socket = null
    }

    private fun conectar() {
        _estado.value = if (_estado.value == Estado.REINTENTANDO) Estado.REINTENTANDO else Estado.CONECTANDO
        http.newWebSocket(Request.Builder().url(enlace.urlSocket).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                socket = webSocket
                ultimoEnviado = ""
                webSocket.send(JSONObject().put("type", "hello").put("name", nombre).put("code", enlace.codigo).toString())
                _estado.value = Estado.CONECTADO
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val mensaje = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (mensaje.optString("type") == "joined") _estado.value = Estado.DENTRO
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.w("ConexionJuego", "cerrado por el juego: code=$code reason=$reason")
                perdida()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.w("ConexionJuego", "fallo de conexión: ${t.message}", t)
                perdida()
            }
        })
    }

    private fun perdida() {
        socket = null
        if (!ambito.isActive) return
        _estado.value = Estado.REINTENTANDO
        reintento?.cancel()
        reintento = ambito.launch {
            delay(1500)
            conectar()
        }
    }
}
