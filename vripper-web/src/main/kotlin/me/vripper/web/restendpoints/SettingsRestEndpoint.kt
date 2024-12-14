package me.vripper.web.restendpoints

import kotlinx.coroutines.runBlocking
import me.vripper.model.Settings
import me.vripper.services.IAppEndpointService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api")
class SettingsRestEndpoint : KoinComponent {

    private val appEndpointService: IAppEndpointService by inject(named("localAppEndpointService"))

    @GetMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    fun getAppSettingsService(): Settings {
        return runBlocking { appEndpointService.getSettings() }
    }

    @PostMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    fun postSettings(@RequestBody settings: Settings): Settings {
        return runBlocking {
            appEndpointService.saveSettings(settings)
            appEndpointService.getSettings()
        }
    }

    @GetMapping("/settings/proxies")
    @ResponseStatus(value = HttpStatus.OK)
    fun mirrors(): List<String> {
        return runBlocking { appEndpointService.getProxies() }
    }

}