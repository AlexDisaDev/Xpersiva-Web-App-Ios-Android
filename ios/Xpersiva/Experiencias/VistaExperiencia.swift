import MapKit
import SwiftUI

/// Ficha de una experiencia. También se abre con <web>/e/<id>.
struct VistaExperiencia: View {
    let id: String
    @Binding var ruta: [Destino]
    @Environment(CatalogoExperiencias.self) private var catalogo
    @Environment(Ubicacion.self) private var ubicacion
    @State private var escaneando = false

    var body: some View {
        Group {
            if let experiencia = catalogo.todas.first(where: { $0.id == id }) {
                contenido(experiencia)
            } else if catalogo.cargando || catalogo.todas.isEmpty && catalogo.error == nil {
                ProgressView()
            } else {
                ContentUnavailableView("Esta experiencia ya no está disponible", systemImage: "calendar.badge.exclamationmark")
            }
        }
        .task { if catalogo.todas.isEmpty { await catalogo.cargar() } }
        .sheet(isPresented: $escaneando) {
            VistaEscaner { enlace in
                escaneando = false
                ruta.append(.entrar(enlace))
            }
        }
    }

    private func contenido(_ e: Experiencia) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                if let brand = e.brand {
                    Text(brand.uppercased()).font(.caption.bold()).foregroundStyle(.tint)
                }
                Text(e.name).font(.largeTitle.bold())
                Text(lugar(e)).foregroundStyle(.secondary)
                if let fechas = fechas(e) { Text(fechas).foregroundStyle(.secondary) }
                if let description = e.description { Text(description).padding(.top, 8) }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
        }
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 12) {
                Button("Cómo llegar") { abrirMapa(e) }
                    .buttonStyle(.bordered).frame(maxWidth: .infinity)
                Button { escaneando = true } label: {
                    Label("Estoy allí · escanear QR", systemImage: "qrcode.viewfinder").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent).controlSize(.large)
            }
            .padding()
        }
    }

    private func lugar(_ e: Experiencia) -> String {
        let distancia = ubicacion.actual.map { Experiencia.formatoDistancia(metros: e.ubicacion.distance(from: $0)) }
        return [e.venue, e.city, distancia].compactMap { $0 }.joined(separator: " · ")
    }

    private func fechas(_ e: Experiencia) -> String? {
        let partes = [e.startsAt, e.endsAt].compactMap { $0?.formatted(.dateTime.day().month()) }
        guard !partes.isEmpty else { return nil }
        return partes.joined(separator: " – ") + (e.estaEnMarcha() ? " · ahora" : "")
    }

    private func abrirMapa(_ e: Experiencia) {
        let destino = MKMapItem(placemark: MKPlacemark(coordinate: e.ubicacion.coordinate))
        destino.name = e.name
        destino.openInMaps()
    }
}
