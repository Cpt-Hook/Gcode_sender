package ui

import gcode.ArcExpander
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import tornadofx.*
import javafx.scene.control.Alert.AlertType



fun main(args: Array<String>) {
    println("Starting TornadoFX app")
    launch<MyApp>(args)
}

class MyApp : App(MainView::class)

abstract class MyView(title: String? = null, icon: Node? = null) : View(title, icon) {
    protected val controller: MyViewController by inject()
}

class MainView : MyView("Gcode_sender") {
    override val root = borderpane {
        primaryStage.onCloseRequest = controller.closeRequestHandler
        top<MenuView>()
        left<LeftView>()
        center<CenterView>()
        bottom<BottomView>()
    }
}

class MenuView : MyView() {
    override val root =
        menubar {
            menu("File") {
                item("Open").action {
                    val files = chooseFile(
                        "Select a gcode file",
                        arrayOf(FileChooser.ExtensionFilter("GcodeCommand files (.gcode, .nc, .ngc)", "*")),
                        FileChooserMode.Single,
                        primaryStage
                    )
                    controller.openFile(files)
                }
                item("Close").action {
                    controller.closeFile()
                }
                item("Save").action {
                    val files = chooseFile(
                        "Select a file to save",
                        arrayOf(FileChooser.ExtensionFilter("GcodeCommand file (.gcode)", ".gcode")),
                        FileChooserMode.Save,
                        primaryStage
                    )
                    controller.saveFile(files)
                }
            }
            menu("Other") {
                item("Info").action {
                    val alert = Alert(AlertType.INFORMATION)
                    alert.title = "Info"
                    alert.headerText = null
                    alert.contentText = """
                        Gcode_Sender v1.0
                        Ondřej Staníček
                        ondra.stanicek@gmail.com
                        www.github.com/Cpt-Hook/Gcode_sender
                    """.trimIndent()

                    alert.showAndWait()
                }
            }
        }
}

class LeftView : MyView() {
    override val root =
        borderpane {
            top {
                form {
                    fieldset("Connection") {
                        field("IP Address") {
                            textfield {
                                controller.ipAddressProperty.bind(textProperty())
                                enableWhen { !controller.connectedProperty }
                            }
                        }
                        field("Port") {
                            textfield {
                                controller.portProperty.bind(textProperty())
                                enableWhen { !controller.connectedProperty }
                            }
                        }
                    }
                    fieldset("Options") {
                        field("Fans") {
                            checkbox {
                                isSelected = true
                                enableWhen { !controller.connectedProperty }
                                controller.fansProperty.bind(selectedProperty())
                            }
                        }
                    }
                    fieldset("File") {
                        field {
                            label {
                                this@label.textProperty().bind(controller.fileNameProperty)
                            }
                        }
                    }
                    buttonbar {
                        button("Send") {
                            enableWhen { !controller.connectedProperty }
                            action {
                                controller.startStreaming()
                            }
                        }
                        button("Stop") {
                            enableWhen { controller.connectedProperty }
                            action {
                                controller.stopStreaming()
                            }
                        }
                    }
                }
            }
            bottom {
                hbox(spacing = 10, alignment = Pos.BOTTOM_LEFT) {
                    paddingAll = 10
                    label("Status: ")
                    label("NOT CONNECTED") {
                        alignment = Pos.CENTER
                        style {
                            textFill = Color.RED
                        }
                        controller.connectedProperty.addListener { _, _, connected ->
                            if (connected) {
                                style {
                                    textFill = Color.GREEN
                                }
                                text = "CONNECTED"
                            } else {
                                style {
                                    textFill = Color.RED
                                }
                                text = "NOT CONNECTED"
                            }
                        }
                    }
                }
            }
        }
}

class CenterView : MyView() {
    override val root =
        tabpane {
            style {
                backgroundColor += Color.LIGHTGREY
            }
            tab("Visualization") {
                this.isClosable = false

                pane {
                    minWidth = 450.0
                    minHeight = 450.0

                    canvas {
                        controller.canvas = this

                        widthProperty().bind(this@pane.widthProperty())
                        heightProperty().bind(this@pane.heightProperty())

                        widthProperty().addListener(controller.resizeHandler)
                        heightProperty().addListener(controller.resizeHandler)
                    }
                }
            }
            tab("Settings") {
                this.isClosable = false
                form {
                    fieldset("Visualization") {
                        field("Pen width") {
                            hbox(spacing = 10) {
                                slider(1..10, 1) {
                                    isShowTickLabels = true
                                    isShowTickMarks = true
                                    minorTickCount = 0
                                    controller.penWidthProperty.bind(valueProperty())

                                    valueProperty().addListener { _, _, new -> value = new.toInt().toDouble() }
                                    valueProperty().addListener(controller.penWidthHandler)
                                }
                                label {
                                    textProperty().bindBidirectional(
                                        controller.penWidthProperty,
                                        PenWidthStringConverter()
                                    )
                                }
                            }
                        }
                        field("Pen color") {
                            colorpicker(color = Color.BLACK) {
                                controller.colorProperty.bind(valueProperty())
                                valueProperty().addListener(controller.colourHandler)
                            }
                        }
                    }
                    fieldset("Plotting") {
                        field("Max expanded arc length") {
                            hbox(spacing = 10) {
                                slider(0.1..10.0, ArcExpander.maxArcLength) {
                                    isShowTickLabels = true
                                    isShowTickMarks = true
                                    minorTickCount = 0

                                    controller.maxArcLengthProperty.bind(valueProperty())
                                    onMouseReleased = EventHandler { controller.maxArcLengthSelected(value.toFloat()) }
                                }
                                label {
                                    textProperty().bindBidirectional(
                                        controller.maxArcLengthProperty,
                                        MaxArcLengthStringConverter()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
}

class BottomView : MyView() {
    override val root =
        vbox {
            separator(Orientation.HORIZONTAL)

            hbox(spacing = 10, alignment = Pos.CENTER) {
                paddingAll = 10.0

                label("Progress:")

                progressbar {
                    hgrow = Priority.ALWAYS
                    maxWidthProperty().bind(this@hbox.widthProperty())
                    progressProperty().bind(controller.progressPercentProperty)
                }
                label {
                    textProperty().bind(controller.progressLinesProperty)
                }

                label {
                    textProperty().bindBidirectional(controller.progressPercentProperty, ProgressStringConverter())
                }
            }
        }
}
