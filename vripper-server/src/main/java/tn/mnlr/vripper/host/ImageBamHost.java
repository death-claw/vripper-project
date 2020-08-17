package tn.mnlr.vripper.host;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.HtmlProcessorException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.services.ConnectionManager;
import tn.mnlr.vripper.services.HostService;
import tn.mnlr.vripper.services.HtmlProcessorService;
import tn.mnlr.vripper.services.XpathService;

import java.io.IOException;

@Service
@Slf4j
public class ImageBamHost extends Host {

    private static final String host = "imagebam.com";
    private static final String CONTINUE_BUTTON_XPATH = "//a[@title='Continue to your image']";
    private static final String IMG_XPATH = "//img[@class='image']";

    private final HostService hostService;
    private final ConnectionManager cm;
    private final XpathService xpathService;
    private final HtmlProcessorService htmlProcessorService;

    @Autowired
    public ImageBamHost(HostService hostService, ConnectionManager cm, XpathService xpathService, HtmlProcessorService htmlProcessorService) {
        this.hostService = hostService;
        this.cm = cm;
        this.xpathService = xpathService;
        this.htmlProcessorService = htmlProcessorService;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getLookup() {
        return host;
    }

    @Override
    public HostService.NameUrl getNameAndUrl(final String url, final HttpClientContext context) throws HostException {

        HostService.Response response = hostService.getResponse(url, context);
        Document doc = response.getDocument();

        Node contDiv;
        try {
            log.debug(String.format("Looking for xpath expression %s in %s", CONTINUE_BUTTON_XPATH, url));
            contDiv = xpathService.getAsNode(doc, CONTINUE_BUTTON_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        if (contDiv != null) {
            log.debug(String.format("Click button found for %s", url));
            HttpClient client = cm.getClient().build();
            HttpGet httpGet = cm.buildHttpGet(url);
            httpGet.addHeader("Referer", url);
            log.debug(String.format("Requesting %s", httpGet));
            try (CloseableHttpResponse res = (CloseableHttpResponse) client.execute(httpGet, context)) {
                String s = EntityUtils.toString(res.getEntity());
                log.debug(String.format("%s response is:%n%s", httpGet, s));
                log.debug(String.format("Cleaning response for %s", httpGet));
                doc = htmlProcessorService.clean(s);
                EntityUtils.consumeQuietly(res.getEntity());
            } catch (IOException | HtmlProcessorException e) {
                throw new HostException(e);
            }
        }

        Node imgNode;
        try {
            log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            imgNode = xpathService.getAsNode(doc, IMG_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        try {
            log.debug(String.format("Resolving name and image url for %s", url));
            String imgTitle = imgNode.getAttributes().getNamedItem("id").getTextContent().trim();
            String imgUrl = imgNode.getAttributes().getNamedItem("src").getTextContent().trim();

            return new HostService.NameUrl(imgTitle.isEmpty() ? imgUrl.substring(imgUrl.lastIndexOf('/') + 1) : imgTitle, imgUrl);
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
