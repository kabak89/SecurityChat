import SwiftUI
import shared

struct ContentView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        RootViewControllerKt.rootViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }

}
