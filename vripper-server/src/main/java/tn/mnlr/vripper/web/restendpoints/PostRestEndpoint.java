package tn.mnlr.vripper.web.restendpoints;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@CrossOrigin(value = "*")
public class PostRestEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(PostRestEndpoint.class);

    private static final Pattern VG_URL_PATTERN = Pattern.compile("https://vipergirls\\.to/threads/(\\d+)((.*p=)(\\d+))?");

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
        logger.debug(String.format("Starting to process thread: %s", url.getUrl()));
        if (url.getUrl() == null || url.getUrl().isEmpty()) {
            return new ResponseEntity<>("Failed to process empty request", HttpStatus.BAD_REQUEST);
        } else if (!url.getUrl().startsWith("https://vipergirls.to")) {
            return new ResponseEntity<>("ViperGirls links only are supported", HttpStatus.BAD_REQUEST);
        }

        String threadId, postId;
        try {
            Matcher m = VG_URL_PATTERN.matcher(url.getUrl());
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
    public synchronized void restartPost(@RequestBody @NonNull List<PostId> postIds) throws Exception {
        for (PostId postId : postIds) {
            downloadQ.restart(postId.getPostId());
        }
    }

    @PostMapping("/post/add")
    @ResponseStatus(value = HttpStatus.OK)
    public void addPost(@RequestBody List<PostToAdd> posts) {
        for (PostToAdd post : posts) {
            VripperApplication.commonExecutor.submit(() -> {
                try {
                    postParser.addPost(post.getPostId(), post.getThreadId());
                } catch (PostParseException e) {
                    logger.error(String.format("Failed to add post %s", post.getPostId()), e);
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
    public synchronized void stop(@RequestBody @NonNull List<PostId> postIds) {
        for (PostId postId : postIds) {
            downloadQ.stop(postId.getPostId());
        }
    }

    @PostMapping("/post/stop/all")
    @ResponseStatus(value = HttpStatus.OK)
    public void stopAll() {
        downloadQ.stopAll();
    }

    @PostMapping("/post/remove")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity remove(@RequestBody @NonNull List<PostId> postIds) {
        List<RemoveResult> result = new ArrayList<>();
        for (PostId postId : postIds) {
            downloadQ.stop(postId.getPostId());
            appStateService.remove(postId.getPostId());
            result.add(new RemoveResult(postId.getPostId()));
        }
        return ResponseEntity.ok(result);
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
}

@Getter
@Setter
@NoArgsConstructor
class ThreadUrl {
    private String url;

    public ThreadUrl(String url) {
        this.url = url;
    }
}

@Getter
@Setter
@NoArgsConstructor
class PostId {
    private String postId;

    public PostId(String postId) {
        this.postId = postId;
    }
}

@Getter
@Setter
@NoArgsConstructor
class PairThreadIdPostId {
    private String threadId;
    private String postId;

    PairThreadIdPostId(String threadId, String postId) {
        this.threadId = threadId;
        this.postId = postId;
    }
}

@Getter
@Setter
@NoArgsConstructor
class PostToAdd {
    private String threadId;
    private String postId;

    public PostToAdd(String threadId, String postId) {
        this.threadId = threadId;
        this.postId = postId;
    }
}

@Getter
@Setter
@NoArgsConstructor
class RemoveAllResult {
    private List<String> postIds;
    private int removed;

    RemoveAllResult(List<String> postIds) {
        this.removed = postIds.size();
        this.postIds = postIds;
    }
}

@Getter
@Setter
@NoArgsConstructor
class RemoveResult {
    private String postId;

    RemoveResult(String postId) {
        this.postId = postId;
    }
}

@Getter
@Setter
@NoArgsConstructor
class DownloadPath {
    private String path;

    DownloadPath(String path) {
        this.path = path;
    }
}