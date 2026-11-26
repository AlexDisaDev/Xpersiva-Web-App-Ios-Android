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
            Text("EXPERIENCIA \(enlace.codigo)").font(.caption.bold()).foregroundStyle(.tint)
            Text("¿Cómo te llamas?").font(.largeTitle.bold())
            TextField("Tu nombre en la partida", text: $nombre)
                .textContentType(.givenName)
                .submitLabel(.go)
                .focused($enfocado)
                .padding()
                .background(.quaternary, in: RoundedRectangle(cornerRadius: 12))
                .onChange(of: nombre) { _, nuevo in
                    if nuevo.count > 24 { nombre = String(nuevo.prefix(24)) }
                }
                .onSubmit(entrar)
            Spacer()
            Button(action: entrar) {
                Text("Entrar").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(limpio.isEmpty)
        }
        .padding()
        .onAppear { enfocado = nombre.isEmpty }
    }

    private func entrar() {
        guard !limpio.isEmpty else { return }
        Perfil.nombre = limpio
        ruta.append(.mando(enlace))
    }
}
