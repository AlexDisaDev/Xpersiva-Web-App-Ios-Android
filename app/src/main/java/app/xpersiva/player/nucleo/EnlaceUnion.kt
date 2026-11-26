package app.xpersiva.player.nucleo

import java.net.URI

/**
 * Qué pantalla de control mostrar para una experiencia.
 *
 * Lo decide el propio QR (parámetro `c`), no la versión de la app: así cada
 * evento elige entre las plantillas YA incluidas en la app sin pedir una
 * actualización. Añadir una plantilla nueva (un tercer valor aquí) sí sigue
 * necesitando código nuevo y pasar por revisión; elegir entre las que ya
 * existen, no.
 */
enum class TipoControl {
    /** Volante + pedales (el mando completo de conducción). */
    VOLANTE,
    /** Un único botón grande (p. ej. una experiencia de "pulsa para jugar"). */
    BOTON,
    ;

    companion object {
        /** `null` o un valor que no reconocemos cae en VOLANTE por compatibilidad. */
        fun desdeParametro(valor: String?): TipoControl =
            entries.firstOrNull { it.name.equals(valor, ignoreCase = true) } ?: VOLANTE
    }
}

/**
 * A qué experiencia hay que unirse, sacado de un QR o de un enlace.
 *
 * Formatos aceptados:
 *   https://<web>/j/F1?h=192.168.0.17:8080&c=boton   (QR recomendado; c es opcional)
 *   xpersiva://j/F1?h=192.168.0.17:8080              (esquema propio)
 *   http://192.168.0.17:8080                         (QR actual del plugin, solo red local)
 */
data class EnlaceUnion(
    /** Código de la experiencia. "LAN" si el QR no traía ninguno. */
    val codigo: String,
    /** host:puerto del PC que corre el juego. */
    val host: String,
    /** true para wss:// (juego detrás de un túnel con TLS). */
    val seguro: Boolean = false,
    /** Qué pantalla de control mostrar en esta experiencia. */
    val control: TipoControl = TipoControl.VOLANTE,
) {
    val urlSocket: String get() = "${if (seguro) "wss" else "ws"}://$host"

    companion object {
        private val HOST = Regex("^[A-Za-z0-9.-]+(:\\d{1,5})?$")
        private val CODIGO = Regex("^[A-Za-z0-9][A-Za-z0-9-]{1,31}$")

        fun crear(
            codigo: String,
            host: String?,
            seguro: Boolean = false,
            control: TipoControl = TipoControl.VOLANTE,
        ): EnlaceUnion? {
            if (host == null || !HOST.matches(host) || !CODIGO.matches(codigo)) return null
            return EnlaceUnion(codigo.uppercase(), host, seguro, control)
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
                return crear(
                    trozos[1],
                    consulta["h"],
                    consulta["s"] == "1",
                    TipoControl.desdeParametro(consulta["c"]),
                )
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
