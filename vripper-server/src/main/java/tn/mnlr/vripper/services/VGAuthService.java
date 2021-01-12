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
    private final DataService dataService;

    private final Disposable disposable;

    @Getter
    private final HttpClientContext context = HttpClientContext.create();
    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
    @Getter
    private boolean authenticated = false;
    @Getter
    private String loggedUser = "";

    @Autowired
    public VGAuthService(ConnectionService cm, SettingsService settingsService, ThreadPoolService threadPoolService, DataService dataService) {
        this.cm = cm;
        this.settingsService = settingsService;
        this.threadPoolService = threadPoolService;
        this.dataService = dataService;
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

        HttpPost postAuth = cm.buildHttpPost("https://vipergirls.to/login.php?do=login", null);
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
            log.error("Failed to authenticate user with vipergirls.to", e);
            return;
        }

        postAuth.addHeader("Referer", "https://vipergirls.to/");
        postAuth.addHeader("Host", "vipergirls.to");

        CloseableHttpClient client = cm.getClient().build();

        try (CloseableHttpResponse response = client.execute(postAuth, context)) {

            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                throw new VripperException(String.format("Unexpected response code returned %s", response.getStatusLine().getStatusCode()));
            }
            String responseBody = EntityUtils.toString(response.getEntity());
            log.debug(String.format("Authentication with ViperGirls response body:%n%s", responseBody));
            EntityUtils.consumeQuietly(response.getEntity());
            if (context.getCookieStore().getCookies().stream().map(Cookie::getName).noneMatch(e -> e.equals("vg_userid"))) {
                log.error("Failed to authenticate user with vipergirls.to, missing vg_userid cookie");
                return;
            }
        } catch (Exception e) {
            context.getCookieStore().clear();
            loggedUser = "";
            sink.emitNext(loggedUser, EmitHandler.RETRY);
            log.error("Failed to authenticate user with vipergirls.to", e);
            return;
        }
        authenticated = true;
        loggedUser = username;
        log.info(String.format("Authenticated: %s", username));
        sink.emitNext(loggedUser, EmitHandler.RETRY);
    }

    public void leaveThanks(Post post) {
        if (!settingsService.getSettings().getVLogin()) {
            log.debug("Authentication with ViperGirls option is disabled");
            return;
        }
        if (!settingsService.getSettings().getVThanks()) {
            log.debug("Leave thanks option is disabled");
            return;
        }
        if (!authenticated) {
            log.error("You are not authenticated");
            return;
        }
        if (post.isThanked()) {
            log.debug("Already left a thanks");
            return;
        }
        threadPoolService.getGeneralExecutor().submit(() -> {
            try {
                postThanks(post);
            } catch (Exception e) {
                log.error(String.format("Failed to leave a thanks for url %s, post id %s", post.getUrl(), post.getPostId()), e);
            }
        });
    }

    private void postThanks(Post post) throws VripperException {

        HttpPost postThanks = cm.buildHttpPost("https://vipergirls.to/post_thanks.php", null);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("do", "post_thanks_add"));
        params.add(new BasicNameValuePair("using_ajax", "1"));
        params.add(new BasicNameValuePair("p", post.getPostId()));
        params.add(new BasicNameValuePair("securitytoken", post.getSecurityToken()));
        try {
            postThanks.setEntity(new UrlEncodedFormEntity(params));
        } catch (Exception e) {
            throw new VripperException(e);
        }

        postThanks.addHeader("Referer", "https://vipergirls.to/");
        postThanks.addHeader("Host", "vipergirls.to");

        CloseableHttpClient client = cm.getClient().build();

        try (CloseableHttpResponse response = client.execute(postThanks, context)) {
            if (response.getStatusLine().getStatusCode() / 100 == 2) {
                post.setThanked(true);
                dataService.updatePostThanked(post.isThanked(), post.getId());
            }
            EntityUtils.consumeQuietly(response.getEntity());
        } catch (Exception e) {
            throw new VripperException(e);
        }

        if (!post.isThanked()) {
            throw new VripperException("Failed to leave");
        }
    }
}
