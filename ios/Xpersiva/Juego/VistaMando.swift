import SwiftUI

/// El mando. Cada zona tiene su propio gesto, así que SwiftUI deja usar
/// varias a la vez: girar con un dedo y acelerar con otro.
struct VistaMando: View {
    @State private var conexion: ConexionJuego
    @State private var giro = VolanteGiroscopio()
    @State private var invertido = false
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
            case .giroscopio:
                EmptyView()
            }
        }
        .padding()
        .navigationBarTitleDisplayMode(.inline)
        .persistentSystemOverlays(.hidden)
    }

    /// Mando horizontal de marca: los pedales quedan al alcance de los pulgares.
    private var cuerpoGiroscopio: some View {
        HStack(spacing: 12) {
            Pedal(titulo: "FRENO", pulsado: $conexion.freno, acento: .freno)
                .frame(width: 105)
            VStack(spacing: 10) {
                HStack {
                    Text("XPERSIVA / F1").font(.caption.bold()).tracking(1.5).foregroundStyle(Color.cian)
                    Spacer()
                }
                Text("Tú llevas el control.")
                    .font(.title2.weight(.heavy))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                cabecera
                IndicadorGiroscopio(valor: conexion.volante)
                HStack(spacing: 8) {
                    Button("◎  Calibrar giro") { giro.calibrar() }
                    Button(invertido ? "↔  Invertido" : "↔  Invertir") {
                        invertido.toggle()
                        giro.invertido = invertido
                    }
                }
                .buttonStyle(.bordered)
                .tint(.white)
            }
            Pedal(titulo: "ACELERAR", pulsado: $conexion.acelerador, acento: .gas)
                .frame(width: 105)
        }
        .padding(16)
        .background(Color.noche.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
        .persistentSystemOverlays(.hidden)
        .statusBarHidden()
    }

    private var cabecera: some View {
        HStack(spacing: 8) {
            Circle().fill(conexion.estado == .dentro ? .ahora : .malo).frame(width: 9, height: 9)
            Text("\(nombre) · \(textoEstado)").font(.footnote)
                .foregroundStyle(control == .giroscopio ? Color.white.opacity(0.7) : Color.textoSuave)
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
        VStack(spacing: 5) {
            Image(systemName: "steeringwheel").font(.title).foregroundStyle(Color.cian)
            Text("DIRECCIÓN").font(.subheadline.weight(.heavy)).tracking(1.5).foregroundStyle(.white)
            Text("Gira el móvil como un volante").font(.caption).foregroundStyle(Color.white.opacity(0.7))
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color(hex: 0x303657)).frame(height: 7)
                    Capsule().fill(Color(hex: 0x7980A5)).frame(width: 2, height: 18)
                        .position(x: geo.size.width / 2, y: geo.size.height / 2)
                    Circle().fill(Color.cian).frame(width: 24, height: 24)
                        .position(x: (valor + 1) / 2 * geo.size.width, y: geo.size.height / 2)
                }
            }
            .frame(height: 28)
            HStack {
                Text("IZQUIERDA")
                Spacer()
                Text("DERECHA")
            }
            .font(.caption2.bold())
            .foregroundStyle(Color.white.opacity(0.5))
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(hex: 0x111630), in: RoundedRectangle(cornerRadius: 24))
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color(hex: 0x353B5D)))
    }
}

private struct Pedal: View {
    let titulo: String
    @Binding var pulsado: Bool
    var acento: Color? = nil

    var body: some View {
        RoundedRectangle(cornerRadius: acento == nil ? 16 : 24)
            .fill(acento.map { $0.opacity(pulsado ? 0.52 : 0.22) } ?? (pulsado ? Color.acento : Color.acentoSuave))
            .overlay(RoundedRectangle(cornerRadius: acento == nil ? 16 : 24)
                .stroke(acento ?? Color.clear, lineWidth: 1))
            .overlay(Text(titulo).font(.headline.weight(.heavy))
                .foregroundStyle(acento ?? (pulsado ? Color.white : Color.texto)))
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
