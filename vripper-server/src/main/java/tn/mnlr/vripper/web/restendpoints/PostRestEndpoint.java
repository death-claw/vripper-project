package tn.mnlr.vripper.web.restendpoints;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.q.DownloadQ;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.AppStateService;
import tn.mnlr.vripper.services.PathService;
import tn.mnlr.vripper.services.PostParser;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@CrossOrigin(value = "*")
public class PostRestEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(PostRestEndpoint.class);


    private static final Pattern VG_URL_PATTERN = Pattern.compile("https:\\/\\/vipergirls\\.to\\/threads\\/(\\d+)((.*p=)(\\d+))?");

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private AppSettingsService appSettingsService;

    @Autowired
    private PathService pathService;

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

        String threadId, postId;
        try {
            Matcher m = VG_URL_PATTERN.matcher(url.url);
            if (m.find()) {
                threadId = m.group(1);
                postId = m.group(4);
            } else {
                throw new PostParseException(String.format("Cannot retrieve thread id from URL %s", url));
            }
        } catch (Exception e) {
            throw new PostParseException(String.format("Cannot retrieve thread id from URL %s", url), e);
        }
        return ResponseEntity.ok(new PairThreadIdPostId(threadId, postId));
    }

    @PostMapping("/post/restart")
    @ResponseStatus(value = HttpStatus.OK)
    public void restartPost(@RequestBody PostId postId) throws Exception {
        downloadQ.restart(postId.getPostId());
    }

    @PostMapping("/post/add")
    @ResponseStatus(value = HttpStatus.OK)
    public void addPost(@RequestBody List<PostToAdd> posts) {
        for (PostToAdd post : posts) {
            VripperApplication.commonExecutor.submit(() -> {
                try {
                    postParser.addPost(post.postId, post.threadId);
                } catch (PostParseException e) {
                    logger.error(String.format("Failed to add post %s", post.postId), e);
                }
            });
        }
    }

    @GetMapping("/post/path/{postId}")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<DownloadPath> folderPath(@PathVariable("postId") String postId) {
        File destinationFolder = pathService.getDownloadDestinationFolder(postId);
        return ResponseEntity.ok(new DownloadPath(destinationFolder.getPath()));
    }

    @PostMapping("/post/restart/all")
    @ResponseStatus(value = HttpStatus.OK)
    public void restartPost() throws Exception {
        downloadQ.restartAll();
    }

    @PostMapping("/post/stop")
    @ResponseStatus(value = HttpStatus.OK)
    public void stop(@RequestBody PostId postId) {
        downloadQ.stop(postId.getPostId());
    }

    @PostMapping("/post/stop/all")
    @ResponseStatus(value = HttpStatus.OK)
    public void stopAll() {
        downloadQ.stopAll();
    }

    @PostMapping("/post/remove")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity remove(@RequestBody PostId postId) {
        downloadQ.stop(postId.getPostId());
        appStateService.remove(postId.getPostId());
        return ResponseEntity.ok(new RemoveResult(postId.getPostId()));
    }

    @PostMapping("/post/clear/all")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity clearAll() {
        return ResponseEntity.ok(new RemoveAllResult(appStateService.clearAll()));
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
    private static class PairThreadIdPostId {
        private String threadId;
        private String postId;

        public PairThreadIdPostId(String threadId, String postId) {
            this.threadId = threadId;
            this.postId = postId;
        }
    }

    @Getter
    private static class PostToAdd {
        private String threadId;
        private String postId;
    }

    @Getter
    private static class ParseResult {

        private List<PostResult> posts;
        private int count;
        private String threadId;

        public ParseResult(List<PostResult> posts, int count, String threadId) {
            this.posts = posts;
            this.count = count;
            this.threadId = threadId;
        }

        @Getter
        public static class PostResult {

            private String title;
            private int counter;
            private String url;
            private String postId;
            private List<String> previews;

            public PostResult(String title, int counter, String url, String postId, List<String> previews) {
                this.title = title;
                this.counter = counter;
                this.url = url;
                this.postId = postId;
                this.previews = previews;
            }
        }
    }

    @Getter
    private static class RemoveAllResult {
        private List<String> postIds;
        private int removed;

        RemoveAllResult(List<String> postIds) {
            this.removed = postIds.size();
            this.postIds = postIds;
        }
    }

    @Getter
    private static class RemoveResult {
        private String postId;

        RemoveResult(String postId) {
            this.postId = postId;
        }
    }

    @Getter
    private static class DownloadPath {
        private String path;

        DownloadPath(String path) {
            this.path = path;
        }
    }
}
