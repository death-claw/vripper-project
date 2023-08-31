package me.mnlr.vripper

import jakarta.annotation.PreDestroy
import me.mnlr.vripper.download.DownloadService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class Management(
    private val jdbcTemplate: JdbcTemplate,
    private val downloadService: DownloadService
) {

    @PreDestroy
    fun destroy() {
        downloadService.destroy()
        jdbcTemplate.execute("SHUTDOWN")
    }
}