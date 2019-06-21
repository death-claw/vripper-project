package tn.mnlr.vripper.services;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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

    private final static String VIPER_GIRLS_BASE_ADRESS = "https://vipergirls.to/";
    private final static String POSTS_XPATH = "//li[contains(@id,'post_')][not(contains(@id,'post_thank'))]";
    private final static String REAL_THREAD_XPATH = ".//a[@class='postcounter']";
    private final static String THREAD_TITLE_XPATH = "//li[contains(@class, 'lastnavbit')]/span";
    private final static String POST_TITLE_XPATH = ".//h2";
    private final static String POST_LINKS_XPATH = ".//a";
    private Logger logger = LoggerFactory.getLogger(PostParser.class);

    @Autowired
    private ConnectionManager cm;

    @Autowired
    private HtmlProcessorService htmlProcessorService;

    @Autowired
    private XpathService xpathService;

    @Autowired
    private List<Host> supportedHosts;

    @Autowired
    private AppStateService appStateService;

    public List<Post> parse(String threadUrl) throws PostParseException {

        logger.debug(String.format("Parsing thread %s", threadUrl));
        List<Post> posts = new ArrayList<>();
        String postResponse;

        HttpClient connection = this.cm.getClient().build();
        HttpGet httpGet = this.cm.buildHttpGet(threadUrl);

        logger.debug(String.format("Requesting %s", httpGet));
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
            logger.debug(String.format("Looking for thread title using xpath: %s", THREAD_TITLE_XPATH));
            Node threadTitleNode = xpathService.getAsNode(document, THREAD_TITLE_XPATH);
            if (threadTitleNode != null) {
                threadTitle = threadTitleNode.getTextContent().trim();
                logger.debug(String.format("Thread title found %s", threadTitle));
            }
            if (threadTitle == null) {
                logger.warn("Cannot find thread's title");
            }
        } catch (Exception e) {
            throw new PostParseException(e);
        }

        NodeList postsNodeList;
        try {
            logger.debug(String.format("Looking for posts using xpath: %s", POSTS_XPATH));
            postsNodeList = xpathService.getAsNodeList(document, POSTS_XPATH);
        } catch (Exception e) {
            throw new PostParseException(e);
        }

        logger.debug(String.format("Found %d posts in thread %s", postsNodeList.getLength(), threadUrl));

        for (int i = 0; i < postsNodeList.getLength(); i++) {
            String realUrl;
            logger.debug(String.format("Parsing post #%d", i + 1));
            try {
                logger.debug(String.format("Finding posts's link"));
                realUrl = VIPER_GIRLS_BASE_ADRESS.concat(xpathService
                        .getAsNode(postsNodeList.item(i), REAL_THREAD_XPATH)
                        .getAttributes()
                        .getNamedItem("href")
                        .getTextContent()
                        .trim());
                logger.debug(String.format("Post's link: %s", realUrl));
            } catch (Exception e) {
                throw new PostParseException(e);
            }

            logger.debug("Finding posts's id");
            String postId = realUrl.substring(realUrl.indexOf("#")).replace("#post", "");
            logger.debug(String.format("Post's id: %s", postId));

            if (appStateService.getCurrentPosts().containsKey(postId)) {
                logger.warn(String.format("Post with id %s is already loaded, skipping", postId));
                continue;
            }

            String postTitle;
            try {
                logger.debug(String.format("Finding post's title"));
                Node titleNode = xpathService.getAsNode(postsNodeList.item(i), POST_TITLE_XPATH);
                if (titleNode != null) {
                    postTitle = titleNode.getTextContent().trim();
                    logger.debug(String.format("Found post's title: %s", postTitle));
                } else {
                    logger.debug("Cannot find post's title");
                    if (threadTitle != null) {
                        logger.debug("Falling back to thread title to generate a post name");
                        postTitle = threadTitle + "#" + postId;
                    } else {
                        logger.debug("Falling back to post id to generate a post name");
                        postTitle = "#" + postId;
                    }
                }
            } catch (Exception e) {
                throw new PostParseException(e);
            }

            ArrayList<Image> imagesList = new ArrayList<>();
            try {
                logger.debug(String.format("Finding all links for post with id %s using xpath %s", postId, POST_LINKS_XPATH));
                NodeList imagesNodeList = xpathService.getAsNodeList(postsNodeList.item(i), POST_LINKS_XPATH);
                for (int j = 0; j < imagesNodeList.getLength(); j++) {

                    Node imageHref = imagesNodeList.item(j).getAttributes().getNamedItem("href");
                    Host foundHost;
                    if (imageHref != null) {
                        String imageUrl = imageHref.getTextContent().trim();
                        logger.debug(String.format("Scanning %s", imageUrl));
                        foundHost = supportedHosts.stream().filter(host -> host.isSupported(imageUrl)).findFirst().orElse(null);
                    } else {
                        logger.warn("href is null, skipping");
                        continue;
                    }
                    if (foundHost != null) {
                        logger.debug(String.format("Found supported host %s for %s", foundHost.getClass().getSimpleName(), imageHref));
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
                logger.debug(String.format("Found %d images for post with id %s", imagesList.size(), postId));
                posts.add(appStateService.createPost(postTitle, realUrl, imagesList, null, postId));
            } else {
                logger.warn(String.format("No images found for post with id %s, skipping", postId));
            }
        }
        return posts;
    }
}
