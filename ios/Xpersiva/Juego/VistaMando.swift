import SwiftUI

/// El mando. Cada zona tiene su propio gesto, así que SwiftUI deja usar
/// varias a la vez: girar con un dedo y acelerar con otro.
struct VistaMando: View {
    @State private var conexion: ConexionJuego
    @State private var giro = VolanteGiroscopio()
    private let nombre: String
    private let control: TipoControl

    init(enlace: EnlaceUnion) {
        nombre = Perfil.nombre
        control = enlace.control
        _conexion = State(initialValue: ConexionJuego(enlace: enlace, nombre: Perfil.nombre))
    }

    var body: some View {
        Group {
            if control == .giroscopio {
                cuerpoGiroscopio
            } else {
                cuerpoTactil
            }
        }
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true // que no se apague la pantalla jugando
            conexion.iniciar()
            if control == .giroscopio {
                bloquearHorizontal(true)
                giro.iniciar { conexion.volante = $0 }
            }
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
            conexion.detener()
            if control == .giroscopio {
                giro.detener()
                bloquearHorizontal(false)
            }
        }
    }

    private var cuerpoTactil: some View {
        VStack(spacing: 12) {
            cabecera
            switch control {
            case .volante:
                Volante(valor: $conexion.volante)
                HStack(spacing: 12) {
                    Pedal(titulo: "FRENO", pulsado: $conexion.freno)
                    Pedal(titulo: "GAS", pulsado: $conexion.acelerador)
                }
                .frame(height: 260)
            case .boton:
                BotonUnico(pulsado: $conexion.acelerador)
                    .frame(maxHeight: .infinity)
            }
        }
        .padding()
        .navigationBarTitleDisplayMode(.inline)
        .persistentSystemOverlays(.hidden)
    }

    /// Horizontal: el giro del teléfono ya controla el volante, así que aquí solo
    /// hace falta el indicador y los dos botones de gas/freno, uno a cada lado.
    private var cuerpoGiroscopio: some View {
        HStack(spacing: 16) {
            Pedal(titulo: "FRENO", pulsado: $conexion.freno)
                .frame(width: 100)
            VStack(spacing: 10) {
                cabecera
                IndicadorGiroscopio(valor: conexion.volante)
            }
            Pedal(titulo: "GAS", pulsado: $conexion.acelerador)
                .frame(width: 100)
        }
        .padding()
        .navigationBarTitleDisplayMode(.inline)
        .persistentSystemOverlays(.hidden)
        .statusBarHidden()
    }

    private var cabecera: some View {
        HStack(spacing: 8) {
            Circle().fill(conexion.estado == .dentro ? .ahora : .malo).frame(width: 9, height: 9)
            Text("\(nombre) · \(textoEstado)").font(.footnote).foregroundStyle(.secondary)
            Spacer()
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

    /// La app es de retrato; solo esta pantalla necesita girar a horizontal.
    private func bloquearHorizontal(_ horizontal: Bool) {
        OrientacionApp.permitidas = horizontal ? .landscape : .portrait
        guard let escena = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return }
        let geometria = UIWindowScene.GeometryPreferences.iOS(
            interfaceOrientations: horizontal ? .landscape : .portrait
        )
        escena.requestGeometryUpdate(geometria) { _ in }
        escena.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
    }
}

private extension UIWindowScene {
    var keyWindow: UIWindow? { windows.first { $0.isKeyWindow } }
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

/// Experiencias que solo necesitan "pulsa para jugar": reusa `acelerador`
/// (el `throttle` del protocolo) como señal de pulsado/soltado.
private struct BotonUnico: View {
    @Binding var pulsado: Bool

    var body: some View {
        RoundedRectangle(cornerRadius: 24)
            .fill(pulsado ? AnyShapeStyle(.tint) : AnyShapeStyle(.quaternary))
            .overlay(Text("PULSA").font(.largeTitle.bold()).foregroundStyle(pulsado ? .white : .primary))
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

/// Solo lectura: el giro lo manda `VolanteGiroscopio`, aquí no hay gesto de arrastre.
private struct IndicadorGiroscopio: View {
    let valor: Double

    var body: some View {
        GeometryReader { geo in
            ZStack {
                RoundedRectangle(cornerRadius: 16).stroke(.quaternary)
                Text("inclina el móvil para girar").foregroundStyle(.secondary)
                Capsule().fill(.tint)
                    .frame(width: 4)
                    .padding(.vertical, 10)
                    .position(x: (valor + 1) / 2 * geo.size.width, y: geo.size.height / 2)
            }
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
