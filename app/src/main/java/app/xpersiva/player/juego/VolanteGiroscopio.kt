package app.xpersiva.player.juego

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.max
import kotlin.math.min

/**
 * Convierte el giro del teléfono en un valor de volante -1...1.
 *
 * Igual que en iOS: se guarda la postura del teléfono al empezar como
 * "centro" en vez de un ángulo absoluto fijo, así da igual cómo sujete cada
 * uno el móvil. Los ejes del sensor se remapean según en qué lado esté el
 * teléfono en horizontal, porque `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`
 * permite cualquiera de los dos giros.
 */
class VolanteGiroscopio(private val actividad: Activity) {
    private val sensorManager = actividad.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var referencia: Float? = null
    private var listener: SensorEventListener? = null

    fun iniciar(alCambiar: (Float) -> Unit) {
        val sensorActual = sensor ?: return
        referencia = null
        val matriz = FloatArray(9)
        val remapada = FloatArray(9)
        val orientacion = FloatArray(3)
        val l = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matriz, evento.values)
                @Suppress("DEPRECATION")
                val rotacionPantalla = actividad.windowManager.defaultDisplay.rotation
                val (ejeX, ejeY) = if (rotacionPantalla == Surface.ROTATION_270) {
                    SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                } else {
                    // ROTATION_90, el giro habitual, y por defecto.
                    SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                }
                SensorManager.remapCoordinateSystem(matriz, ejeX, ejeY, remapada)
                SensorManager.getOrientation(remapada, orientacion)
                val giro = orientacion[2]
                if (referencia == null) referencia = giro
                var relativo = giro - referencia!!
                while (relativo > Math.PI) relativo -= (2 * Math.PI).toFloat()
                while (relativo < -Math.PI) relativo += (2 * Math.PI).toFloat()
                // ±45º de giro desde la postura inicial = tope del volante.
                alCambiar(min(1f, max(-1f, relativo / (Math.PI.toFloat() / 4f))))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        listener = l
        sensorManager.registerListener(l, sensorActual, SensorManager.SENSOR_DELAY_GAME)
    }

    fun detener() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
        referencia = null
    }
}
