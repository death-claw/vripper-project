package tn.mnlr.vripper.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.exception.ValidationException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.*;

@Service
@Getter
@Setter
@Slf4j
public class AppSettingsService {

    private final String MAX_TOTAL_THREADS = "MAX_TOTAL_THREADS";
    private final String baseDir;

    private final Path configPath;
    private final ObjectMapper om = new ObjectMapper();

    private Settings settings = new Settings();

    public AppSettingsService(@Value("${base.dir}") String baseDir, @Value("${base.dir.name}") String baseDirName) {
        this.baseDir = baseDir;
        this.configPath = Paths.get(baseDir, baseDirName, "config.json");
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PostConstruct
    private void init() {
        restore();
    }

    public void newSettings(Settings settings) {

        if (settings.getVLogin()) {
            if (!this.settings.getVPassword().equals(settings.getVPassword())) {
                settings.setVPassword(settings.getVPassword());
            }
        } else {
            settings.setVUsername("");
            settings.setVPassword("");
            settings.setVThanks(false);
        }
        this.settings = settings;

        save();
    }

    public void restore() {
        try {
            settings = om.readValue(configPath.toFile(), Settings.class);
            try {
                check(this.settings);
            } catch (ValidationException e) {
                log.error(String.format("Your settings are invalid, either remove %s, or fix it", configPath.toString()), e);
                SpringContext.close();
            }
        } catch (IOException e) {
            log.error("Failed restore user settings", e);
            settings = new Settings();
        }

        if (settings.getDownloadPath() == null) {
            settings.setDownloadPath(baseDir);
        }

        if (settings.getMaxThreads() == null) {
            settings.setMaxThreads(4);
        }

        if (settings.getMaxTotalThreads() == null) {
            settings.setMaxTotalThreads(0);
        }

        if (settings.getAutoStart() == null) {
            settings.setAutoStart(true);
        }

        if (settings.getVLogin() == null) {
            settings.setVLogin(false);
        }

        if (settings.getVUsername() == null) {
            settings.setVUsername("");
        }

        if (settings.getVPassword() == null) {
            settings.setVPassword("");
        }

        if (settings.getVThanks() == null) {
            settings.setVThanks(false);
        }

        if (settings.getDesktopClipboard() == null) {
            settings.setDesktopClipboard(false);
        }

        if (settings.getForceOrder() == null) {
            settings.setForceOrder(false);
        }

        if (settings.getSubLocation() == null) {
            settings.setSubLocation(false);
        }

        if (settings.getThreadSubLocation() == null) {
            settings.setThreadSubLocation(false);
        }

        if (settings.getClearCompleted() == null) {
            settings.setClearCompleted(false);
        }

        if (settings.getDarkTheme() == null) {
            settings.setDarkTheme(false);
        }

        if (settings.getViewPhotos() == null) {
            settings.setViewPhotos(false);
        }

        if (settings.getNotification() == null) {
            settings.setNotification(false);
        }


        save();
    }

    @PreDestroy
    public void save() {
        try {
            // force disable gallery
            settings.setViewPhotos(false);

            Files.write(configPath, om.writeValueAsBytes(settings), CREATE, WRITE, TRUNCATE_EXISTING, SYNC);
        } catch (IOException e) {
            log.error("Failed to store user settings", e);
        }
    }

    public void check(Settings settings) throws ValidationException {

        Path path;
        try {
            path = Paths.get(settings.getDownloadPath());
        } catch (InvalidPathException e) {
            throw new ValidationException(String.format("%s is invalid", settings.getDownloadPath()));
        }
        if (!Files.exists(path)) {
            throw new ValidationException(String.format("%s does not exist", settings.getDownloadPath()));
        } else if (!Files.isDirectory(path)) {
            throw new ValidationException(String.format("%s is not a directory", settings.getDownloadPath()));
        }

        if (settings.getMaxTotalThreads() < 0 || settings.getMaxTotalThreads() > 12) {
            throw new ValidationException(String.format("Invalid max global concurrent download settings, values must be in [%d,%d]", 0, 12));
        }

        if (settings.getMaxThreads() < 1 || settings.getMaxThreads() > 4) {
            throw new ValidationException(String.format("Invalid max concurrent download settings, values must be in [%d,%d]", 1, 4));
        }
    }

    public Theme getTheme() {
        return new Theme(this.settings.getDarkTheme());
    }

    public void setTheme(Theme theme) {
        this.settings.setDarkTheme(theme.darkTheme);
        save();
    }

    @Getter
    @NoArgsConstructor
    public static class Theme {

        @JsonProperty("darkTheme")
        private boolean darkTheme;

        Theme(boolean darkTheme) {
            this.darkTheme = darkTheme;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Settings {

        @JsonProperty("downloadPath")
        private String downloadPath;
        @JsonProperty("maxThreads")
        private Integer maxThreads;
        @JsonProperty("maxTotalThreads")
        private Integer maxTotalThreads;
        @JsonProperty("autoStart")
        private Boolean autoStart;
        @JsonProperty("vLogin")
        private Boolean vLogin;
        @JsonProperty("vUsername")
        private String vUsername;
        @JsonProperty("vPassword")
        private String vPassword;
        @JsonProperty("vThanks")
        private Boolean vThanks;
        @JsonProperty("desktopClipboard")
        private Boolean desktopClipboard;
        @JsonProperty("forceOrder")
        private Boolean forceOrder;
        @JsonProperty("subLocation")
        private Boolean subLocation;
        @JsonProperty("threadSubLocation")
        private Boolean threadSubLocation;
        @JsonProperty("clearCompleted")
        private Boolean clearCompleted;
        @JsonProperty("viewPhotos")
        private Boolean viewPhotos;
        @JsonProperty("notification")
        private Boolean notification;
        @JsonProperty("darkTheme")
        private Boolean darkTheme;

        public void setVPassword(String vPassword) {
            if (vPassword == null || vPassword.isEmpty()) {
                this.vPassword = "";
            } else {
                this.vPassword = DigestUtils.md5Hex(vPassword);
            }
        }
    }
}
