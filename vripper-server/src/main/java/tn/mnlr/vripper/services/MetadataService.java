package tn.mnlr.vripper.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.jpa.domain.Event;
import tn.mnlr.vripper.jpa.domain.Metadata;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.repositories.IEventRepository;
import tn.mnlr.vripper.services.domain.tasks.MetadataRunnable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetadataService {

    private static final List<String> dictionary = Arrays.asList("download", "link", "rapidgator", "filefactory", "filefox");
    private final LoadingCache<Key, Metadata> cache;
    private final ConnectionService cm;
    private final VGAuthService VGAuthService;
    private final HtmlProcessorService htmlProcessorService;
    private final XpathService xpathService;
    private final Map<String, MetadataRunnable> fetchingMetadata = new ConcurrentHashMap<>();
    private final ThreadPoolService threadPoolService;
    private final IEventRepository eventRepository;

    @Autowired
    public MetadataService(ConnectionService cm, VGAuthService VGAuthService, HtmlProcessorService htmlProcessorService, XpathService xpathService, ThreadPoolService threadPoolService, IEventRepository eventRepository) {
        this.cm = cm;
        this.VGAuthService = VGAuthService;
        this.htmlProcessorService = htmlProcessorService;
        this.xpathService = xpathService;
        this.threadPoolService = threadPoolService;
        this.eventRepository = eventRepository;

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

    public Metadata get(Post post) {
        Metadata metadata = new Metadata();
        Key key = new Key(post.getPostId(), post.getThreadId(), post.getUrl());
        Metadata cachedMetadata = cache.getIfPresent(key);
        if (cachedMetadata == null) {
            Event event = new Event(Event.Type.METADATA_CACHE_MISS, Event.Status.PROCESSING, LocalDateTime.now(), "Loading metadata for " + post.getUrl());
            eventRepository.save(event);
            try {
                cachedMetadata = cache.get(key);
                event.setStatus(Event.Status.DONE);
                eventRepository.update(event);
            } catch (Exception e) {
                String error = "Failed to load metadata for " + post.getUrl();
                log.error(error, e);
                event.setStatus(Event.Status.ERROR);
                event.setMessage(error + "\n" + Utils.throwableToString(e));
                eventRepository.update(event);
                return null;
            }
        }
        metadata.setPostedBy(cachedMetadata.getPostedBy());
        metadata.setPostId(cachedMetadata.getPostId());
        metadata.setResolvedNames(List.copyOf(cachedMetadata.getResolvedNames()));
        return metadata;
    }

    public void startFetchingMetadata(Post post) {
        MetadataRunnable runnable = new MetadataRunnable(post);
        threadPoolService.getGeneralExecutor().submit(runnable);
        fetchingMetadata.put(post.getPostId(), runnable);
    }

    public void stopFetchingMetadata(Post post) {
        this.fetchingMetadata.forEach((k, v) -> {
            if (k.equals(post.getPostId())) {
                v.setInterrupted(true);
            }
        });
        fetchingMetadata.remove(post.getPostId());
    }

    private Metadata fetchMetadata(Key key) {
        HttpGet httpGet = cm.buildHttpGet(key.getUrl(), null);
        Metadata metadata = new Metadata();
        Failsafe.with(cm.getRetryPolicy())
                .onFailure(e -> {
                    if (e.getFailure() instanceof InterruptedException || e.getFailure().getCause() instanceof InterruptedException) {
                        log.debug("Fetching interrupted");
                        return;
                    }
                    log.error(String.format("Error occurred when getting post metadata, postId %s", key.getPostId()), e.getFailure());
                })
                .run(() -> {
                    HttpClient connection = cm.getClient().build();
                    try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet, VGAuthService.getContext())) {
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

                            metadata.setPostId(key.getPostId());
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
}
