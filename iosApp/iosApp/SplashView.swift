import SwiftUI
import AVKit
import FirebaseFirestore

struct SplashView: View {
    @EnvironmentObject var appState: AppState
    @State private var handedOff = false
    @State private var videoFinished = false
    @State private var routeResolved = false
    @State private var needsForceUpdate = false

    private let player: AVPlayer? = {
        guard let url = Bundle.main.url(forResource: "splash_intro", withExtension: "mp4") else { return nil }
        let p = AVPlayer(url: url)
        p.volume = 0
        return p
    }()

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let p = player {
                SplashVideoPlayer(player: p)
                    .ignoresSafeArea()
            }
        }
        .ignoresSafeArea()
        .onTapGesture { requestHandoff() }
        .onAppear {
            setupPlayer()
            resolveRoute()
            // 10s hard ceiling — same as Android MAX_SPLASH_MS
            Task {
                try? await Task.sleep(nanoseconds: 10_000_000_000)
                videoFinished = true
                tryHandOff()
            }
        }
        .fullScreenCover(isPresented: $handedOff) {
            AppRootView()
                .environmentObject(appState)
        }
        .fullScreenCover(isPresented: $needsForceUpdate) {
            ForceUpdateView()
        }
    }

    private func setupPlayer() {
        guard let player else {
            // No video bundled — skip straight to handoff
            videoFinished = true
            tryHandOff()
            return
        }
        NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: player.currentItem,
            queue: .main
        ) { _ in
            videoFinished = true
            tryHandOff()
        }
        player.play()
    }

    private func resolveRoute() {
        Task { @MainActor in
            // Check minimum required build number. Fail-open if Firestore is
            // unreachable — don't block offline users on a network error.
            let currentBuild = Int(Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "0") ?? 0
            let minBuild: Int = await {
                do {
                    let doc = try await Firestore.firestore()
                        .collection("dobi_meta").document("appConfig").getDocument()
                    return Int(doc.get("minVersionCodeIos") as? Int64 ?? 0)
                } catch {
                    NSLog("[SplashView] version check failed (fail-open): \(error)")
                    return 0
                }
            }()

            if currentBuild < minBuild {
                NSLog("[SplashView] force update: build=\(currentBuild) < min=\(minBuild)")
                needsForceUpdate = true
                return
            }

            // AppState.init() restores auth immediately (simulator: UserDefaults,
            // device: Firebase Auth listener). 200ms headroom before video ends
            // so AppRootView can navigate without a blank frame.
            try? await Task.sleep(nanoseconds: 200_000_000)
            routeResolved = true
            tryHandOff()
        }
    }

    private func requestHandoff() {
        videoFinished = true
        tryHandOff()
    }

    private func tryHandOff() {
        guard videoFinished && routeResolved && !handedOff else { return }
        withAnimation(.easeInOut(duration: 0.3)) { handedOff = true }
    }
}

// Full-screen AVPlayerLayer wrapper — resizeAspectFill mirrors Android's RESIZE_MODE_ZOOM
private struct SplashVideoPlayer: UIViewRepresentable {
    let player: AVPlayer

    func makeUIView(context: Context) -> PlayerUIView { PlayerUIView(player: player) }
    func updateUIView(_ uiView: PlayerUIView, context: Context) {}

    final class PlayerUIView: UIView {
        override class var layerClass: AnyClass { AVPlayerLayer.self }
        private var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }

        init(player: AVPlayer) {
            super.init(frame: .zero)
            playerLayer.player = player
            playerLayer.videoGravity = .resizeAspectFill
            backgroundColor = .black
        }
        required init?(coder: NSCoder) { fatalError() }
    }
}
