package tn.mnlr.vripper.services.domain;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.services.ConnectionService;
import tn.mnlr.vripper.services.VGAuthService;

import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ApiThreadParser {

    private static final String VR_API = "https://vipergirls.to/vr.php";

    private static final SAXParserFactory factory = SAXParserFactory.newInstance();
    private final Queued queued;
    private final ConnectionService cm;
    private final VGAuthService VGAuthService;

    public ApiThreadParser(Queued queued) {
        this.queued = queued;
        this.cm = SpringContext.getBean(ConnectionService.class);
        this.VGAuthService = SpringContext.getBean(VGAuthService.class);
    }

    public List<MultiPostItem> parse() throws PostParseException {

        log.debug(String.format("Parsing thread %s", queued));
        HttpGet httpGet;
        try {
            URIBuilder uriBuilder = new URIBuilder(VR_API);
            uriBuilder.setParameter("t", queued.getThreadId());
            httpGet = cm.buildHttpGet(uriBuilder.build(), null);
        } catch (URISyntaxException e) {
            throw new PostParseException(e);
        }

        ApiThreadHandler apiThreadHandler = new ApiThreadHandler(queued);
        AtomicReference<Throwable> thr = new AtomicReference<>();
        log.debug(String.format("Requesting %s", httpGet));
        List<MultiPostItem> posts = Failsafe.with(cm.getRetryPolicy())
                .onFailure(e -> thr.set(e.getFailure()))
                .get(() -> {
                    HttpClient connection = cm.getClient().build();
                    try (CloseableHttpResponse response = (CloseableHttpResponse) connection.execute(httpGet, VGAuthService.getContext())) {
                        if (response.getStatusLine().getStatusCode() / 100 != 2) {
                            throw new DownloadException(String.format("Unexpected response code '%d' for %s", response.getStatusLine().getStatusCode(), httpGet));
                        }

                        try {
                            factory.newSAXParser().parse(new BufferedInputStream(response.getEntity().getContent()), apiThreadHandler);
                            return apiThreadHandler.getPosts();
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
