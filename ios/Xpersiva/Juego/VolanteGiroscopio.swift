import CoreMotion

/// Convierte el giro del teléfono en un valor de volante -1...1.
///
/// Se guarda la postura del teléfono al empezar como "centro" (en vez de un
/// ángulo absoluto fijo), así da igual cómo sujete cada uno el móvil: lo que
/// cuenta es cuánto lo gira desde donde empezó.
@MainActor
final class VolanteGiroscopio {
    private let gestor = CMMotionManager()
    private var referencia: CMAttitude?

    func iniciar(alCambiar: @escaping (Double) -> Void) {
        guard gestor.isDeviceMotionAvailable else { return }
        referencia = nil
        gestor.deviceMotionUpdateInterval = 1.0 / 60
        gestor.startDeviceMotionUpdates(using: .xArbitraryZVertical, to: .main) { [weak self] datos, _ in
            guard let self, let datos else { return }
            // Copia aparte: `multiply(byInverseOf:)` muta el objeto en el sitio,
            // y la primera vez `datos.attitude` y la referencia serían el mismo objeto.
            if referencia == nil { referencia = datos.attitude.copy() as? CMAttitude }
            let relativa = datos.attitude
            relativa.multiply(byInverseOf: referencia!)
            // ±45º de giro desde la postura inicial = tope del volante.
            alCambiar(min(1, max(-1, relativa.roll / (.pi / 4))))
        }
    }

    func detener() {
        gestor.stopDeviceMotionUpdates()
        referencia = nil
    }
}
