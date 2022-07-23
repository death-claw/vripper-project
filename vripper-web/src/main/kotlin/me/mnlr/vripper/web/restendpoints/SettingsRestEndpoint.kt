package me.mnlr.vripper.web.restendpoints

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.exception.ValidationException
import me.mnlr.vripper.model.Settings
import me.mnlr.vripper.services.SettingsService
import me.mnlr.vripper.web.restendpoints.domain.Theme
import me.mnlr.vripper.web.restendpoints.exceptions.BadRequestException


@RestController
@CrossOrigin(value = ["*"])
class SettingsRestEndpoint @Autowired constructor(private val settingsService: SettingsService) {

    private val log by LoggerDelegate()

    @PostMapping("/settings/theme")
    @ResponseStatus(value = HttpStatus.OK)
    fun postTheme(@RequestBody theme: Theme): Theme {
        return theme
    }

    @GetMapping("/settings/theme")
    @ResponseStatus(value = HttpStatus.OK)
    fun getTheme(): Theme? {
        return Theme(true)
    }

    @GetMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    fun getAppSettingsService(): Settings? {
        return settingsService.settings
    }

    @PostMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    fun postSettings(@RequestBody settings: Settings?): Settings {
        try {
            settingsService.check(settings!!)
        } catch (e: ValidationException) {
            log.error("Invalid settings", e)
            throw BadRequestException(e.message)
        }
        settingsService.newSettings(settings)
        return settingsService.settings
    }

    @GetMapping("/settings/proxies")
    @ResponseStatus(value = HttpStatus.OK)
    fun mirrors(): List<String> {
        return settingsService.getProxies()
    }
}