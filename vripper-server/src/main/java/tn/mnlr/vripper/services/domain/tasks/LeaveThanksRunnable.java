package tn.mnlr.vripper.services.domain.tasks;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.jpa.domain.Event;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.repositories.IEventRepository;
import tn.mnlr.vripper.services.ConnectionService;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.SettingsService;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LeaveThanksRunnable implements Runnable {

    private final ConnectionService cm;
    private final DataService dataService;
    private final HttpClientContext context;
    private final SettingsService settingsService;
    private final IEventRepository eventRepository;
    private final boolean authenticated;
    private final Post post;
    private final Event event;

    public LeaveThanksRunnable(Post post, boolean authenticated, HttpClientContext context) {
        this.post = post;
        this.authenticated = authenticated;
        this.context = context;
        cm = SpringContext.getBean(ConnectionService.class);
        dataService = SpringContext.getBean(DataService.class);
        eventRepository = SpringContext.getBean(IEventRepository.class);
        settingsService = SpringContext.getBean(SettingsService.class);
        event = new Event(Event.Type.THANKS, Event.Status.PENDING, LocalDateTime.now(), String.format("Leaving thanks for %s", post.getUrl()));
        eventRepository.save(event);
    }

    @Override
    public void run() {
        try {
            event.setStatus(Event.Status.PROCESSING);
            eventRepository.update(event);
            if (!settingsService.getSettings().getVLogin()) {
                event.setMessage(String.format("Will not leave a thanks for %s\nAuthentication with ViperGirls option is disabled", post.getUrl()));
                event.setStatus(Event.Status.DONE);
                eventRepository.update(event);
                return;
            }
            if (!settingsService.getSettings().getVThanks()) {
                event.setMessage(String.format("Will not leave a thanks for %s\nLeave thanks option is disabled", post.getUrl()));
                event.setStatus(Event.Status.DONE);
                eventRepository.update(event);
                return;
            }
            if (!authenticated) {
                event.setMessage(String.format("Will not leave a thanks for %s\nYou are not authenticated", post.getUrl()));
                event.setStatus(Event.Status.ERROR);
                eventRepository.update(event);
                return;
            }
            if (post.isThanked()) {
                event.setMessage(String.format("Will not leave a thanks for %s\nAlready left a thanks", post.getUrl()));
                event.setStatus(Event.Status.DONE);
                eventRepository.update(event);
                return;
            }

            HttpPost postThanks = cm.buildHttpPost(settingsService.getSettings().getVProxy() + "/post_thanks.php", null);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("do", "post_thanks_add"));
            params.add(new BasicNameValuePair("using_ajax", "1"));
            params.add(new BasicNameValuePair("p", post.getPostId()));
            params.add(new BasicNameValuePair("securitytoken", post.getSecurityToken()));
            try {
                postThanks.setEntity(new UrlEncodedFormEntity(params));
            } catch (UnsupportedEncodingException e) {
                String error = String.format("Request error for %s", post.getUrl());
                log.error(error, e);
                event.setMessage(error + "\n" + Utils.throwableToString(e));
                event.setStatus(Event.Status.ERROR);
                eventRepository.update(event);
                return;
            }

            postThanks.addHeader("Referer", settingsService.getSettings().getVProxy());
            postThanks.addHeader("Host", settingsService.getSettings().getVProxy().replace("https://", "").replace("http://", ""));

            CloseableHttpClient client = cm.getClient().build();

            try (CloseableHttpResponse response = client.execute(postThanks, context)) {
                if (response.getStatusLine().getStatusCode() / 100 == 2) {
                    post.setThanked(true);
                    dataService.updatePostThanked(post.isThanked(), post.getId());
                }
                EntityUtils.consumeQuietly(response.getEntity());
            }
            event.setStatus(Event.Status.DONE);
            eventRepository.update(event);
        } catch (Exception e) {
            String error = String.format("Failed to leave a thanks for %s", post.getUrl());
            log.error(error, e);
            event.setMessage(error + "\n" + Utils.throwableToString(e));
            event.setStatus(Event.Status.ERROR);
            eventRepository.update(event);
        }
    }
}
