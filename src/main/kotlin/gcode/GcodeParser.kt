package gcode

import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class GcodeParser(val file: File) : Iterable<String>, Iterator<String>, Closeable {

    init{
        println("Opening file \"${file.name}\"")
    }

    val numOfLines: Long = Files.lines(Paths.get(file.toURI())).let {
        val lines = it.count()
        it.close()
        lines
    }
    var lineNumber: Long = 0
    private val reader = file.bufferedReader()

    override fun hasNext() = lineNumber < numOfLines

    override fun next(): String {
        lineNumber++
        val nextLine = reader.readLine()
        return parseGcode(nextLine)
    }

    private fun parseGcode(gcode: String): String {
        return gcode + "\n"
    }

    override fun iterator(): Iterator<String> {
        return this
    }

    override fun close() {
        println("Closing file \"${file.name}\"")
        reader.close()
    }

    companion object {
        const val enableMotorsCommand = "E1\n"
        const val enableFansCommand = "F1\n"
        const val disableFansCommand = "F0\n"
        const val resetCommand = "R\n"
    }

}
