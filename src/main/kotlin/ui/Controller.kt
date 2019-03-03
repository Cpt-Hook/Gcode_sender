package ui

import gcode.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.stage.WindowEvent
import tornadofx.Controller
import tornadofx.singleAssign
import java.io.File


class MyViewController : Controller() {

    var canvas: Canvas by singleAssign()
    private val g: GraphicsContext
        get() = canvas.graphicsContext2D

    val resizeHandler: ChangeListener<Number> = ChangeListener { _, _, _ -> renderCanvas() }
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

    private var gcodeList: List<GCode>? = null

    private var connection: PlotterConnection? = null


    private fun renderCanvas() {
        g.clearRect(0.0, 0.0, canvas.width, canvas.height)
    }

    fun openFile(files: List<File>) {
        if (files.isEmpty()) {
            return
        }
        val gcodeFile = files[0]
        fileNameProperty.value = gcodeFile.name

        try {
            gcodeList = GcodeParser(gcodeFile).use {
                it.iterator().asSequence().filterNotNull().toList()
            }
            println("${gcodeList?.size} gcode lines parsed")
        } catch (e: InvalidGcodeException) {
            System.err.println(e.message)
            // TODO popup
            closeFile()
        }
    }

    fun closeFile() {
        gcodeList = null
        fileNameProperty.value = "choose a file"
    }

    fun saveFile(files: List<File>) {
        if(files.isEmpty()) {
            return
        }else if(gcodeList == null) {
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
}
