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
    /// Horario del día (hora local, "10:00"), opcional: la campaña
    /// (`startsAt`/`endsAt`) puede durar meses.
    let dailyOpensAt: String?
    let dailyClosesAt: String?
    let description: String?
    let tags: [String]?

    var ubicacion: CLLocation { CLLocation(latitude: lat, longitude: lng) }

    var subtitulo: String {
        [brand, venue, city].compactMap { $0 }.joined(separator: " · ")
    }

    /// "10:00 – 20:00", o nil si no tiene horario diario.
    var horario: String? {
        guard dailyOpensAt != nil || dailyClosesAt != nil else { return nil }
        return [dailyOpensAt, dailyClosesAt].compactMap { $0 }.joined(separator: " – ")
    }

    /// Dentro de la campaña (`startsAt`/`endsAt`) y, si tiene horario diario,
    /// dentro de él. Sin horario diario, una campaña larga (como el demo de
    /// portfolio, que dura más de un año) estaría "en marcha" las 24 horas del día.
    func estaEnMarcha(_ ahora: Date = .now) -> Bool {
        guard (startsAt ?? .distantPast) <= ahora, ahora <= (endsAt ?? .distantFuture) else { return false }
        let minutosAhora = Self.minutosDelDia(ahora)
        if let apertura = dailyOpensAt, let minutos = Self.minutosDeHora(apertura), minutosAhora < minutos { return false }
        if let cierre = dailyClosesAt, let minutos = Self.minutosDeHora(cierre), minutosAhora > minutos { return false }
        return true
    }

    private static func minutosDelDia(_ fecha: Date) -> Int {
        let c = Calendar.current.dateComponents([.hour, .minute], from: fecha)
        return (c.hour ?? 0) * 60 + (c.minute ?? 0)
    }

    private static func minutosDeHora(_ texto: String) -> Int? {
        let partes = texto.split(separator: ":")
        guard partes.count == 2, let h = Int(partes[0]), let m = Int(partes[1]) else { return nil }
        return h * 60 + m
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
