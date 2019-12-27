package tn.mnlr.vripper.services;

import io.reactivex.processors.ReplayProcessor;
import lombok.Getter;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.host.Host;

import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static tn.mnlr.vripper.services.PostParser.VR_API;

public class VRThreadParser {

    private static final Logger logger = LoggerFactory.getLogger(VRThreadParser.class);

    private static RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
            .handleIf(e -> e instanceof IOException)
            .withDelay(1, 3, ChronoUnit.SECONDS)
            .withMaxRetries(2)
            .withMaxDuration(Duration.of(10, ChronoUnit.SECONDS))
            .abortOn(InterruptedException.class)
            .onFailedAttempt(e -> logger.warn(String.format("#%d tries failed", e.getAttemptCount()), e.getLastFailure()));
    private static SAXParserFactory factory = SAXParserFactory.newInstance();
    @Getter
    private final ReplayProcessor<VRPostState> postPublishProcessor = ReplayProcessor.create();
    private final String threadId;
    private final ConnectionManager cm;
    private final VipergirlsAuthService vipergirlsAuthService;
    private final List<Host> hosts;


    VRThreadParser(String threadId, ConnectionManager cm, VipergirlsAuthService vipergirlsAuthService, List<Host> hosts) {
        this.threadId = threadId;
        this.cm = cm;
        this.vipergirlsAuthService = vipergirlsAuthService;
        this.hosts = hosts;
    }

    public Void parse() throws PostParseException {

        logger.debug(String.format("Parsing thread %s", threadId));
        HttpGet httpGet;
        try {
            URIBuilder uriBuilder = new URIBuilder(VR_API);
            uriBuilder.setParameter("t", threadId);
            httpGet = cm.buildHttpGet(uriBuilder.build());
        } catch (URISyntaxException e) {
            throw new PostParseException(e);
        }

        VRThreadHandler handler = new VRThreadHandler(threadId, postPublishProcessor, hosts);
        AtomicReference<Throwable> thr = new AtomicReference<>();
        logger.debug(String.format("Requesting %s", httpGet));
        Failsafe.with(retryPolicy)
                .onFailure(e -> thr.set(e.getFailure()))
                .get(() -> {
                    HttpClient connection = cm.getClient().build();
                    try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet, vipergirlsAuthService.getContext())) {
                        if (response.getStatusLine().getStatusCode() / 100 != 2) {
                            throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                        }

                        try {
                            factory.newSAXParser().parse(new BufferedInputStream(response.getEntity().getContent()), handler);
                        } catch (Exception e) {
                            throw new PostParseException(String.format("Failed to parse thread %s", threadId), e);
                        } finally {
                            EntityUtils.consumeQuietly(response.getEntity());
                        }
                    }
                    return null;
                });
        if (thr.get() != null) {
            logger.error(String.format("parsing failed for thread %s", threadId), thr.get());
            throw new PostParseException(thr.get());
        }
        return null;
    }
}

class VRThreadHandler extends DefaultHandler {

    private final String threadId;
    private final ReplayProcessor<VRPostState> vrPostPublishProcessor;
    private final List<Host> supportedHosts;
    private final Map<Host, AtomicInteger> hostMap = new HashMap<>();
    private List<String> previews = new ArrayList<>();
    private String threadTitle;
    private String postId;
    private String postTitle;
    private int imageCount;
    private int postCounter;
    private int previewCounter = 0;

    VRThreadHandler(String threadId, ReplayProcessor<VRPostState> vrPostPublishProcessor, List<Host> supportedHosts) {
        this.vrPostPublishProcessor = vrPostPublishProcessor;
        this.threadId = threadId;
        this.supportedHosts = supportedHosts;
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
                vrPostPublishProcessor.onNext(new VRPostState(
                        threadId,
                        postId,
                        postCounter,
                        postTitle,
                        imageCount,
                        String.format("https://vipergirls.to/threads/%s/?p=%s&viewfull=1#post%s", threadId, postId, postId),
                        previews,
                        hostMap.entrySet().stream().filter(v -> v.getValue().get() > 0).map(e -> e.getKey().getHost() + " (" + e.getValue().get() + ")").collect(Collectors.joining(", "))
                ));
            }
            previewCounter = 0;
            previews = new ArrayList<>();
            hostMap.clear();
        }
    }

    @Override
    public void endDocument() {
        vrPostPublishProcessor.onComplete();
    }
}
