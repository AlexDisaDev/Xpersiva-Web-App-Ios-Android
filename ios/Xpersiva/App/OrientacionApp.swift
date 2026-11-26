import UIKit

/// La app es de retrato por defecto, pero el mando por giroscopio necesita
/// horizontal. `VistaMando` cambia este valor al entrar/salir de esa pantalla
/// y pide el giro con `requestGeometryUpdate`; iOS consulta este delegate
/// para saber qué orientaciones admitir mientras tanto.
final class OrientacionApp: NSObject, UIApplicationDelegate {
    static var permitidas: UIInterfaceOrientationMask = .portrait

    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        Self.permitidas
    }
}
