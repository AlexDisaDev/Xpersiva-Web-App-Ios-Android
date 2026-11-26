package app.xpersiva.player.experiencias

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.xpersiva.player.nucleo.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class Posicion(val lat: Double, val lng: Double)

data class EstadoCatalogo(
    val todas: List<Experiencia> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val aqui: Posicion? = null,
) {
    /** Con ubicación: de más cerca a más lejos. Sin ella: primero las que están en marcha. */
    fun visibles(busqueda: String): List<Experiencia> {
        val filtradas = todas.filter { it.coincide(busqueda) }
        return if (aqui != null) {
            filtradas.sortedBy { it.distanciaDesde(aqui.lat, aqui.lng) }
        } else {
            filtradas.sortedWith(compareByDescending<Experiencia> { it.estaEnMarcha() }.thenBy { it.inicio })
        }
    }
}

class CatalogoViewModel(app: Application) : AndroidViewModel(app) {
    private val http = OkHttpClient()
    private val _estado = MutableStateFlow(EstadoCatalogo())
    val estado: StateFlow<EstadoCatalogo> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.value = _estado.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            val resultado = runCatching {
                withContext(Dispatchers.IO) {
                    http.newCall(Request.Builder().url(Config.URL_EXPERIENCIAS).build()).execute().use { r ->
                        check(r.isSuccessful) { "HTTP ${r.code}" }
                        val lista = JSONObject(r.body!!.string()).getJSONArray("experiences")
                        List(lista.length()) { Experiencia.desdeJson(lista.getJSONObject(it)) }
                            .filterNotNull()
                            .filterNot { it.haTerminado() }
                    }
                }
            }
            _estado.value = resultado.fold(
                onSuccess = { _estado.value.copy(todas = it, cargando = false) },
                onFailure = {
                    _estado.value.copy(cargando = false, error = "No hemos podido cargar las experiencias. Revisa tu conexión.")
                },
            )
        }
    }

    /**
     * Una sola lectura de ubicación aproximada. Llamar solo con el permiso concedido.
     *
     * Usamos la API clásica de [LocationManager] (no `LocationManagerCompat.getCurrentLocation`):
     * en este proyecto esa llamada da "overload resolution ambiguity" porque hay dos versiones
     * de `androidx.core` en el classpath con la misma firma, y evitarla es más simple que
     * resolver el conflicto de dependencias.
     */
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun localizar() {
        val lm = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val proveedor = if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER
        else LocationManager.PASSIVE_PROVIDER
        lm.getLastKnownLocation(proveedor)?.let { _estado.value = _estado.value.copy(aqui = Posicion(it.latitude, it.longitude)) }
        lm.requestSingleUpdate(
            proveedor,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    _estado.value = _estado.value.copy(aqui = Posicion(location.latitude, location.longitude))
                }
            },
            Looper.getMainLooper(),
        )
    }
}
