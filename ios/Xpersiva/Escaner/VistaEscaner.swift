import AVFoundation
import SwiftUI

/// Escáner de QR. La primera vez explica para qué es la cámara antes de que
/// salga el aviso del sistema.
struct VistaEscaner: View {
    let alEncontrar: (EnlaceUnion) -> Void

    @Environment(\.dismiss) private var cerrar
    @Environment(\.openURL) private var abrirURL
    @State private var permiso = AVCaptureDevice.authorizationStatus(for: .video)
    @State private var aviso: String?

    var body: some View {
        NavigationStack {
            Group {
                switch permiso {
                case .authorized:
                    ZStack(alignment: .bottom) {
                        CamaraQR { texto in
                            if let enlace = EnlaceUnion(texto: texto) {
                                UINotificationFeedbackGenerator().notificationOccurred(.success)
                                alEncontrar(enlace)
                            } else {
                                aviso = "Este QR no es de una experiencia Xpersiva."
                            }
                        }
                        .ignoresSafeArea()
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(.white, lineWidth: 3)
                            .frame(width: 240, height: 240)
                            .frame(maxHeight: .infinity)
                            .allowsHitTesting(false)
                        Text(aviso ?? "Apunta al QR de la pantalla")
                            .font(.headline).foregroundStyle(.white)
                            .padding().background(.black.opacity(0.5), in: Capsule())
                            .padding(.bottom, 40)
                    }
                case .notDetermined:
                    explicacion(boton: "Permitir cámara") {
                        Task {
                            _ = await AVCaptureDevice.requestAccess(for: .video)
                            permiso = AVCaptureDevice.authorizationStatus(for: .video)
                        }
                    }
                default:
                    explicacion(boton: "Abrir Ajustes") {
                        if let url = URL(string: UIApplication.openSettingsURLString) { abrirURL(url) }
                    }
                }
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { cerrar() } }
            }
        }
    }

    private func explicacion(boton: String, accion: @escaping () -> Void) -> some View {
        ContentUnavailableView {
            Label("Necesitamos la cámara", systemImage: "camera")
        } description: {
            Text("Solo para leer el código QR de la experiencia. No guardamos fotos ni vídeo.")
        } actions: {
            Button(boton, action: accion).buttonStyle(.borderedProminent)
        }
    }
}

/// Vista de cámara de AVFoundation que avisa con el texto de cada QR que ve.
private struct CamaraQR: UIViewControllerRepresentable {
    let alLeer: (String) -> Void

    func makeUIViewController(context: Context) -> ControladorCamara {
        ControladorCamara(alLeer: alLeer)
    }

    func updateUIViewController(_ controlador: ControladorCamara, context: Context) {}
}

private final class ControladorCamara: UIViewController, @preconcurrency AVCaptureMetadataOutputObjectsDelegate {
    private let alLeer: (String) -> Void
    // AVCaptureSession no es Sendable, pero solo la tocamos aquí y en la cola de arranque.
    nonisolated(unsafe) private let sesion = AVCaptureSession()
    private var ultimo: String?

    init(alLeer: @escaping (String) -> Void) {
        self.alLeer = alLeer
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) no se usa") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        guard let camara = AVCaptureDevice.default(for: .video),
              let entrada = try? AVCaptureDeviceInput(device: camara),
              sesion.canAddInput(entrada)
        else { return }
        sesion.addInput(entrada)

        let salida = AVCaptureMetadataOutput()
        guard sesion.canAddOutput(salida) else { return }
        sesion.addOutput(salida)
        salida.setMetadataObjectsDelegate(self, queue: .main)
        salida.metadataObjectTypes = [.qr]

        let capa = AVCaptureVideoPreviewLayer(session: sesion)
        capa.videoGravity = .resizeAspectFill
        capa.frame = view.bounds
        view.layer.addSublayer(capa)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        view.layer.sublayers?.first?.frame = view.bounds
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        nonisolated(unsafe) let sesion = self.sesion
        // startRunning bloquea: fuera del hilo principal.
        DispatchQueue.global(qos: .userInitiated).async { sesion.startRunning() }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        nonisolated(unsafe) let sesion = self.sesion
        DispatchQueue.global(qos: .userInitiated).async { sesion.stopRunning() }
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput,
                        didOutput metadataObjects: [AVMetadataObject],
                        from connection: AVCaptureConnection) {
        guard let qr = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              let texto = qr.stringValue, texto != ultimo
        else { return }
        ultimo = texto
        alLeer(texto)
    }
}
