package tn.mnlr.vripper.services;

import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.exception.VripperException;
import tn.mnlr.vripper.jpa.domain.Post;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class VipergirlsAuthService {

    private static final Logger logger = LoggerFactory.getLogger(VipergirlsAuthService.class);

    private final ConnectionManager cm;
    private final AppSettingsService appSettingsService;
    private final CommonExecutor commonExecutor;
    private final PostDataService postDataService;

    @Getter
    private final HttpClientContext context = HttpClientContext.create();

    @Getter
    private boolean authenticated = false;

    @Getter
    private String loggedUser = "";

    @Getter
    private final PublishProcessor<String> loggedInUser = PublishProcessor.create();

    @Autowired
    public VipergirlsAuthService(ConnectionManager cm, AppSettingsService appSettingsService, CommonExecutor commonExecutor, PostDataService postDataService) {
        this.cm = cm;
        this.appSettingsService = appSettingsService;
        this.commonExecutor = commonExecutor;
        this.postDataService = postDataService;
    }

    @PostConstruct
    private void init() {
        context.setCookieStore(new BasicCookieStore());
        try {
            authenticate();
        } catch (VripperException e) {
            logger.error("Cannot authenticate user with ViperGirls", e);
        }
    }

    public void authenticate() throws VripperException {

        logger.info("Authenticating using ViperGirls credentials");
        authenticated = false;

        if (!appSettingsService.getSettings().getVLogin()) {
            logger.debug("Authentication option is disabled");
            context.getCookieStore().clear();
            loggedUser = "";
            loggedInUser.onNext(loggedUser);
            return;
        }

        String username = appSettingsService.getSettings().getVUsername();
        String password = appSettingsService.getSettings().getVPassword();

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            logger.error("Cannot authenticate with ViperGirls credentials, username or password is empty");
            context.getCookieStore().clear();
            loggedUser = "";
            loggedInUser.onNext(loggedUser);
            return;
        }

        HttpPost postAuth = cm.buildHttpPost("https://vipergirls.to/login.php?do=login");
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
            loggedInUser.onNext(loggedUser);
            throw new VripperException(e);
        }

        postAuth.addHeader("Referer", "https://vipergirls.to/");
        postAuth.addHeader("Host", "vipergirls.to");

        CloseableHttpClient client = cm.getClient().build();

        try (CloseableHttpResponse response = client.execute(postAuth, context)) {

            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                throw new VripperException(String.format("Unexpected response code returned %s", response.getStatusLine().getStatusCode()));
            }
            String responseBody = EntityUtils.toString(response.getEntity());
            logger.debug(String.format("Authentication with ViperGirls response body:%n%s", responseBody));
            EntityUtils.consumeQuietly(response.getEntity());
            if (context.getCookieStore().getCookies().stream().map(Cookie::getName).noneMatch(e -> e.equals("vg_userid"))) {
                throw new VripperException("Failed to authenticate user with ViperRipper");
            }
        } catch (Exception e) {
            context.getCookieStore().clear();
            loggedUser = "";
            loggedInUser.onNext(loggedUser);
            if (e instanceof VripperException) {
                throw (VripperException) e;
            } else {
                throw new VripperException(e);
            }
        }
        authenticated = true;
        loggedUser = username;
        logger.info(String.format("Authenticated: %s", username));
        loggedInUser.onNext(loggedUser);
    }

    public void leaveThanks(Post post) {
        if (!appSettingsService.getSettings().getVLogin()) {
            logger.debug("Authentication with ViperGirls option is disabled");
            return;
        }
        if (!appSettingsService.getSettings().getVThanks()) {
            logger.debug("Leave thanks option is disabled");
            return;
        }
        if (!authenticated) {
            logger.error("You are not authenticated");
            return;
        }
        if (post.isThanked()) {
            logger.debug("Already left a thanks");
            return;
        }
        commonExecutor.getGeneralExecutor().submit(() -> {
            try {
                postThanks(post);
            } catch (Exception e) {
                logger.error(String.format("Failed to leave a thanks for url %s, post id %s", post.getUrl(), post.getPostId()), e);
            }
        });
    }

    private void postThanks(Post post) throws VripperException {

        HttpPost postThanks = cm.buildHttpPost("https://vipergirls.to/post_thanks.php");
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
                postDataService.updatePostThanked(post.isThanked(), post.getId());
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
