import SwiftUI

/// Mismos colores que `Colores.kt` en Android: acento morado y verde de
/// "conectado". El resto de la UI usa `.tint`/colores del sistema.
extension Color {
    static let acento = Color(red: 0x6D / 255, green: 0x4A / 255, blue: 0xFF / 255)
    static let ahora = Color(red: 0x1F / 255, green: 0x9D / 255, blue: 0x55 / 255)
    static let malo = Color(red: 0xD1 / 255, green: 0x43 / 255, blue: 0x4A / 255)
}
