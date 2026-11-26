import CoreMotion
import UIKit

/// Convierte el giro del teléfono en un valor de volante -1...1.
///
/// Se sujeta en horizontal y se gira como un volante: lo que cuenta es hacia
/// dónde cae la gravedad dentro del plano de la pantalla. Inclinarlo hacia
/// delante o hacia atrás no cambia ese ángulo, así que no gira el coche.
@MainActor
final class VolanteGiroscopio {
    private let gestor = CMMotionManager()
    private var ultimoAngulo = 0.0
    private var centro = 0.0
    var invertido = false

    func calibrar() { centro = ultimoAngulo }

    func iniciar(alCambiar: @escaping (Double) -> Void) {
        guard gestor.isDeviceMotionAvailable else { return }
        gestor.deviceMotionUpdateInterval = 1.0 / 60
        gestor.startDeviceMotionUpdates(to: .main) { datos, _ in
            guard let datos else { return }
            // CoreMotion da la gravedad hacia abajo en ejes del teléfono (x a la
            // derecha e y hacia arriba en vertical). En horizontal, el arriba de la
            // pantalla es +x (landscapeRight) o -x (landscapeLeft), y el ángulo sale
            // positivo al girar a la derecha.
            let g = datos.gravity
            let orientacion = (UIApplication.shared.connectedScenes.first as? UIWindowScene)?.interfaceOrientation
            let angulo = orientacion == .landscapeLeft ? atan2(g.y, g.x) : atan2(-g.y, -g.x)
            MainActor.assumeIsolated {
                self.ultimoAngulo = angulo
                let diferencia = atan2(sin(angulo - self.centro), cos(angulo - self.centro))
                let direccion = self.invertido ? -1.0 : 1.0
                // ±45º = tope del volante.
                alCambiar(min(1, max(-1, diferencia / (.pi / 4) * direccion)))
            }
        }
    }

    func detener() {
        gestor.stopDeviceMotionUpdates()
    }
}
