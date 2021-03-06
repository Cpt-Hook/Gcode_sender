package ui

import javafx.util.StringConverter
import kotlin.math.roundToInt

class ProgressStringConverter : StringConverter<Number>() {
    override fun toString(number: Number?): String {
        return if (number is Double && number >= 0) {
            "${(number * 100).roundToInt()}%"
        } else {
            ""
        }
    }

    override fun fromString(string: String?): Number {
        throw NotImplementedError()
    }
}

class PenWidthStringConverter : StringConverter<Number>() {
    override fun toString(number: Number?): String {
        return "${number?.toInt()}px"
    }

    override fun fromString(string: String?): Number {
        throw NotImplementedError()
    }
}

class MaxArcLengthStringConverter : StringConverter<Number>() {
    override fun toString(number: Number?): String {
        return "%.2fmm".format(number)
    }

    override fun fromString(string: String?): Number {
        throw NotImplementedError()
    }
}
