package tn.mnlr.vripper.web.restendpoints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.exception.ValidationException;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.VipergirlsAuthService;
import tn.mnlr.vripper.web.restendpoints.exceptions.BadRequestException;

@RestController
@CrossOrigin(value = "*")
public class SettingsRestEndpoint {

    private final AppSettingsService appSettingsService;
    private final VipergirlsAuthService vipergirlsAuthService;

    @Autowired
    public SettingsRestEndpoint(AppSettingsService appSettingsService, VipergirlsAuthService vipergirlsAuthService) {
        this.appSettingsService = appSettingsService;
        this.vipergirlsAuthService = vipergirlsAuthService;
    }

    @PostMapping("/settings/theme")
    @ResponseStatus(value = HttpStatus.OK)
    public AppSettingsService.Theme postTheme(@RequestBody AppSettingsService.Theme theme) {
        synchronized (this.appSettingsService) {
            this.appSettingsService.setTheme(theme);
            return appSettingsService.getTheme();
        }
    }

    @GetMapping("/settings/theme")
    @ResponseStatus(value = HttpStatus.OK)
    public AppSettingsService.Theme getTheme() {
        return appSettingsService.getTheme();
    }

    @PostMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    public AppSettingsService.Settings postSettings(@RequestBody AppSettingsService.Settings settings) throws Exception {

        synchronized (this.appSettingsService) {
            try {
                this.appSettingsService.check(settings);
            } catch (ValidationException e) {
                throw new BadRequestException(e.getMessage());
            }

            this.appSettingsService.newSettings(settings);
            vipergirlsAuthService.authenticate();
        }
        return getAppSettingsService();
    }

    @GetMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    public AppSettingsService.Settings getAppSettingsService() {

        return appSettingsService.getSettings();
    }
}
