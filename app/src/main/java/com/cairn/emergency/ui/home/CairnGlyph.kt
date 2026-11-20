package com.cairn.emergency.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale

internal enum class CairnIcon {
    Home, Household, Kit, Assistant, Simulator, More, Settings, Phone, Back, Microphone, Keyboard, Edit, Check, Stop,
    Water, Medicine, Baby, Paw, Mountain, Next, Down, Health, Tools, Documents, Mobility, Chat,
    Siren, Book, RescueCard, Language, Speaker, TextSize, Contrast, Vibration,
}

@Composable
internal fun CairnGlyph(icon: CairnIcon, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = 1.8f) =
                drawLine(color, Offset(x1, y1), Offset(x2, y2), width, StrokeCap.Round)

            fun path(vararg points: Pair<Float, Float>) = drawPath(
                Path().apply {
                    moveTo(points.first().first, points.first().second)
                    points.drop(1).forEach { (x, y) -> lineTo(x, y) }
                    close()
                },
                color,
            )

            when (icon) {
                CairnIcon.Home -> path(
                    1f to 11f, 12f to 1.5f, 23f to 11f, 20f to 11f, 20f to 22f,
                    14f to 22f, 14f to 15f, 10f to 15f, 10f to 22f, 4f to 22f, 4f to 11f,
                )
                CairnIcon.Household -> {
                    drawCircle(color, 3.2f, Offset(12f, 6f))
                    drawRoundRect(color, Offset(6.6f, 11f), Size(10.8f, 9.8f), CornerRadius(3.5f))
                    for (x in listOf(4f, 20f)) {
                        drawCircle(color, 2.25f, Offset(x, 8.5f))
                        drawRoundRect(color, Offset(x - 2.8f, 12.3f), Size(5.6f, 6.7f), CornerRadius(2f))
                    }
                }
                CairnIcon.Kit -> {
                    drawRoundRect(color, Offset(7f, 2f), Size(10f, 6f), CornerRadius(1.5f), style = Stroke(1.8f))
                    drawRoundRect(color, Offset(2f, 6f), Size(20f, 15f), CornerRadius(2.5f), style = Stroke(2.3f))
                    line(12f, 10f, 12f, 17f, 2.5f)
                    line(8.5f, 13.5f, 15.5f, 13.5f, 2.5f)
                }
                CairnIcon.Assistant -> {
                    path(10f to 2f, 12.7f to 9.3f, 20f to 12f, 12.7f to 14.7f, 10f to 22f, 7.3f to 14.7f, 0f to 12f, 7.3f to 9.3f)
                    path(20f to 1f, 21.2f to 3.8f, 24f to 5f, 21.2f to 6.2f, 20f to 9f, 18.8f to 6.2f, 16f to 5f, 18.8f to 3.8f)
                }
                CairnIcon.Simulator -> path(9f to 1f, 20f to 1f, 14f to 9f, 20f to 9f, 7f to 23f, 10f to 14f, 4f to 14f)
                CairnIcon.More -> for (row in 0..2) {
                    for (column in 0..2) drawCircle(color, 1.4f, Offset(5f + column * 7f, 5f + row * 7f))
                }
                CairnIcon.Back -> {
                    line(5f, 12f, 20f, 12f)
                    line(5f, 12f, 11f, 6f)
                    line(5f, 12f, 11f, 18f)
                }
                CairnIcon.Microphone -> {
                    drawRoundRect(color, Offset(9f, 2f), Size(6f, 12f), CornerRadius(3f))
                    drawArc(color, 0f, 180f, false, Offset(5f, 5f), Size(14f, 13f), style = Stroke(1.7f, cap = StrokeCap.Round))
                    line(12f, 18f, 12f, 22f)
                }
                CairnIcon.Keyboard -> {
                    drawRoundRect(color, Offset(2f, 5f), Size(20f, 14f), CornerRadius(2f), style = Stroke(1.8f))
                    for (x in 6..18 step 4) {
                        drawCircle(color, 1f, Offset(x.toFloat(), 9f))
                        drawCircle(color, 1f, Offset(x.toFloat(), 12f))
                    }
                    line(8f, 15.5f, 16f, 15.5f)
                }
                CairnIcon.Edit -> {
                    line(5f, 19f, 17f, 7f, 3f)
                    line(18.5f, 5.5f, 20f, 4f, 3f)
                }
                CairnIcon.Check -> {
                    line(4f, 12f, 9f, 17f, 2.5f)
                    line(9f, 17f, 20f, 6f, 2.5f)
                }
                CairnIcon.Stop -> drawRoundRect(color, Offset(6f, 6f), Size(12f, 12f), CornerRadius(2f))
                CairnIcon.Chat -> {
                    drawRoundRect(color, Offset(2f, 3f), Size(20f, 15f), CornerRadius(4f))
                    path(6f to 16f, 6f to 22f, 12f to 16f)
                    for (x in listOf(7f, 12f, 17f)) drawCircle(Color.White, 1.3f, Offset(x, 10.5f))
                }
                CairnIcon.Siren -> {
                    drawRoundRect(color, Offset(6f, 7f), Size(12f, 10f), CornerRadius(4f, 4f))
                    drawRoundRect(color, Offset(4f, 18f), Size(16f, 3f), CornerRadius(1f))
                    line(12f, 1f, 12f, 4f, 1.7f)
                    line(3f, 4f, 5f, 6f, 1.7f)
                    line(19f, 6f, 21f, 4f, 1.7f)
                    line(1f, 11f, 3.5f, 11f, 1.7f)
                    line(20.5f, 11f, 23f, 11f, 1.7f)
                    drawLine(Color.White, Offset(9f, 11f), Offset(9f, 14f), 1.5f, StrokeCap.Round)
                }
                CairnIcon.Book -> {
                    drawRoundRect(color, Offset(2f, 4f), Size(20f, 15f), CornerRadius(2f), style = Stroke(2f))
                    line(12f, 5f, 12f, 20f, 2f)
                    line(5.5f, 9f, 9f, 9f, 1.2f)
                    line(15f, 9f, 18.5f, 9f, 1.2f)
                }
                CairnIcon.RescueCard -> {
                    drawRoundRect(color, Offset(1f, 4f), Size(22f, 16f), CornerRadius(2.4f))
                    drawCircle(Color.White, 2.4f, Offset(7f, 9f))
                    drawRoundRect(Color.White, Offset(3.8f, 12.5f), Size(6.4f, 4f), CornerRadius(1.6f))
                    drawLine(Color.White, Offset(13f, 9f), Offset(19.5f, 9f), 1.3f, StrokeCap.Round)
                    drawLine(Color.White, Offset(13f, 14f), Offset(19.5f, 14f), 1.3f, StrokeCap.Round)
                }
                CairnIcon.Language -> {
                    drawCircle(color, 9f, Offset(12f, 12f), style = Stroke(1.7f))
                    drawOval(color, Offset(7.5f, 3f), Size(9f, 18f), style = Stroke(1.4f))
                    line(3f, 12f, 21f, 12f, 1.4f)
                }
                CairnIcon.TextSize -> {
                    line(1f, 17f, 5f, 8f)
                    line(5f, 8f, 9f, 17f)
                    line(2.5f, 14f, 7.5f, 14f)
                    line(10f, 18f, 16f, 4f)
                    line(16f, 4f, 22f, 18f)
                    line(12.5f, 13f, 19.5f, 13f)
                }
                CairnIcon.Contrast -> {
                    drawCircle(color, 9f, Offset(12f, 12f), style = Stroke(1.8f))
                    drawArc(color, 90f, 180f, true, Offset(3f, 3f), Size(18f, 18f))
                }
                CairnIcon.Vibration -> {
                    drawRoundRect(color, Offset(7f, 3f), Size(10f, 18f), CornerRadius(2f), style = Stroke(1.8f))
                    line(4f, 7f, 4f, 17f)
                    line(20f, 7f, 20f, 17f)
                    line(1f, 10f, 1f, 14f)
                    line(23f, 10f, 23f, 14f)
                }
                CairnIcon.Speaker -> {
                    path(2f to 9f, 6f to 9f, 11f to 4f, 11f to 20f, 6f to 15f, 2f to 15f)
                    drawArc(color, -64f, 128f, false, Offset(6f, 4f), Size(15f, 16f), style = Stroke(1.8f))
                }
                CairnIcon.Water -> {
                    val drop = Path().apply {
                        moveTo(12f, 1.5f)
                        cubicTo(9f, 6f, 4f, 11f, 4f, 15f)
                        cubicTo(4f, 19.5f, 7.5f, 23f, 12f, 23f)
                        cubicTo(16.5f, 23f, 20f, 19.5f, 20f, 15f)
                        cubicTo(20f, 11f, 15f, 6f, 12f, 1.5f)
                        close()
                    }
                    drawPath(drop, color)
                    drawArc(Color.White, 92f, 80f, false, Offset(7.5f, 12.5f), Size(8f, 7f), style = Stroke(1.5f))
                }
                CairnIcon.Medicine ->
                    line(6f, 18f, 18f, 6f, 8f)
                CairnIcon.Baby -> {
                    drawRoundRect(color, Offset(7f, 8f), Size(10f, 14f), CornerRadius(2f), style = Stroke(2f))
                    line(7f, 7f, 17f, 7f, 2.3f)
                    line(11f, 7f, 11f, 3f, 3f)
                    line(13f, 13f, 16f, 13f)
                    line(13f, 17f, 16f, 17f)
                }
                CairnIcon.Paw -> {
                    drawRoundRect(color, Offset(6.5f, 11f), Size(11f, 10f), CornerRadius(5f))
                    drawCircle(color, 2.6f, Offset(4f, 10f))
                    drawCircle(color, 2.6f, Offset(9f, 5f))
                    drawCircle(color, 2.6f, Offset(15f, 5f))
                    drawCircle(color, 2.6f, Offset(20f, 10f))
                }
                CairnIcon.Mountain -> {
                    path(1f to 20f, 8f to 9f, 12f to 13f, 17f to 4f, 24f to 20f)
                    line(14.5f, 9f, 17f, 6f, 1.6f)
                    drawLine(Color.White, Offset(15f, 9f), Offset(17f, 7f), 1.5f, StrokeCap.Round)
                    drawLine(Color.White, Offset(17f, 7f), Offset(19f, 9.5f), 1.5f, StrokeCap.Round)
                }
                CairnIcon.Next -> {
                    line(9f, 6f, 15f, 12f)
                    line(15f, 12f, 9f, 18f)
                }
                CairnIcon.Down -> {
                    line(6f, 9f, 12f, 15f)
                    line(12f, 15f, 18f, 9f)
                }
                CairnIcon.Health -> {
                    line(12f, 3.5f, 12f, 20.5f, 5f)
                    line(3.5f, 12f, 20.5f, 12f, 5f)
                }
                CairnIcon.Tools -> {
                    line(5f, 19f, 15f, 9f, 5f)
                    drawArc(color, 12f, 265f, false, Offset(11f, 2f), Size(10f, 10f), style = Stroke(3f))
                }
                CairnIcon.Documents -> {
                    drawRoundRect(color, Offset(5f, 2f), Size(14f, 20f), CornerRadius(2f), style = Stroke(2f))
                    for (y in listOf(7f, 12f, 17f)) line(8.5f, y, 15.5f, y, 1.4f)
                }
                CairnIcon.Mobility -> {
                    drawCircle(color, 2.4f, Offset(10f, 4f))
                    line(10f, 8f, 12f, 14f)
                    line(12f, 14f, 17f, 14f)
                    line(17f, 14f, 20f, 19f)
                    drawArc(color, 10f, 236f, false, Offset(4f, 11f), Size(11f, 11f), style = Stroke(1.8f))
                }
                CairnIcon.Settings -> {
                    drawCircle(color, 6f, Offset(12f, 12f), style = Stroke(2.5f))
                    for (step in 0..7) {
                        val angle = step * Math.PI / 4
                        val x = kotlin.math.cos(angle).toFloat()
                        val y = kotlin.math.sin(angle).toFloat()
                        line(12f + 6f * x, 12f + 6f * y, 12f + 9.5f * x, 12f + 9.5f * y, 2.8f)
                    }
                }
                CairnIcon.Phone -> drawPath(
                    Path().apply {
                        moveTo(4f, 2f)
                        cubicTo(1f, 3f, 1f, 7f, 4f, 12f)
                        cubicTo(7f, 17f, 12f, 21f, 17f, 22f)
                        cubicTo(20f, 23f, 22f, 21f, 22f, 18f)
                        lineTo(16f, 14.6f)
                        lineTo(13.5f, 17.2f)
                        cubicTo(10f, 15.7f, 7.9f, 13.2f, 6.7f, 10f)
                        lineTo(9.5f, 7.5f)
                        lineTo(6f, 2f)
                        close()
                    },
                    color,
                )
            }
        }
    }
}
