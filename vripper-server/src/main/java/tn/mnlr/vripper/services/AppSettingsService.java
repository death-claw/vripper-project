package tn.mnlr.vripper.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.exception.ValidationException;

import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

@Service
@Getter
@Setter
public class AppSettingsService {

    @Value("${base.dir}")
    private String defaultDownloadPath;

    private static final Logger logger = LoggerFactory.getLogger(AppSettingsService.class);

    private Preferences prefs = Preferences.userNodeForPackage(AppSettingsService.class);

    private final String DOWNLOAD_PATH = "DOWNLOAD_PATH";
    private final String MAX_THREADS = "MAX_THREADS";
    private final String AUTO_START = "AUTO_START";
    private final String V_LOGIN = "VLOGIN";
    private final String V_USERNAME = "VUSERNAME";
    private final String V_PASSWORD = "VPASSWORD";
    private final String V_THANKS = "VTHANKS";
    private final String DESKTOP_CLIPBOARD = "DESKTOP_CLIPBOARD";
    private final String FORCE_ORDER = "FORCE_ORDER";
    private final String SUBFOLDER = "SUBFOLDER";
    private final String THREADSUBFOLDER = "THREADSUBFOLDER";
    private final String CLEAR = "CLEAR";
    private final String DARK_THEME = "DARK_THEME";

    private String downloadPath;
    private int maxThreads;
    private boolean autoStart;
    private boolean vLogin;
    private String vUsername;
    private String vPassword;
    private boolean vThanks;
    private boolean desktopClipboard;
    private boolean subLocation;
    private boolean threadSubLocation;
    private boolean forceOrder;
    private boolean clearCompleted;
    private boolean darkTheme;

    public void setVPassword(String vPassword) {
        if(vPassword.isEmpty()) {
            this.vPassword = "";
        } else {
            this.vPassword = DigestUtils.md5Hex(vPassword);
        }
    }

    public void restore() {

        downloadPath = prefs.get(DOWNLOAD_PATH, defaultDownloadPath);
        maxThreads = prefs.getInt(MAX_THREADS, 4);
        autoStart = prefs.getBoolean(AUTO_START, true);
        vLogin = prefs.getBoolean(V_LOGIN, false);
        vUsername = prefs.get(V_USERNAME, "");
        vPassword = prefs.get(V_PASSWORD, "");
        vThanks = prefs.getBoolean(V_THANKS, false);
        desktopClipboard = prefs.getBoolean(DESKTOP_CLIPBOARD, false);
        forceOrder = prefs.getBoolean(FORCE_ORDER, false);
        subLocation = prefs.getBoolean(SUBFOLDER, false);
        threadSubLocation = prefs.getBoolean(THREADSUBFOLDER, false);
        clearCompleted = prefs.getBoolean(CLEAR, false);
        darkTheme = prefs.getBoolean(DARK_THEME, false);
    }

    @PreDestroy
    public void save() {

        prefs.put(DOWNLOAD_PATH, downloadPath);
        prefs.putInt(MAX_THREADS, maxThreads);
        prefs.putBoolean(AUTO_START, autoStart);
        prefs.putBoolean(V_LOGIN, vLogin);
        prefs.put(V_USERNAME, vUsername);
        prefs.put(V_PASSWORD, vPassword);
        prefs.putBoolean(V_THANKS, vThanks);
        prefs.putBoolean(DESKTOP_CLIPBOARD, desktopClipboard);
        prefs.putBoolean(FORCE_ORDER, forceOrder);
        prefs.putBoolean(SUBFOLDER, subLocation);
        prefs.putBoolean(THREADSUBFOLDER, threadSubLocation);
        prefs.putBoolean(CLEAR, clearCompleted);
        prefs.putBoolean(DARK_THEME, darkTheme);

        try {
            prefs.sync();
        } catch (BackingStoreException e) {
            logger.error("Failed to store user settings", e);
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

        if (settings.getMaxThreads() < 1 || settings.getMaxThreads() > 4) {
            throw new ValidationException(String.format("Invalid max concurrent download settings, values must be in [%d,%d]", 1, 4));
        }
    }

    public Theme getTheme() {
        return new Theme(this.darkTheme);
    }

    public void setTheme(Theme theme) {
        this.darkTheme = theme.darkTheme;
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
    public static class Settings {

        @JsonProperty("downloadPath")
        private String downloadPath;
        @JsonProperty("maxThreads")
        private int maxThreads;
        @JsonProperty("autoStart")
        private boolean autoStart;
        @JsonProperty("vLogin")
        private boolean vLogin;
        @JsonProperty("vUsername")
        private String vUsername;
        @JsonProperty("vPassword")
        private String vPassword;
        @JsonProperty("vThanks")
        private boolean vThanks;
        @JsonProperty("desktopClipboard")
        private boolean desktopClipboard;
        @JsonProperty("forceOrder")
        private boolean forceOrder;
        @JsonProperty("subLocation")
        private boolean subLocation;
        @JsonProperty("threadSubLocation")
        private boolean threadSubLocation;
        @JsonProperty("clearCompleted")
        private boolean clearCompleted;

        public Settings(String downloadPath, int maxThreads, boolean autoStart, boolean vLogin, String vUsername, String vPassword, boolean vThanks, boolean desktopClipboard, boolean forceOrder, boolean subLocation, boolean threadSubLocation, boolean clearCompleted) {
            this.downloadPath = downloadPath;
            this.maxThreads = maxThreads;
            this.autoStart = autoStart;
            this.vLogin = vLogin;
            this.vUsername = vUsername;
            this.vPassword = vPassword;
            this.vThanks = vThanks;
            this.desktopClipboard = desktopClipboard;
            this.forceOrder = forceOrder;
            this.subLocation = subLocation;
            this.threadSubLocation = threadSubLocation;
            this.clearCompleted = clearCompleted;
        }
    }
}
