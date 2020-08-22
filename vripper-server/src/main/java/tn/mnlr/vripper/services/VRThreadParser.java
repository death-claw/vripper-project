package tn.mnlr.vripper.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.services.post.CachedPost;

import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public class VRThreadParser {

    private static final String VR_API = "https://vipergirls.to/vr.php";

    private static final SAXParserFactory factory = SAXParserFactory.newInstance();
    private final Queued queued;
    private final ConnectionManager cm;
    private final VipergirlsAuthService vipergirlsAuthService;

    VRThreadParser(Queued queued) {
        this.queued = queued;
        this.cm = SpringContext.getBean(ConnectionManager.class);
        this.vipergirlsAuthService = SpringContext.getBean(VipergirlsAuthService.class);
    }

    public List<CachedPost> parse() throws PostParseException {

        log.debug(String.format("Parsing thread %s", queued));
        HttpGet httpGet;
        try {
            URIBuilder uriBuilder = new URIBuilder(VR_API);
            uriBuilder.setParameter("t", queued.getThreadId());
            httpGet = cm.buildHttpGet(uriBuilder.build(), null);
        } catch (URISyntaxException e) {
            throw new PostParseException(e);
        }

        VRThreadHandler handler = new VRThreadHandler(queued);
        AtomicReference<Throwable> thr = new AtomicReference<>();
        log.debug(String.format("Requesting %s", httpGet));
        List<CachedPost> posts = Failsafe.with(VripperApplication.retryPolicy)
                .onFailure(e -> thr.set(e.getFailure()))
                .get(() -> {
                    HttpClient connection = cm.getClient().build();
                    try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet, vipergirlsAuthService.getContext())) {
                        if (response.getStatusLine().getStatusCode() / 100 != 2) {
                            throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                        }

                        try {
                            factory.newSAXParser().parse(new BufferedInputStream(response.getEntity().getContent()), handler);
                            return handler.getPosts();
                        } catch (Exception e) {
                            throw new PostParseException(String.format("Failed to parse thread %s", queued), e);
                        } finally {
                            EntityUtils.consumeQuietly(response.getEntity());
                        }
                    }
                });
        if (thr.get() != null) {
            log.error(String.format("parsing failed for thread %s", queued), thr.get());
            throw new PostParseException(thr.get());
        }
        return posts;
    }
}

class VRThreadHandler extends DefaultHandler {

    private final Queued queued;
    private final Collection<Host> supportedHosts;
    private final Map<Host, AtomicInteger> hostMap = new HashMap<>();
    private List<String> previews = new ArrayList<>();
    private String threadTitle;
    private String postId;
    private String postTitle;
    private int imageCount;
    private int postCounter;
    private int previewCounter = 0;

    @Getter
    private List<CachedPost> posts = new ArrayList<>();

    VRThreadHandler(Queued queued) {
        this.queued = queued;
        this.supportedHosts = SpringContext.getBeansOfType(Host.class).values();
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

        switch (qName.toLowerCase()) {
            case "thread":
                threadTitle = attributes.getValue("title").trim();
                break;
            case "post":
                imageCount = Integer.parseInt(attributes.getValue("imagecount").trim());
                postId = attributes.getValue("id").trim();
                postCounter = Integer.parseInt(attributes.getValue("number").trim());
                postTitle = Optional.ofNullable(attributes.getValue("title")).map(e -> e.trim().isEmpty() ? null : e.trim()).orElse(threadTitle);
                break;
            case "image":
                Optional.ofNullable(attributes.getValue("main_url"))
                        .map(String::trim)
                        .flatMap(mainUrl -> supportedHosts.stream().filter(host -> host.isSupported(mainUrl)).findFirst())
                        .ifPresent(host -> Optional.ofNullable(hostMap.get(host)).ifPresentOrElse(AtomicInteger::incrementAndGet, () -> hostMap.put(host, new AtomicInteger(0))));
                if (previewCounter++ < 4) {
                    Optional.ofNullable(attributes.getValue("thumb_url")).map(String::trim).ifPresent(previews::add);
                }
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("post".equals(qName.toLowerCase())) {
            if (imageCount != 0) {
                posts.add(new CachedPost(
                        queued.getThreadId(),
                        postId,
                        postCounter,
                        postTitle,
                        imageCount,
                        String.format("https://vipergirls.to/threads/?p=%s&viewfull=1#post%s", postId, postId),
                        previews,
                        hostMap.entrySet().stream().filter(v -> v.getValue().get() > 0).map(e -> e.getKey().getHost() + " (" + e.getValue().get() + ")").collect(Collectors.joining(", "))
                ));
                queued.increment();
            }
            previewCounter = 0;
            previews = new ArrayList<>();
            hostMap.clear();
        }
    }

    @Override
    public void endDocument() {
        queued.done();
    }
}
