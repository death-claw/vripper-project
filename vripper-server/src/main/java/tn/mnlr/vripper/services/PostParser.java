package tn.mnlr.vripper.services;

import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.q.DownloadQ;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class PostParser {

    private static final Logger logger = LoggerFactory.getLogger(PostParser.class);

    private static final String VR_API = "https://vipergirls.to/vr.php";

    @Autowired
    private ConnectionManager cm;

    SAXParserFactory factory = SAXParserFactory.newInstance();

    @Autowired
    private List<Host> supportedHosts;

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private VipergirlsAuthService authService;

    @Autowired
    private AppSettingsService appSettingsService;

    @Autowired
    private DownloadQ downloadQ;

    @Autowired
    private VipergirlsAuthService vipergirlsAuthService;

    public PostParser() throws ParserConfigurationException, SAXException {
    }

    public void addPost(String postId, String threadId) throws PostParseException {

        if (appStateService.getCurrentPosts().containsKey(postId)) {
            logger.info(String.format("skipping %s, already loaded", postId));
            return;
        }

        VRPostParser vrPostParser = new VRPostParser(threadId, postId);
        Post post = vrPostParser.parse();

        post.setAppStateService(appStateService);
        post.getImages().forEach(e -> e.setAppStateService(appStateService));
        authService.leaveThanks(post.getUrl(), post.getPostId());
        if (appSettingsService.isAutoStart()) {
            logger.info("Auto start downloads option is enabled");
            logger.info(String.format("Starting to enqueue %d jobs for %s", post.getImages().size(), post.getUrl()));
            try {
                downloadQ.enqueue(post);
            } catch (InterruptedException e) {
                logger.warn("Interruption was caught");
                Thread.currentThread().interrupt();
                return;
            }
            logger.info(String.format("Done enqueuing jobs for %s", post.getUrl()));
        } else {
            logger.info("Auto start downloads option is disabled");
        }
    }

    @Getter
    public static abstract class VRPostState {
        private final String threadId;

        protected VRPostState(String threadId) {
            this.threadId = threadId;
        }
    }

    @Getter
    public static class VRPostParse extends VRPostState {

        private final String type = "postParse";
        private String postId;
        private int number;
        private String title;
        private int imageCount;
        private String url;
        private List<String> previews;


        public VRPostParse(String threadId, String postId, int number, String title, int imageCount, String url, List<String> previews) {
            super(threadId);
            this.postId = postId;
            this.number = number;
            this.title = title;
            this.imageCount = imageCount;
            this.previews = previews;
            this.url = url;
        }
    }

    @Getter
    public static class VRThreadParseState extends VRPostState {

        private final String type = "threadParseState";
        private final String state;

        public VRThreadParseState(String threadId, String state) {
            super(threadId);
            this.state = state;
        }
    }

    public class VRThreadParser {

        @Getter
        private final PublishProcessor<VRPostState> postPublishProcessor = PublishProcessor.create();

        private String threadId;

        public VRThreadParser(String threadId) {
            this.threadId = threadId;
        }

        public Void parse() throws PostParseException {

            logger.info(String.format("Parsing thread %s", threadId));
            HttpGet httpGet;
            try {
                URIBuilder uriBuilder = new URIBuilder(VR_API);
                uriBuilder.setParameter("t", threadId);
                httpGet = cm.buildHttpGet(uriBuilder.build());
                if (vipergirlsAuthService.getCookies() != null) {
                    httpGet.addHeader("Cookie", vipergirlsAuthService.getCookies());
                }
            } catch (URISyntaxException e) {
                throw new PostParseException(e);
            }
            VRThreadHandler handler = new VRThreadHandler(threadId, postPublishProcessor);
            HttpClient connection = cm.getClient().build();
            logger.info(String.format("Requesting %s", httpGet));
            try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() / 100 != 2) {
                    throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                }

                factory.newSAXParser().parse(new BufferedInputStream(response.getEntity().getContent()), handler);
                EntityUtils.consumeQuietly(response.getEntity());
            } catch (Exception e) {
                logger.error("parsing failed", e);
                throw new PostParseException(e);
            }
            return null;
        }
    }

    private class VRThreadHandler extends DefaultHandler {

        private final String threadId;
        private final PublishProcessor<VRPostState> vrPostPublishProcessor;
        private String threadTitle;
        private String postId;
        private String postTitle;
        private int imageCount;
        private int postCounter;
        private int previewCounter = 0;
        private List<String> previews = new ArrayList<>();

        public VRThreadHandler(String threadId, PublishProcessor<VRPostState> vrPostPublishProcessor) {
            this.vrPostPublishProcessor = vrPostPublishProcessor;
            this.threadId = threadId;
        }

        @Override
        public void startDocument() {
            vrPostPublishProcessor.onNext(new VRThreadParseState(threadId, "START"));
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
                    if (previewCounter++ < 4) {
                        String thumbUrl = Optional.ofNullable(attributes.getValue("thumb_url")).map(String::trim).orElse(null);
                        if (thumbUrl != null) {
                            previews.add(thumbUrl);
                        }
                    }
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (qName.toLowerCase()) {
                case "post":
                    if (imageCount != 0) {
                        vrPostPublishProcessor.onNext(new VRPostParse(
                                threadId,
                                postId,
                                postCounter,
                                postTitle,
                                imageCount,
                                String.format("https://vipergirls.to/threads/%s/?p=%s&viewfull=1#post%s", threadId, postId, postId),
                                previews
                        ));
                    }
                    previewCounter = 0;
                    previews = new ArrayList<>();
                    break;
            }
        }

        @Override
        public void endDocument() {
            vrPostPublishProcessor.onNext(new VRThreadParseState(threadId, "END"));
            vrPostPublishProcessor.onComplete();
        }
    }

    public class VRPostParser {

        private String threadId;
        private String postId;

        public VRPostParser(String threadId, String postId) {
            this.threadId = threadId;
            this.postId = postId;
        }

        public Post parse() throws PostParseException {

            Post post;
            logger.info(String.format("Parsing post %s", postId));
            HttpGet httpGet;
            try {
                URIBuilder uriBuilder = new URIBuilder(VR_API);
                uriBuilder.setParameter("p", postId);
                httpGet = cm.buildHttpGet(uriBuilder.build());
                if (vipergirlsAuthService.getCookies() != null) {
                    httpGet.addHeader("Cookie", vipergirlsAuthService.getCookies());
                }
            } catch (URISyntaxException e) {
                throw new PostParseException(e);
            }
            VRPostHandler handler = new VRPostHandler(threadId, postId);
            HttpClient connection = cm.getClient().build();
            logger.info(String.format("Requesting %s", httpGet));
            try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() / 100 != 2) {
                    throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                }

                factory.newSAXParser().parse(new BufferedInputStream(response.getEntity().getContent()), handler);
                post = handler.getParsedPost();
                EntityUtils.consumeQuietly(response.getEntity());
            } catch (Exception e) {
                logger.error("parsing failed", e);
                throw new PostParseException(e);
            }
            return post;
        }
    }

    private class VRPostHandler extends DefaultHandler {

        private final String threadId;
        private final String postId;
        private String threadTitle;

        private int previewCounter = 0;
        private int index = 0;

        private String postTitle;
        private int imageCount;

        private String forum;

        private List<String> previews = new ArrayList<>();
        private List<Image> images = new ArrayList<>();
        @Getter
        private Post parsedPost;

        public VRPostHandler(String threadId, String postId) {
            this.threadId = threadId;
            this.postId = postId;
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
                        String thumbUrl = Optional.ofNullable(attributes.getValue("thumb_url")).map(String::trim).orElse(null);
                        if (thumbUrl != null) {
                            previews.add(thumbUrl);
                        }
                    }

                    String mainUrl = Optional.ofNullable(attributes.getValue("main_url")).map(String::trim).orElse(null);
                    if (mainUrl != null) {
                        Host foundHost = supportedHosts.stream().filter(host -> host.isSupported(mainUrl)).findFirst().orElse(null);
                        if (foundHost != null) {
                            logger.info(String.format("Found supported host %s for %s", foundHost.getClass().getSimpleName(), mainUrl));
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
            switch (qName.toLowerCase()) {
                case "post":
                    if (imageCount != 0) {
                        HashMap<String, Object> metadata = new HashMap<>();
                        metadata.put("PREVIEWS", previews);
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
                    break;
            }
        }
    }
}

