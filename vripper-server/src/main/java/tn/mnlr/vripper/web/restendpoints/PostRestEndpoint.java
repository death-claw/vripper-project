package tn.mnlr.vripper.web.restendpoints;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.entities.Post.METADATA;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.q.ExecutionService;
import tn.mnlr.vripper.services.*;
import tn.mnlr.vripper.web.restendpoints.domain.posts.*;
import tn.mnlr.vripper.web.restendpoints.exceptions.BadRequestException;
import tn.mnlr.vripper.web.restendpoints.exceptions.NotFoundException;
import tn.mnlr.vripper.web.restendpoints.exceptions.ServerErrorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @PostMapping("/post")
    @ResponseStatus(code = HttpStatus.OK)
    public void processPost(@RequestBody ThreadUrl _url) {
        if (_url.getUrl() == null || _url.getUrl().isEmpty()) {
            throw new BadRequestException("Failed to process empty request");
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
            Matcher m = VG_URL_PATTERN.matcher(url);
            if (m.find()) {
                threadId = m.group(1);
                postId = m.group(4);
            } else {
                throw new BadRequestException(String.format("Cannot retrieve thread id from URL %s", url));
            }
            queuedVGLinks.add(new QueuedVGLink(url, threadId, postId));
        }
        try {
            vgHandler.handle(queuedVGLinks);
        } catch (Exception e) {
            throw new ServerErrorException(e.getMessage());
        }
    }

    @PostMapping("/post/restart")
    @ResponseStatus(value = HttpStatus.OK)
    public void restartPost(@RequestBody @NonNull List<PostId> postIds) {
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
    public DownloadPath folderPath(@PathVariable("postId") String postId) {
        Post post = appStateExchange.getPost(postId);
        if (post.getPostFolderName() == null) {
            throw new NotFoundException("Download has not been started yet for this post");
        } else {
            return new DownloadPath(pathService.getDownloadDestinationFolder(post).getPath());
        }
    }

    @PostMapping("/post/restart/all")
    @ResponseStatus(value = HttpStatus.OK)
    public void restartPost() {
        executionService.restartAll(null);
    }

    @PostMapping("/post/stop")
    @ResponseStatus(value = HttpStatus.OK)
    public void stop(@RequestBody @NonNull List<PostId> postIds) {
        executionService.stopAll(postIds.stream().map(PostId::getPostId).collect(Collectors.toList()));
    }

    @PostMapping("/post/stop/all")
    @ResponseStatus(value = HttpStatus.OK)
    public void stopAll() {
        executionService.stopAll(null);
    }

    @PostMapping("/post/remove")
    @ResponseStatus(value = HttpStatus.OK)
    public List<RemoveResult> remove(@RequestBody @NonNull List<PostId> postIds) {
        List<RemoveResult> result = new ArrayList<>();
        List<String> collect = postIds.stream().map(PostId::getPostId).peek(e -> result.add(new RemoveResult(e))).collect(Collectors.toList());
        executionService.stopAll(collect);
        appStateService.removeAll(collect);
        return result;
    }

    @PostMapping("/post/rename")
    @ResponseStatus(value = HttpStatus.OK)
    public List<AltPostName> rename(@RequestBody @NonNull List<AltPostName> postToRename) {
        for (AltPostName altPostName : postToRename) {
            Post post = appStateExchange.getPost(altPostName.getPostId());
            try {
                pathService.rename(post, altPostName.getAltName());
            } catch (Exception e) {
                throw new ServerErrorException(e.getMessage());
            }
        }
        return postToRename;
    }

    @PostMapping("/post/rename/first")
    @ResponseStatus(value = HttpStatus.OK)
    public List<PostId> renameFirst(@RequestBody @NonNull List<PostId> postToRename) {
        for (PostId postId : postToRename) {
            Post post = appStateExchange.getPost(postId.getPostId());
            if (post.getMetadata().get(METADATA.RESOLVED_NAME.name()) != null) {
                List<String> resolvedNames = ((List<String>) post.getMetadata().get(METADATA.RESOLVED_NAME.name()));
                if (!resolvedNames.isEmpty()) {
                    try {
                        pathService.rename(post, resolvedNames.get(0));
                    } catch (Exception e) {
                        throw new ServerErrorException(e.getMessage());
                    }
                }
            }
        }
        return postToRename;
    }

    @PostMapping("/post/clear/all")
    @ResponseStatus(value = HttpStatus.OK)
    public RemoveAllResult clearAll() {
        return new RemoveAllResult(appStateService.clearAll());
    }

    @PostMapping("/post/remove/all")
    @ResponseStatus(value = HttpStatus.OK)
    public RemoveAllResult removeAll() {
        return new RemoveAllResult(appStateService.removeAll(null));
    }

    @GetMapping("/grab/{threadId}")
    @ResponseStatus(value = HttpStatus.OK)
    public List<VRPostState> grab(@PathVariable("threadId") @NonNull String threadId) throws Exception {
        QueuedVGLink queuedVGLink = appStateExchange.getQueue().values().stream().filter(e -> e.getThreadId().equals(threadId)).findFirst().orElseThrow();
        return vgHandler.getCache().get(queuedVGLink);
    }

    @PostMapping("/grab/remove")
    @ResponseStatus(value = HttpStatus.OK)
    public ThreadId grabRemove(@RequestBody @NonNull ThreadId threadId) {
        vgHandler.remove(threadId.getThreadId());
        return threadId;
    }
}
