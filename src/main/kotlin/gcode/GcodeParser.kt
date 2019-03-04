package gcode

import java.io.Closeable
import java.io.File
import java.lang.Exception
import java.lang.NumberFormatException


class GcodeParser(private val gcodeFile: File) {

    private var xPos: Float = 0f
    private var yPos: Float = 0f

    init {
        println("Opening file \"${gcodeFile.name}\"")
    }

    private val reader = gcodeFile.bufferedReader()

    private val gcodeList = ArrayList<GcodeCommand>()

    init {
        try {
            reader.forEachLine { nextLine ->
                val gcode = parseGcode(nextLine)

                gcode?.let {
                    if (it.isArc()) {
                        try {
                            gcodeList.addAll(ArcExpander(it, xPos, yPos))
                        } catch (e: NoSuchElementException) {
                            throw InvalidGcodeException(nextLine)
                        }
                    } else {
                        gcodeList.add(it)
                    }

                    it.setPos()
                }
            }
        } finally {
            close()
        }
    }

    fun getGcodeList(): List<GcodeCommand> = gcodeList

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

    private fun GcodeCommand.setPos() {
        if (containsKey('G')) {
            val gCommand = getValue('G')
            if (gCommand in 0.0..3.0) {
                if (containsKey('X')) {
                    xPos = this.getValue('X')
                }
                if (containsKey('Y')) {
                    yPos = this.getValue('Y')
                }
            }
        }
    }

    private fun GcodeCommand.isArc() = this.containsKey('G') && (this.getValue('G') == 2f || this.getValue('G') == 3f)
    private fun GcodeCommand.isLine() = this.containsKey('G') && (this.getValue('G') == 0f || this.getValue('G') == 1f)

    private fun close() {
        println("Closing file \"${gcodeFile.name}\"")
        reader.close()
    }

    companion object {
        const val maxWidthMM: Float = 40f
        const val maxHeightMM: Float = 40f

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
