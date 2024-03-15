package me.vripper.gui.components.views

import javafx.scene.control.TabPane
import javafx.scene.layout.Priority
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.View
import tornadofx.tab
import tornadofx.tabpane
import tornadofx.vgrow

class MainView : View("Center view") {

    private val threadTableView: ThreadTableView by inject()
    private val logTableView: LogTableView by inject()
    private val postsTabView: PostsTabView by inject()

    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        vgrow = Priority.ALWAYS
        tab(postsTabView) {
            graphic = FontIcon(Feather.DOWNLOAD)
            id = "download-tab"
        }
        tab(threadTableView) {
            graphic = FontIcon(Feather.FILE_TEXT)
            id = "thread-tab"
        }
        tab(logTableView) {
            graphic = FontIcon(Feather.DATABASE)
            id = "log-tab"
        }
    }
}