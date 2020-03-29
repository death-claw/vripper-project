package tn.mnlr.vripper.web.restendpoints;

import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.entities.Post;
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

    private final AppStateService appStateService;
    private final AppStateExchange appStateExchange;
    private final PathService pathService;
    private final VGHandler vgHandler;
    private final ExecutionService executionService;
    private final PostParser postParser;
    private final CommonExecutor commonExecutor;

    @Autowired
    public PostRestEndpoint(AppStateService appStateService, AppStateExchange appStateExchange, PathService pathService, VGHandler vgHandler, ExecutionService executionService, PostParser postParser, CommonExecutor commonExecutor) {
        this.appStateService = appStateService;
        this.appStateExchange = appStateExchange;
        this.pathService = pathService;
        this.vgHandler = vgHandler;
        this.executionService = executionService;
        this.postParser = postParser;
        this.commonExecutor = commonExecutor;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        logger.error("Error when process request", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }

    @PostMapping("/post")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity processPost(@RequestBody ThreadUrl _url) throws Exception {
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
    public void restartPost(@RequestBody @NonNull List<PostId> postIds) throws Exception {
        executionService.restartAll(postIds.stream().map(PostId::getPostId).collect(Collectors.toList()));
    }

    @PostMapping("/post/add")
    @ResponseStatus(value = HttpStatus.OK)
    public void addPost(@RequestBody List<PostToAdd> posts) {
        for (PostToAdd post : posts) {
            commonExecutor.getGeneralExecutor().submit(() -> {
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
        Post post = appStateExchange.getPost(postId);
        File destinationFolder = pathService.getDownloadDestinationFolder(post.getTitle(), post.getForum(), post.getThreadTitle(), post.getMetadata(), post.getDestFolder());
        return ResponseEntity.ok(new DownloadPath(destinationFolder.getPath()));
    }

    @PostMapping("/post/restart/all")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized void restartPost() throws Exception {
        executionService.restartAll(null);
    }

    @PostMapping("/post/stop")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized void stop(@RequestBody @NonNull List<PostId> postIds) {
        executionService.stopAll(postIds.stream().map(PostId::getPostId).collect(Collectors.toList()));
    }

    @PostMapping("/post/stop/all")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized void stopAll() {
        executionService.stopAll(null);
    }

    @PostMapping("/post/remove")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity<List<RemoveResult>> remove(@RequestBody @NonNull List<PostId> postIds) {
        List<RemoveResult> result = new ArrayList<>();
        List<String> collect = postIds.stream().map(PostId::getPostId).peek(e -> result.add(new RemoveResult(e))).collect(Collectors.toList());
        executionService.stopAll(collect);
        appStateService.removeAll(collect);
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
        return ResponseEntity.ok(new RemoveAllResult(appStateService.removeAll(null)));
    }

    @GetMapping("/grab/{threadId}")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity<List<VRPostState>> grab(@PathVariable("threadId") @NonNull ThreadId threadId) throws Exception {
        QueuedVGLink queuedVGLink = appStateExchange.getQueue().values().stream().filter(e -> e.getThreadId().equals(threadId.getThreadId())).findFirst().orElseThrow();
        return ResponseEntity.ok(vgHandler.getCache().get(queuedVGLink));
    }

    @PostMapping("/grab/remove")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized void grabRemove(@RequestBody @NonNull ThreadId threadId) {
        vgHandler.remove(threadId.getThreadId());
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