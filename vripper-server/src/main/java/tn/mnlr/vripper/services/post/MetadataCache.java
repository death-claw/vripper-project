package tn.mnlr.vripper.services.post;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.jpa.domain.Metadata;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.services.ConnectionManager;
import tn.mnlr.vripper.services.HtmlProcessorService;
import tn.mnlr.vripper.services.VipergirlsAuthService;
import tn.mnlr.vripper.services.XpathService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetadataCache {

    @Getter
    static class Key {
        private final String postId;
        private final String threadId;
        private final String url;

        Key(String postId, String threadId, String url) {
            this.postId = postId;
            this.threadId = threadId;
            this.url = url;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(postId, key.postId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId);
        }
    }

    private static final List<String> dictionary = Arrays.asList("download", "link", "rapidgator", "filefactory", "filefox");

    private final LoadingCache<Key, Metadata> cache;
    private final ConnectionManager cm;
    private final VipergirlsAuthService vipergirlsAuthService;
    private final HtmlProcessorService htmlProcessorService;
    private final XpathService xpathService;

    @Autowired
    public MetadataCache(ConnectionManager cm, VipergirlsAuthService vipergirlsAuthService, HtmlProcessorService htmlProcessorService, XpathService xpathService) {
        this.cm = cm;
        this.vipergirlsAuthService = vipergirlsAuthService;
        this.htmlProcessorService = htmlProcessorService;
        this.xpathService = xpathService;

        CacheLoader<Key, Metadata> loader = new CacheLoader<>() {
            @Override
            public Metadata load(@NonNull Key key) {
                return fetchMetadata(key);
            }
        };
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build(loader);
    }

    public Metadata get(Post post) throws ExecutionException {
        Metadata metadata = new Metadata();
        Metadata cachedMetadata = cache.get(new Key(post.getPostId(), post.getThreadId(), post.getUrl()));
        metadata.setPostedBy(cachedMetadata.getPostedBy());
        metadata.setResolvedNames(List.copyOf(cachedMetadata.getResolvedNames()));
        return metadata;
    }

    private Metadata fetchMetadata(Key key) {
        HttpGet httpGet = cm.buildHttpGet(key.getUrl(), null);
        Metadata metadata = new Metadata();
        Failsafe.with(VripperApplication.retryPolicy)
                .onFailure(e -> {
                    if (e.getFailure() instanceof InterruptedException || e.getFailure().getCause() instanceof InterruptedException) {
                        log.debug("Fetching interrupted");
                        return;
                    }
                    log.error(String.format("Error occurred when getting post metadata, postId %s", key.getPostId()), e.getFailure());
                })
                .run(() -> {
                    HttpClient connection = cm.getClient().build();
                    try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet, vipergirlsAuthService.getContext())) {
                        if (response.getStatusLine().getStatusCode() / 100 != 2) {
                            throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                        }
                        try {
                            if (Thread.interrupted()) {
                                return;
                            }
                            Document document = htmlProcessorService.clean(EntityUtils.toString(response.getEntity()));
                            Node postNode = xpathService.getAsNode(document, String.format("//li[@id='post_%s']/div[contains(@class, 'postdetails')]", key.getPostId()));
                            String postedBy = xpathService.getAsNode(postNode, "./div[contains(@class, 'userinfo')]//a[contains(@class, 'username')]//font").getTextContent().trim();
                            metadata.setPostedBy(postedBy);

                            Node node = xpathService.getAsNode(document, String.format("//div[@id='post_message_%s']", key.getPostId()));
                            metadata.setResolvedNames(findTitleInContent(node));
                        } catch (Exception e) {
                            throw new PostParseException(String.format("Failed to parse thread %s, post %s", key.getThreadId(), key.getPostId()), e);
                        } finally {
                            EntityUtils.consumeQuietly(response.getEntity());
                        }
                    }
                });
        return metadata;
    }

    private List<String> findTitleInContent(Node node) {
        List<String> altTitle = new ArrayList<>();
        findTitle(node, altTitle, new AtomicBoolean(true));
        return altTitle.stream().distinct().collect(Collectors.toList());
    }

    private void findTitle(Node node, List<String> altTitle, AtomicBoolean keepGoing) {
        if (!keepGoing.get()) {
            return;
        }
        if (node.getNodeName().equals("a") || node.getNodeName().equals("img")) {
            keepGoing.set(false);
            return;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            for (int i = 0; i < node.getChildNodes().getLength(); i++) {
                Node item = node.getChildNodes().item(i);
                findTitle(item, altTitle, keepGoing);
                if (!keepGoing.get()) {
                    return;
                }
            }

        } else if (node.getNodeType() == Node.TEXT_NODE) {
            String text = node.getTextContent().trim();
            if (!text.isBlank() && dictionary.stream().noneMatch(e -> text.toLowerCase().contains(e.toLowerCase()))) {
                altTitle.add(text);
            }
        }
    }
}
