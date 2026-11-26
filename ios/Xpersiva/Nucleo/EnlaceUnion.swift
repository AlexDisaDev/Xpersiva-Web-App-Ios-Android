import Foundation

/// A qué experiencia hay que unirse, sacado de un QR o de un enlace.
///
/// Formatos aceptados:
///   https://<web>/j/F1?h=192.168.0.17:8080   (QR recomendado)
///   xpersiva://j/F1?h=192.168.0.17:8080      (esquema propio)
///   http://192.168.0.17:8080                 (QR actual del plugin, solo red local)
struct EnlaceUnion: Hashable, Sendable {
    /// Código de la experiencia. "LAN" si el QR no traía ninguno.
    let codigo: String
    /// host:puerto del PC que corre el juego.
    let host: String
    /// true para wss:// (juego detrás de un túnel con TLS).
    let seguro: Bool

    var urlSocket: URL? { URL(string: "\(seguro ? "wss" : "ws")://\(host)") }

    init?(codigo: String, host: String?, seguro: Bool = false) {
        guard let host,
              host.range(of: #"^[A-Za-z0-9.-]+(:\d{1,5})?$"#, options: .regularExpression) != nil,
              codigo.range(of: #"^[A-Za-z0-9][A-Za-z0-9-]{1,31}$"#, options: .regularExpression) != nil
        else { return nil }
        self.codigo = codigo.uppercased()
        self.host = host
        self.seguro = seguro
    }

    init?(texto: String) {
        guard let url = URL(string: texto.trimmingCharacters(in: .whitespacesAndNewlines)),
              let partes = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let esquema = partes.scheme?.lowercased()
        else { return nil }

        let consulta = Dictionary(
            (partes.queryItems ?? []).map { ($0.name, $0.value ?? "") },
            uniquingKeysWith: { primero, _ in primero }
        )
        let esNuestro = esquema == Config.esquema
            || (esquema == "https" && partes.host == Config.urlWeb.host())

        if esNuestro {
            // En xpersiva://j/F1 la "j" llega como host; en https llega en la ruta.
            let trozos = ([esquema == Config.esquema ? partes.host ?? "" : ""] + partes.path.split(separator: "/").map(String.init))
                .filter { !$0.isEmpty }
            guard trozos.count >= 2, trozos[0] == "j" else { return nil }
            self.init(codigo: trozos[1], host: consulta["h"], seguro: consulta["s"] == "1")
            return
        }

        // QR antiguo del plugin: solo si apunta a la red local.
        if esquema == "http" || esquema == "https", let host = partes.host, Self.esIPPrivada(host) {
            let conPuerto = partes.port.map { "\(host):\($0)" } ?? host
            self.init(codigo: "LAN", host: conPuerto, seguro: esquema == "https")
            return
        }
        return nil
    }

    static func esIPPrivada(_ host: String) -> Bool {
        let n = host.split(separator: ".").compactMap { Int($0) }
        guard n.count == 4 else { return false }
        return n[0] == 10 || (n[0] == 192 && n[1] == 168) || (n[0] == 172 && (16...31).contains(n[1]))
    }
}
