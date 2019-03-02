package gcode

import javafx.application.Platform
import javafx.concurrent.Task
import ui.MyViewController
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.lang.Exception
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
        val response = reader.readLine()
        if(checkAnswer && !answerRegex.matches(response)){
            throw IllegalAnswerException("Answer \"$response\" does not match the expected answer")
        }
    }

    private fun streamFile() {
        socket?.let {
            val writer = it.getOutputStream().bufferedWriter()
            val reader = it.getInputStream().bufferedReader()

            try {
                sendLine(GcodeParser.enableMotorsCommand, writer, reader, true)
                if (enableFans) {
                    sendLine(GcodeParser.enableFansCommand, writer, reader, true)
                }

                val gcodeParser = GcodeParser(file)

                for (line in gcodeParser) {
                    if (isCancelled) {
                        break
                    }
                    sendLine(line, writer, reader, true)
                    updateProgress(gcodeParser.lineNumber, gcodeParser.numOfLines)
                    updateMessage("(${gcodeParser.lineNumber}/${gcodeParser.numOfLines})")
                }
            } catch(e: IllegalAnswerException){
                System.err.println(e.message)
            } finally {
                sendLine(GcodeParser.resetCommand, writer, reader, false)
            }
        }
    }

    private fun connect(): Boolean {

        println("Connecting to: $ip:$port")
        try {
            socket = Socket(ip, port)

        } catch (e: IOException) {
            println("Could not connect:")
            System.err.println(e.message)
        } catch (e: IllegalArgumentException) {
            println("Port \"$port\" is out of range:")
            System.err.println(e.message)
        } finally {
            if (socket?.isConnected == true) {
                return true
            }
        }
        return false
    }

    private fun disconnect() {
        println("Disconnecting from $ip:$port")
        socket?.close()
        socket = null
    }

    private class IllegalAnswerException(message: String) : Exception(message)
}