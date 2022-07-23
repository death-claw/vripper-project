package me.mnlr.vripper.tasks

import me.mnlr.vripper.SpringContext
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.entities.LogEvent.Status.*
import me.mnlr.vripper.formatToString
import me.mnlr.vripper.model.ThreadInput
import me.mnlr.vripper.repositories.impl.LogEventRepositoryImpl
import me.mnlr.vripper.services.SettingsService
import me.mnlr.vripper.services.ThreadPoolService
import java.time.LocalDateTime
import java.util.regex.Pattern

class LinkScanRunnable(private val urlList: List<String>) : Runnable {
    private val log by LoggerDelegate()
    private val settingsService: SettingsService =
        SpringContext.getBean(SettingsService::class.java)
    private val threadPoolService: ThreadPoolService =
        SpringContext.getBean(ThreadPoolService::class.java)
    private val eventRepository: LogEventRepositoryImpl =
        SpringContext.getBean(LogEventRepositoryImpl::class.java)
    private val logEvent: LogEvent

    init {
        logEvent = eventRepository.save(
            LogEvent(
                type = LogEvent.Type.SCAN,
                status = PENDING,
                time = LocalDateTime.now(),
                message = "Links to scan:\n\t${urlList.joinToString("\n\t")}"
            )
        )
    }

    override fun run() {
        try {
            eventRepository.update(logEvent.copy(status = PROCESSING))
            val threadInputList = mutableListOf<ThreadInput>()
            val unsupported = mutableListOf<String>()
            val unrecognized = mutableListOf<String>()
            for (link in urlList) {
                log.debug("Starting to process thread: $link")
                if (!link.startsWith(settingsService.settings.viperSettings.host)) {
                    log.error("Unsupported link $link")
                    unsupported.add(link)
                    continue
                }
                var threadId: String
                val m = Pattern.compile(
                    Pattern.quote(settingsService.settings.viperSettings.host) + "/threads/(\\d+).*"
                ).matcher(link)
                if (m.find()) {
                    threadId = m.group(1)
                } else {
                    log.error("Cannot retrieve thread id from URL $link")
                    unrecognized.add(link)
                    continue
                }
                threadInputList.add(ThreadInput(link = link, threadId = threadId))
            }
            val errorMessage: String = if (unsupported.isNotEmpty()) {
                """Unsupported links:
    ${unsupported.joinToString("\n\t")}
    """
            } else if (unrecognized.isNotEmpty()) {
                """Unrecognized links:
    ${unrecognized.joinToString("\n\t")}
    """
            } else {
                ""
            }
            if (errorMessage.isNotBlank()) {
                eventRepository.update(
                    logEvent.copy(
                        status = ERROR, message = "Some links failed to be scanned: \n$errorMessage"
                    )
                )
            }
            for (threadInput in threadInputList) {
//                if (thread.postId.isNotBlank()) {
//                    threadPoolService.generalExecutor.submit(
//                            SinglePostRunnable(
//                                thread.threadId, thread.postId
//                            )
//                        )
//                } else {
                threadPoolService.generalExecutor.submit(
                    ThreadLookupRunnable(
                        threadInput,
                        settingsService.settings
                    )
                )
//                }
            }
            eventRepository.update(logEvent.copy(status = DONE))
        } catch (e: Exception) {
            val error = "Error when scanning links"
            log.error(error, e)
            eventRepository.update(
                logEvent.copy(
                    status = ERROR, message = """
                $error
                ${e.formatToString()}
                """.trimIndent()
                )
            )
        }
    }
}