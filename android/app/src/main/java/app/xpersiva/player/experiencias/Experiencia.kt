package app.xpersiva.player.experiencias

import android.location.Location
import org.json.JSONObject
import java.time.OffsetDateTime
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
    val descripcion: String?,
    val etiquetas: List<String>,
) {
    val subtitulo: String get() = listOfNotNull(marca, lugar, ciudad).joinToString(" · ")

    fun estaEnMarcha(ahora: OffsetDateTime = OffsetDateTime.now()): Boolean =
        (inicio == null || !ahora.isBefore(inicio)) && (fin == null || !ahora.isAfter(fin))

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
