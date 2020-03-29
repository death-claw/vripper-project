package tn.mnlr.vripper.web.restendpoints;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.exception.ValidationException;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.VipergirlsAuthService;

@RestController
@CrossOrigin(value = "*")
public class SettingsRestEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(SettingsRestEndpoint.class);
    private final AppSettingsService appSettingsService;
    private final VipergirlsAuthService vipergirlsAuthService;

    @Autowired
    public SettingsRestEndpoint(AppSettingsService appSettingsService, VipergirlsAuthService vipergirlsAuthService) {
        this.appSettingsService = appSettingsService;
        this.vipergirlsAuthService = vipergirlsAuthService;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception e) {
        logger.error("Error when process request", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
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
    public ResponseEntity postSettings(@RequestBody AppSettingsService.Settings settings) throws Exception {

        synchronized (this.appSettingsService) {
            try {
                this.appSettingsService.check(settings);
            } catch (ValidationException e) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(e.getMessage());
            }

            this.appSettingsService.newSettings(settings);
            vipergirlsAuthService.authenticate();
        }
        return ResponseEntity.ok(getAppSettingsService());
    }

    @GetMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    public AppSettingsService.Settings getAppSettingsService() {

        return appSettingsService.getSettings();
    }

    @Getter
    static class Response {
        String message;

        Response(String message) {
            this.message = message;
        }
    }
}
