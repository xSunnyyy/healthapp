package com.sunny.healthapp.ui.theme

import androidx.compose.ui.graphics.Color

// Deep navy-ink base — never pure black. The whole app sits on this.
val Ink950 = Color(0xFF07080E)
val Ink900 = Color(0xFF0B0D14)
val Ink850 = Color(0xFF10131C)
val Ink800 = Color(0xFF161A25)
val Ink750 = Color(0xFF1C2130)
val Ink700 = Color(0xFF222839)
val Ink600 = Color(0xFF2C3346)
val Ink500 = Color(0xFF394056)

// Typographic greys
val TextPrimary = Color(0xFFF1F2F6)
val TextSecondary = Color(0xFFA8ADBD)
val TextMuted = Color(0xFF656B7E)
val TextDim = Color(0xFF3F465A)

// Hairlines / edges
val EdgeBright = Color(0x26FFFFFF)
val EdgeSoft = Color(0x14FFFFFF)
val EdgeFaint = Color(0x08FFFFFF)

// Tile gradient pairs — bright glow at one corner fading to dim, sitting on the ink base.
// Designed to feel like backlit pastel on glass.
val TileCoolStart = Color(0xFF7090FF)   // cornflower
val TileCoolEnd = Color(0xFF1C2452)
val TileWarmStart = Color(0xFFFFB089)   // apricot
val TileWarmEnd = Color(0xFF4A2A22)
val TileSoftStart = Color(0xFFD7B7FF)   // lavender
val TileSoftEnd = Color(0xFF2E1F4F)
val TileRoseStart = Color(0xFFFF9BB3)   // soft rose
val TileRoseEnd = Color(0xFF4E1F2D)
val TileMintStart = Color(0xFF8FE0C6)   // mint
val TileMintEnd = Color(0xFF1F4438)

// Functional accents
val Accent = Color(0xFF6F8DFF)          // primary action / focus
val AccentSoft = Color(0xFF93AAFF)
val AccentDeep = Color(0xFF3858DA)

val Crimson = Color(0xFFFF5A6E)          // reserved for heart + warnings
val CrimsonSoft = Color(0xFFFF93A0)
val CrimsonDeep = Color(0xFFB72D40)

val Sunflare = Color(0xFFFFB962)         // calories / warm-toned
val Sunflare2 = Color(0xFFFF8C5C)

val Lavender = Color(0xFFB99CFF)
val LavenderDeep = Color(0xFF6A4FBE)

val MintGlow = Color(0xFF7FE0BD)

// Back-compat aliases (used by older screens until they are rewritten)
val SleepBlue = Accent
val SleepBlueDeep = AccentDeep
val ReadinessLilac = Lavender
val ReadinessLilacDeep = LavenderDeep
val ActivityGreen = Accent
val ActivityGreenDeep = AccentDeep
val WarmPeach = Sunflare
val HeartRed = Crimson
val Amber = Sunflare
val GlassEdgeLight = EdgeBright
val GlassEdgeDark = EdgeSoft
val InnerHighlight = EdgeSoft
val HeroBlack = Color(0xFF030406)
