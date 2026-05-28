import SwiftUI

extension Color {
    // Primary / Brand
    static let dobiPrimary       = Color(hex: 0x2F7DFF)
    static let dobiPrimaryDark   = Color(hex: 0x1E6BEF)
    static let dobiPrimaryLight  = Color(hex: 0x4A90FF)
    static let dobiPrimaryContainer = Color(hex: 0xDBEAFE)
    static let dobiPrimarySoft   = Color(hex: 0xEEF4FF)

    // Surfaces / Neutrals
    static let dobiBackground    = Color(hex: 0xFFFFFF)
    static let dobiSurface       = Color(hex: 0xFFFFFF)
    static let dobiSurfaceAlt    = Color(hex: 0xF8FAFF)
    static let dobiBorder        = Color(hex: 0xE5EBF5)
    static let dobiBorderMuted   = Color(hex: 0xF0F2F7)
    static let dobiPageBg        = Color(hex: 0xF3F6FD)

    // Text
    static let dobiTextPrimary   = Color(hex: 0x1A2B4A)
    static let dobiTextSecondary = Color(hex: 0x6B7A99)
    static let dobiTextTertiary  = Color(hex: 0xA0ADC2)

    // Semantic
    static let dobiSuccess           = Color(hex: 0x22C55E)
    static let dobiSuccessContainer  = Color(hex: 0xDCFCE7)
    static let dobiSuccessSoft       = Color(hex: 0x86EFAC)
    static let dobiWarning           = Color(hex: 0xF59E0B)
    static let dobiWarningContainer  = Color(hex: 0xFEF3C7)
    static let dobiError             = Color(hex: 0xEF4444)
    static let dobiErrorContainer    = Color(hex: 0xFEE2E2)

    // Parent Dashboard pastels
    static let dobiPastelCream        = Color(hex: 0xFFF8EC)
    static let dobiPastelLavender     = Color(hex: 0xE9EBFF)
    static let dobiPastelLavenderDeep = Color(hex: 0xD9DCFF)
    static let dobiPastelSkyBlue      = Color(hex: 0xE8F1FF)
    static let dobiPastelMint         = Color(hex: 0xDFF7E7)
    static let dobiPastelPink         = Color(hex: 0xFFE4E0)
    static let dobiPastelYellow       = Color(hex: 0xFFF1C7)
    static let dobiPastelCoral        = Color(hex: 0xFF6B5B)

    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red:   Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8)  & 0xFF) / 255,
            blue:  Double( hex        & 0xFF) / 255,
            opacity: alpha
        )
    }
}
