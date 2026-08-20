package com.unbound.messageme.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.unbound.messageme.ui.theme.EmeraldDeep
import com.unbound.messageme.ui.theme.EmeraldGreen
import com.unbound.messageme.ui.theme.GoldAccent
import com.unbound.messageme.ui.theme.LemonWash
import com.unbound.messageme.ui.theme.NavyBlue
import com.unbound.messageme.ui.theme.PastelYellow
import kotlin.math.min
import kotlin.random.Random

@Composable
fun WatercolorBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val m = min(w, h)

            drawRect(NavyBlue)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldDeep.copy(alpha = 0.95f), Color.Transparent),
                    center = Offset(w * 0.22f, h * 0.38f),
                    radius = m * 0.85f
                ),
                radius = m * 0.85f,
                center = Offset(w * 0.22f, h * 0.38f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldGreen.copy(alpha = 0.9f), Color.Transparent),
                    center = Offset(w * 0.72f, h * 0.48f),
                    radius = m * 0.9f
                ),
                radius = m * 0.9f,
                center = Offset(w * 0.72f, h * 0.48f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldGreen.copy(alpha = 0.75f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.78f),
                    radius = m * 0.7f
                ),
                radius = m * 0.7f,
                center = Offset(w * 0.5f, h * 0.78f)
            )

            // Central pastel-yellow column of light
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        0.46f to PastelYellow.copy(alpha = 0.55f),
                        0.5f to LemonWash.copy(alpha = 0.92f),
                        0.54f to PastelYellow.copy(alpha = 0.55f),
                        0.68f to Color.Transparent,
                        1.0f to Color.Transparent
                    )
                )
            )
            listOf(0.12f, 0.28f, 0.46f, 0.64f, 0.82f).forEachIndexed { i, y ->
                val cx = w * (0.48f + if (i % 2 == 0) -0.03f else 0.04f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(LemonWash.copy(alpha = 0.7f), PastelYellow.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(cx, h * y),
                        radius = m * 0.28f
                    ),
                    radius = m * 0.28f,
                    center = Offset(cx, h * y)
                )
            }

            // Darker navy corners to frame the wash
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NavyBlue.copy(alpha = 0.85f), Color.Transparent),
                    center = Offset(w * 0.95f, h * 0.06f),
                    radius = m * 0.55f
                ),
                radius = m * 0.55f,
                center = Offset(w * 0.95f, h * 0.06f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NavyBlue.copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(w * 0.06f, h * 0.94f),
                    radius = m * 0.5f
                ),
                radius = m * 0.5f,
                center = Offset(w * 0.06f, h * 0.94f)
            )

            val gold = Random(19)
            repeat(28) {
                val start = Offset(w * gold.nextFloat(), h * gold.nextFloat())
                val end = start + Offset(
                    (gold.nextFloat() - 0.5f) * m * 0.18f,
                    (gold.nextFloat() - 0.5f) * m * 0.14f
                )
                drawLine(
                    color = GoldAccent.copy(alpha = 0.35f + gold.nextFloat() * 0.4f),
                    start = start,
                    end = end,
                    strokeWidth = 1.5f + gold.nextFloat() * 3.5f,
                    cap = StrokeCap.Round
                )
            }
            repeat(18) {
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.28f + gold.nextFloat() * 0.4f),
                    radius = 2f + gold.nextFloat() * 7f,
                    center = Offset(w * gold.nextFloat(), h * gold.nextFloat())
                )
            }

            // Soft knife-stroke ribbons
            val ribbon = Path().apply {
                moveTo(w * 0.18f, h * 0.22f)
                quadraticTo(w * 0.4f, h * 0.18f, w * 0.58f, h * 0.32f)
                quadraticTo(w * 0.72f, h * 0.44f, w * 0.62f, h * 0.58f)
            }
            drawPath(
                ribbon,
                color = GoldAccent.copy(alpha = 0.22f),
                style = Stroke(width = m * 0.012f, cap = StrokeCap.Round)
            )
        }
        content()
    }
}
