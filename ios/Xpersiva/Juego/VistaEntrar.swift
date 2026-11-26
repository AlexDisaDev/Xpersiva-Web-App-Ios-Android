import SwiftUI

/// Entrada a una experiencia: aquí aterrizan el escáner y los enlaces del QR.
struct VistaEntrar: View {
    let enlace: EnlaceUnion
    @Binding var ruta: [Destino]
    @State private var nombre = Perfil.nombre
    @FocusState private var enfocado: Bool

    private var limpio: String { nombre.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Spacer()
            Text("EXPERIENCIA \(enlace.codigo)").font(.caption.bold()).tracking(1.5).foregroundStyle(Color.acento)
            Text("¿Cómo te llamas?").font(.largeTitle.bold()).foregroundStyle(Color.texto)
            TextField("Tu nombre en la partida", text: $nombre)
                .textContentType(.givenName)
                .submitLabel(.go)
                .focused($enfocado)
                .padding()
                .background(.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(enfocado ? Color.acento : Color.borde))
                .onChange(of: nombre) { _, nuevo in
                    if nuevo.count > 24 { nombre = String(nuevo.prefix(24)) }
                }
                .onSubmit(entrar)
            Spacer()
            BotonPrincipal(titulo: "ENTRAR", icono: nil, accion: entrar)
                .disabled(limpio.isEmpty)
        }
        .padding()
        .background(Color.fondo)
        .onAppear { enfocado = nombre.isEmpty }
    }

    private func entrar() {
        guard !limpio.isEmpty else { return }
        Perfil.nombre = limpio
        ruta.append(.mando(enlace))
    }
}
