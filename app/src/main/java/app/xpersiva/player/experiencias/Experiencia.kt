package app.xpersiva.player.experiencias

import android.location.Location
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.text.Normalizer

/**
 * Una experiencia publicada en la web. La app no trae ninguna escrita:
 * las lee de experiences.json, así que crear una nueva no pasa por revisión.
 */
data class Experiencia(
    val id: String,
    val nombre: String,
    val marca: String?,
    val lugar: String,
    val ciudad: String?,
    val lat: Double,
    val lng: Double,
    val inicio: OffsetDateTime?,
    val fin: OffsetDateTime?,
    /** Horario del día (hora local), opcional: la campaña (`inicio`/`fin`) puede durar meses. */
    val horaApertura: LocalTime?,
    val horaCierre: LocalTime?,
    val descripcion: String?,
    val etiquetas: List<String>,
) {
    val subtitulo: String get() = listOfNotNull(marca, lugar, ciudad).joinToString(" · ")

    /** "10:00 – 20:00", o null si no tiene horario diario. */
    val horario: String?
        get() {
            if (horaApertura == null && horaCierre == null) return null
            val formato = DateTimeFormatter.ofPattern("HH:mm")
            return listOfNotNull(horaApertura?.format(formato), horaCierre?.format(formato)).joinToString(" – ")
        }

    /**
     * Dentro de la campaña (`inicio`/`fin`) y, si tiene horario diario, dentro
     * de él. Sin horario diario, una campaña larga (como el demo de portfolio,
     * que dura más de un año) estaría "en marcha" las 24 horas del día.
     */
    fun estaEnMarcha(ahora: OffsetDateTime = OffsetDateTime.now()): Boolean {
        if (inicio != null && ahora.isBefore(inicio)) return false
        if (fin != null && ahora.isAfter(fin)) return false
        val horaActual = ahora.toLocalTime()
        if (horaApertura != null && horaActual.isBefore(horaApertura)) return false
        if (horaCierre != null && horaActual.isAfter(horaCierre)) return false
        return true
    }

    fun haTerminado(ahora: OffsetDateTime = OffsetDateTime.now()): Boolean = fin?.isBefore(ahora) ?: false

    /** Metros desde un punto. */
    fun distanciaDesde(lat: Double, lng: Double): Float {
        val r = FloatArray(1)
        Location.distanceBetween(lat, lng, this.lat, this.lng, r)
        return r[0]
    }

    /** Todas las palabras de la búsqueda en algún campo, sin importar mayúsculas ni tildes. */
    fun coincide(busqueda: String): Boolean {
        val palabras = normalizar(busqueda).split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (palabras.isEmpty()) return true
        val texto = normalizar((listOfNotNull(nombre, marca, lugar, ciudad, descripcion) + etiquetas).joinToString(" "))
        return palabras.all { it in texto }
    }

    companion object {
        fun normalizar(s: String): String =
            Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

        fun desdeJson(o: JSONObject): Experiencia? = runCatching {
            Experiencia(
                id = o.getString("id"),
                nombre = o.getString("name"),
                marca = o.optStringOrNull("brand"),
                lugar = o.getString("venue"),
                ciudad = o.optStringOrNull("city"),
                lat = o.getDouble("lat"),
                lng = o.getDouble("lng"),
                inicio = o.optStringOrNull("startsAt")?.let(OffsetDateTime::parse),
                fin = o.optStringOrNull("endsAt")?.let(OffsetDateTime::parse),
                horaApertura = o.optStringOrNull("dailyOpensAt")?.let(LocalTime::parse),
                horaCierre = o.optStringOrNull("dailyClosesAt")?.let(LocalTime::parse),
                descripcion = o.optStringOrNull("description"),
                etiquetas = o.optJSONArray("tags")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList(),
            )
        }.getOrNull()

        fun formatoDistancia(metros: Float): String {
            val km = metros / 1000f
            return when {
                km < 1f -> "${metros.toInt()} m"
                km < 10f -> String.format(java.util.Locale.getDefault(), "%.1f km", km)
                else -> "${km.toInt()} km"
            }
        }
    }
}

private fun JSONObject.optStringOrNull(clave: String): String? =
    if (has(clave) && !isNull(clave)) getString(clave) else null
