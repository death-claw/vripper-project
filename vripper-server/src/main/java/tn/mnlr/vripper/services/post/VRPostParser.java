package tn.mnlr.vripper.services.post;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.services.ConnectionManager;
import tn.mnlr.vripper.services.VipergirlsAuthService;

import javax.xml.parsers.SAXParserFactory;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class VRPostParser {

    private static final String VR_API = "https://vipergirls.to/vr.php";

    private static final SAXParserFactory factory = SAXParserFactory.newInstance();

    private final String threadId;
    private final String postId;
    private final ConnectionManager cm;
    private final VipergirlsAuthService vipergirlsAuthService;

    public VRPostParser(String threadId, String postId) {
        this.threadId = threadId;
        this.postId = postId;
        this.cm = SpringContext.getBean(ConnectionManager.class);
        this.vipergirlsAuthService = SpringContext.getBean(VipergirlsAuthService.class);
    }

    public ParseResult parse() throws PostParseException {

        log.debug(String.format("Parsing post %s", postId));
        HttpGet httpGet;
        try {
            URIBuilder uriBuilder = new URIBuilder(VR_API);
            uriBuilder.setParameter("p", postId);
            httpGet = cm.buildHttpGet(uriBuilder.build(), null);
        } catch (URISyntaxException e) {
            throw new PostParseException(e);
        }

        AtomicReference<Throwable> thr = new AtomicReference<>();
        VRApiPostHandler handler = new VRApiPostHandler(threadId, postId);
        log.debug(String.format("Requesting %s", httpGet));
        ParseResult post = getPost(httpGet, handler, thr);
        if (thr.get() != null) {
            log.error(String.format("parsing failed for thread %s, post %s", threadId, postId), thr.get());
            throw new PostParseException(thr.get());
        }
        return post;
    }

    private ParseResult getPost(HttpGet httpGet, VRApiPostHandler handler, AtomicReference<Throwable> thr) {
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
}
