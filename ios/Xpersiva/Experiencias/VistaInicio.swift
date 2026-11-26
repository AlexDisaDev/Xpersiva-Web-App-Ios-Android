import SwiftUI

/// Inicio: buscador de experiencias, de más cerca a más lejos.
struct VistaInicio: View {
    @Binding var ruta: [Destino]
    @Environment(CatalogoExperiencias.self) private var catalogo
    @Environment(Ubicacion.self) private var ubicacion
    @Environment(\.openURL) private var abrirURL
    @State private var busqueda = ""
    @State private var escaneando = false

    var body: some View {
        let lista = catalogo.visibles(busqueda: busqueda, desde: ubicacion.actual)

        List {
            if !ubicacion.estaPermitida {
                Section { botonUbicacion }
            }
            if let error = catalogo.error {
                Section {
                    Text(error).foregroundStyle(.secondary)
                    Button("Reintentar") { Task { await catalogo.cargar() } }
                }
            }
            Section {
                ForEach(lista) { experiencia in
                    NavigationLink(value: Destino.experiencia(experiencia.id)) {
                        FilaExperiencia(experiencia: experiencia, distancia: distancia(a: experiencia))
                    }
                }
            } footer: {
                if lista.isEmpty && !catalogo.cargando && catalogo.error == nil {
                    Text(busqueda.isEmpty ? "Aún no hay experiencias publicadas." : "Ninguna experiencia coincide con tu búsqueda.")
                }
            }
        }
        .listStyle(.plain)
        .navigationTitle("Xpersiva")
        .searchable(text: $busqueda, placement: .navigationBarDrawer(displayMode: .always),
                    prompt: "Experiencia, marca o ciudad")
        .overlay { if catalogo.cargando && catalogo.todas.isEmpty { ProgressView() } }
        .refreshable { await catalogo.cargar() }
        .task { if catalogo.todas.isEmpty { await catalogo.cargar() } }
        .safeAreaInset(edge: .bottom) {
            Button { escaneando = true } label: {
                Label("Escanear QR", systemImage: "qrcode.viewfinder").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding()
        }
        .sheet(isPresented: $escaneando) {
            VistaEscaner { enlace in
                escaneando = false
                ruta.append(.entrar(enlace))
            }
        }
    }

    @ViewBuilder private var botonUbicacion: some View {
        if ubicacion.estaDenegada {
            Button("Activa la ubicación en Ajustes para ver primero lo que tienes cerca") {
                if let url = URL(string: UIApplication.openSettingsURLString) { abrirURL(url) }
            }
        } else {
            Button {
                ubicacion.pedir()
            } label: {
                Label("Ver primero lo que tengo cerca", systemImage: "location")
            }
        }
    }

    private func distancia(a experiencia: Experiencia) -> String? {
        ubicacion.actual.map { Experiencia.formatoDistancia(metros: experiencia.ubicacion.distance(from: $0)) }
    }
}

struct FilaExperiencia: View {
    let experiencia: Experiencia
    let distancia: String?

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(experiencia.name).font(.headline)
                Text(experiencia.subtitulo).font(.subheadline).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                if let distancia { Text(distancia).font(.subheadline.bold()) }
                if experiencia.estaEnMarcha() {
                    Text("AHORA").font(.caption2.bold()).foregroundStyle(.green)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
