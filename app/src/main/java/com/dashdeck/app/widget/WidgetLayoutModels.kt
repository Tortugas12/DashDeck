package com.dashdeck.app.widget

import com.dashdeck.app.data.db.DashboardModuleType
import com.dashdeck.app.data.db.ModuleCardSize
import com.dashdeck.app.data.db.ModuleSizeTier
import com.dashdeck.app.data.db.WidgetType
import kotlinx.serialization.Serializable

@Serializable
enum class RowTemplate(val displayName: String) {
    FULL_WIDTH("Full Width"),
    SPLIT_TWO("2-Column Split"),
    LEFT_TALL_RIGHT_STACK("1 Left + 2 Right"),
    LEFT_STACK_RIGHT_TALL("2 Left + 1 Right"),
}

@Serializable
enum class SplitWidthRatio(val label: String, val leftFraction: Float) {
    EVEN_50_50("50 / 50", 0.50f),
    LEFT_60_40("60 / 40", 0.60f),
    RIGHT_40_60("40 / 60", 0.40f),
    LEFT_65_35("65 / 35", 0.65f),
    RIGHT_35_65("35 / 65", 0.35f),
}

@Serializable
enum class CardHeightPreset(val shortLabel: String, val targetHeightDp: Int?) {
    AUTO("Auto", null),
    COMPACT("Compact", 96),
    MEDIUM("Medium", 152),
    TALL("Tall", 224),
}

@Serializable
enum class ClockStylePreset(val displayName: String) {
    CLASSIC("Classic"),
    EDITORIAL("Editorial"),
    SPLIT("Split"),
}

@Serializable
enum class ClockHorizontalAlignment(val displayName: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right"),
}

@Serializable
data class WidgetSlotSpec(
    val module: DashboardModuleType,
    val sizeTier: ModuleSizeTier = ModuleSizeTier.STANDARD,
    val heightPreset: CardHeightPreset = CardHeightPreset.AUTO,
)

@Serializable
data class WidgetRowSpec(
    val id: String,
    val template: RowTemplate = RowTemplate.FULL_WIDTH,
    val splitRatio: SplitWidthRatio = SplitWidthRatio.EVEN_50_50,
    val leftSlots: List<WidgetSlotSpec> = emptyList(),
    val rightSlots: List<WidgetSlotSpec> = emptyList(),
) {
    fun allSlots(): List<WidgetSlotSpec> = leftSlots + rightSlots
}

fun defaultLayoutRowsFor(type: WidgetType): List<WidgetRowSpec> = when (type) {
    WidgetType.DASHBOARD -> listOf(
        WidgetRowSpec(
            id = "row_1",
            template = RowTemplate.SPLIT_TWO,
            splitRatio = SplitWidthRatio.EVEN_50_50,
            leftSlots = listOf(WidgetSlotSpec(DashboardModuleType.CLOCK)),
            rightSlots = listOf(WidgetSlotSpec(DashboardModuleType.MEDIA)),
        ),
        WidgetRowSpec(
            id = "row_2",
            template = RowTemplate.SPLIT_TWO,
            splitRatio = SplitWidthRatio.EVEN_50_50,
            leftSlots = listOf(WidgetSlotSpec(DashboardModuleType.CALENDAR)),
            rightSlots = listOf(WidgetSlotSpec(DashboardModuleType.NOTIFICATIONS)),
        ),
        WidgetRowSpec(
            id = "row_3",
            template = RowTemplate.SPLIT_TWO,
            splitRatio = SplitWidthRatio.EVEN_50_50,
            leftSlots = listOf(WidgetSlotSpec(DashboardModuleType.NOTES)),
            rightSlots = listOf(WidgetSlotSpec(DashboardModuleType.LAUNCHER)),
        ),
    )

    WidgetType.MODULAR -> listOf(
        WidgetRowSpec(
            id = "row_1",
            template = RowTemplate.SPLIT_TWO,
            splitRatio = SplitWidthRatio.EVEN_50_50,
            leftSlots = listOf(WidgetSlotSpec(DashboardModuleType.CLOCK)),
            rightSlots = listOf(WidgetSlotSpec(DashboardModuleType.MEDIA)),
        ),
    )
}

fun legacyModulesToLayoutRows(
    enabledModules: List<DashboardModuleType>,
    moduleSizes: Map<DashboardModuleType, ModuleCardSize>,
): List<WidgetRowSpec> {
    if (enabledModules.isEmpty()) return emptyList()
    val rows = mutableListOf<WidgetRowSpec>()
    var pendingHalf: WidgetSlotSpec? = null
    var rowIdx = 1

    for (module in enabledModules) {
        val size = moduleSizes[module] ?: ModuleCardSize()
        val slot = WidgetSlotSpec(module = module, sizeTier = size.tier)
        if (size.fullWidth) {
            if (pendingHalf != null) {
                rows += WidgetRowSpec(
                    id = "row_${rowIdx++}",
                    template = RowTemplate.SPLIT_TWO,
                    leftSlots = listOf(pendingHalf),
                    rightSlots = emptyList(),
                )
                pendingHalf = null
            }
            rows += WidgetRowSpec(
                id = "row_${rowIdx++}",
                template = RowTemplate.FULL_WIDTH,
                leftSlots = listOf(slot),
            )
        } else {
            if (pendingHalf == null) {
                pendingHalf = slot
            } else {
                rows += WidgetRowSpec(
                    id = "row_${rowIdx++}",
                    template = RowTemplate.SPLIT_TWO,
                    leftSlots = listOf(pendingHalf),
                    rightSlots = listOf(slot),
                )
                pendingHalf = null
            }
        }
    }

    if (pendingHalf != null) {
        rows += WidgetRowSpec(
            id = "row_${rowIdx++}",
            template = if (rows.isEmpty()) RowTemplate.FULL_WIDTH else RowTemplate.SPLIT_TWO,
            leftSlots = listOf(pendingHalf),
            rightSlots = emptyList(),
        )
    }

    return rows
}
