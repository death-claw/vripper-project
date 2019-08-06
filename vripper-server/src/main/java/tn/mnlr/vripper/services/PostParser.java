package tn.mnlr.vripper.services;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.host.Host;

import java.util.ArrayList;
import java.util.List;

@Service
public class PostParser {

    private static final Logger logger = LoggerFactory.getLogger(PostParser.class);

    private final static String VIPER_GIRLS_BASE_ADRESS = "https://vipergirls.to/";
    private final static String POSTS_XPATH = "//li[contains(@id,'post_')][not(contains(@id,'post_thank'))]";
    private final static String REAL_THREAD_XPATH = ".//a[@class='postcounter']";
    private final static String THREAD_TITLE_XPATH = "//li[contains(@class, 'lastnavbit')]/span";
    private final static String POST_TITLE_XPATH = ".//h2[contains(@class, 'title')]";
    private final static String POST_COUNTER_XPATH = ".//a[contains(@class, 'postcounter')]";
    private final static String POST_LINKS_XPATH = ".//a";
    public static final String THREAD_TITLE_API_XPATH = "//thread";
    public static final String POSTS_API_XPATH = "//post[@imagecount>0]";
    private static final String VR_ENDPOINT = "https://vipergirls.to/vr.php";

    @Autowired
    private ConnectionManager cm;

    @Autowired
    private HtmlProcessorService htmlProcessorService;

    @Autowired
    private XpathService xpathService;

    @Autowired
    private List<Host> supportedHosts;
    @Autowired
    private VipergirlsAuthService vipergirlsAuthService;

    @Autowired
    private AppStateService appStateService;

    public List<Post> parse(String threadUrl) throws PostParseException {

        logger.info(String.format("Parsing thread %s", threadUrl));
        List<Post> posts = new ArrayList<>();
        String postResponse;

        HttpClient connection = cm.getClient().build();
        HttpGet httpGet = cm.buildHttpGet(threadUrl);
        if (vipergirlsAuthService.getCookies() != null) {
            httpGet.addHeader("Cookie", vipergirlsAuthService.getCookies());
        }

        logger.info(String.format("Requesting %s", httpGet));
        try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
            }
            postResponse = EntityUtils.toString(response.getEntity());
            logger.debug(String.format("Content received for %s:%n%s", httpGet, postResponse));
            EntityUtils.consumeQuietly(response.getEntity());
        } catch (Exception e) {
            throw new PostParseException(e);
        }

        Document document;
        try {
            logger.debug(String.format("Cleaning HTML response for XML parsing: %s", postResponse));
            document = htmlProcessorService.clean(postResponse);
        } catch (Exception e) {
            throw new PostParseException(e);
        }

        String threadTitle = null;
        try {
            logger.info(String.format("Looking for thread title using xpath: %s", THREAD_TITLE_XPATH));
            Node threadTitleNode = xpathService.getAsNode(document, THREAD_TITLE_XPATH);
            if (threadTitleNode != null) {
                threadTitle = threadTitleNode.getTextContent().trim();
                logger.info(String.format("Thread title found %s", threadTitle));
            }
            if (threadTitle == null) {
                logger.warn("Cannot find thread's title");
            }
        } catch (Exception e) {
            throw new PostParseException(e);
        }

        NodeList postsNodeList;
        try {
            logger.info(String.format("Looking for posts using xpath: %s", POSTS_XPATH));
            postsNodeList = xpathService.getAsNodeList(document, POSTS_XPATH);
        } catch (Exception e) {
            throw new PostParseException(e);
        }

        logger.info(String.format("Found %d posts in thread %s", postsNodeList.getLength(), threadUrl));

        for (int i = 0; i < postsNodeList.getLength(); i++) {
            String realUrl;
            logger.info(String.format("Parsing post #%d", i + 1));
            try {
                logger.info(String.format("Finding posts's link"));
                realUrl = VIPER_GIRLS_BASE_ADRESS.concat(xpathService
                        .getAsNode(postsNodeList.item(i), REAL_THREAD_XPATH)
                        .getAttributes()
                        .getNamedItem("href")
                        .getTextContent()
                        .trim());
                logger.info(String.format("Post's link: %s", realUrl));
            } catch (Exception e) {
                throw new PostParseException(e);
            }

            logger.info("Finding posts's id");
            String postId = realUrl.substring(realUrl.indexOf("#")).replace("#post", "");
            logger.info(String.format("Post's id: %s", postId));

            if (appStateService.getCurrentPosts().containsKey(postId)) {
                logger.warn(String.format("Post with id %s is already loaded, skipping", postId));
                continue;
            }

            String postTitle;
            try {
                logger.info(String.format("Finding post's title"));
                Node titleNode = xpathService.getAsNode(postsNodeList.item(i), POST_TITLE_XPATH);
                if (titleNode != null) {
                    postTitle = titleNode.getTextContent().trim();
                    logger.info(String.format("Found post's title: %s", postTitle));
                } else {
                    logger.info("Cannot find post's title");
                    if (threadTitle != null) {
                        logger.info("Falling back to thread title to generate a post name");
                        postTitle = threadTitle + "#" + postId;
                    } else {
                        logger.info("Falling back to post id to generate a post name");
                        postTitle = "#" + postId;
                    }
                }
            } catch (Exception e) {
                throw new PostParseException(e);
            }

            String postCounter;
            try {
                logger.info(String.format("Finding post's counter"));
                Node counterNode = xpathService.getAsNode(postsNodeList.item(i), POST_COUNTER_XPATH);
                if (counterNode != null) {
                    postCounter = counterNode.getTextContent().trim();
                    logger.info(String.format("Found post's counter: %s", postCounter));
                } else {
                    postCounter = "";
                }
            } catch (Exception e) {
                throw new PostParseException(e);
            }

            ArrayList<Image> imagesList = new ArrayList<>();
            try {
                logger.info(String.format("Finding all links for post with id %s using xpath %s", postId, POST_LINKS_XPATH));
                NodeList imagesNodeList = xpathService.getAsNodeList(postsNodeList.item(i), POST_LINKS_XPATH);
                for (int j = 0; j < imagesNodeList.getLength(); j++) {

                    Node imageHref = imagesNodeList.item(j).getAttributes().getNamedItem("href");
                    Host foundHost;
                    if (imageHref != null) {
                        String imageUrl = imageHref.getTextContent().trim();
                        logger.info(String.format("Scanning %s", imageUrl));
                        foundHost = supportedHosts.stream().filter(host -> host.isSupported(imageUrl)).findFirst().orElse(null);
                    } else {
                        logger.warn("href is null, skipping");
                        continue;
                    }
                    if (foundHost != null) {
                        logger.info(String.format("Found supported host %s for %s", foundHost.getClass().getSimpleName(), imageHref));
                        imagesList.add(appStateService.createImage(imageHref.getTextContent(), postId, postTitle, foundHost));
                    } else {
                        logger.warn(String.format("unsupported host for %s, skipping", imageHref));
                        continue;
                    }
                }
            } catch (Exception e) {
                throw new PostParseException(e);
            }

            if (!imagesList.isEmpty()) {
                logger.info(String.format("Found %d images for post with id %s", imagesList.size(), postId));
                posts.add(appStateService.createPost(postTitle, realUrl, imagesList, null, postId, postCounter));
            } else {
                logger.warn(String.format("No images found for post with id %s, skipping", postId));
            }
        }
        return posts;
    }

    public List<Post> parseByThreadId(String threadId) throws PostParseException {

        logger.info(String.format("Parsing thread %s", threadId));
        List<Post> posts = new ArrayList<>();
        String postResponse;

        HttpClient connection = cm.getClient().build();
        URIBuilder uriBuilder = new URIBuilder(VR_ENDPOINT);
        uriBuilder.setParameter("t", threadId);
        HttpGet httpGet = cm.buildHttpGet(uriBuilder.build());
        if (vipergirlsAuthService.getCookies() != null) {
            httpGet.addHeader("Cookie", vipergirlsAuthService.getCookies());
        }

        logger.info(String.format("Requesting %s", httpGet));
        try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
            }
            postResponse = EntityUtils.toString(response.getEntity());
            logger.debug(String.format("Content received for %s:%n%s", httpGet, postResponse));
            EntityUtils.consumeQuietly(response.getEntity());
        } catch (Exception e) {
            throw new PostParseException(e);
        }

        Document document;
        try {
            logger.debug(String.format("Cleaning HTML response for XML parsing: %s", postResponse));
            document = htmlProcessorService.clean(postResponse);
        } catch (Exception e) {
            throw new PostParseException(e);
        }

        String threadTitle = null;
        try {
            logger.info(String.format("Looking for thread title using xpath: %s", THREAD_TITLE_API_XPATH));
            Node threadTitleNode = xpathService.getAsNode(document, THREAD_TITLE_API_XPATH);
            if (threadTitleNode != null) {
                threadTitle = threadTitleNode.getAttributes().getNamedItem("title").getTextContent().trim();
                logger.info(String.format("Thread title found %s", threadTitle));
            }
            if (threadTitle == null) {
                logger.warn("Cannot find thread's title");
            }
        } catch (Exception e) {
            throw new PostParseException(e);
        }

        NodeList postsNodeList;
        try {
            logger.info(String.format("Looking for posts using xpath: %s", POSTS_API_XPATH));
            postsNodeList = xpathService.getAsNodeList(document, POSTS_API_XPATH);
        } catch (Exception e) {
            throw new PostParseException(e);
        }

        logger.info(String.format("Found %d posts in thread Id %s", postsNodeList.getLength(), threadId));

        for (int i = 0; i < postsNodeList.getLength(); i++) {
            String realUrl;
            logger.info(String.format("Parsing post #%d", i + 1));
            try {
                logger.info(String.format("Finding posts's link"));
                realUrl = VIPER_GIRLS_BASE_ADRESS.concat(xpathService
                        .getAsNode(postsNodeList.item(i), REAL_THREAD_XPATH)
                        .getAttributes()
                        .getNamedItem("href")
                        .getTextContent()
                        .trim());
                logger.info(String.format("Post's link: %s", realUrl));
            } catch (Exception e) {
                throw new PostParseException(e);
            }

            logger.info("Finding posts's id");
            String postId = realUrl.substring(realUrl.indexOf("#")).replace("#post", "");
            logger.info(String.format("Post's id: %s", postId));

            if (appStateService.getCurrentPosts().containsKey(postId)) {
                logger.warn(String.format("Post with id %s is already loaded, skipping", postId));
                continue;
            }

            String postTitle;
            try {
                logger.info(String.format("Finding post's title"));
                Node titleNode = xpathService.getAsNode(postsNodeList.item(i), POST_TITLE_XPATH);
                if (titleNode != null) {
                    postTitle = titleNode.getTextContent().trim();
                    logger.info(String.format("Found post's title: %s", postTitle));
                } else {
                    logger.info("Cannot find post's title");
                    if (threadTitle != null) {
                        logger.info("Falling back to thread title to generate a post name");
                        postTitle = threadTitle + "#" + postId;
                    } else {
                        logger.info("Falling back to post id to generate a post name");
                        postTitle = "#" + postId;
                    }
                }
            } catch (Exception e) {
                throw new PostParseException(e);
            }

            String postCounter;
            try {
                logger.info(String.format("Finding post's counter"));
                Node counterNode = xpathService.getAsNode(postsNodeList.item(i), POST_COUNTER_XPATH);
                if (counterNode != null) {
                    postCounter = counterNode.getTextContent().trim();
                    logger.info(String.format("Found post's counter: %s", postCounter));
                } else {
                    postCounter = "";
                }
            } catch (Exception e) {
                throw new PostParseException(e);
            }

            ArrayList<Image> imagesList = new ArrayList<>();
            try {
                logger.info(String.format("Finding all links for post with id %s using xpath %s", postId, POST_LINKS_XPATH));
                NodeList imagesNodeList = xpathService.getAsNodeList(postsNodeList.item(i), POST_LINKS_XPATH);
                for (int j = 0; j < imagesNodeList.getLength(); j++) {

                    Node imageHref = imagesNodeList.item(j).getAttributes().getNamedItem("href");
                    Host foundHost;
                    if (imageHref != null) {
                        String imageUrl = imageHref.getTextContent().trim();
                        logger.info(String.format("Scanning %s", imageUrl));
                        foundHost = supportedHosts.stream().filter(host -> host.isSupported(imageUrl)).findFirst().orElse(null);
                    } else {
                        logger.warn("href is null, skipping");
                        continue;
                    }
                    if (foundHost != null) {
                        logger.info(String.format("Found supported host %s for %s", foundHost.getClass().getSimpleName(), imageHref));
                        imagesList.add(appStateService.createImage(imageHref.getTextContent(), postId, postTitle, foundHost));
                    } else {
                        logger.warn(String.format("unsupported host for %s, skipping", imageHref));
                        continue;
                    }
                }
            } catch (Exception e) {
                throw new PostParseException(e);
            }

            if (!imagesList.isEmpty()) {
                logger.info(String.format("Found %d images for post with id %s", imagesList.size(), postId));
                posts.add(appStateService.createPost(postTitle, realUrl, imagesList, null, postId, postCounter));
            } else {
                logger.warn(String.format("No images found for post with id %s, skipping", postId));
            }
        }
        return posts;
    }
}
