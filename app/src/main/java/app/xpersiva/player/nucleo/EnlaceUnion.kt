package app.xpersiva.player.nucleo

import java.net.URI

/**
 * A qué experiencia hay que unirse, sacado de un QR o de un enlace.
 *
 * Formatos aceptados:
 *   https://<web>/j/F1?h=192.168.0.17:8080   (QR recomendado)
 *   xpersiva://j/F1?h=192.168.0.17:8080      (esquema propio)
 *   http://192.168.0.17:8080                 (QR actual del plugin, solo red local)
 */
data class EnlaceUnion(
    /** Código de la experiencia. "LAN" si el QR no traía ninguno. */
    val codigo: String,
    /** host:puerto del PC que corre el juego. */
    val host: String,
    /** true para wss:// (juego detrás de un túnel con TLS). */
    val seguro: Boolean = false,
) {
    val urlSocket: String get() = "${if (seguro) "wss" else "ws"}://$host"

    companion object {
        private val HOST = Regex("^[A-Za-z0-9.-]+(:\\d{1,5})?$")
        private val CODIGO = Regex("^[A-Za-z0-9][A-Za-z0-9-]{1,31}$")

        fun crear(codigo: String, host: String?, seguro: Boolean = false): EnlaceUnion? {
            if (host == null || !HOST.matches(host) || !CODIGO.matches(codigo)) return null
            return EnlaceUnion(codigo.uppercase(), host, seguro)
        }

        fun desdeTexto(texto: String): EnlaceUnion? {
            val uri = runCatching { URI(texto.trim()) }.getOrNull() ?: return null
            val esquema = uri.scheme?.lowercase() ?: return null
            val consulta = (uri.rawQuery ?: "").split("&").mapNotNull {
                val partes = it.split("=", limit = 2)
                if (partes.size == 2) partes[0] to java.net.URLDecoder.decode(partes[1], "UTF-8") else null
            }.toMap()

            val esNuestro = esquema == Config.ESQUEMA || (esquema == "https" && uri.host == Config.HOST_WEB)
            if (esNuestro) {
                // En xpersiva://j/F1 la "j" llega como host; en https llega en la ruta.
                val trozos = (listOf(if (esquema == Config.ESQUEMA) uri.host ?: "" else "") +
                    (uri.path ?: "").split("/")).filter { it.isNotEmpty() }
                if (trozos.size < 2 || trozos[0] != "j") return null
                return crear(trozos[1], consulta["h"], consulta["s"] == "1")
            }

            // QR antiguo del plugin: solo si apunta a la red local.
            val host = uri.host
            if ((esquema == "http" || esquema == "https") && host != null && esIpPrivada(host)) {
                val conPuerto = if (uri.port > 0) "$host:${uri.port}" else host
                return crear("LAN", conPuerto, esquema == "https")
            }
            return null
        }

        fun esIpPrivada(host: String): Boolean {
            val n = host.split(".").mapNotNull { it.toIntOrNull() }
            if (n.size != 4) return false
            return n[0] == 10 || (n[0] == 192 && n[1] == 168) || (n[0] == 172 && n[1] in 16..31)
        }
    }
}
