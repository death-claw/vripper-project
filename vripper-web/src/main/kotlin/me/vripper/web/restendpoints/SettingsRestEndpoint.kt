package me.vripper.web.restendpoints

import me.vripper.exception.ValidationException
import me.vripper.model.Settings
import me.vripper.services.SettingsService
import me.vripper.web.restendpoints.domain.Theme
import me.vripper.web.restendpoints.exceptions.BadRequestException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api")
class SettingsRestEndpoint : KoinComponent {

    private val log by me.vripper.delegate.LoggerDelegate()
    private val settingsService: SettingsService by inject()

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