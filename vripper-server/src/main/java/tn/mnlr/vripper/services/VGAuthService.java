package tn.mnlr.vripper.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tn.mnlr.vripper.exception.VripperException;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.listener.EmitHandler;
import tn.mnlr.vripper.services.domain.tasks.LeaveThanksRunnable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VGAuthService {

    private final ConnectionService cm;
    private final SettingsService settingsService;
    private final ThreadPoolService threadPoolService;

    private final Disposable disposable;

    @Getter
    private final HttpClientContext context = HttpClientContext.create();
    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
    @Getter
    private boolean authenticated = false;
    @Getter
    private String loggedUser = "";

    @Autowired
    public VGAuthService(ConnectionService cm, SettingsService settingsService, ThreadPoolService threadPoolService) {
        this.cm = cm;
        this.settingsService = settingsService;
        this.threadPoolService = threadPoolService;
        disposable = settingsService.getSettingsFlux().subscribe(settings -> this.authenticate());
    }

    @PostConstruct
    private void init() {
        context.setCookieStore(new BasicCookieStore());
        authenticate();
    }

    @PreDestroy
    private void destroy() {
        sink.emitComplete(EmitHandler.RETRY);
        disposable.dispose();
    }

    public Flux<String> getLoggedInUser() {
        return sink.asFlux();
    }

    public void authenticate() {

        log.info("Authenticating using ViperGirls credentials");
        authenticated = false;

        if (!settingsService.getSettings().getVLogin()) {
            log.debug("Authentication option is disabled");
            context.getCookieStore().clear();
            loggedUser = "";
            sink.emitNext(loggedUser, EmitHandler.RETRY);
            return;
        }

        String username = settingsService.getSettings().getVUsername();
        String password = settingsService.getSettings().getVPassword();

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            log.error("Cannot authenticate with ViperGirls credentials, username or password is empty");
            context.getCookieStore().clear();
            loggedUser = "";
            sink.emitNext(loggedUser, EmitHandler.RETRY);
            return;
        }

        HttpPost postAuth = cm.buildHttpPost(settingsService.getSettings().getVProxy() + "/login.php?do=login", null);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("vb_login_username", username));

        params.add(new BasicNameValuePair("cookieuser", "1"));
        params.add(new BasicNameValuePair("do", "login"));
        params.add(new BasicNameValuePair("vb_login_md5password", password));
        try {
            postAuth.setEntity(new UrlEncodedFormEntity(params));
        } catch (Exception e) {
            context.getCookieStore().clear();
            loggedUser = "";
            sink.emitNext(loggedUser, EmitHandler.RETRY);
            log.error("Failed to authenticate user with " + settingsService.getSettings().getVProxy(), e);
            return;
        }

        postAuth.addHeader("Referer", settingsService.getSettings().getVProxy());
        postAuth.addHeader("Host", settingsService.getSettings().getVProxy().replace("https://", "").replace("http://", ""));

        CloseableHttpClient client = cm.getClient().build();

        try (CloseableHttpResponse response = client.execute(postAuth, context)) {

            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                throw new VripperException(String.format("Unexpected response code returned %s", response.getStatusLine().getStatusCode()));
            }
            String responseBody = EntityUtils.toString(response.getEntity());
            log.debug(String.format("Authentication with ViperGirls response body:%n%s", responseBody));
            EntityUtils.consumeQuietly(response.getEntity());
            if (context.getCookieStore().getCookies().stream().map(Cookie::getName).noneMatch(e -> e.equals("vg_userid"))) {
                log.error(String.format("Failed to authenticate user with %s, missing vg_userid cookie", settingsService.getSettings().getVProxy()));
                return;
            }
        } catch (Exception e) {
            context.getCookieStore().clear();
            loggedUser = "";
            sink.emitNext(loggedUser, EmitHandler.RETRY);
            log.error("Failed to authenticate user with " + settingsService.getSettings().getVProxy(), e);
            return;
        }
        authenticated = true;
        loggedUser = username;
        log.info(String.format("Authenticated: %s", username));
        sink.emitNext(loggedUser, EmitHandler.RETRY);
    }

    public void leaveThanks(Post post) {
        threadPoolService.getGeneralExecutor().submit(new LeaveThanksRunnable(post, authenticated, context));
    }
}
