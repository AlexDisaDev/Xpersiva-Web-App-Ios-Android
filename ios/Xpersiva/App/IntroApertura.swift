import AVFoundation
import Combine
import SwiftUI

/// Vídeo de apertura (`Recursos/startup-intro.mp4`, 3,4 s, sin sonido), una vez
/// por arranque. Su primer y último fotograma son del mismo azul noche que el
/// fondo de arranque del sistema (`FondoArranque`), así que no se nota el paso.
/// Con "Reducir movimiento" se muestra solo el símbolo un momento. Tocar lo salta.
struct IntroApertura: View {
    let alTerminar: () -> Void

    @Environment(\.accessibilityReduceMotion) private var sinMovimiento
    @State private var jugador: AVPlayer?
    @State private var terminada = false

    var body: some View {
        ZStack {
            Color.noche.ignoresSafeArea()
            if sinMovimiento {
                Image("SimboloArranque")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 160, height: 160)
                    .accessibilityLabel("Xpersiva")
            } else if let jugador {
                VideoSinControles(jugador: jugador)
                    .ignoresSafeArea()
                    .accessibilityHidden(true)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: terminar)
        .accessibilityElement(children: .contain)
        .accessibilityAction(named: "Saltar la introducción", terminar)
        .task {
            if !sinMovimiento, let url = Bundle.main.url(forResource: "startup-intro", withExtension: "mp4") {
                // .ambient: no corta la música de quien abre la app (el vídeo no tiene sonido).
                try? AVAudioSession.sharedInstance().setCategory(.ambient)
                let nuevo = AVPlayer(url: url)
                nuevo.isMuted = true
                jugador = nuevo
                nuevo.play()
            }
            // Tope por si el vídeo no llega a terminar (falta el archivo, la app pasa a segundo plano...).
            do {
                try await Task.sleep(for: sinMovimiento ? .milliseconds(800) : .seconds(6))
            } catch {
                return // la intro ya se fue
            }
            terminar()
        }
        // El aviso puede llegar desde otro hilo: se pasa al principal antes de tocar el estado.
        .onReceive(NotificationCenter.default.publisher(for: AVPlayerItem.didPlayToEndTimeNotification)
            .receive(on: DispatchQueue.main)) { aviso in
            if let item = aviso.object as? AVPlayerItem, item === jugador?.currentItem { terminar() }
        }
    }

    private func terminar() {
        guard !terminada else { return }
        terminada = true
        jugador?.pause()
        alTerminar()
    }
}

/// `AVPlayerLayer` sin los controles de `VideoPlayer`, con el vídeo entero a la
/// vista (9:16) y el azul noche rellenando lo que sobre.
private struct VideoSinControles: UIViewRepresentable {
    let jugador: AVPlayer

    func makeUIView(context: Context) -> VistaCapa {
        let vista = VistaCapa()
        vista.backgroundColor = UIColor(red: 0x09 / 255, green: 0x0B / 255, blue: 0x1E / 255, alpha: 1)
        vista.capa.videoGravity = .resizeAspect
        vista.capa.player = jugador
        return vista
    }

    func updateUIView(_ vista: VistaCapa, context: Context) {
        vista.capa.player = jugador
    }

    final class VistaCapa: UIView {
        override class var layerClass: AnyClass { AVPlayerLayer.self }
        var capa: AVPlayerLayer { layer as! AVPlayerLayer }
    }
}
