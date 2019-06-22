package tn.mnlr.vripper.web.restendpoints;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.AppSettings;
import tn.mnlr.vripper.exception.ValidationException;
import tn.mnlr.vripper.services.VipergirlsAuthService;

@RestController
@CrossOrigin(value = "*")
public class SettingsRestEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(SettingsRestEndpoint.class);

    @Autowired
    private AppSettings settings;

    @Autowired
    private VipergirlsAuthService vipergirlsAuthService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception e) {
        logger.error("Error when process request", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }

    @PostMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity postSettings(@RequestBody AppSettings.Settings settings) throws Exception {

        try {
            this.settings.check(settings);
        } catch (ValidationException e) {
            return new ResponseEntity(new Response(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
        this.settings.setDownloadPath(settings.getDownloadPath());
        this.settings.setMaxThreads(settings.getMaxThreads());
        this.settings.setAutoStart(settings.isAutoStart());
        this.settings.setVLogin(settings.isVLogin());

        if(settings.isVLogin()) {
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

        this.settings.save();

        vipergirlsAuthService.authenticate();
        return new ResponseEntity(HttpStatus.OK);
    }

    @GetMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    public AppSettings.Settings getSettings() throws Exception {

        return new AppSettings.Settings(
                settings.getDownloadPath(),
                settings.getMaxThreads(),
                settings.isAutoStart(),
                settings.isVLogin(),
                settings.getVUsername(),
                settings.getVPassword(),
                settings.isVThanks()
        );
    }

    @Getter
    class Response {
        String message;

        public Response(String message) {
            this.message = message;
        }
    }
}
