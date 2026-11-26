import Foundation

/// Lo que el jugador nos deja saber de él. Solo el nombre, de momento.
enum Perfil {
    private static let clave = "xpersiva.nombre"

    static var nombre: String {
        get { UserDefaults.standard.string(forKey: clave) ?? "" }
        set { UserDefaults.standard.set(String(newValue.prefix(24)), forKey: clave) }
    }
}
