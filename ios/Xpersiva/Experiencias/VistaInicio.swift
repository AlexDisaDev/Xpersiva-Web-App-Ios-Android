import SwiftUI

/// Inicio: logo arriba sobre fondo claro, bloque de bienvenida azul noche con el
/// buscador, filtros ("Cerca de mí" pide la ubicación solo al pulsarlo, "En
/// directo" deja las que están en marcha) y las experiencias de la web en
/// tarjetas. El botón "Escanear QR" queda siempre a mano abajo.
struct VistaInicio: View {
    @Binding var ruta: [Destino]
    @Environment(CatalogoExperiencias.self) private var catalogo
    @Environment(Ubicacion.self) private var ubicacion
    @Environment(\.openURL) private var abrirURL
    @State private var busqueda = ""
    @State private var soloEnDirecto = false
    @State private var escaneando = false

    var body: some View {
        let lista = catalogo.visibles(busqueda: busqueda, desde: ubicacion.actual)
            .filter { !soloEnDirecto || $0.estaEnMarcha() }

        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                Bienvenida(busqueda: $busqueda)

                VStack(alignment: .leading, spacing: 10) {
                    Text("Explora experiencias")
                        .font(.title2.bold())
                        .foregroundStyle(Color.texto)
                        .accessibilityAddTraits(.isHeader)
                    HStack(spacing: 8) {
                        Filtro(titulo: ubicacion.estaDenegada ? "Activar ubicación" : "Cerca de mí",
                               activo: ubicacion.estaPermitida) {
                            Image(systemName: "location.fill").font(.footnote)
                        } accion: {
                            if ubicacion.estaDenegada, let url = URL(string: UIApplication.openSettingsURLString) {
                                abrirURL(url)
                            } else {
                                ubicacion.pedir()
                            }
                        }
                        Filtro(titulo: "En directo", activo: soloEnDirecto) {
                            PuntoEnDirecto(color: soloEnDirecto ? .white : .ahora)
                        } accion: {
                            soloEnDirecto.toggle()
                        }
                    }
                    if ubicacion.estaDenegada {
                        Text("Sin ubicación las verás igual, ordenadas por fecha.")
                            .font(.footnote)
                            .foregroundStyle(Color.textoSuave)
                    }
                }
                .padding(.top, 12)

                if let error = catalogo.error {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(error).foregroundStyle(Color.textoSuave)
                        Button("Reintentar") { Task { await catalogo.cargar() } }
                    }
                }

                if catalogo.cargando && catalogo.todas.isEmpty {
                    ProgressView().frame(maxWidth: .infinity).padding(24)
                }

                ForEach(lista) { experiencia in
                    NavigationLink(value: Destino.experiencia(experiencia.id)) {
                        TarjetaExperiencia(experiencia: experiencia, distancia: distancia(a: experiencia))
                    }
                    .buttonStyle(.plain)
                }

                if lista.isEmpty && !catalogo.cargando && catalogo.error == nil {
                    Text(textoVacio)
                        .foregroundStyle(Color.textoSuave)
                        .padding(.vertical, 16)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.fondo)
        .navigationTitle("Xpersiva")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Image("LogoCabecera")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 28)
                    .accessibilityLabel("Xpersiva")
            }
        }
        .toolbarBackground(.white, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .refreshable { await catalogo.cargar() }
        .task { if catalogo.todas.isEmpty { await catalogo.cargar() } }
        .safeAreaInset(edge: .bottom) {
            BotonPrincipal(titulo: "ESCANEAR QR") { escaneando = true }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color.fondo)
        }
        .sheet(isPresented: $escaneando) {
            VistaEscaner { enlace in
                escaneando = false
                ruta.append(.entrar(enlace))
            }
        }
    }

    private var textoVacio: String {
        if !busqueda.isEmpty { return "Ninguna experiencia coincide con tu búsqueda." }
        if soloEnDirecto { return "Ahora mismo no hay ninguna experiencia en directo." }
        return "Aún no hay experiencias publicadas."
    }

    private func distancia(a experiencia: Experiencia) -> String? {
        ubicacion.actual.map { Experiencia.formatoDistancia(metros: experiencia.ubicacion.distance(from: $0)) }
    }
}

/// Bloque azul noche: frase de marca y buscador.
private struct Bienvenida: View {
    @Binding var busqueda: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("EXPERIENCIAS INMERSIVAS")
                .font(.caption.bold())
                .tracking(1.5)
                .foregroundStyle(Color.cian)
            Text("El evento empieza contigo.")
                .font(.title.weight(.heavy))
                .foregroundStyle(.white)
                .padding(.top, 8)
                .accessibilityAddTraits(.isHeader)
            Text("Escanea el QR del evento y tu móvil se convierte en el mando.")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.72))
                .padding(.top, 6)
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass").foregroundStyle(Color.textoSuave)
                TextField("Busca una experiencia o ciudad", text: $busqueda)
                    .foregroundStyle(Color.texto)
                    .submitLabel(.search)
                    .autocorrectionDisabled()
                if !busqueda.isEmpty {
                    Button { busqueda = "" } label: {
                        Image(systemName: "xmark.circle.fill").foregroundStyle(Color.textoSuave)
                    }
                    .accessibilityLabel("Borrar búsqueda")
                }
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 52)
            .background(.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .padding(.top, 18)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color.noche, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

/// Píldora de filtro: violeta lleno si está activo, violeta suave si no.
private struct Filtro<Icono: View>: View {
    let titulo: String
    let activo: Bool
    @ViewBuilder let icono: () -> Icono
    let accion: () -> Void

    var body: some View {
        Button(action: accion) {
            HStack(spacing: 6) {
                icono()
                Text(titulo).font(.subheadline.weight(.semibold))
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 40)
            .foregroundStyle(activo ? .white : Color.acento)
            .background(activo ? Color.acento : Color.acentoSuave, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(activo ? .isSelected : [])
    }
}

struct TarjetaExperiencia: View {
    let experiencia: Experiencia
    let distancia: String?

    var body: some View {
        HStack(spacing: 14) {
            // Inicial de la marca (o del nombre) en un cuadro violeta suave: da ritmo a la lista sin inventar imágenes.
            Text((experiencia.brand ?? experiencia.name).prefix(1).uppercased())
                .font(.title2.weight(.heavy))
                .foregroundStyle(Color.acento)
                .frame(width: 48, height: 48)
                .background(Color.acentoSuave, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                if experiencia.estaEnMarcha() {
                    HStack(spacing: 6) {
                        PuntoEnDirecto()
                        Text("EN DIRECTO").font(.caption2.weight(.heavy)).foregroundStyle(Color.ahora)
                    }
                }
                Text(experiencia.name).font(.headline).foregroundStyle(Color.texto)
                Text(experiencia.subtitulo).font(.subheadline).foregroundStyle(Color.textoSuave).lineLimit(1)
                if let distancia {
                    Text(distancia).font(.subheadline.bold()).foregroundStyle(Color.acento).padding(.top, 2)
                }
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).foregroundStyle(Color.textoSuave)
        }
        .padding(16)
        .background(.white, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(Color.borde))
        .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}
