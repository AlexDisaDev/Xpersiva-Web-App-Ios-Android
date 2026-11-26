import SwiftUI

@main
struct XpersivaApp: App {
    var body: some Scene {
        WindowGroup {
            VistaRaiz()
        }
    }
}

/// Pantallas a las que se puede navegar desde el inicio.
enum Destino: Hashable {
    case experiencia(String)
    case entrar(EnlaceUnion)
    case mando(EnlaceUnion)
}

struct VistaRaiz: View {
    @State private var ruta: [Destino] = []
    @State private var catalogo = CatalogoExperiencias()
    @State private var ubicacion = Ubicacion()

    var body: some View {
        NavigationStack(path: $ruta) {
            VistaInicio(ruta: $ruta)
                .navigationDestination(for: Destino.self) { destino in
                    switch destino {
                    case .experiencia(let id): VistaExperiencia(id: id, ruta: $ruta)
                    case .entrar(let enlace): VistaEntrar(enlace: enlace, ruta: $ruta)
                    case .mando(let enlace): VistaMando(enlace: enlace)
                    }
                }
        }
        .environment(catalogo)
        .environment(ubicacion)
        .tint(.indigo)
        // Enlaces del QR (https://<web>/j/…) y de la web (/e/<id>) con la app instalada.
        .onOpenURL(perform: abrir)
    }

    private func abrir(_ url: URL) {
        if let enlace = EnlaceUnion(texto: url.absoluteString) {
            ruta = [.entrar(enlace)]
        } else if let id = idExperiencia(en: url) {
            ruta = [.experiencia(id)]
        }
    }

    private func idExperiencia(en url: URL) -> String? {
        let trozos = url.pathComponents.filter { $0 != "/" }
        guard trozos.count >= 2, trozos[0] == "e" else { return nil }
        return trozos[1]
    }
}
