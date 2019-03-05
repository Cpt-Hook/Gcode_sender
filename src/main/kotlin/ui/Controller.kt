package ui

import gcode.*
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.stage.WindowEvent
import tornadofx.Controller
import tornadofx.singleAssign
import java.io.File


class MyViewController : Controller() {

    var canvas: Canvas by singleAssign()

    val resizeHandler: ChangeListener<Number> = ChangeListener { _, _, _ -> canvasRenderer.render() }

    val penWidthHandler: ChangeListener<Number> = ChangeListener { _, _, newValue ->
        canvasRenderer.penWidth = newValue.toDouble()}

    val colourHandler: ChangeListener<Color> = ChangeListener { _, _, color -> canvasRenderer.penColor = color }

    val closeRequestHandler = EventHandler<WindowEvent> {
        println("Closing")
        connection?.cancel()
    }

    val progressPercentProperty = SimpleDoubleProperty(0.0)
    val progressLinesProperty = SimpleStringProperty("")
    val fileNameProperty = SimpleStringProperty("choose a file")
    val ipAddressProperty = SimpleStringProperty()
    val portProperty = SimpleStringProperty()
    val connectedProperty = SimpleBooleanProperty(false)
    val fansProperty = SimpleBooleanProperty()
    val penWidthProperty = SimpleDoubleProperty(1.0)
    val colorProperty= SimpleObjectProperty<Color>()
    val maxArcLengthProperty = SimpleFloatProperty(ArcExpander.maxArcLength)

    private var gcodeList: List<GcodeCommand>? = null
    private var gcodeFile: File? = null

    private var connection: PlotterConnection? = null

    private val canvasRenderer = CanvasRenderer(this)


    fun openFile(files: List<File>) {
        if (files.isEmpty()) {
            return
        }
        gcodeFile = files[0]
        gcodeFile?.let{
            fileNameProperty.value = it.name
            parseFile(it)
        }
    }

    fun closeFile() {
        gcodeFile = null
        gcodeList = null
        canvasRenderer.deletePoints()
        fileNameProperty.value = "choose a file"
    }

    fun saveFile(files: List<File>) {
        if (files.isEmpty()) {
            return
        } else if (gcodeList == null) {
            println("choose a file")
            return
        }

        val saveFile = files[0]
        println("Saving gcode to ${saveFile.path}")

        saveFile.bufferedWriter().use { writer ->
            gcodeList?.forEach {
                writer.write(it.stringify())
            }
            writer.newLine()
        }
        println("Saved successfully")
    }

    fun startStreaming() {
        if (gcodeList == null) {
            println("choose a file")
            return
        }

        try {
            val ip = ipAddressProperty.value.trim()
            val port = portProperty.value.trim().toInt()

            connection = PlotterConnection(this, ip, port, gcodeList!!, fansProperty.value)
            progressLinesProperty.bind(connection?.messageProperty())
            progressPercentProperty.bind(connection?.progressProperty())

            Thread(connection).apply { isDaemon = false }.start()

        } catch (e: NumberFormatException) {
            // TODO popup
            println("Port not a number: ${portProperty.value}")
        }
    }

    fun stopStreaming() {
        connection?.cancel()
    }

    fun maxArcLengthSelected(newArcLength: Float) {
        ArcExpander.maxArcLength = newArcLength
        gcodeFile?.let {parseFile(it)}
    }

    private fun parseFile(file: File) {
        try {
            gcodeList = GcodeParser(file).getGcodeList()

            println("${gcodeList?.size} gcode lines parsed")
            gcodeList?.let { canvasRenderer.generatePointsFromGcode(it) }
            println("Visualization generated")

        } catch (e: InvalidGcodeException) {
            System.err.println(e.message)
            // TODO popup
            closeFile()
        }
    }
}
