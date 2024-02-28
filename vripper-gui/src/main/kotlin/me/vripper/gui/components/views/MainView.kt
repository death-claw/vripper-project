package me.vripper.gui.components.views

import javafx.scene.control.TabPane
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.ChangeListener
import tornadofx.View
import tornadofx.tabpane

class MainView : View("Center view") {

    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        selectionModel.selectedItemProperty()
            .addListener(ChangeListener { _, _, newValue ->
                run {
                }
            })
        tab<PostsTableView> {
            graphic = FontIcon(Feather.DOWNLOAD)
            id = "download-tab"
        }
        tab<ThreadTableView> {
            graphic = FontIcon(Feather.FILE_TEXT)
            id = "thread-tab"
        }
        tab<LogTableView> {
            graphic = FontIcon(Feather.DATABASE)
            id = "log-tab"
        }
    }
}