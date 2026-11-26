import SwiftUI

@main
struct XpersivaApp: App {
    @UIApplicationDelegateAdaptor(OrientacionApp.self) private var orientacionApp

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
    /// `@State` en la vista raíz: vive lo que vive el proceso, así que la intro
    /// sale una vez por arranque y no al volver de segundo plano.
    @State private var introVista = false
    /// Un enlace que llega durante la intro espera a que acabe: así el teclado de
    /// "¿Cómo te llamas?" no sale encima del vídeo.
    @State private var enlacePendiente: URL?

    var body: some View {
        ZStack {
            // Debajo desde el principio: el catálogo empieza a cargar mientras suena la intro.
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
            if !introVista {
                IntroApertura(alTerminar: terminarIntro)
                    .transition(.opacity)
                    .zIndex(1)
            }
        }
        .environment(catalogo)
        .environment(ubicacion)
        .tint(.acento)
        .statusBarHidden(!introVista)
        // Enlaces del QR (https://<web>/j/…) y de la web (/e/<id>) con la app instalada.
        .onOpenURL { url in
            if introVista { abrir(url) } else { enlacePendiente = url }
        }
    }

    private func terminarIntro() {
        withAnimation(.easeOut(duration: 0.35)) { introVista = true }
        if let url = enlacePendiente {
            enlacePendiente = nil
            abrir(url)
        }
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
