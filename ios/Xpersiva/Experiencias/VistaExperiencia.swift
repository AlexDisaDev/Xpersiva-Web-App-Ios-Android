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
            VStack(alignment: .leading, spacing: 12) {
                // Cabecera azul noche, como el bloque de bienvenida del inicio.
                VStack(alignment: .leading, spacing: 8) {
                    if let brand = e.brand {
                        Text(brand.uppercased()).font(.caption.bold()).tracking(1.5).foregroundStyle(Color.cian)
                    }
                    Text(e.name).font(.largeTitle.weight(.heavy)).foregroundStyle(.white)
                        .accessibilityAddTraits(.isHeader)
                    if e.estaEnMarcha() {
                        HStack(spacing: 6) {
                            PuntoEnDirecto(color: .white)
                            Text("EN DIRECTO").font(.caption.weight(.heavy))
                        }
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.ahora, in: Capsule())
                        .padding(.top, 4)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
                .background(Color.noche, in: RoundedRectangle(cornerRadius: 24, style: .continuous))

                VStack(alignment: .leading, spacing: 10) {
                    dato("Dónde", lugar(e))
                    if let fechas = fechas(e) { dato("Cuándo", fechas) }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(.white, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(Color.borde))

                if let description = e.description {
                    Text(description).foregroundStyle(Color.textoSuave).padding(.top, 4)
                }
            }
            .padding()
        }
        .background(Color.fondo)
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 10) {
                Button { abrirMapa(e) } label: {
                    Label("Cómo llegar", systemImage: "arrow.triangle.turn.up.right.diamond")
                        .font(.headline)
                        .frame(maxWidth: .infinity, minHeight: 52)
                        .foregroundStyle(Color.acento)
                        .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(Color.acento, lineWidth: 1.5))
                        .contentShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                }
                .buttonStyle(.plain)
                BotonPrincipal(titulo: "ESTOY ALLÍ · ESCANEAR QR") { escaneando = true }
            }
            .padding()
            .background(Color.fondo)
        }
    }

    private func dato(_ etiqueta: String, _ valor: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(etiqueta.uppercased()).font(.caption2.bold()).tracking(1).foregroundStyle(Color.textoSuave)
            Text(valor).font(.body.weight(.semibold)).foregroundStyle(Color.texto)
        }
        .accessibilityElement(children: .combine)
    }

    private func lugar(_ e: Experiencia) -> String {
        let distancia = ubicacion.actual.map { Experiencia.formatoDistancia(metros: e.ubicacion.distance(from: $0)) }
        return [e.venue, e.city, distancia].compactMap { $0 }.joined(separator: " · ")
    }

    private func fechas(_ e: Experiencia) -> String? {
        let partes = [e.startsAt, e.endsAt].compactMap { $0?.formatted(.dateTime.day().month()) }
        guard !partes.isEmpty else { return nil }
        return partes.joined(separator: " – ")
    }

    private func abrirMapa(_ e: Experiencia) {
        let destino = MKMapItem(placemark: MKPlacemark(coordinate: e.ubicacion.coordinate))
        destino.name = e.name
        destino.openInMaps()
    }
}
