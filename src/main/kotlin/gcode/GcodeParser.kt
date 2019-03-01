package gcode

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class GcodeParser(file: File) : Iterable<String>, Iterator<String> {

    val numOfLines = Files.lines(Paths.get(file.toURI())).count()
    var lineNumber: Long = 0
    private val reader = file.bufferedReader()


    override fun hasNext() = lineNumber < numOfLines

    override fun next(): String {
        lineNumber++
        return parseGcode(reader.readLine())
    }

    private fun parseGcode(gcode: String): String {
        return gcode + "\n"
    }

    override fun iterator(): Iterator<String> {
        return this
    }


    companion object {
        const val enableMotorsCommand = "E1\n"
        const val enableFansCommand = "F1\n"
        const val disableFansCommand = "F0\n"
        const val resetCommand = "R\n"
    }

}