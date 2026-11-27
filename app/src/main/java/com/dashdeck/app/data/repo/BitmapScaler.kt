package com.dashdeck.app.data.repo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapScaler {

    fun downscaleBitmap(source: Bitmap, maxDimensionPx: Int): Bitmap {
        if (maxDimensionPx <= 0) return source
        val width = source.width
        val height = source.height
        if (width <= maxDimensionPx && height <= maxDimensionPx) {
            return source
        }
        val scale = maxDimensionPx.toFloat() / max(width, height).toFloat()
        val targetWidth = max(1, (width * scale).roundToInt())
        val targetHeight = max(1, (height * scale).roundToInt())
        return source.scale(targetWidth, targetHeight)
    }

    fun drawableToDownscaledBitmap(drawable: Drawable, maxDimensionPx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return downscaleBitmap(drawable.bitmap, maxDimensionPx)
        }
        val intrinsicW = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxDimensionPx
        val intrinsicH = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxDimensionPx
        val scale = if (intrinsicW > maxDimensionPx || intrinsicH > maxDimensionPx) {
            maxDimensionPx.toFloat() / max(intrinsicW, intrinsicH).toFloat()
        } else {
            1f
        }
        val targetW = max(1, (intrinsicW * scale).roundToInt())
        val targetH = max(1, (intrinsicH * scale).roundToInt())
        val bitmap = createBitmap(targetW, targetH)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, targetW, targetH)
        drawable.draw(canvas)
        return bitmap
    }
}
