import Foundation
import Observation

/// Conexión con el plugin `websocket` del juego.
///
/// Protocolo (el mismo que el mando web):
///   app → juego  {"type":"hello","name","code"}
///   juego → app  {"type":"joined","id"}
///   app → juego  {"type":"input","steer","throttle","brake"}  hasta 30 veces por segundo
@MainActor
@Observable
final class ConexionJuego {
    enum Estado { case conectando, conectado, dentro, reintentando }

    private(set) var estado: Estado = .conectando
    private(set) var idJugador: String?

    var volante: Double = 0
    var acelerador = false
    var freno = false

    private let enlace: EnlaceUnion
    private let nombre: String
    private var socket: URLSessionWebSocketTask?
    private var tareas: [Task<Void, Never>] = []
    private var ultimoEnviado = ""

    init(enlace: EnlaceUnion, nombre: String) {
        self.enlace = enlace
        self.nombre = nombre
    }

    func iniciar() {
        guard tareas.isEmpty else { return }
        tareas.append(Task { await bucleConexion() })
        tareas.append(Task { await bucleEnvio() })
    }

    func detener() {
        tareas.forEach { $0.cancel() }
        tareas.removeAll()
        socket?.cancel(with: .goingAway, reason: nil)
        socket = nil
    }

    private func bucleConexion() async {
        while !Task.isCancelled {
            guard let url = enlace.urlSocket else { return }
            let socket = URLSession.shared.webSocketTask(with: url)
            self.socket = socket
            socket.resume()
            do {
                try await socket.send(.string(json(["type": "hello", "name": nombre, "code": enlace.codigo])))
                estado = .conectado
                ultimoEnviado = ""
                while !Task.isCancelled {
                    if case .string(let texto) = try await socket.receive() { recibir(texto) }
                }
            } catch {
                // Cerrado o sin red: reintentamos.
            }
            idJugador = nil
            estado = .reintentando
            try? await Task.sleep(for: .seconds(1.5))
        }
    }

    private func recibir(_ texto: String) {
        guard let datos = texto.data(using: .utf8),
              let mensaje = try? JSONSerialization.jsonObject(with: datos) as? [String: Any],
              mensaje["type"] as? String == "joined"
        else { return }
        idJugador = (mensaje["id"]).map { "\($0)" }
        estado = .dentro
    }

    /// Solo mandamos cuando algo cambió, igual que el mando web.
    private func bucleEnvio() async {
        while !Task.isCancelled {
            try? await Task.sleep(for: .milliseconds(33))
            guard estado == .conectado || estado == .dentro, let socket else { continue }
            let carga = json([
                "type": "input",
                "steer": (volante * 1000).rounded() / 1000,
                "throttle": acelerador ? 1 : 0,
                "brake": freno ? 1 : 0,
            ])
            guard carga != ultimoEnviado else { continue }
            ultimoEnviado = carga
            try? await socket.send(.string(carga))
        }
    }

    private func json(_ objeto: [String: Any]) -> String {
        guard let datos = try? JSONSerialization.data(withJSONObject: objeto, options: [.sortedKeys]) else { return "{}" }
        return String(decoding: datos, as: UTF8.self)
    }
}
