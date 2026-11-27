package com.dashdeck.app.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ThemePresetIdSerializer : KSerializer<ThemePresetId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ThemePresetId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ThemePresetId) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): ThemePresetId {
        return ThemePresetId.fromSerializedName(decoder.decodeString())
    }
}

@Serializable(with = ThemePresetIdSerializer::class)
enum class ThemePresetId(val displayName: String) {
    FOLLOW_SYSTEM("Follow System"),
    DARK_SLATE("Dark Slate"),
    LIGHT("Light"),
    FROSTED_GLASS("Frosted Glass"),
    AMOLED_BLACK("AMOLED Black"),
    CUSTOM("Custom");

    companion object {
        fun fromSerializedName(raw: String): ThemePresetId = when (raw.trim().uppercase()) {
            "FOLLOW_SYSTEM", "SYSTEM_AUTO" -> FOLLOW_SYSTEM
            "DARK_SLATE", "NORD_SLATE" -> DARK_SLATE
            "LIGHT", "CLEAN_PAPER_LIGHT" -> LIGHT
            "FROSTED_GLASS" -> FROSTED_GLASS
            "AMOLED_BLACK", "MIDNIGHT_OLED" -> AMOLED_BLACK
            "CUSTOM" -> CUSTOM
            else -> FOLLOW_SYSTEM
        }
    }
}

@Serializable
data class ThemeTokens(
    val presetId: ThemePresetId = ThemePresetId.FOLLOW_SYSTEM,
    val followSystemDarkMode: Boolean = true,
    val isDark: Boolean = true,
    val backgroundHex: String = "#111418",
    val surfaceHex: String = "#181C22",
    val accentHex: String = "#6366F1",
    val textPrimaryHex: String = "#F1F5F9",
    val textSecondaryHex: String = "#94A3B8",
    val borderHex: String = "#272E3B",
    val backgroundAlpha: Float = 0.94f,
    val cornerRadiusDp: Int = 20,
    val contentSpacingDp: Int = 12,
) {
    fun validated(): ThemeTokens = copy(
        backgroundHex = normalizeHexColor(backgroundHex, "#111418"),
        surfaceHex = normalizeHexColor(surfaceHex, "#181C22"),
        accentHex = normalizeHexColor(accentHex, "#6366F1"),
        textPrimaryHex = normalizeHexColor(textPrimaryHex, "#F1F5F9"),
        textSecondaryHex = normalizeHexColor(textSecondaryHex, "#94A3B8"),
        borderHex = normalizeHexColor(borderHex, "#272E3B"),
        backgroundAlpha = backgroundAlpha.coerceIn(0.20f, 1.0f),
        cornerRadiusDp = cornerRadiusDp.coerceIn(0, 32),
        contentSpacingDp = contentSpacingDp.coerceIn(4, 24),
    )

    fun withAutoContrastText(): ThemeTokens {
        val normSurface = normalizeHexColor(surfaceHex, "#181C22")
        val surfaceLum = hexLuminance(normSurface)
        val darkSurface = surfaceLum < 0.50f
        return copy(
            isDark = darkSurface,
            textPrimaryHex = if (darkSurface) "#F8FAFC" else "#0F172A",
            textSecondaryHex = if (darkSurface) "#94A3B8" else "#475569",
            borderHex = if (darkSurface) "#2D3545" else "#CBD5E1",
        ).validated()
    }
}

object ThemeSwatches {
    val backgroundSwatches: List<String> = listOf(
        "#000000",
        "#0B0F14",
        "#111418",
        "#121821",
        "#1E1B2E",
        "#0F1D1A",
        "#F8FAFC",
        "#F1F5F9",
    )

    val surfaceSwatches: List<String> = listOf(
        "#121216",
        "#181C22",
        "#1E293B",
        "#252238",
        "#182C25",
        "#27272A",
        "#FFFFFF",
        "#F1F5F9",
    )

    val accentSwatches: List<String> = listOf(
        "#6366F1",
        "#38BDF8",
        "#10B981",
        "#F59E0B",
        "#EC4899",
        "#EF4444",
        "#8B5CF6",
        "#14B8A6",
    )
}

object BuiltInThemes {

    val FollowSystem = ThemeTokens(
        presetId = ThemePresetId.FOLLOW_SYSTEM,
        followSystemDarkMode = true,
        isDark = false,
        backgroundHex = "#F8FAFC",
        surfaceHex = "#FFFFFF",
        accentHex = "#4F46E5",
        textPrimaryHex = "#0F172A",
        textSecondaryHex = "#475569",
        borderHex = "#E2E8F0",
        backgroundAlpha = 0.96f,
        cornerRadiusDp = 20,
        contentSpacingDp = 12,
    )

    val FollowSystemDarkFallback = ThemeTokens(
        presetId = ThemePresetId.FOLLOW_SYSTEM,
        followSystemDarkMode = true,
        isDark = true,
        backgroundHex = "#111418",
        surfaceHex = "#181C22",
        accentHex = "#6366F1",
        textPrimaryHex = "#F1F5F9",
        textSecondaryHex = "#94A3B8",
        borderHex = "#272E3B",
        backgroundAlpha = 0.94f,
        cornerRadiusDp = 20,
        contentSpacingDp = 12,
    )

    val DarkSlate = ThemeTokens(
        presetId = ThemePresetId.DARK_SLATE,
        followSystemDarkMode = false,
        isDark = true,
        backgroundHex = "#111418",
        surfaceHex = "#181C22",
        accentHex = "#6366F1",
        textPrimaryHex = "#F1F5F9",
        textSecondaryHex = "#94A3B8",
        borderHex = "#272E3B",
        backgroundAlpha = 0.94f,
        cornerRadiusDp = 20,
        contentSpacingDp = 12,
    )

    val Light = ThemeTokens(
        presetId = ThemePresetId.LIGHT,
        followSystemDarkMode = false,
        isDark = false,
        backgroundHex = "#F8FAFC",
        surfaceHex = "#FFFFFF",
        accentHex = "#4F46E5",
        textPrimaryHex = "#0F172A",
        textSecondaryHex = "#475569",
        borderHex = "#E2E8F0",
        backgroundAlpha = 0.96f,
        cornerRadiusDp = 20,
        contentSpacingDp = 12,
    )

    val FrostedGlass = ThemeTokens(
        presetId = ThemePresetId.FROSTED_GLASS,
        followSystemDarkMode = false,
        isDark = true,
        backgroundHex = "#121821",
        surfaceHex = "#1E293B",
        accentHex = "#38BDF8",
        textPrimaryHex = "#F8FAFC",
        textSecondaryHex = "#CBD5E1",
        borderHex = "#475569",
        backgroundAlpha = 0.72f,
        cornerRadiusDp = 22,
        contentSpacingDp = 12,
    )

    val AmoledBlack = ThemeTokens(
        presetId = ThemePresetId.AMOLED_BLACK,
        followSystemDarkMode = false,
        isDark = true,
        backgroundHex = "#000000",
        surfaceHex = "#121216",
        accentHex = "#6366F1",
        textPrimaryHex = "#FFFFFF",
        textSecondaryHex = "#A1A1AA",
        borderHex = "#27272A",
        backgroundAlpha = 1.0f,
        cornerRadiusDp = 18,
        contentSpacingDp = 12,
    )

    val allPresets: List<ThemeTokens> = listOf(
        FollowSystem,
        DarkSlate,
        Light,
        FrostedGlass,
        AmoledBlack,
    )

    fun byId(id: ThemePresetId): ThemeTokens = when (id) {
        ThemePresetId.FOLLOW_SYSTEM -> FollowSystem
        ThemePresetId.DARK_SLATE -> DarkSlate
        ThemePresetId.LIGHT -> Light
        ThemePresetId.FROSTED_GLASS -> FrostedGlass
        ThemePresetId.AMOLED_BLACK -> AmoledBlack
        ThemePresetId.CUSTOM -> DarkSlate.copy(presetId = ThemePresetId.CUSTOM)
    }
}

fun normalizeHexColor(raw: String, fallback: String): String {
    val cleaned = raw.trim().removePrefix("#").uppercase()
    return when {
        cleaned.length == 6 && cleaned.all { it in '0'..'9' || it in 'A'..'F' } -> "#$cleaned"
        cleaned.length == 8 && cleaned.all { it in '0'..'9' || it in 'A'..'F' } -> "#$cleaned"
        else -> fallback
    }
}

fun parseHexColorToArgbLong(hex: String, fallback: Long = 0xFF111418L): Long {
    val cleaned = hex.trim().removePrefix("#")
    val parsed = cleaned.toLongOrNull(16) ?: return fallback
    return when (cleaned.length) {
        6 -> 0xFF000000L or parsed
        8 -> parsed
        else -> fallback
    }
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF111418)): Color {
    val argb = parseHexColorToArgbLong(hex, fallback = -1L)
    return if (argb == -1L) fallback else Color(argb)
}

fun hexLuminance(hex: String): Float {
    val argb = parseHexColorToArgbLong(hex, fallback = 0xFF181C22L)
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return 0.299f * r + 0.587f * g + 0.114f * b
}
