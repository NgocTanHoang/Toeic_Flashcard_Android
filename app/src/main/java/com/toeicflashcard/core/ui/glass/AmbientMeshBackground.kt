package com.toeicflashcard.core.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.IntSize
import com.toeicflashcard.core.ui.theme.LocalDarkTone
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.core.ui.theme.MeshLightArcticCyan
import com.toeicflashcard.core.ui.theme.MeshLightBase
import com.toeicflashcard.core.ui.theme.MeshLightIceBlue
import com.toeicflashcard.core.ui.theme.MeshLightPearlLavender
import com.toeicflashcard.core.ui.theme.MeshLightSkyMist
import com.toeicflashcard.domain.model.DarkTone
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Static backdrop for Apple Liquid Glass surfaces:
 * - Dark Mode: pure solid single-color backdrop matching active [DarkTone].
 * - Light Mode: soft pearl-ice ambient mesh gradient rasterized once per viewport [Size]
 *   into a compact [ImageBitmap] so scrolling/haze frames blit a single texture instead of
 *   re-evaluating five full-screen radial gradient shaders per frame.
 */
@Composable
fun AmbientMeshBackground(
    modifier: Modifier = Modifier,
    darkTone: DarkTone = LocalDarkTone.current
) {
    val isDark = LocalLiquidGlassColors.current.isDark

    if (isDark) {
        val darkBg = remember(darkTone) { Color(darkTone.backgroundHex) }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(darkBg)
        )
        return
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) {
                    return@drawWithCache onDrawBehind {}
                }

                val bw = (w / 4f).roundToInt().coerceIn(64, 360)
                val bh = (h / 4f).roundToInt().coerceIn(64, 640)
                val bwF = bw.toFloat()
                val bhF = bh.toFloat()
                val maxSide = max(bwF, bhF)

                val c1 = Offset(bwF * 0.12f, bhF * 0.10f)
                val r1 = maxSide * 0.66f
                val b1 = Brush.radialGradient(
                    colors = listOf(MeshLightIceBlue.copy(alpha = 0.68f), Color.Transparent),
                    center = c1,
                    radius = r1
                )

                val c2 = Offset(bwF * 0.90f, bhF * 0.20f)
                val r2 = maxSide * 0.56f
                val b2 = Brush.radialGradient(
                    colors = listOf(MeshLightPearlLavender.copy(alpha = 0.52f), Color.Transparent),
                    center = c2,
                    radius = r2
                )

                val c3 = Offset(bwF * 0.84f, bhF * 0.58f)
                val r3 = maxSide * 0.60f
                val b3 = Brush.radialGradient(
                    colors = listOf(MeshLightArcticCyan.copy(alpha = 0.56f), Color.Transparent),
                    center = c3,
                    radius = r3
                )

                val c4 = Offset(bwF * 0.22f, bhF * 0.86f)
                val r4 = maxSide * 0.64f
                val b4 = Brush.radialGradient(
                    colors = listOf(MeshLightSkyMist.copy(alpha = 0.60f), Color.Transparent),
                    center = c4,
                    radius = r4
                )

                val c5 = Offset(bwF * 0.50f, bhF * 0.42f)
                val r5 = maxSide * 0.52f
                val b5 = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    center = c5,
                    radius = r5
                )

                val bitmap = ImageBitmap(width = bw, height = bh)
                val canvas = Canvas(bitmap)
                CanvasDrawScope().draw(
                    density = this,
                    layoutDirection = layoutDirection,
                    canvas = canvas,
                    size = Size(bwF, bhF)
                ) {
                    drawRect(color = MeshLightBase, size = size)
                    drawCircle(brush = b1, center = c1, radius = r1)
                    drawCircle(brush = b2, center = c2, radius = r2)
                    drawCircle(brush = b3, center = c3, radius = r3)
                    drawCircle(brush = b4, center = c4, radius = r4)
                    drawCircle(brush = b5, center = c5, radius = r5)
                }

                val dstSize = IntSize(
                    width = ceil(w).toInt().coerceAtLeast(1),
                    height = ceil(h).toInt().coerceAtLeast(1)
                )

                onDrawBehind {
                    drawImage(
                        image = bitmap,
                        dstSize = dstSize,
                        filterQuality = FilterQuality.Medium
                    )
                }
            }
    )
}
