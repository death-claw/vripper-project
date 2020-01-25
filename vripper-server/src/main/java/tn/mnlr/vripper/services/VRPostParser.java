package tn.mnlr.vripper.services;

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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.host.Host;

import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static tn.mnlr.vripper.services.PostParser.VR_API;

class VRPostParser {

    private static final Logger logger = LoggerFactory.getLogger(VRPostParser.class);

    private static SAXParserFactory factory = SAXParserFactory.newInstance();

    private static RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
            .handleIf(e -> e instanceof IOException)
            .withDelay(1, 3, ChronoUnit.SECONDS)
            .withMaxRetries(2)
            .withMaxDuration(Duration.of(10, ChronoUnit.SECONDS))
            .abortOn(InterruptedException.class)
            .onFailedAttempt(e -> logger.warn(String.format("#%d tries failed", e.getAttemptCount()), e.getLastFailure()));

    private final String threadId;
    private final String postId;
    private final ConnectionManager cm;
    private final VipergirlsAuthService vipergirlsAuthService;
    private final List<Host> supportedHosts;
    private final HtmlProcessorService htmlProcessorService;
    private final XpathService xpathService;

    VRPostParser(String threadId, String postId, ConnectionManager cm, VipergirlsAuthService vipergirlsAuthService, List<Host> supportedHosts, HtmlProcessorService htmlProcessorService, XpathService xpathService) {
        this.threadId = threadId;
        this.postId = postId;
        this.cm = cm;
        this.vipergirlsAuthService = vipergirlsAuthService;
        this.supportedHosts = supportedHosts;
        this.htmlProcessorService = htmlProcessorService;
        this.xpathService = xpathService;
    }

    public Post parse() throws PostParseException {

        logger.debug(String.format("Parsing post %s", postId));
        HttpGet httpGet;
        try {
            URIBuilder uriBuilder = new URIBuilder(VR_API);
            uriBuilder.setParameter("p", postId);
            httpGet = cm.buildHttpGet(uriBuilder.build());
        } catch (URISyntaxException e) {
            throw new PostParseException(e);
        }
        VRPostHandler handler = new VRPostHandler(threadId, postId, supportedHosts);
        AtomicReference<Throwable> thr = new AtomicReference<>();
        logger.debug(String.format("Requesting %s", httpGet));
        Post post = getPost(httpGet, handler, thr);
        if (thr.get() != null || post == null) {
            logger.error(String.format("parsing failed for thread %s, post %s", threadId, postId), thr.get());
            throw new PostParseException(thr.get());
        }
        return post;
    }

    private Post getPost(HttpGet httpGet, VRPostHandler handler, AtomicReference<Throwable> thr) {
        return Failsafe.with(retryPolicy)
                .onFailure(e -> thr.set(e.getFailure()))
                .get(() -> {
                    HttpClient connection = cm.getClient().build();
                    try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet, vipergirlsAuthService.getContext())) {
                        if (response.getStatusLine().getStatusCode() / 100 != 2) {
                            throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                        }

                        try {
                            factory.newSAXParser().parse(new BufferedInputStream(response.getEntity().getContent()), handler);
                            return handler.getParsedPost();
                        } catch (Exception e) {
                            throw new PostParseException(String.format("Failed to parse thread %s, post %s", threadId, postId), e);
                        } finally {
                            EntityUtils.consumeQuietly(response.getEntity());
                        }
                    }
                });
    }

    private void getPostExtraMetadata(Post post, AtomicReference<Throwable> thr) {
        HttpGet httpGet = cm.buildHttpGet(post.getUrl());
        Failsafe.with(retryPolicy)
                .onFailure(e -> thr.set(e.getFailure()))
                .get(() -> {
                    HttpClient connection = cm.getClient().build();
                    try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet, vipergirlsAuthService.getContext())) {
                        if (response.getStatusLine().getStatusCode() / 100 != 2) {
                            throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                        }

                        try {

                            Document document = htmlProcessorService.clean(EntityUtils.toString(response.getEntity()));
                            Node postNode = xpathService.getAsNode(document, String.format("//li[@id='post_%s']/div[contains(@class, 'postdetails')]", post.getPostId()));
                            String postedBy = xpathService.getAsNode(postNode, "./div[contains(@class, 'userinfo')]//a[contains(@class, 'username')]//font").getTextContent().trim();
                            HashMap<String, Object> stringObjectHashMap = new HashMap<>();
                            response.getEntity().getContent();
                            return stringObjectHashMap;
                        } catch (Exception e) {
                            throw new PostParseException(String.format("Failed to parse thread %s, post %s", threadId, postId), e);
                        } finally {
                            EntityUtils.consumeQuietly(response.getEntity());
                        }
                    }
                });
    }
}

class VRPostHandler extends DefaultHandler {

    private static final Logger logger = LoggerFactory.getLogger(VRPostHandler.class);

    private final List<Host> supportedHosts;
    private final String threadId;
    private final String postId;
    private List<Image> images = new ArrayList<>();
    private List<String> previews = new ArrayList<>();
    private String threadTitle;
    private String postTitle;
    private String forum;
    private int previewCounter = 0;
    private int index = 0;
    private int imageCount;

    @Getter
    private Post parsedPost;

    VRPostHandler(String threadId, String postId, List<Host> supportedHosts) {
        this.threadId = threadId;
        this.postId = postId;
        this.supportedHosts = supportedHosts;
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

        switch (qName.toLowerCase()) {
            case "forum":
                forum = attributes.getValue("title").trim();
                break;
            case "thread":
                threadTitle = attributes.getValue("title").trim();
                break;
            case "post":
                imageCount = Integer.parseInt(attributes.getValue("imagecount").trim());
                postTitle = Optional.ofNullable(attributes.getValue("title")).map(e -> e.trim().isEmpty() ? null : e.trim()).orElse(threadTitle);
                break;
            case "image":
                index++;
                if (previewCounter++ < 4) {
                    Optional.ofNullable(attributes.getValue("thumb_url")).map(String::trim).ifPresent(previews::add);
                }

                String mainUrl = Optional.ofNullable(attributes.getValue("main_url")).map(String::trim).orElse(null);
                if (mainUrl != null) {
                    Host foundHost = supportedHosts.stream().filter(host -> host.isSupported(mainUrl)).findFirst().orElse(null);
                    if (foundHost != null) {
                        logger.debug(String.format("Found supported host %s for %s", foundHost.getClass().getSimpleName(), mainUrl));
                        images.add(new Image(mainUrl, postId, postTitle, foundHost, index));
                    } else {
                        logger.warn(String.format("unsupported host for %s, skipping", mainUrl));
                    }
                }
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("post".equals(qName.toLowerCase())) {
            if (imageCount != 0) {
                HashMap<String, Object> metadata = new HashMap<>();
                metadata.put(Post.METADATA.PREVIEWS.name(), previews);
                parsedPost = new Post(
                        postTitle,
                        String.format("https://vipergirls.to/threads/%s/?p=%s&viewfull=1#post%s", threadId, postId, postId),
                        images,
                        metadata,
                        postId,
                        threadId,
                        threadTitle,
                        forum
                );
            }
            index = 0;
            previewCounter = 0;
            previews = new ArrayList<>();
            images = new ArrayList<>();
        }
    }
}