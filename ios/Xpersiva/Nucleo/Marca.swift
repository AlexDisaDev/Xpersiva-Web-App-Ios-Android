import SwiftUI

/// Colores de la marca (rama `brand`, `brand/INSTALL_FOR_CLAUDE.md`). Los mismos
/// que `Colores` en `ui/Tema.kt` de Android.
extension Color {
    static let acento = Color(hex: 0x6D4AFF)
    static let cian = Color(hex: 0x42E3F2)
    static let noche = Color(hex: 0x090B1E)
    static let fondo = Color(hex: 0xF5F6FA)
    static let texto = Color(hex: 0x111318)
    static let textoSuave = Color(hex: 0x5B6170)
    static let borde = Color(hex: 0xE3E5EC)
    static let acentoSuave = Color(hex: 0xECE8FF)
    static let ahora = Color(hex: 0x1F9D55)
    static let malo = Color(hex: 0xD1434A)

    init(hex: UInt32) {
        self.init(red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255)
    }
}

/// Botón principal de la app ("ESCANEAR QR"): el mismo abajo del inicio y en la ficha.
struct BotonPrincipal: View {
    let titulo: String
    var icono: String? = "qrcode.viewfinder"
    let accion: () -> Void

    var body: some View {
        Button(action: accion) {
            HStack(spacing: 10) {
                if let icono { Image(systemName: icono).font(.title3.weight(.semibold)) }
                Text(titulo).font(.headline.weight(.heavy)).tracking(1)
            }
            .frame(maxWidth: .infinity, minHeight: 60)
        }
        .buttonStyle(EstiloBotonPrincipal())
    }
}

/// Violeta lleno, esquinas de 18 pt; más apagado si está desactivado y al pulsar.
struct EstiloBotonPrincipal: ButtonStyle {
    @Environment(\.isEnabled) private var activo

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundStyle(.white)
            .background(Color.acento.opacity(activo ? (configuration.isPressed ? 0.85 : 1) : 0.4),
                        in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

/// Punto verde de "en directo".
struct PuntoEnDirecto: View {
    var color: Color = .ahora
    var body: some View { Circle().fill(color).frame(width: 8, height: 8) }
}
