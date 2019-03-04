package gcode

import kotlin.math.*

class ArcExpander(private val gcode: GcodeCommand, private val xStart: Float, private val yStart: Float) :
    Iterable<GcodeCommand>,
    Iterator<GcodeCommand> {

    private val clockWise = gcode.getValue('G') == 2f

    private val startPoint = Point(xStart, yStart)
    private val aroundPoint = Point(startPoint.x + gcode.getValue('I'), startPoint.y + gcode.getValue('J'))
    private val endPoint = run {
        val x = if (gcode.containsKey('X')) gcode.getValue('X') else xStart
        val y = if (gcode.containsKey('Y')) gcode.getValue('Y') else yStart
        Point(x, y)
    }

    private val radius: Float = startPoint.distance(aroundPoint)
    private val angle = run {
        val endAngle = endPoint.translateToOrigin(aroundPoint).xAxisAngle()
        val startAngle = startPoint.translateToOrigin(aroundPoint).xAxisAngle()

        var angle = if (clockWise) {
            startAngle - endAngle
        } else {
            endAngle - startAngle
        }
        if (angle <= 0) {
            angle += 2 * PI.toFloat()
        }
        angle
    }

    private val points = floor((angle * radius) / maxArcLength).toInt()
    private val incrementAngle = if (clockWise) -(maxArcLength / radius) else (maxArcLength / radius)

    private var currentAngle = 0f
    private var pointsCreated = 0

    override fun hasNext() = pointsCreated <= points

    override fun next(): GcodeCommand {
        if (pointsCreated == points) {
            pointsCreated++
            return endPoint.toGcodeCommand()
        }

        currentAngle += incrementAngle
        pointsCreated++

        val nextPoint = startPoint.rotate(aroundPoint, currentAngle)
        return nextPoint.toGcodeCommand()
    }

    override fun iterator() = this

    private fun Point.toGcodeCommand(): GcodeCommand {
        // TODO add Z axis (pen down or up on the arc command?)
        val feed: Float? = gcode['F']
        val g = 'G' to 1f
        val x = 'X' to this.x
        val y = 'Y' to this.y

        return if (feed != null) {
            mapOf(g, x, y, 'F' to feed)
        } else {
            mapOf(g, x, y)
        }
    }

    companion object {
        var maxArcLength = 0.5f
    }

    private data class Point(val x: Float, var y: Float) {

        fun translateToOrigin(point: Point): Point {
            return Point(x - point.x, y - point.y)
        }

        fun translateFromOrigin(point: Point): Point {
            return Point(x + point.x, y + point.y)
        }

        fun rotate(aroundPoint: Point, angle: Float): Point {
            val translated = this.translateToOrigin(aroundPoint)

            val newX = translated.x * cos(angle) - translated.y * sin(angle)
            val newY = translated.x * sin(angle) + translated.y * cos(angle)

            return Point(newX, newY).translateFromOrigin(aroundPoint)
        }

        fun distance(point: Point): Float {
            return sqrt((x - point.x).pow(2) + (y - point.y).pow(2))
        }

        fun xAxisAngle(): Float {
            val angle = atan2(y, x)
            return if (angle < 0) {
                2 * PI.toFloat() + angle
            } else {
                angle
            }
        }
    }
}
