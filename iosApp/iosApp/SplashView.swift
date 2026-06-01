import SwiftUI
import AVKit

struct SplashView: View {
    @EnvironmentObject var appState: AppState
    @State private var handedOff = false
    @State private var videoFinished = false
    @State private var routeResolved = false

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
        // AppState.init() restores auth immediately (simulator: UserDefaults,
        // device: Firebase Auth listener). 200ms is enough headroom before
        // the video ends so AppRootView navigates without a blank frame.
        Task { @MainActor in
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
