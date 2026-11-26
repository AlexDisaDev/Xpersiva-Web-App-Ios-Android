import SwiftUI

/// El mando. Cada zona tiene su propio gesto, así que SwiftUI deja usar
/// varias a la vez: girar con un dedo y acelerar con otro.
struct VistaMando: View {
    @State private var conexion: ConexionJuego
    private let nombre: String

    init(enlace: EnlaceUnion) {
        nombre = Perfil.nombre
        _conexion = State(initialValue: ConexionJuego(enlace: enlace, nombre: Perfil.nombre))
    }

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                Circle().fill(conexion.estado == .dentro ? .green : .red).frame(width: 9, height: 9)
                Text("\(nombre) · \(textoEstado)").font(.footnote).foregroundStyle(.secondary)
                Spacer()
            }
            Volante(valor: $conexion.volante)
            HStack(spacing: 12) {
                Pedal(titulo: "FRENO", pulsado: $conexion.freno)
                Pedal(titulo: "GAS", pulsado: $conexion.acelerador)
            }
            .frame(height: 260)
        }
        .padding()
        .navigationBarTitleDisplayMode(.inline)
        .persistentSystemOverlays(.hidden)
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true // que no se apague la pantalla jugando
            conexion.iniciar()
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
            conexion.detener()
        }
    }

    private var textoEstado: String {
        switch conexion.estado {
        case .conectando: "conectando…"
        case .conectado: "conectado, esperando a la experiencia…"
        case .dentro: "dentro"
        case .reintentando: "sin conexión, reintentando…"
        }
    }
}

private struct Volante: View {
    @Binding var valor: Double

    var body: some View {
        GeometryReader { geo in
            ZStack {
                RoundedRectangle(cornerRadius: 16).stroke(.quaternary)
                Text("arrastra para girar").foregroundStyle(.secondary)
                Capsule().fill(.tint)
                    .frame(width: 4)
                    .padding(.vertical, 10)
                    .position(x: (valor + 1) / 2 * geo.size.width, y: geo.size.height / 2)
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { g in valor = min(1, max(-1, g.location.x / geo.size.width * 2 - 1)) }
                    .onEnded { _ in valor = 0 }
            )
        }
    }
}

private struct Pedal: View {
    let titulo: String
    @Binding var pulsado: Bool

    var body: some View {
        RoundedRectangle(cornerRadius: 16)
            .fill(pulsado ? AnyShapeStyle(.tint) : AnyShapeStyle(.quaternary))
            .overlay(Text(titulo).font(.headline).foregroundStyle(pulsado ? .white : .primary))
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        if !pulsado {
                            pulsado = true
                            UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        }
                    }
                    .onEnded { _ in pulsado = false }
            )
    }
}
