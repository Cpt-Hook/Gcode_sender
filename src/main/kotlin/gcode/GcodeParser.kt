package gcode

import java.io.Closeable
import java.io.File
import java.lang.Exception
import java.lang.NumberFormatException


class GcodeParser(private val gcodeFile: File) : Iterable<GcodeCommand?>, Iterator<GcodeCommand?>, Closeable {

    init {
        println("Opening file \"${gcodeFile.name}\"")
    }

    private val reader = gcodeFile.bufferedReader()
    private var nextLine = reader.readLine()
    private var iteratorAvailable = true

    private fun parseGcode(line: String?): GcodeCommand? {
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

    override fun iterator(): Iterator<GcodeCommand?> {
        if (iteratorAvailable) {
            iteratorAvailable = false
            return this
        } else {
            throw IteratorNotAvailableException("Iterator can be created only once for an instance of this class")
        }
    }

    override fun hasNext() = nextLine != null

    override fun next(): GcodeCommand? {
        val gcode: GcodeCommand? = parseGcode(nextLine)
        nextLine = reader.readLine()
        return gcode
    }

    override fun close() {
        println("Closing file \"${gcodeFile.name}\"")
        reader.close()
    }

    companion object {
        const val maxWidthMM: Double = 40.0
        const val maxHeightMM: Double = 40.0

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
