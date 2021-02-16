package tn.mnlr.vripper.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.exception.ValidationException;
import tn.mnlr.vripper.listener.EmitHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.file.StandardOpenOption.*;

@Service
@Setter
@Slf4j
public class SettingsService {

    private final Path configPath;
    private final Path customProxiesPath;
    private final ObjectMapper om = new ObjectMapper();

    private final Set<String> proxies = new HashSet<>();

    private Sinks.Many<Settings> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Getter
    private Settings settings = new Settings();
    @Value("classpath:proxies.json")
    private Resource defaultProxies;

    public SettingsService(@Value("${base.dir}") String baseDir, @Value("${base.dir.name}") String baseDirName) {
        this.configPath = Paths.get(baseDir, baseDirName, "config.json");
        this.customProxiesPath = Paths.get(baseDir, baseDirName, "proxies.json");
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PostConstruct
    private void init() {
        loadViperProxies();
        restore();
        sink.emitNext(settings, EmitHandler.RETRY);
    }

    private void loadViperProxies() {
        try (InputStream defaultProxiesIs = defaultProxies.getInputStream()) {
            List<String> defaultProxies = om.readValue(defaultProxiesIs, new TypeReference<>() {
            });
            List<String> customProxies = new ArrayList<>();
            if (customProxiesPath.toFile().exists() && customProxiesPath.toFile().isFile()) {
                customProxies = om.readValue(customProxiesPath.toFile(), new TypeReference<>() {
                });
            } else {
                if (!customProxiesPath.toFile().createNewFile()) {
                    log.warn("Unable to create " + customProxiesPath.toFile().getAbsolutePath());
                } else {
                    try (FileWriter fw = new FileWriter(customProxiesPath.toFile())) {
                        fw.append("[]").append(System.lineSeparator());
                        fw.flush();
                    } catch (IOException e) {
                        log.warn("Unable to create " + customProxiesPath.toFile().getAbsolutePath(), e);
                    }
                }
            }
            proxies.addAll(customProxies);
            proxies.addAll(defaultProxies);
        } catch (IOException e) {
            log.error("Failed to load vipergirls proxies list", e);
        }
        proxies.add("https://vipergirls.to");
    }

    Flux<Settings> getSettingsFlux() {
        return sink.asFlux();
    }

    public List<String> getProxies() {
        return new ArrayList<>(proxies);
    }

    public void newSettings(Settings settings) {

        if (settings.getVLogin() != null && settings.getVLogin()) {
            if (!this.settings.getVPassword().equals(settings.getVPassword())) {
                settings.setVPassword(DigestUtils.md5Hex(settings.getVPassword()));
            }
        } else {
            settings.setVUsername("");
            settings.setVPassword("");
            settings.setVThanks(false);
            settings.setVLogin(false);
        }
        this.settings = settings;

        save();
        sink.emitNext(settings, EmitHandler.RETRY);
    }

    public void restore() {
        try {
            if (configPath.toFile().exists()) {
                settings = om.readValue(configPath.toFile(), Settings.class);
            }
        } catch (IOException e) {
            log.error("Failed restore user settings", e);
            settings = new Settings();
        }

        if (settings.getDownloadPath() == null) {
            settings.setDownloadPath(System.getProperty("user.home"));
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

        if (settings.getAppendPostId() == null) {
            settings.setAppendPostId(true);
        }

        if (settings.getLeaveThanksOnStart() == null) {
            settings.setLeaveThanksOnStart(false);
        }

        if (settings.getConnectionTimeout() == null) {
            settings.setConnectionTimeout(30);
        }

        if (settings.getMaxAttempts() == null) {
            settings.setMaxAttempts(5);
        }

        if (settings.getVProxy() == null || !proxies.contains(settings.getVProxy())) {
            settings.setVProxy("https://vipergirls.to");
        }

        if (settings.getMaxEventLog() == null) {
            settings.setMaxEventLog(1000);
        }

        try {
            check(this.settings);
        } catch (ValidationException e) {
            log.error(String.format("Your settings are invalid, either remove %s, or fix it", configPath.toString()), e);
            SpringContext.close();
        }

        save();
    }


    public void save() {
        try {
            Files.write(configPath, om.writeValueAsBytes(settings), CREATE, WRITE, TRUNCATE_EXISTING, SYNC);
        } catch (IOException e) {
            log.error("Failed to store user settings", e);
        }
    }

    @PreDestroy
    private void destroy() {
        save();
        sink.emitComplete(EmitHandler.RETRY);
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

        if (settings.getConnectionTimeout() < 1 || settings.getConnectionTimeout() > 300) {
            throw new ValidationException(String.format("Invalid connection timeout settings, values must be in [%d,%d]", 1, 300));
        }

        if (settings.getMaxAttempts() < 1 || settings.getMaxAttempts() > 10) {
            throw new ValidationException(String.format("Invalid maximum attempts settings, values must be in [%d,%d]", 1, 10));
        }

        if (settings.getMaxEventLog() < 100 || settings.getMaxEventLog() > 10_000) {
            throw new ValidationException(String.format("Invalid maximum event log record settings, values must be in [%d,%d]", 100, 10_000));
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

        @JsonProperty("darkTheme")
        private Boolean darkTheme;

        @JsonProperty("appendPostId")
        private Boolean appendPostId;

        @JsonProperty("leaveThanksOnStart")
        private Boolean leaveThanksOnStart;

        @JsonProperty("connectionTimeout")
        private Integer connectionTimeout;

        @JsonProperty("maxAttempts")
        private Integer maxAttempts;

        @JsonProperty("vProxy")
        private String vProxy;

        @JsonProperty("maxEventLog")
        private Integer maxEventLog;

    }
}
