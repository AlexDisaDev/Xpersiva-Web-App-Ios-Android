import Foundation

/// Web de Xpersiva: de aquí salen las experiencias y los enlaces de los QR.
/// Cambiar a dominio propio ANTES de publicar (también en Xpersiva.entitlements).
enum Config {
    static let urlWeb = URL(string: "https://xpersiva.netlify.app")!
    static let esquema = "xpersiva"
    static var urlExperiencias: URL { urlWeb.appending(path: "experiences.json") }
}
