import SwiftUI
import UIKit

private let BgDark        = Color(red: 0.043, green: 0.055, blue: 0.180)
private let PurplePrimary = Color(red: 0.424, green: 0.376, blue: 0.941)
private let PurpleSoft    = Color(red: 0.933, green: 0.925, blue: 1.000)
private let PurpleMid     = Color(red: 0.831, green: 0.816, blue: 1.000)
private let TextPrimary   = Color(red: 0.102, green: 0.169, blue: 0.290)
private let TextSecondary = Color(red: 0.420, green: 0.478, blue: 0.600)
private let TextTertiary  = Color(red: 0.627, green: 0.678, blue: 0.761)

// Replace with actual App Store URL once the app is live.
private let appStoreURL = "https://eduksha.in/download"

struct ForceUpdateView: View {
    var body: some View {
        ZStack {
            BgDark.ignoresSafeArea()
            StarDots()

            ScrollView {
                VStack {
                    Spacer(minLength: 32)

                    VStack(spacing: 0) {
                        // ── Illustration ─────────────────────────────────────
                        PhoneIllustration()
                            .padding(.top, 28)
                            .padding(.bottom, 20)

                        // ── Title ─────────────────────────────────────────────
                        HStack(spacing: 12) {
                            ZStack {
                                Circle()
                                    .fill(PurpleSoft)
                                    .frame(width: 44, height: 44)
                                Image(systemName: "arrow.up")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundStyle(PurplePrimary)
                            }
                            Text("Update Required")
                                .font(.system(size: 24, weight: .heavy))
                                .foregroundStyle(TextPrimary)
                        }
                        .padding(.bottom, 14)

                        // ── Body ──────────────────────────────────────────────
                        Text("We've released a new update with improvements and bug fixes to make Eduksha even better for you.")
                            .font(.system(size: 14))
                            .foregroundStyle(TextSecondary)
                            .multilineTextAlignment(.center)
                            .lineSpacing(4)
                            .padding(.horizontal, 8)
                            .padding(.bottom, 20)

                        // ── Why update card ───────────────────────────────────
                        HStack(alignment: .top, spacing: 14) {
                            ZStack {
                                Circle()
                                    .fill(PurpleMid)
                                    .frame(width: 42, height: 42)
                                Image(systemName: "checkmark.shield.fill")
                                    .font(.system(size: 20))
                                    .foregroundStyle(PurplePrimary)
                            }
                            VStack(alignment: .leading, spacing: 3) {
                                Text("Why update?")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundStyle(TextPrimary)
                                Text("This update includes important improvements and is required to keep using the app.")
                                    .font(.system(size: 13))
                                    .foregroundStyle(TextSecondary)
                                    .lineSpacing(3)
                            }
                            Spacer()
                        }
                        .padding(16)
                        .background(PurpleSoft)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .padding(.bottom, 24)

                        // ── Update Now button ─────────────────────────────────
                        Button {
                            if let url = URL(string: appStoreURL) {
                                UIApplication.shared.open(url)
                            }
                        } label: {
                            HStack(spacing: 10) {
                                Image(systemName: "arrow.down.circle.fill")
                                    .font(.system(size: 18))
                                Text("Update Now")
                                    .font(.system(size: 16, weight: .bold))
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .foregroundStyle(.white)
                            .background(PurplePrimary)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                        .padding(.bottom, 16)

                        // ── Footer ────────────────────────────────────────────
                        HStack(spacing: 6) {
                            Image(systemName: "lock.fill")
                                .font(.system(size: 11))
                                .foregroundStyle(TextTertiary)
                            Text("Your data is safe and secure")
                                .font(.system(size: 12))
                                .foregroundStyle(TextTertiary)
                        }
                        .padding(.bottom, 28)
                    }
                    .padding(.horizontal, 24)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 28))
                    .padding(.horizontal, 20)

                    Spacer(minLength: 32)
                }
            }
        }
        .navigationBarHidden(true)
    }
}

// MARK: - Phone illustration

private struct PhoneIllustration: View {
    var body: some View {
        ZStack {
            // Soft blob
            Circle()
                .fill(PurpleSoft)
                .frame(width: 150, height: 150)

            // Phone shell
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(red: 0.165, green: 0.125, blue: 0.396))
                .frame(width: 88, height: 144)
                .shadow(color: .black.opacity(0.25), radius: 10, x: 0, y: 6)
                .overlay(
                    // Phone screen
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.white)
                        .frame(width: 74, height: 130)
                        .overlay(
                            VStack(spacing: 4) {
                                // App icon
                                RoundedRectangle(cornerRadius: 9)
                                    .fill(PurplePrimary)
                                    .frame(width: 34, height: 34)
                                    .overlay(
                                        Image(systemName: "sparkles")
                                            .font(.system(size: 16, weight: .bold))
                                            .foregroundStyle(.white)
                                    )
                                Text("Eduksha")
                                    .font(.system(size: 8, weight: .semibold))
                                    .foregroundStyle(TextPrimary)
                                Spacer().frame(height: 4)
                                Text("Updating...")
                                    .font(.system(size: 7))
                                    .foregroundStyle(TextSecondary)
                                Spacer().frame(height: 3)
                                // Progress bar
                                ZStack(alignment: .leading) {
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(Color(white: 0.9))
                                        .frame(height: 5)
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(PurplePrimary)
                                        .frame(width: 30, height: 5)
                                }
                                .frame(width: 52)
                            }
                        )
                )

            // Cloud download badge (bottom-right)
            ZStack {
                Circle()
                    .fill(PurplePrimary)
                    .frame(width: 48, height: 48)
                Image(systemName: "icloud.and.arrow.down.fill")
                    .font(.system(size: 22))
                    .foregroundStyle(.white)
            }
            .offset(x: 52, y: 28)
        }
        .frame(height: 172)
    }
}

// MARK: - Decorative dots

private struct StarDots: View {
    private let dots: [(CGFloat, CGFloat, CGFloat)] = [
        (0.08, 0.10, 8), (0.92, 0.07, 6), (0.04, 0.52, 10),
        (0.96, 0.44, 7), (0.14, 0.90, 5), (0.86, 0.84, 9), (0.50, 0.04, 6),
    ]

    var body: some View {
        GeometryReader { geo in
            ForEach(dots.indices, id: \.self) { i in
                let (xF, yF, size) = dots[i]
                Circle()
                    .fill(Color.white.opacity(0.12))
                    .frame(width: size, height: size)
                    .position(x: geo.size.width * xF, y: geo.size.height * yF)
            }
        }
        .ignoresSafeArea()
    }
}
