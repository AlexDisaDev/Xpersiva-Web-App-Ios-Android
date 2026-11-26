import CoreLocation
import Foundation
import Observation

/// Lista de experiencias descargada de la web, filtrada y ordenada para la UI.
@MainActor
@Observable
final class CatalogoExperiencias {
    private(set) var todas: [Experiencia] = []
    private(set) var cargando = false
    private(set) var error: String?

    func cargar() async {
        cargando = true
        error = nil
        defer { cargando = false }
        do {
            var peticion = URLRequest(url: Config.urlExperiencias)
            peticion.cachePolicy = .reloadRevalidatingCacheData
            let (datos, respuesta) = try await URLSession.shared.data(for: peticion)
            guard (respuesta as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
            let decodificador = JSONDecoder()
            decodificador.dateDecodingStrategy = .iso8601
            let lista = try decodificador.decode(ListaExperiencias.self, from: datos).experiences
            todas = lista.filter { !$0.haTerminado() }
        } catch {
            self.error = "No hemos podido cargar las experiencias. Revisa tu conexión."
        }
    }

    /// Con ubicación: de más cerca a más lejos. Sin ella: primero las que
    /// están en marcha y luego por fecha de inicio.
    func visibles(busqueda: String, desde aqui: CLLocation?) -> [Experiencia] {
        let filtradas = todas.filter { $0.coincide(con: busqueda) }
        if let aqui {
            return filtradas.sorted { $0.ubicacion.distance(from: aqui) < $1.ubicacion.distance(from: aqui) }
        }
        return filtradas.sorted {
            if $0.estaEnMarcha() != $1.estaEnMarcha() { return $0.estaEnMarcha() }
            return ($0.startsAt ?? .distantFuture) < ($1.startsAt ?? .distantFuture)
        }
    }
}

private struct ListaExperiencias: Decodable {
    let experiences: [Experiencia]
}
