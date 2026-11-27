package com.dashdeck.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dashdeck.app.theme.BuiltInThemes
import com.dashdeck.app.theme.ThemePresetId
import com.dashdeck.app.theme.ThemeSwatches
import com.dashdeck.app.theme.ThemeTokens
import com.dashdeck.app.theme.parseHexColor
import com.dashdeck.app.ui.settings.TokenSlider
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedThemePicker(
    tokens: ThemeTokens,
    onTokensChange: (ThemeTokens) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Theme Studio",
    showSpacingSlider: Boolean = false,
) {
    DashDeckCard(modifier = modifier) {
        ModuleCardHeader(title = title, icon = Icons.Outlined.Palette)
        Spacer(modifier = Modifier.height(10.dp))

        // 5 built-in presets
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BuiltInThemes.allPresets.forEach { preset ->
                val isSelected = tokens.presetId == preset.presetId
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = if (isSelected) 0.92f else 0.45f,
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                    modifier = Modifier
                        .desktopHoverHighlight(cornerRadius = 14.dp)
                        .clickable { onTokensChange(preset) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(preset.backgroundHex))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(preset.accentHex)),
                            )
                        }
                        Text(
                            text = preset.presetId.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Background swatches
        SwatchRow(
            label = "Background",
            swatches = ThemeSwatches.backgroundSwatches,
            selectedHex = tokens.backgroundHex,
            onPick = { hex ->
                onTokensChange(
                    tokens.copy(
                        presetId = ThemePresetId.CUSTOM,
                        followSystemDarkMode = false,
                        backgroundHex = hex,
                    ).withAutoContrastText(),
                )
            },
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Card Surface swatches
        SwatchRow(
            label = "Card Surface",
            swatches = ThemeSwatches.surfaceSwatches,
            selectedHex = tokens.surfaceHex,
            onPick = { hex ->
                onTokensChange(
                    tokens.copy(
                        presetId = ThemePresetId.CUSTOM,
                        followSystemDarkMode = false,
                        surfaceHex = hex,
                    ).withAutoContrastText(),
                )
            },
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Accent swatches
        SwatchRow(
            label = "Accent",
            swatches = ThemeSwatches.accentSwatches,
            selectedHex = tokens.accentHex,
            onPick = { hex ->
                onTokensChange(
                    tokens.copy(
                        presetId = ThemePresetId.CUSTOM,
                        followSystemDarkMode = false,
                        accentHex = hex,
                    ).validated(),
                )
            },
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Opacity · ${(tokens.backgroundAlpha * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
        )
        TokenSlider(
            value = tokens.backgroundAlpha,
            valueRange = 0.20f..1.0f,
            onValueChange = { alpha ->
                onTokensChange(tokens.copy(backgroundAlpha = alpha))
            },
        )

        Text(
            text = "Corner Radius · ${tokens.cornerRadiusDp}dp",
            style = MaterialTheme.typography.labelMedium,
        )
        TokenSlider(
            value = tokens.cornerRadiusDp.toFloat(),
            valueRange = 0f..32f,
            onValueChange = { radius ->
                onTokensChange(tokens.copy(cornerRadiusDp = radius.roundToInt()))
            },
        )

        if (showSpacingSlider) {
            Text(
                text = "Content Spacing · ${tokens.contentSpacingDp}dp",
                style = MaterialTheme.typography.labelMedium,
            )
            TokenSlider(
                value = tokens.contentSpacingDp.toFloat(),
                valueRange = 6f..24f,
                onValueChange = { spacing ->
                    onTokensChange(tokens.copy(contentSpacingDp = spacing.roundToInt()))
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(
    label: String,
    swatches: List<String>,
    selectedHex: String,
    onPick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            swatches.forEach { hex ->
                val selected = selectedHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(if (selected) 32.dp else 26.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(hex))
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape,
                        )
                        .clickable { onPick(hex) },
                )
            }
        }
    }
}
