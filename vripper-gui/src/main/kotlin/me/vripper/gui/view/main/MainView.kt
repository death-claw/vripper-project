package me.vripper.gui.view.main

import javafx.scene.control.TabPane
import javafx.scene.image.ImageView
import javafx.util.Duration
import me.vripper.gui.view.actionbar.ActionBarView
import me.vripper.gui.view.actionbar.DownloadActionsView
import me.vripper.gui.view.actionbar.LogActionsView
import me.vripper.gui.view.actionbar.ThreadActionsView
import me.vripper.gui.view.tables.LogTableView
import me.vripper.gui.view.tables.PostsTableView
import me.vripper.gui.view.tables.ThreadTableView
import tornadofx.*

class MainView : View("Center view") {

    private val actionBarView: ActionBarView by inject()

    private val downloadActionBar: DownloadActionsView by inject()
    private val threadActionsView: ThreadActionsView by inject()
    private val logActionsView: LogActionsView by inject()

    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        selectionModel.selectedItemProperty()
            .addListener(ChangeListener { _, _, newValue ->
                run {
                    when (newValue.id) {
                        "download-tab" -> {
                            actionBarView.root.center.replaceWith(
                                downloadActionBar.root,
                                ViewTransition.FadeThrough(Duration.millis(100.0))
                            )
                        }

                        "thread-tab" -> {
                            actionBarView.root.center.replaceWith(
                                threadActionsView.root,
                                ViewTransition.FadeThrough(Duration.millis(100.0))
                            )
                        }

                        "log-tab" -> {
                            actionBarView.root.center.replaceWith(
                                logActionsView.root,
                                ViewTransition.FadeThrough(Duration.millis(100.0))
                            )
                        }
                    }
                }
            })
        tab<PostsTableView> {
            val imageView = ImageView("download.png")
            imageView.fitWidth = 18.0
            imageView.fitHeight = 18.0
            id = "download-tab"
            graphic = imageView
        }
        tab<ThreadTableView> {
            val imageView = ImageView("analyze.png")
            imageView.fitWidth = 18.0
            imageView.fitHeight = 18.0
            id = "thread-tab"
            graphic = imageView
        }
        tab<LogTableView> {
            val imageView = ImageView("bug.png")
            imageView.fitWidth = 18.0
            imageView.fitHeight = 18.0
            id = "log-tab"
            graphic = imageView
        }
    }
}