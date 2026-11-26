package app.xpersiva.player.nucleo

import android.content.Context

/** Lo que el jugador nos deja saber de él. Solo el nombre, de momento. */
class Perfil(context: Context) {
    private val prefs = context.getSharedPreferences("xpersiva", Context.MODE_PRIVATE)

    var nombre: String
        get() = prefs.getString("nombre", "") ?: ""
        set(valor) = prefs.edit().putString("nombre", valor.take(24)).apply()
}
