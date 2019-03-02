package gcode

import java.io.Closeable
import java.io.File
import java.lang.Exception
import java.lang.NumberFormatException
import java.lang.StringBuilder

typealias GCode = Map<Char, Float>

const val decimalPlaces: Int = 3

fun GCode.stringify(): String {
    val builder = StringBuilder(this.size * 6) // one letter char and max 5 chars for number

    val skipKeys = listOf('G', 'X', 'Y', 'Z', 'F')
    if (this.contains('G')) {
        builder.appendField(this, 'G')
        builder.appendField(this, 'X')
        builder.appendField(this, 'Y')
        builder.appendField(this, 'Z')
        builder.appendField(this, 'F')
    }

    for (key in this.keys) {
        if (key in skipKeys) {
            continue
        }
        builder.appendField(this, key)
    }

    builder.append("\n")
    return builder.toString()
}

fun StringBuilder.appendField(gcode: GCode, key: Char) {
    if(gcode.containsKey(key)) {
        append(key)
        append(gcode.getValue(key).roundTo(decimalPlaces))
    }
}

fun Float.roundTo(decimalPlaces: Int): String {
    return "%.${decimalPlaces}f".format(this)
}


class GcodeParser(private val gcodeFile: File) : Iterable<GCode?>, Iterator<GCode?>, Closeable {

    init {
        println("Opening file \"${gcodeFile.name}\"")
    }

    private val reader = gcodeFile.bufferedReader()
    private var nextLine = reader.readLine()
    private var iteratorAvailable = true

    private fun parseGcode(line: String?): GCode? {
        if (line == null) {
            return null
        }
        val gcodeMap: MutableMap<Char, Float> = mutableMapOf()

        if (line.startsWith('%')) {
            return null
        }

        var gcodeLine = line.replace(whiteSpaceRegex, "")
        gcodeLine = gcodeLine.replace(bracketCommentsRegex, "")
        gcodeLine = gcodeLine.replace(semicolonCommentsRegex, "")

        if (gcodeLine.isEmpty()) {
            return null
        }
        gcodeLine = gcodeLine.toUpperCase()

        try {
            val numbers = gcodeLine.substring(1).split(letterRegex).map { if (it == "") 0f else it.toFloat() }

            var i = 0
            for (char in gcodeLine) {
                if (char.isLetter()) {
                    gcodeMap[char] = numbers[i++]
                }
            }
        } catch (e: NumberFormatException) {
            throw InvalidGcodeException(line)
        }
        return gcodeMap
    }

    override fun iterator(): Iterator<GCode?> {
        if (iteratorAvailable) {
            iteratorAvailable = false
            return this
        } else {
            throw IteratorNotAvailableException("Iterator can be created only once for an instance of this class")
        }
    }

    override fun hasNext() = nextLine != null

    override fun next(): GCode? {
        val gcode: GCode? = parseGcode(nextLine)
        nextLine = reader.readLine()
        return gcode
    }

    override fun close() {
        println("Closing file \"${gcodeFile.name}\"")
        reader.close()
    }

    companion object {
        const val enableMotorsCommand = "E1\n"
        const val enableFansCommand = "F1\n"
        const val disableFansCommand = "F0\n"
        const val resetCommand = "R\n"

        private val whiteSpaceRegex = Regex("\\s")
        private val semicolonCommentsRegex = Regex(";.*")
        private val bracketCommentsRegex = Regex("\\(.*?\\)")
        private val letterRegex = Regex("[A-Za-z]")
    }

    private class IteratorNotAvailableException(message: String) : Exception(message)
}

class InvalidGcodeException(gcode: String) : Exception("Invalid gcode: \"$gcode\"")
