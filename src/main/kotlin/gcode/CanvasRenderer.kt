package gcode

import gcode.GcodeParser.Companion.maxHeightMM
import gcode.GcodeParser.Companion.maxWidthMM
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Paint
import ui.MyViewController

class CanvasRenderer(private val controller: MyViewController) {
    private val g: GraphicsContext get() = controller.canvas.graphicsContext2D
    private val width: Double get() = controller.canvas.width
    private val height: Double get() = controller.canvas.height

    private var points: List<Point>? = null

    private var xPos = 0f
    private var yPos = 0f

    var penWidth: Double
        get() = g.lineWidth
        set(value) {
            g.lineWidth = value
            render()
        }

    var penColor: Paint
        get() = g.stroke
        set(value) {
            g.stroke = value
            render()
        }

    fun generatePointsFromGcode(gcodeList: List<GcodeCommand>) {
        val newPoints: ArrayList<Point> = ArrayList()

        var penDown = false

        for(gcode in gcodeList) {
            if(gcode.isLine()) {
                if(gcode.containsKey('Z')){
                    penDown = gcode.getValue('Z') < 0f
                }
                if(gcode.containsKey('X')){
                    xPos = gcode.getValue('X')
                }
                if(gcode.containsKey('Y')){
                    yPos = gcode.getValue('Y')
                }
                newPoints.add(Point(xPos,  maxHeightMM - yPos, penDown))
            }
        }

        points = newPoints
        render()
    }

    fun deletePoints() {
        points = null
        render()
    }

    fun render() {
        g.clearRect(0.0, 0.0, width, height)

        points?.let { points ->
            val xScale = width / maxWidthMM
            val yScale = height / maxHeightMM

            g.beginPath()
            points.forEach { it.render(g, xScale, yScale) }
            g.stroke()
        }
    }

    data class Point(val x: Float, val y: Float, val penDown: Boolean) {
        fun render(g: GraphicsContext, xScale: Double, yScale: Double) {
            if (penDown) {
                g.lineTo(x * xScale, y * yScale)
            } else {
                g.moveTo(x * xScale, y * yScale)
            }
        }
    }
}
