package main

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ChangeListener
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.layout.Priority
import tornadofx.*

fun main(args: Array<String>) {
    launch<MyApp>(args)
}

class MyApp : App(MainView::class)

class MainView : View("Gcode_sender") {
    private val controller: MainViewController by inject()

    override val root = borderpane {
        top {
            menubar {

                menu("File") {

                    item("Open")
                }
                menu("Settings") {

                    item("Pen width")

                    item("Arc length")
                }
                menu("Other") {

                    item("Info")

                    item("Help")
                }
            }
        }

        left {
            hbox {
                vbox(spacing = 10, alignment = Pos.CENTER) {
                    paddingAll = 10.0

                    hbox(spacing = 10.0) {

                        button("Start")

                        button("Stop")
                    }
                    button(
                        "Draw") {
                        action {
                            println("${controller.canvas.width}x${controller.canvas.height}")
                            controller.reDraw()
                        }
                    }
                }
                separator(Orientation.VERTICAL)
            }
        }

        center {

            pane {
                minHeight = 250.0
                minWidth = 250.0

                style {
                    //                    backgroundColor += Color.GREY
                }
                canvas {
                    controller.canvas = this

                    widthProperty().bind(this@pane.widthProperty())
                    heightProperty().bind(this@pane.heightProperty())

                    widthProperty().addListener(controller.resizeHandler)
                    heightProperty().addListener(controller.resizeHandler)
                }
            }
        }

        bottom {

            vbox {
                separator(Orientation.HORIZONTAL)

                hbox(spacing = 10, alignment = Pos.CENTER) {
                    paddingAll = 10.0

                    label("Progress:")

                    progressbar {
                        hgrow = Priority.ALWAYS
                        maxWidthProperty().bind(this@hbox.widthProperty())
                        progressProperty().bind(controller.progressValueProperty)
                    }

                    label("50%")
                }
            }
        }
    }
}

class MainViewController : Controller() {
    val progressValueProperty = SimpleDoubleProperty()
    var canvas: Canvas by singleAssign()
    val g: GraphicsContext
        get() = canvas.graphicsContext2D

    val resizeHandler: ChangeListener<Number> = ChangeListener { _, _, _ -> reDraw() }

    fun reDraw() {
        g.clearRect(0.0, 0.0, canvas.width, canvas.height)
        g.fillRect(canvas.width/4, canvas.height/4, canvas.width/2, canvas.height/2)
    }
}