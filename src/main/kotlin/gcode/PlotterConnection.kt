package gcode

import javafx.application.Platform
import javafx.concurrent.Task
import ui.MyViewController
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.net.Socket

class PlotterConnection(
    private val controller: MyViewController,
    private val ip: String,
    private val port: Int,
    private val file: File,
    private val enableFans: Boolean
) :
    Task<Any>() {

    private var socket: Socket? = null

    override fun call() {
        if (connect()) {
            Platform.runLater { controller.connectedProperty.value = true }
            streamFile()
            disconnect()
            Platform.runLater {
                controller.connectedProperty.value = false
            }
        }
    }

    private fun sendLine(line: String, writer: BufferedWriter, reader: BufferedReader) {
        writer.write(line)
        writer.flush()
        reader.readLine()
    }

    private fun streamFile() {
        socket?.let {
            val writer = it.getOutputStream().bufferedWriter()
            val reader = it.getInputStream().bufferedReader()

            sendLine(GcodeParser.enableMotorsCommand, writer, reader)
            if (enableFans) {
                sendLine(GcodeParser.enableFansCommand, writer, reader)
            }

            val gcodeParser = GcodeParser(file)

            for (line in gcodeParser) {
                if (isCancelled) {
                    break
                }
                sendLine(line, writer, reader)
                updateProgress(gcodeParser.lineNumber, gcodeParser.numOfLines)
            }

            sendLine(GcodeParser.resetCommand, writer, reader)
        }
    }

    private fun connect(): Boolean {

        println("Connecting to: $ip:$port")
        try {
            socket = Socket(ip, port)

        } catch (e: IOException) {
            println("Could not connect")
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            println("Port \"$port\"is out of range")
        } finally {
            if (socket?.isConnected == true) {
                return true
            }
        }
        return false
    }

    private fun disconnect() {
        println("Disconnecting")
        socket?.close()
        socket = null
    }
}