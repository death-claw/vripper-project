package tn.mnlr.vripper.services;

import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.exception.VripperException;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VipergirlsAuthService {

    private static final Logger logger = LoggerFactory.getLogger(VipergirlsAuthService.class);

    @Autowired
    private ConnectionManager cm;

    @Autowired
    private HtmlProcessorService htmlProcessorService;

    @Autowired
    private XpathService xpathService;

    @Autowired
    private AppSettingsService appSettingsService;

    @Getter
    private String cookies;

    private boolean authenticated = false;

    @Getter
    private String loggedUser;

    @Getter
    private PublishProcessor<String> loggedInUser = PublishProcessor.create();

    @PreDestroy
    private void destroy() throws InterruptedException {
        logger.info("Shutting down VipergirlsAuthService");
        VripperApplication.commonExecutor.shutdown();
        VripperApplication.commonExecutor.awaitTermination(10, TimeUnit.SECONDS);
    }

    public void authenticate() throws VripperException {

        logger.info("Authenticating using ViperGirls credentials");
        authenticated = false;

        if(!appSettingsService.isVLogin()) {
            logger.warn("Authentication option is disabled");
            cookies = null;
            loggedUser = "";
            loggedInUser.onNext(loggedUser);
            return;
        }

        String username = appSettingsService.getVUsername();
        String password = appSettingsService.getVPassword();

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            logger.error("Cannot authenticate with ViperGirls credentials, username or password is empty");
            cookies = null;
            loggedUser = "";
            loggedInUser.onNext(loggedUser);
            return;
        }

        HttpPost postAuth = cm.buildHttpPost("https://vipergirls.to/login.php?do=login");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("vb_login_username", username));
        params.add(new BasicNameValuePair("vb_login_password", ""));
        params.add(new BasicNameValuePair("vb_login_password_hint", "Password"));

        params.add(new BasicNameValuePair("cookieuser", "1"));
        params.add(new BasicNameValuePair("securitytoken", "guest"));
        params.add(new BasicNameValuePair("do", "login"));
        params.add(new BasicNameValuePair("vb_login_md5password", password));
        params.add(new BasicNameValuePair("vb_login_md5password_utf", password));
        try {
            postAuth.setEntity(new UrlEncodedFormEntity(params));
        } catch (Exception e) {
            cookies = null;
            loggedUser = "";
            loggedInUser.onNext(loggedUser);
            throw new VripperException(e);
        }

        postAuth.addHeader("Referer", "https://vipergirls.to/");
        postAuth.addHeader("Host", "vipergirls.to");

        CloseableHttpClient client = cm.getClient().build();

        try (CloseableHttpResponse response = client.execute(postAuth)) {

            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                throw new VripperException(String.format("Unexpected response code returned %s", response.getStatusLine().getStatusCode()));
            }
            StringBuilder sb = new StringBuilder();
            Arrays.asList(response.getHeaders("set-cookie"))
                    .stream()
                    .map(e -> e.getValue())
                    .map(e -> e.split(";")[0])
                    .filter(e -> e.startsWith("vg_lastactivity") || e.startsWith("vg_userid") || e.startsWith("vg_password"))
                    .forEach(e -> sb.append(e).append(";"));
            String cookies = sb.toString();
            String responseBody = EntityUtils.toString(response.getEntity());
            logger.debug(String.format("Authentication with ViperGirls response body:%n%s", responseBody));
            EntityUtils.consumeQuietly(response.getEntity());
            if(!cookies.contains("vg_userid")) {
                throw new VripperException("Failed to authenticate user with ViperRipper");
            }
            this.cookies = cookies;
        } catch (Exception e) {
            cookies = null;
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

    public void leaveThanks(String postUrl, String postId) {
        VripperApplication.commonExecutor.submit(() -> {
            if (!appSettingsService.isVLogin()) {
                logger.warn("Authentication with ViperGirls option is disabled");
                return;
            }
            if (!appSettingsService.isVThanks()) {
                logger.warn("Leave thanks option is disabled");
                return;
            }
            if (!authenticated) {
                logger.error("You are not authenticated");
                return;
            }
            try {
                postThanks(postId, getSecurityToken(postUrl));
            } catch (Exception e) {
                logger.error(String.format("Failed to leave a thanks for url %s, post id %s", postUrl, postId), e);
            }
        });
    }

    private String getSecurityToken(String url) throws VripperException {

        String securityToken = "";

        HttpGet httpGet = cm.buildHttpGet(url);
        httpGet.addHeader("Referer", "https://vipergirls.to/");
        httpGet.addHeader("Host", "vipergirls.to");
        httpGet.addHeader("Cookie", cookies);

        CloseableHttpClient client = cm.getClient().build();

        try (CloseableHttpResponse response = client.execute(httpGet)) {

            String postPage = EntityUtils.toString(response.getEntity());
            Document document = htmlProcessorService.clean(postPage);

            String thanksUrl = xpathService
                    .getAsNode(document, "//li[contains(@id,'post_')][not(contains(@id,'post_thank'))]//a[@class='post_thanks_button']")
                    .getAttributes()
                    .getNamedItem("href")
                    .getTextContent()
                    .trim();

            securityToken = Arrays.asList(thanksUrl.split("&amp;")).stream()
                    .filter(v -> v.startsWith("securitytoken"))
                    .findAny()
                    .orElse("")
                    .replace("securitytoken=", "");

            EntityUtils.consumeQuietly(response.getEntity());
        } catch (Exception e) {
            throw new VripperException(e);
        }
        return securityToken;
    }

    private void postThanks(String postId, String securityKey) throws VripperException {

        HttpPost postThanks = cm.buildHttpPost("https://vipergirls.to/post_thanks.php");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("do", "post_thanks_add"));
        params.add(new BasicNameValuePair("using_ajax", "1"));
        params.add(new BasicNameValuePair("p", postId));
        params.add(new BasicNameValuePair("securitytoken", securityKey));
        try {
            postThanks.setEntity(new UrlEncodedFormEntity(params));
        } catch (Exception e) {
            throw new VripperException(e);
        }

        postThanks.addHeader("Referer", "https://vipergirls.to/");
        postThanks.addHeader("Host", "vipergirls.to");
        postThanks.addHeader("Cookie", cookies);

        CloseableHttpClient client = cm.getClient().build();

        try (CloseableHttpResponse response = client.execute(postThanks)) {
            EntityUtils.consumeQuietly(response.getEntity());
        } catch (Exception e) {
            throw new VripperException(e);
        }
    }
}
