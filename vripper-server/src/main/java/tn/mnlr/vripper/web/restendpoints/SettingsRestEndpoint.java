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

    @Autowired
    private AppSettingsService settings;

    @Autowired
    private VipergirlsAuthService vipergirlsAuthService;

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
        synchronized (this.settings) {
            this.settings.setTheme(theme);
            return settings.getTheme();
        }
    }

    @GetMapping("/settings/theme")
    @ResponseStatus(value = HttpStatus.OK)
    public AppSettingsService.Theme getTheme() {
        return settings.getTheme();
    }

    @PostMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity postSettings(@RequestBody AppSettingsService.Settings settings) throws Exception {

        synchronized (this.settings) {
            try {
                this.settings.check(settings);
            } catch (ValidationException e) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(e.getMessage());
            }
            this.settings.setDownloadPath(settings.getDownloadPath());
            this.settings.setMaxThreads(settings.getMaxThreads());
            this.settings.setAutoStart(settings.isAutoStart());
            this.settings.setVLogin(settings.isVLogin());

            if (settings.isVLogin()) {
                this.settings.setVUsername(settings.getVUsername());
                if (!this.settings.getVPassword().equals(settings.getVPassword())) {
                    this.settings.setVPassword(settings.getVPassword());
                }
                this.settings.setVThanks(settings.isVThanks());
            } else {
                this.settings.setVUsername("");
                this.settings.setVPassword("");
                this.settings.setVThanks(false);
            }
            this.settings.setDesktopClipboard(settings.isDesktopClipboard());
            this.settings.setForceOrder(settings.isForceOrder());
            this.settings.setSubLocation(settings.isSubLocation());
            this.settings.setThreadSubLocation(settings.isThreadSubLocation());
            this.settings.setClearCompleted(settings.isClearCompleted());

            this.settings.save();

            vipergirlsAuthService.authenticate();
        }
        return ResponseEntity.ok(getSettings());
    }

    @GetMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    public AppSettingsService.Settings getSettings() {

        return new AppSettingsService.Settings(
                settings.getDownloadPath(),
                settings.getMaxThreads(),
                settings.isAutoStart(),
                settings.isVLogin(),
                settings.getVUsername(),
                settings.getVPassword(),
                settings.isVThanks(),
                settings.isDesktopClipboard(),
                settings.isForceOrder(),
                settings.isSubLocation(),
                settings.isThreadSubLocation(),
                settings.isClearCompleted()
        );
    }

    @Getter
    static class Response {
        String message;

        Response(String message) {
            this.message = message;
        }
    }
}
