package ui

import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import tornadofx.*

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
                        arrayOf(FileChooser.ExtensionFilter("Gcode files (.gcode, .nc)", "*")),
                        FileChooserMode.Single,
                        null
                    )
                    controller.chooseFile(files)
                }
                item("Close").action {
                    controller.closeFile()
                }
                item("Save")
            }
            menu("Other") {
                item("Info")
                item("Help")
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
                            if(connected) {
                                style {
                                    textFill = Color.GREEN
                                }
                                text = "CONNECTED"
                            }else{
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
                    minWidth = 400.0
                    minHeight = 400.0

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
                label{
                    textProperty().bind(controller.progressLinesProperty)
                }

                label {
                    textProperty().bindBidirectional(controller.progressPercentProperty, ProgressStringConverter())
                }
            }
        }
}

