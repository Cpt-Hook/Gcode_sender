package gcode

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
