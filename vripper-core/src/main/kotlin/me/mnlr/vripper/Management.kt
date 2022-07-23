package me.mnlr.vripper

import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import me.mnlr.vripper.download.DownloadService
import me.mnlr.vripper.services.ThreadPoolService
import java.nio.file.Path
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern

@Component
class Management(
    private val jdbcTemplate: JdbcTemplate,
    @Value("\${base.dir}") baseDir: String,
    @Value("\${base.dir.name}") baseDirName: String,
    private val threadPoolService: ThreadPoolService,
    private val downloadService: DownloadService
) {
    private val backupFolder: Path

    init {
        backupFolder = Path.of(baseDir, baseDirName, "backup")
    }

    @PreDestroy
    fun destroy() {
        threadPoolService.destroy()
        downloadService.destroy()
        jdbcTemplate.execute("SHUTDOWN")
    }

//    @PostConstruct
//    @Scheduled(cron = "0 0 0 ? * *")
//    private fun backup() {
//
//        for (file in Files.list(backupFolder)) {
//            val matcher = BACKUP_FILE_PATTERN.matcher(file.fileName.toString())
//            if (matcher.find()) {
//                val localDate = LocalDate.parse(matcher.group(1), FORMATTER)
//                if (localDate.isEqual(LocalDate.now())) {
//                    return
//                }
//            }
//        }
//
//        // create a backup file
//        val backupFile = backupFolder.resolve("db_${LocalDateTime.now().format(FORMATTER)}.tar.gz")
//        jdbcTemplate.execute("BACKUP DATABASE TO '${backupFile}' BLOCKING")
//    }

    companion object {
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val BACKUP_FILE_PATTERN = Pattern.compile("^db_(\\d{4}-\\d{2}-\\d{2})\\.tar\\.gz$")
    }
}