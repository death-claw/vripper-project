package tn.mnlr.vripper.web.restendpoints;

import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.q.ExecutionService;
import tn.mnlr.vripper.services.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    @Autowired
    private VGHandler vgHandler;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        logger.error("Error when process request", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }

    @Autowired
    private ExecutionService executionService;


    @Autowired
    private PostParser postParser;

    @PostMapping("/post")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity processPost(@RequestBody ThreadUrl _url) throws Exception {
        if (_url.getUrl() == null || _url.getUrl().isEmpty()) {
            return new ResponseEntity("Failed to process empty request", HttpStatus.BAD_REQUEST);
        }
        List<String> urls = Arrays.stream(_url.getUrl().split("\\r?\\n")).map(String::trim).filter(e -> !e.isEmpty()).collect(Collectors.toList());
        ArrayList<QueuedVGLink> queuedVGLinks = new ArrayList<>();
        for (String url : urls) {
            logger.debug(String.format("Starting to process thread: %s", url));
            if (!url.startsWith("https://vipergirls.to")) {
                logger.error(String.format("Unsupported link %s", url));
                continue;
            }

            String threadId, postId;
            try {
                Matcher m = VG_URL_PATTERN.matcher(url);
                if (m.find()) {
                    threadId = m.group(1);
                    postId = m.group(4);
                } else {
                    throw new PostParseException(String.format("Cannot retrieve thread id from URL %s", url));
                }
            } catch (Exception e) {
                throw new PostParseException(String.format("Cannot retrieve thread id from URL %s", url), e);
            }
            queuedVGLinks.add(new QueuedVGLink(url, threadId, postId));
        }
        vgHandler.handle(queuedVGLinks);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/post/restart")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized void restartPost(@RequestBody @NonNull List<PostId> postIds) throws Exception {
        for (PostId postId : postIds) {
            executionService.restart(postId.getPostId());
        }
    }

    @PostMapping("/post/add")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized void addPost(@RequestBody List<PostToAdd> posts) {
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
    public synchronized void restartPost() throws Exception {
        executionService.restartAll();
    }

    @PostMapping("/post/stop")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized void stop(@RequestBody @NonNull List<PostId> postIds) {
        for (PostId postId : postIds) {
            executionService.stop(postId.getPostId());
        }
    }

    @PostMapping("/post/stop/all")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized void stopAll() {
        executionService.stopAll();
    }

    @PostMapping("/post/remove")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity<List<RemoveResult>> remove(@RequestBody @NonNull List<PostId> postIds) {
        List<RemoveResult> result = new ArrayList<>();
        for (PostId postId : postIds) {
            executionService.stop(postId.getPostId());
            appStateService.remove(postId.getPostId());
            result.add(new RemoveResult(postId.getPostId()));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/post/clear/all")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity<RemoveAllResult> clearAll() {
        return ResponseEntity.ok(new RemoveAllResult(appStateService.clearAll()));
    }

    @PostMapping("/post/remove/all")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity<RemoveAllResult> removeAll() {
        return ResponseEntity.ok(new RemoveAllResult(appStateService.removeAll()));
    }

    @GetMapping("/grab/{threadId}")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity<List<VRPostState>> grab(@PathVariable("threadId") @NonNull ThreadId threadId) throws Exception {
        return ResponseEntity.ok(vgHandler.getCache().get(threadId.getThreadId()));
    }

    @PostMapping("/grab/remove")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized void grabRemove(@RequestBody @NonNull ThreadUrl threadUrl) {
        vgHandler.remove(threadUrl.getUrl());
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
class ThreadId {
    private String threadId;

    public ThreadId(String threadId) {
        this.threadId = threadId;
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

@Getter
@Setter
@NoArgsConstructor
@ToString
class PairThreadIdPostId {
    private String threadId;
    private String postId;

    public PairThreadIdPostId(String threadId, String postId) {
        this.threadId = threadId;
        this.postId = postId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PairThreadIdPostId that = (PairThreadIdPostId) o;
        return Objects.equals(threadId, that.threadId) &&
                Objects.equals(postId, that.postId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threadId, postId);
    }
}