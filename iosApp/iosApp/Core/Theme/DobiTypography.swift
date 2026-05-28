import SwiftUI

// SF Pro is the native system font on iOS — no embedding needed.
// These match the SFProBold / SFProSemiBold / SFProMedium / SFProRegular weights used in Android.

extension Font {
    static func dobiLargeTitle()  -> Font { .system(size: 28, weight: .bold) }
    static func dobiTitle1()      -> Font { .system(size: 22, weight: .bold) }
    static func dobiTitle2()      -> Font { .system(size: 18, weight: .semibold) }
    static func dobiTitle3()      -> Font { .system(size: 16, weight: .semibold) }
    static func dobiBody()        -> Font { .system(size: 15, weight: .regular) }
    static func dobiBodyMedium()  -> Font { .system(size: 15, weight: .medium) }
    static func dobiBodyBold()    -> Font { .system(size: 15, weight: .bold) }
    static func dobiCaption()     -> Font { .system(size: 13, weight: .regular) }
    static func dobiCaptionMedium() -> Font { .system(size: 13, weight: .medium) }
    static func dobiCaptionBold() -> Font { .system(size: 13, weight: .semibold) }
    static func dobiSmall()       -> Font { .system(size: 11, weight: .regular) }
}
