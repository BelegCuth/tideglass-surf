package com.tideglass.surf.provider.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.createBitmap
import kotlin.math.abs

object TideGraphRenderer {
    const val WIDTH = 346
    const val HEIGHT = 44

    fun render(points: List<TideGraphPoint>, nowMillis: Long): Bitmap {
        require(points.size >= 2) { "A tide graph needs at least two points" }
        val sorted = points.sortedBy { it.epochMillis }
        val bitmap = createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(5, 7, 7))

        val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(41, 48, 47)
            strokeWidth = 1f
        }
        canvas.drawLine(0f, HEIGHT / 2f, WIDTH.toFloat(), HEIGHT / 2f, guide)

        val inset = 3f
        fun x(index: Int) = inset + index.toFloat() / sorted.lastIndex * (WIDTH - inset * 2)
        fun y(level: Int) = inset + (100 - level.coerceIn(0, 100)) / 100f * (HEIGHT - inset * 2)

        val line = Path()
        sorted.forEachIndexed { index, point ->
            val px = x(index)
            val py = y(point.levelPercent)
            if (index == 0) line.moveTo(px, py) else line.lineTo(px, py)
        }
        val area = Path(line).apply {
            lineTo(x(sorted.lastIndex), HEIGHT - inset)
            lineTo(x(0), HEIGHT - inset)
            close()
        }
        canvas.drawPath(area, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 116, 217, 212)
            style = Paint.Style.FILL
        })
        canvas.drawPath(line, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(116, 217, 212)
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })

        val currentIndex = sorted.indices.minBy { abs(sorted[it].epochMillis - nowMillis) }
        canvas.drawCircle(
            x(currentIndex), y(sorted[currentIndex].levelPercent), 3.2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 117, 107) },
        )
        return bitmap
    }
}
