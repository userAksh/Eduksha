package com.dobi.app.feature.streak

import androidx.annotation.DrawableRes
import com.dobi.app.R
import com.dobi.app.domain.model.ClanTier

/**
 * Resolves the badge drawable for a clan tier. Returns null for [ClanTier.NONE]
 * (no clan yet — caller should render an empty/placeholder state).
 */
@DrawableRes
fun ClanTier.iconRes(): Int? = when (this) {
    ClanTier.NEXT_GEN_CHAMPS -> R.drawable.clan_next_gen_champs
    ClanTier.RISING_LEGENDS -> R.drawable.clan_rising_legends
    ClanTier.STREAK_WARRIOR -> R.drawable.clan_streak_warrior
    ClanTier.NONE -> null
}
