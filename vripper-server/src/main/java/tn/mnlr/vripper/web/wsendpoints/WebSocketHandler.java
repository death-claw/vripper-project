package tn.mnlr.vripper.web.wsendpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.entities.mixin.ui.ImageUIMixin;
import tn.mnlr.vripper.entities.mixin.ui.PostUIMixin;
import tn.mnlr.vripper.services.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    public WebSocketHandler() {
        om.addMixIn(Image.class, ImageUIMixin.class).addMixIn(Post.class, PostUIMixin.class);
    }

    @Autowired
    private GlobalStateService globalStateService;

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private DownloadSpeedService downloadSpeedService;

    @Autowired
    private VipergirlsAuthService vipergirlsAuthService;

    @Autowired
    private PostParser postParser;

    private Map<String, Disposable> postsSubscriptions = new ConcurrentHashMap<>();
    private Map<String, Disposable> postDetailsSubscriptions = new ConcurrentHashMap<>();
    private Map<String, Disposable> vrPostParserSubscriptions = new ConcurrentHashMap<>();
    private Map<String, Disposable> stateSubscriptions = new ConcurrentHashMap<>();
    private Map<String, Disposable> downloadSpeedSubscriptions = new ConcurrentHashMap<>();
    private Map<String, Disposable> userSubscriptions = new ConcurrentHashMap<>();

    private Map<String, Future<Void>> threadParseRequests = new ConcurrentHashMap<>();

    private ObjectMapper om = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        WSMessage wsMessage = om.readValue(message.getPayload(), WSMessage.class);
        WSMessage.CMD cmd = WSMessage.CMD.valueOf(wsMessage.getCmd());
        switch (cmd) {
            case GLOBAL_STATE_SUB:
                subscribeForGlobalState(session);
                break;
            case SPEED_SUB:
                subscribeForSpeed(session);
                break;
            case USER_SUB:
                subscribeForUser(session);
                break;
            case POSTS_SUB:
                subscribeForPosts(session);
                break;
            case THREAD_PARSING_SUB:
                subscribeForThreadParsing(session, wsMessage.getPayload());
                break;
            case POST_DETAILS_SUB:
                subscribeForPostDetails(session, wsMessage.getPayload());
                break;
            case POST_DETAILS_UNSUB:
                logger.info(String.format("Client %s unsubscribed from post details", session.getId()));
                Optional.ofNullable(postDetailsSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
                break;
            case THREAD_PARSING_UNSUB:
                logger.info(String.format("Client %s unsubscribed from thread parsing", session.getId()));
                Optional.ofNullable(vrPostParserSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
                Optional.ofNullable(threadParseRequests.remove(session.getId())).ifPresent(d -> d.cancel(true));
                break;
            case POSTS_UNSUB:
                logger.info(String.format("Client %s unsubscribed from posts", session.getId()));
                Optional.ofNullable(postsSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
                break;
            case GLOBAL_STATE_UNSUB:
                logger.info(String.format("Client %s unsubscribed from global state", session.getId()));
                Optional.ofNullable(stateSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
                break;
            case SPEED_UNSUB:
                logger.info(String.format("Client %s unsubscribed from download speed info", session.getId()));
                Optional.ofNullable(downloadSpeedSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
                break;
            case USER_UNSUB:
                logger.info(String.format("Client %s unsubscribed from user info", session.getId()));
                Optional.ofNullable(userSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
                break;
        }
    }

    private void subscribeForGlobalState(WebSocketSession session) {

        logger.info(String.format("Client %s subscribed for global state", session.getId()));
        if (stateSubscriptions.containsKey(session.getId())) {
            stateSubscriptions.get(session.getId()).dispose();
        }

        try {
            send(session, new TextMessage(om.writeValueAsString(Arrays.asList(globalStateService.getCurrentState()))));
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }

        stateSubscriptions.put(session.getId(),
                globalStateService.getLiveGlobalState()
                        .onBackpressureBuffer()
                        .observeOn(Schedulers.io())
                        .buffer(5000, TimeUnit.MILLISECONDS)
                        .filter(e -> !e.isEmpty())
                        .map(e -> e.subList(e.size() - 1, e.size()))
                        .map(om::writeValueAsString)
                        .map(TextMessage::new)
                        .subscribe(msg -> send(session, msg), e -> logger.error("Failed to send data to client", e))
        );
    }

    private void subscribeForSpeed(WebSocketSession session) {

        logger.info(String.format("Client %s subscribed for download speed info", session.getId()));
        if (downloadSpeedSubscriptions.containsKey(session.getId())) {
            downloadSpeedSubscriptions.get(session.getId()).dispose();
        }

        try {
            send(session, new TextMessage(om.writeValueAsString(Arrays.asList(new DownloadSpeed(0)))));
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }

        downloadSpeedSubscriptions.put(session.getId(),
                downloadSpeedService.getReadBytesPerSecond()
                        .onBackpressureBuffer()
                        .observeOn(Schedulers.io())
                        .buffer(5000, TimeUnit.MILLISECONDS)
                        .filter(e -> !e.isEmpty())
                        .map(e -> e.subList(e.size() - 1, e.size()))
                        .map(e -> e.stream().map(DownloadSpeed::new).collect(Collectors.toList()))
                        .map(om::writeValueAsString)
                        .map(TextMessage::new)
                        .subscribe(msg -> send(session, msg), e -> logger.error("Failed to send data to client", e))
        );
    }

    private void subscribeForUser(WebSocketSession session) {

        logger.info(String.format("Client %s subscribed for user info", session.getId()));
        if (userSubscriptions.containsKey(session.getId())) {
            userSubscriptions.get(session.getId()).dispose();
        }

        try {
            send(session, new TextMessage(om.writeValueAsString(Arrays.asList(new LoggedUser(vipergirlsAuthService.getLoggedUser())))));
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }

        userSubscriptions.put(session.getId(),
                vipergirlsAuthService.getLoggedInUser()
                        .onBackpressureBuffer()
                        .observeOn(Schedulers.io())
                        .map(e -> Arrays.asList(new LoggedUser(e)))
                        .map(om::writeValueAsString)
                        .map(TextMessage::new)
                        .subscribe(msg -> send(session, msg), e -> logger.error("Failed to send data to client", e))
        );
    }

    private void subscribeForPosts(WebSocketSession session) {

        logger.info(String.format("Client %s subscribed for posts", session.getId()));
        if (postsSubscriptions.containsKey(session.getId())) {
            postsSubscriptions.get(session.getId()).dispose();
        }

        try {
            send(session, new TextMessage(om.writeValueAsString(appStateService.getCurrentPosts().values())));
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }

        postsSubscriptions.put(session.getId(),
                appStateService.getLivePostsState()
                        .onBackpressureBuffer()
                        .observeOn(Schedulers.io())
                        .buffer(2000, TimeUnit.MILLISECONDS, 200)
                        .filter(e -> !e.isEmpty())
                        .map(e -> e.stream().distinct().collect(Collectors.toList()))
                        .map(om::writeValueAsString)
                        .map(TextMessage::new)
                        .subscribe(msg -> send(session, msg), e -> logger.error("Failed to send data to client", e))
        );
    }

    private void subscribeForThreadParsing(WebSocketSession session, String threadId) {

        logger.info(String.format("Client %s subscribed for thread parsing with threadId = %s", session.getId(), threadId));
        if (vrPostParserSubscriptions.containsKey(session.getId())) {
            vrPostParserSubscriptions.get(session.getId()).dispose();
        }

        if (threadParseRequests.containsKey(session.getId())) {
            threadParseRequests.get(session.getId()).cancel(true);
        }

        PostParser.VRThreadParser vrThreadParser = postParser.new VRThreadParser(threadId);

        vrPostParserSubscriptions.put(session.getId(), vrThreadParser.getPostPublishProcessor()
                .onBackpressureBuffer()
                .observeOn(Schedulers.io())
                .buffer(200)
                .filter(e -> !e.isEmpty())
                .map(om::writeValueAsString)
                .map(TextMessage::new)
                .subscribe(msg -> send(session, msg), e -> logger.error("Failed to send data to client", e))
        );

        threadParseRequests.put(session.getId(), VripperApplication.commonExecutor.submit(vrThreadParser::parse));
    }

    private void subscribeForPostDetails(WebSocketSession session, String postId) {

        logger.info(String.format("Client %s subscribed for post details with id = %s", session.getId(), postId));
        if (postDetailsSubscriptions.containsKey(session.getId())) {
            postDetailsSubscriptions.get(session.getId()).dispose();
        }

        try {
            send(session, new TextMessage(om.writeValueAsString(
                    appStateService.getCurrentImages()
                            .values()
                            .stream()
                            .filter(e -> e.getPostId().equals(postId))
                            .collect(Collectors.toList())))
            );
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }

        postDetailsSubscriptions.put(session.getId(), appStateService.getLiveImageUpdates()
                .onBackpressureBuffer()
                .observeOn(Schedulers.io())
                .filter(e -> e.getPostId().equals(postId))
                .buffer(2000, TimeUnit.MILLISECONDS, 500)
                .filter(e -> !e.isEmpty())
                .map(e -> e.stream().distinct().collect(Collectors.toList()))
                .map(om::writeValueAsString)
                .map(TextMessage::new)
                .subscribe(msg -> send(session, msg), e -> logger.error("Failed to send data to client", e))
        );
    }

    private synchronized void send(WebSocketSession session, TextMessage message) throws IOException {
        session.sendMessage(message);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info(String.format("Connection open for client id: %s", session.getId()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        Optional.ofNullable(postsSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
        Optional.ofNullable(postDetailsSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
        Optional.ofNullable(stateSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
        Optional.ofNullable(downloadSpeedSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
        Optional.ofNullable(userSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
        Optional.ofNullable(vrPostParserSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
        Optional.ofNullable(threadParseRequests.remove(session.getId())).ifPresent(d -> d.cancel(true));

        logger.info(String.format("Connection closed for client id: %s", session.getId()));
    }

    @Getter
    private static class WSMessage {

        private String cmd;
        private String payload;

        enum CMD {
            POSTS_SUB,
            POST_DETAILS_SUB,
            POSTS_UNSUB,
            POST_DETAILS_UNSUB,
            GLOBAL_STATE_SUB,
            GLOBAL_STATE_UNSUB,
            SPEED_SUB,
            SPEED_UNSUB,
            THREAD_PARSING_SUB,
            THREAD_PARSING_UNSUB,
            USER_SUB,
            USER_UNSUB
        }
    }

    @Getter
    private static class LoggedUser {

        private final String type = "user";
        private String user;

        public LoggedUser(String user) {
            this.user = user;
        }
    }
}
