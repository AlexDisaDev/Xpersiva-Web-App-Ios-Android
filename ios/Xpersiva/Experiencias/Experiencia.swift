import CoreLocation
import Foundation

/// Una experiencia publicada en la web. La app no trae ninguna escrita:
/// las lee de experiences.json, así que crear una nueva no pasa por revisión.
struct Experiencia: Codable, Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let brand: String?
    let venue: String
    let city: String?
    let lat: Double
    let lng: Double
    let startsAt: Date?
    let endsAt: Date?
    let description: String?
    let tags: [String]?

    var ubicacion: CLLocation { CLLocation(latitude: lat, longitude: lng) }

    var subtitulo: String {
        [brand, venue, city].compactMap { $0 }.joined(separator: " · ")
    }

    func estaEnMarcha(_ ahora: Date = .now) -> Bool {
        (startsAt ?? .distantPast) <= ahora && ahora <= (endsAt ?? .distantFuture)
    }

    func haTerminado(_ ahora: Date = .now) -> Bool {
        endsAt.map { $0 < ahora } ?? false
    }

    /// Todas las palabras de la búsqueda tienen que aparecer en algún campo,
    /// sin importar mayúsculas ni tildes.
    func coincide(con busqueda: String) -> Bool {
        let palabras = busqueda.split(whereSeparator: \.isWhitespace).map(String.init)
        guard !palabras.isEmpty else { return true }
        let texto = ([name, brand, venue, city, description].compactMap { $0 } + (tags ?? []))
            .joined(separator: " ")
        return palabras.allSatisfy {
            texto.range(of: $0, options: [.caseInsensitive, .diacriticInsensitive]) != nil
        }
    }
}

extension Experiencia {
    static func formatoDistancia(metros: CLLocationDistance) -> String {
        let km = metros / 1000
        if km < 1 { return "\(Int(metros.rounded())) m" }
        if km < 10 { return String(format: "%.1f km", km) }
        return "\(Int(km.rounded())) km"
    }
}
