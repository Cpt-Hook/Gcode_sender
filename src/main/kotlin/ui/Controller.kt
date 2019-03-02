package ui

import gcode.PlotterConnection
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
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
    val ipAddressProperty =  SimpleStringProperty()
    val portProperty =  SimpleStringProperty()
    val connectedProperty = SimpleBooleanProperty(false)
    val fansProperty = SimpleBooleanProperty()

    var gcodeFile: File? = null

    var connection: PlotterConnection? = null


    private fun renderCanvas() {
        g.clearRect(0.0, 0.0, canvas.width, canvas.height)
    }

    fun chooseFile(files: List<File>){
        if(files.isEmpty()){
            return
        }
        gcodeFile = files[0]

        fileNameProperty.value = gcodeFile?.name
    }

    fun closeFile() {
        gcodeFile = null
        fileNameProperty.value = "choose a file"
    }

    fun startStreaming() {
        if(gcodeFile == null) {
            println("choose a file")
            return
        }

        try{
            val ip = ipAddressProperty.value.trim()
            val port = portProperty.value.trim().toInt()

            connection = PlotterConnection(this, ip, port, gcodeFile!!, fansProperty.value)
            progressLinesProperty.bind(connection?.messageProperty())
            progressPercentProperty.bind(connection?.progressProperty())

            Thread(connection).apply { isDaemon = false }.start()

        } catch(e: NumberFormatException) {
            println("Port not a number: ${portProperty.value}")
        }
    }

    fun stopStreaming() {
        connection?.cancel()
    }
}