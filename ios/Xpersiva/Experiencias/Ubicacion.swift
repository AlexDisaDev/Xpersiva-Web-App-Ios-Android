import CoreLocation
import Observation

/// Pide la ubicación solo cuando el usuario pulsa el botón, y una sola vez:
/// no hacemos seguimiento continuo ni la guardamos.
@MainActor
@Observable
final class Ubicacion: NSObject, @preconcurrency CLLocationManagerDelegate {
    @ObservationIgnored private let gestor: CLLocationManager
    private(set) var estado: CLAuthorizationStatus
    private(set) var actual: CLLocation?

    override init() {
        let gestor = CLLocationManager()
        self.gestor = gestor
        estado = gestor.authorizationStatus
        super.init()
        gestor.delegate = self
        gestor.desiredAccuracy = kCLLocationAccuracyKilometer // basta con la zona
        if estaPermitida { gestor.requestLocation() }
    }

    var estaPermitida: Bool { estado == .authorizedWhenInUse || estado == .authorizedAlways }
    var estaDenegada: Bool { estado == .denied || estado == .restricted }

    func pedir() {
        if estaPermitida { gestor.requestLocation() } else { gestor.requestWhenInUseAuthorization() }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        estado = manager.authorizationStatus
        if estaPermitida { manager.requestLocation() }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        actual = locations.last
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // GPS apagado o sin señal: seguimos sin ordenar por distancia.
    }
}
