package tn.mnlr.vripper.web.restendpoints;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.q.DownloadQ;
import tn.mnlr.vripper.services.AppStateService;
import tn.mnlr.vripper.services.PostParser;
import tn.mnlr.vripper.services.VipergirlsAuthService;

import java.util.List;

@RestController
@CrossOrigin(value = "*")
public class PostRestEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(PostRestEndpoint.class);

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private AppSettingsService appSettingsService;

    @Autowired
    private VipergirlsAuthService authService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception e) {
        logger.error("Error when process request", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }

    @Autowired
    private DownloadQ downloadQ;

    @Autowired
    private PostParser postParser;

    @PostMapping("/post")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity processPost(@RequestBody ThreadUrl url) throws Exception {
        logger.info(String.format("Starting to process thread: %s", url.url));
        if (url.url == null || url.url.isEmpty()) {
            return new ResponseEntity<>("Failed to process empty request", HttpStatus.BAD_REQUEST);
        } else if (!url.url.startsWith("https://vipergirls.to")) {
            return new ResponseEntity<>("ViperGirls only links are supported", HttpStatus.BAD_REQUEST);
        }
        List<Post> parsed = postParser.parse(url.url);
        logger.info(String.format("%d posts found from thread %s", parsed.size(), url.url));
        parsed.forEach(p -> {
            try {
                authService.leaveThanks(p.getUrl(), p.getPostId());
            } catch (Exception e) {
                logger.error(String.format("Failed to leave thanks for %s", p.getUrl()), e);
            }
        });
        if(appSettingsService.isAutoStart()) {
            logger.info("Auto start downloads option is enabled");
            logger.info(String.format("Starting to enqueue %d jobs for %s", parsed.stream().flatMap(e -> e.getImages().stream()).count(), url.url));
            for (Post post : parsed) {
                downloadQ.enqueue(post);
            }
            logger.info(String.format("Done enqueuing jobs for %s", url.url));
        } else {
            logger.info("Auto start downloads option is disabled");
        }
        logger.info(String.format("Done processing thread: %s", url.url));
        return ResponseEntity.ok(new ParseResult(parsed.size()));
    }

    @PostMapping("/clipboard/post")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity processPostFromClipboard(@RequestBody ThreadUrl url) throws Exception {
        return this.processPost(url);
    }

    @PostMapping("/post/restart")
    @ResponseStatus(value = HttpStatus.OK)
    public void restartPost(@RequestBody PostId postId) throws Exception {
        downloadQ.restart(postId.getPostId());
    }

    @PostMapping("/post/stop")
    @ResponseStatus(value = HttpStatus.OK)
    public void stop(@RequestBody PostId postId) throws Exception {
        downloadQ.stop(postId.getPostId());
    }

    @PostMapping("/post/remove")
    @ResponseStatus(value = HttpStatus.OK)
    public void remove(@RequestBody PostId postId) throws Exception {
        downloadQ.stop(postId.getPostId());
        Thread.sleep(1500);
        appStateService.remove(postId.getPostId());
    }

    @PostMapping("/post/remove/all")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity removeAll() {
        return ResponseEntity.ok(new RemoveAllResult(appStateService.removeAll()));
    }

    @Getter
    @ToString
    private static class ThreadUrl {
        private String url;
    }

    @Getter
    private static class PostId {
        private String postId;
    }

    @Getter
    private static class ParseResult {
        ParseResult(int parsed) {
            this.parsed = parsed;
        }
        private int parsed;
    }

    @Getter
    private static class RemoveAllResult {
        RemoveAllResult(int removed) {
            this.removed = removed;
        }
        private int removed;
    }
}
