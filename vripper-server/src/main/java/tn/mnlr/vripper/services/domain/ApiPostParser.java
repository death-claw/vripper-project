package tn.mnlr.vripper.services.domain;

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
import tn.mnlr.vripper.services.ConnectionService;
import tn.mnlr.vripper.services.VGAuthService;

import javax.xml.parsers.SAXParserFactory;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ApiPostParser {

    private static final String VR_API = "https://vipergirls.to/vr.php";

    private static final SAXParserFactory factory = SAXParserFactory.newInstance();

    private final String threadId;
    private final String postId;
    private final ConnectionService cm;
    private final VGAuthService VGAuthService;

    public ApiPostParser(String threadId, String postId) {
        this.threadId = threadId;
        this.postId = postId;
        this.cm = SpringContext.getBean(ConnectionService.class);
        this.VGAuthService = SpringContext.getBean(VGAuthService.class);
    }

    public ApiPost parse() throws PostParseException {

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
        ApiPostHandler apiPostHandler = new ApiPostHandler(threadId, postId);
        log.debug(String.format("Requesting %s", httpGet));
        ApiPost post = getPost(httpGet, apiPostHandler, thr);
        if (thr.get() != null) {
            log.error(String.format("parsing failed for thread %s, post %s", threadId, postId), thr.get());
            throw new PostParseException(thr.get());
        }
        return post;
    }

    private ApiPost getPost(HttpGet httpGet, ApiPostHandler apiPostHandler, AtomicReference<Throwable> thr) {
        return Failsafe.with(VripperApplication.retryPolicy)
                .onFailure(e -> thr.set(e.getFailure()))
                .get(() -> {
                    HttpClient connection = cm.getClient().build();
                    try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet, VGAuthService.getContext())) {
                        if (response.getStatusLine().getStatusCode() / 100 != 2) {
                            throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                        }

                        factory.newSAXParser().parse(response.getEntity().getContent(), apiPostHandler);
                        EntityUtils.consumeQuietly(response.getEntity());
                        return apiPostHandler.getParsedPost();
                    }
                });
    }
}
