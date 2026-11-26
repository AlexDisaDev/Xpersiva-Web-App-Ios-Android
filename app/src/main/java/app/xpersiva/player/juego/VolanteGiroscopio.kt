package app.xpersiva.player.juego

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.atan2

/**
 * Convierte el giro del teléfono en un valor de volante -1...1.
 *
 * Se sujeta en horizontal y se gira como un volante: lo que cuenta es hacia
 * dónde cae la gravedad dentro del plano de la pantalla. Inclinarlo hacia
 * delante o hacia atrás no cambia ese ángulo, así que no gira el coche.
 */
class VolanteGiroscopio(private val actividad: Activity) {
    private val sensorManager = actividad.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var listener: SensorEventListener? = null

    fun iniciar(alCambiar: (Float) -> Unit) {
        val sensorActual = sensor ?: return
        var x = 0f
        var y = 0f
        var primera = true
        val l = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                // Suavizado ligero; con el acelerómetro de reserva hace falta, con el
                // de gravedad casi no se nota.
                if (primera) {
                    x = evento.values[0]; y = evento.values[1]; primera = false
                } else {
                    x += (evento.values[0] - x) * 0.3f
                    y += (evento.values[1] - y) * 0.3f
                }
                @Suppress("DEPRECATION")
                val rotacionPantalla = actividad.windowManager.defaultDisplay.rotation
                // El sensor da "hacia arriba" en ejes del teléfono (x a la derecha e y
                // hacia arriba en vertical). En horizontal, el arriba de la pantalla es
                // +x (ROTATION_90) o -x (ROTATION_270), y el ángulo sale positivo al
                // girar a la derecha.
                val angulo = if (rotacionPantalla == Surface.ROTATION_270) atan2(-y, -x) else atan2(y, x)
                // ±45º = tope del volante.
                alCambiar((angulo / (Math.PI.toFloat() / 4f)).coerceIn(-1f, 1f))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        listener = l
        sensorManager.registerListener(l, sensorActual, SensorManager.SENSOR_DELAY_GAME)
    }

    fun detener() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
    }
}
