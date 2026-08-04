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
import com.unbound.messageme.ui.theme.PastelYellow
import com.unbound.messageme.ui.theme.SoftSky
import com.unbound.messageme.ui.theme.WashLilac
import com.unbound.messageme.ui.theme.WaterBlue

@Composable
fun WatercolorBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(SoftSky, WashLilac, Color(0xFFC9DEF0))
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PastelYellow.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.15f),
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.2f, size.height * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WaterBlue.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.25f),
                    radius = size.minDimension * 0.5f
                ),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * 0.85f, size.height * 0.25f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66F4A261), Color.Transparent),
                    center = Offset(size.width * 0.55f, size.height * 0.85f),
                    radius = size.minDimension * 0.6f
                ),
                radius = size.minDimension * 0.6f,
                center = Offset(size.width * 0.55f, size.height * 0.85f)
            )
        }
        content()
    }
}
