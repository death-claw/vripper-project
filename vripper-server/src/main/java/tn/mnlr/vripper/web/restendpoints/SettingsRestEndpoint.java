package tn.mnlr.vripper.web.restendpoints;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.exception.ValidationException;
import tn.mnlr.vripper.services.SettingsService;
import tn.mnlr.vripper.services.VGAuthService;
import tn.mnlr.vripper.web.restendpoints.exceptions.BadRequestException;

@RestController
@Slf4j
@CrossOrigin(value = "*")
public class SettingsRestEndpoint {

    private final SettingsService settingsService;
    private final VGAuthService VGAuthService;

    @Autowired
    public SettingsRestEndpoint(SettingsService settingsService, VGAuthService VGAuthService) {
        this.settingsService = settingsService;
        this.VGAuthService = VGAuthService;
    }

    @PostMapping("/settings/theme")
    @ResponseStatus(value = HttpStatus.OK)
    public SettingsService.Theme postTheme(@RequestBody SettingsService.Theme theme) {
        this.settingsService.setTheme(theme);
        return settingsService.getTheme();
    }

    @GetMapping("/settings/theme")
    @ResponseStatus(value = HttpStatus.OK)
    public SettingsService.Theme getTheme() {
        return settingsService.getTheme();
    }

    @PostMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    public SettingsService.Settings postSettings(@RequestBody SettingsService.Settings settings) throws Exception {

        try {
            this.settingsService.check(settings);
        } catch (ValidationException e) {
            log.error("Invalid settings", e);
            throw new BadRequestException(e.getMessage());
        }

        this.settingsService.newSettings(settings);
        VGAuthService.authenticate();
        return getAppSettingsService();
    }

    @GetMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    public SettingsService.Settings getAppSettingsService() {

        return settingsService.getSettings();
    }
}
