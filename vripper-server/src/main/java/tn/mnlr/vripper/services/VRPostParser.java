package tn.mnlr.vripper.services;

import net.jodah.failsafe.Failsafe;
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
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.host.Host;

import javax.xml.parsers.SAXParserFactory;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static tn.mnlr.vripper.services.PostParser.VR_API;

class VRPostParser {

    private static final Logger logger = LoggerFactory.getLogger(VRPostParser.class);

    private static SAXParserFactory factory = SAXParserFactory.newInstance();

    private static List<String> dictionary = Arrays.asList("download", "link", "rapidgator", "filefactory", "filefox");

    private final String threadId;
    private final String postId;
    private final ConnectionManager cm;
    private final VipergirlsAuthService vipergirlsAuthService;
    private final HtmlProcessorService htmlProcessorService;
    private final XpathService xpathService;

    VRPostParser(String threadId, String postId) {
        this.threadId = threadId;
        this.postId = postId;
        this.cm = SpringContext.getBean(ConnectionManager.class);
        this.vipergirlsAuthService = SpringContext.getBean(VipergirlsAuthService.class);
        this.htmlProcessorService = SpringContext.getBean(HtmlProcessorService.class);
        this.xpathService = SpringContext.getBean(XpathService.class);
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

        AtomicReference<Throwable> thr = new AtomicReference<>();

        HashMap<String, Object> metadata = new HashMap<>();
        String postUrl = String.format("https://vipergirls.to/threads/%s/?p=%s&viewfull=1#post%s", threadId, postId, postId);
        getPostExtraMetadata(postId, metadata, postUrl, thr);
        if (thr.get() != null) {
            logger.warn(String.format("Failed to get metadata for thread %s, post %s", threadId, postId), thr.get());
            thr.set(null);
        }

        VRApiPostHandler handler = new VRApiPostHandler(threadId, postId, postUrl, metadata);
        logger.debug(String.format("Requesting %s", httpGet));
        Optional<Post> post = getPost(httpGet, handler, thr);
        if (thr.get() != null || post.isEmpty()) {
            logger.error(String.format("parsing failed for thread %s, post %s", threadId, postId), thr.get());
            throw new PostParseException(thr.get());
        }
        return post.get();
    }

    private Optional<Post> getPost(HttpGet httpGet, VRApiPostHandler handler, AtomicReference<Throwable> thr) {
        return Failsafe.with(VripperApplication.retryPolicy)
                .onFailure(e -> thr.set(e.getFailure()))
                .get(() -> {
                    HttpClient connection = cm.getClient().build();
                    try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet, vipergirlsAuthService.getContext())) {
                        if (response.getStatusLine().getStatusCode() / 100 != 2) {
                            throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                        }

                        factory.newSAXParser().parse(response.getEntity().getContent(), handler);
                        EntityUtils.consumeQuietly(response.getEntity());
                        return handler.getParsedPost();
                    }
                });
    }

    private void getPostExtraMetadata(String postId, HashMap<String, Object> metadata, String url, AtomicReference<Throwable> thr) {
        HttpGet httpGet = cm.buildHttpGet(url);
        Failsafe.with(VripperApplication.retryPolicy)
                .onFailure(e -> thr.set(e.getFailure()))
                .get(() -> {
                    HttpClient connection = cm.getClient().build();
                    try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet, vipergirlsAuthService.getContext())) {
                        if (response.getStatusLine().getStatusCode() / 100 != 2) {
                            throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                        }
                        try {
                            Document document = htmlProcessorService.clean(EntityUtils.toString(response.getEntity()));
                            Node postNode = xpathService.getAsNode(document, String.format("//li[@id='post_%s']/div[contains(@class, 'postdetails')]", postId));
                            String postedBy = xpathService.getAsNode(postNode, "./div[contains(@class, 'userinfo')]//a[contains(@class, 'username')]//font").getTextContent().trim();
                            metadata.put(Post.METADATA.POSTED_BY.name(), postedBy);

                            Node node = xpathService.getAsNode(document, String.format("//div[@id='post_message_%s']", postId));
                            metadata.put(Post.METADATA.RESOLVED_NAME.name(), findTitleInContent(node));

                            return null;
                        } catch (Exception e) {
                            throw new PostParseException(String.format("Failed to parse thread %s, post %s", threadId, postId), e);
                        } finally {
                            EntityUtils.consumeQuietly(response.getEntity());
                        }
                    }
                });
    }

    private List<String> findTitleInContent(Node node) {
        List<String> altTitle = new ArrayList<>();
        findTitle(node, altTitle);
        return altTitle;
    }

    private boolean findTitle(Node node, List<String> altTitle) {
        if (node.getNodeName().equals("a") || node.getNodeName().equals("img")) {
            return false;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            for (int i = 0; i < node.getChildNodes().getLength(); i++) {
                Node item = node.getChildNodes().item(i);
                findTitle(item, altTitle);
            }

        } else if (node.getNodeType() == Node.TEXT_NODE) {
            String text = node.getTextContent().trim();
            if (!text.isEmpty() && dictionary.stream().noneMatch(e -> text.toLowerCase().contains(e.toLowerCase()))) {
                altTitle.add(text);
            }
        }
        return true;
    }
}

class VRApiPostHandler extends DefaultHandler {

    private static final Logger logger = LoggerFactory.getLogger(VRApiPostHandler.class);

    private final Collection<Host> supportedHosts;

    private final String threadId;
    private final String postId;
    private final HashMap<String, Object> metadata;
    private final String postUrl;
    private List<Image> images = new ArrayList<>();
    private List<String> previews = new ArrayList<>();
    private String threadTitle;
    private String postTitle;
    private String forum;
    private int previewCounter = 0;
    private int index = 0;
    private int imageCount;

    private Post parsedPost;

    VRApiPostHandler(String threadId, String postId, String postUrl, HashMap<String, Object> metadata) {
        this.threadId = threadId;
        this.postId = postId;
        this.postUrl = postUrl;
        this.metadata = metadata;
        this.supportedHosts = SpringContext.getBeansOfType(Host.class).values();
    }


    public Optional<Post> getParsedPost() {
        return Optional.ofNullable(parsedPost);
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
                        try {
                            images.add(new Image(mainUrl, postId, postTitle, foundHost, index));
                        } catch (PostParseException e) {
                            logger.error(String.format("Error occurred while parsing postId %s", postId));
                        }
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
                metadata.put(Post.METADATA.PREVIEWS.name(), previews);
                AtomicReference<Throwable> thr = new AtomicReference<>();
                if (thr.get() != null) {
                    logger.error(String.format("Failed to get extra metadata for %s", postUrl), thr.get());
                }
                try {
                    parsedPost = new Post(
                            postTitle,
                            postUrl,
                            images,
                            metadata,
                            postId,
                            threadId,
                            threadTitle,
                            forum);
                } catch (PostParseException e) {
                    logger.error(String.format("Error occurred while parsing postId %s", postId));
                }
            }
            index = 0;
            previewCounter = 0;
            previews = new ArrayList<>();
            images = new ArrayList<>();
        }
    }
}