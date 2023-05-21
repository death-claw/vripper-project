package me.mnlr.vripper.services

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import me.mnlr.vripper.SpringContext
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.exception.ValidationException
import me.mnlr.vripper.model.Settings
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.FileWriter
import java.io.IOException
import java.nio.file.*

@Service
class SettingsService(
    @Value("\${base.dir}") baseDir: String,
    @Value("\${base.dir.name}") baseDirName: String,
    private val eventBus: EventBus,
    @Value("classpath:proxies.json") private val defaultProxies: Resource
) {
    private val log by LoggerDelegate()
    private val configPath: Path
    private val customProxiesPath: Path
    private val om = ObjectMapper(YAMLFactory())
    private val proxies: MutableSet<String> = HashSet()


    var settings = Settings()

    init {
        configPath = Paths.get(baseDir, baseDirName, "config.yml")
        customProxiesPath = Paths.get(baseDir, baseDirName, "proxies.json")
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .findAndRegisterModules().registerKotlinModule()
    }

    @PostConstruct
    private fun init() {
        loadViperProxies()
        restore()
        eventBus.publishEvent(Event(Event.Kind.SETTINGS_UPDATE, settings))
    }

    private fun loadViperProxies() {
        try {
            defaultProxies.inputStream.use { defaultProxiesIs ->
                val defaultProxies: List<String> = om.readValue(defaultProxiesIs)
                val customProxies: List<String> = if (customProxiesPath.toFile()
                        .exists() && Files.isRegularFile(customProxiesPath)
                ) {
                    om.readValue(
                        customProxiesPath.toFile()
                    )
                } else {
                    emptyList()
                }

                try {
                    FileWriter(customProxiesPath.toFile()).use { fw ->
                        fw.append("[]").append(System.lineSeparator())
                        fw.flush()
                    }
                } catch (e: IOException) {
                    log.warn(
                        "Unable to create " + customProxiesPath.toFile().absolutePath, e
                    )
                }

                proxies.addAll(customProxies)
                proxies.addAll(defaultProxies)
            }
        } catch (e: IOException) {
            log.error("Failed to load vipergirls proxies list", e)
        }
        proxies.add("https://vipergirls.to")
    }

    fun getProxies(): List<String> {
        return ArrayList(proxies)
    }

    fun newSettings(settings: Settings) {
        if (settings.viperSettings.login) {
            if (this.settings.viperSettings.password != settings.viperSettings.password) {
                settings.viperSettings.password =
                    DigestUtils.md5Hex(settings.viperSettings.password)
            }
        } else {
            settings.viperSettings.username = ""
            settings.viperSettings.password = ""
            settings.viperSettings.thanks = false
            settings.viperSettings.login = false
        }
        check(settings)
        this.settings = settings
        save()
        eventBus.publishEvent(Event(Event.Kind.SETTINGS_UPDATE, settings))
    }

    fun restore() {
        try {
            if (configPath.toFile().exists()) {
                settings = om.readValue(configPath.toFile())
            }
        } catch (e: IOException) {
            log.error("Failed restore user settings", e)
            settings = Settings()
        }
        if (!proxies.contains(settings.viperSettings.host)) {
            settings.viperSettings.host = "https://vipergirls.to"
        }
        try {
            check(settings)
        } catch (e: ValidationException) {
            log.error(
                String.format(
                    "Your settings are invalid, either remove %s, or fix it", configPath
                ), e
            )
            SpringContext.close()
        }
        save()
    }

    fun save() {
        try {
            Files.write(
                configPath,
                om.writeValueAsBytes(settings),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.SYNC
            )
        } catch (e: IOException) {
            log.error("Failed to store user settings", e)
        }
    }

    @PreDestroy
    private fun destroy() {
        save()
    }

    @Throws(ValidationException::class)
    fun check(settings: Settings) {
        val path: Path = try {
            Paths.get(settings.downloadSettings.downloadPath)
        } catch (e: InvalidPathException) {
            throw ValidationException(
                String.format(
                    "%s is invalid", settings.downloadSettings.downloadPath
                )
            )
        }
        if (!Files.exists(path)) {
            throw ValidationException(
                String.format(
                    "%s does not exist", settings.downloadSettings.downloadPath
                )
            )
        } else if (!Files.isDirectory(path)) {
            throw ValidationException(
                String.format(
                    "%s is not a directory", settings.downloadSettings.downloadPath
                )
            )
        }

        if (settings.downloadSettings.autoQueueThreshold < 0) {
            throw ValidationException("Invalid auto queue settings, value must be a positive integer")
        }

        if (settings.connectionSettings.maxTotalThreads < 0 || settings.connectionSettings.maxTotalThreads > 12) {
            throw ValidationException(
                "Invalid max global concurrent download settings, values must be in [0,12]"
            )
        }
        if (settings.connectionSettings.maxThreads < 1 || settings.connectionSettings.maxThreads > 4) {
            throw ValidationException("Invalid max concurrent download settings, values must be in [1,4]")
        }
        if (settings.connectionSettings.timeout < 1 || settings.connectionSettings.timeout > 300) {
            throw ValidationException(
                "Invalid connection timeout settings, values must be in [1,300]"
            )
        }
        if (settings.connectionSettings.maxAttempts < 1 || settings.connectionSettings.maxAttempts > 10) {
            throw ValidationException("Invalid maximum attempts settings, values must be in [1,10]")
        }
        if (settings.maxEventLog < 100 || settings.maxEventLog > 10000) {
            throw ValidationException(
                "Invalid maximum event log record settings, values must be in [100,10000]"
            )
        }
        if (settings.clipboardSettings.pollingRate < 500) {
            throw ValidationException(
                "Invalid clipboard monitoring polling rate settings, values must be >= 500"
            )
        }
    }
}