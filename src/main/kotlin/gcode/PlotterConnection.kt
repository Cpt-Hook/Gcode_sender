package gcode

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.scene.control.Alert
import ui.MyViewController
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

class PlotterConnection(
    private val controller: MyViewController,
    private val ip: String,
    private val port: Int,
    private val gcodeList: List<GcodeCommand?>,
    private val enableFans: Boolean
) :
    Task<Any>() {

    private var socket: Socket? = null
    private val answerRegex = Regex("OK.*")

    override fun call() {
        updateProgress(0, 1)
        updateMessage("")

        if (connect()) {
            Platform.runLater { controller.connectedProperty.value = true }
            streamFile()
            disconnect()
            Platform.runLater {
                controller.connectedProperty.value = false
            }
        }
    }

    private fun sendLine(line: String, writer: BufferedWriter, reader: BufferedReader, checkAnswer: Boolean) {
        writer.write(line)
        writer.flush()

        val response = reader.readLine() ?: throw IOException("Socket closed unexpectedly")
        if (checkAnswer && !answerRegex.matches(response)) {
            throw IllegalAnswerException("Answer \"$response\" does not match the expected answer")
        }
    }

    private fun streamFile() {
        socket?.let {
            val writer = it.getOutputStream().bufferedWriter()
            val reader = it.getInputStream().bufferedReader()

            try {
                try {
                    println("Starting streaming, sending enable motors command")

                    sendLine(GcodeParser.enableMotorsCommand, writer, reader, true)
                    if (enableFans) {
                        println("Sending enable fans command")
                        sendLine(GcodeParser.enableFansCommand, writer, reader, true)
                    }

                    gcodeList.forEachIndexed { index, gcode ->
                        if (isCancelled) {
                            return@forEachIndexed
                        }

                        if (gcode != null) {
                            sendLine(gcode.stringify(), writer, reader, true)
                        }

                        updateProgress(index.toLong() + 1, gcodeList.size.toLong())
                        updateMessage("(${index + 1}/${gcodeList.size})")
                    }
                } catch (e: IllegalAnswerException) {
                    Platform.runLater {
                        val alert = Alert(Alert.AlertType.ERROR)
                        alert.title = "Error"
                        alert.headerText = null
                        alert.contentText = "Plotter sent illegal information."
                        alert.showAndWait()
                        System.err.println(e.message)
                    }
                } finally {
                    println("Finished streaming, sending reset command")
                    sendLine(GcodeParser.resetCommand, writer, reader, false)
                }
            } catch (e: IOException) {
                System.err.println(e.message)   //socket closed from the other side
            }
        }
    }

    private fun connect(): Boolean {
        println("Connecting to: \"$ip:$port\"")

        try {
            socket = Socket()
            socket?.connect(InetSocketAddress(ip, port), 2500)

        } catch (e: IOException) {
            println("Could not connect")
            System.err.println(e.message)
            Platform.runLater {
                val alert = Alert(Alert.AlertType.ERROR)
                alert.title = "Error"
                alert.headerText = null
                alert.contentText = "Could not connect to the plotter. (connection timed out)"
                alert.showAndWait()
            }
        } catch (e: IllegalArgumentException) {
            println("Port \"$port\" is out of range")
            System.err.println(e.message)
            Platform.runLater {
                val alert = Alert(Alert.AlertType.ERROR)
                alert.title = "Error"
                alert.headerText = null
                alert.contentText = "The port provided is out of range."
                alert.showAndWait()
            }
        } finally {
            if (!isCancelled && socket?.isConnected == true) {
                return true
            }
        }
        return false
    }

    private fun disconnect() {
        println("Disconnecting from \"$ip:$port\"")

        socket?.close()
        socket = null
    }

    private class IllegalAnswerException(message: String) : Exception(message)
}
