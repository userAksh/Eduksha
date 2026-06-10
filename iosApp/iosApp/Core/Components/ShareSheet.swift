import SwiftUI
import UIKit

// UIActivityViewController wrapped for SwiftUI sheet presentation.
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ vc: UIActivityViewController, context: Context) {}
}

// Identifiable wrapper so .sheet(item:) can drive presentation from a String.
struct ShareItem: Identifiable {
    let id = UUID()
    let text: String
}
