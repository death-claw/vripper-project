package tn.mnlr.vripper.host;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.services.ConnectionManager;
import tn.mnlr.vripper.services.HostService;
import tn.mnlr.vripper.services.HtmlProcessorService;
import tn.mnlr.vripper.services.XpathService;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AcidimgHost extends Host {

    private static final String host = "acidimg.cc";
    private static final String CONTINUE_BUTTON_XPATH = "//input[@id='continuebutton']";
    private static final String IMG_XPATH = "//img[@class='centred']";

    private final ConnectionManager cm;
    private final HostService hostService;
    private final XpathService xpathService;
    private final HtmlProcessorService htmlProcessorService;


    @Autowired
    public AcidimgHost(ConnectionManager cm, HostService hostService, XpathService xpathService, HtmlProcessorService htmlProcessorService) {
        this.cm = cm;
        this.hostService = hostService;
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

        Document doc = hostService.getResponse(url, context).getDocument();

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
            HttpPost httpPost = cm.buildHttpPost(url);
            httpPost.addHeader("Referer", url);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("imgContinue", "Continue to your image"));
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(params));
            } catch (Exception e) {
                throw new HostException(e);
            }

            log.debug(String.format("Requesting %s", httpPost));
            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(httpPost, context)) {
                log.debug(String.format("Cleaning response for %s", httpPost));
                doc = htmlProcessorService.clean(EntityUtils.toString(response.getEntity()));
                EntityUtils.consumeQuietly(response.getEntity());
            } catch (Exception e) {
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

        if (imgNode == null) {
            throw new HostException("Cannot find the image node");
        }

        try {
            log.debug(String.format("Resolving name and image url for %s", url));
            String imgTitle = imgNode.getAttributes().getNamedItem("alt").getTextContent().trim();
            String imgUrl = imgNode.getAttributes().getNamedItem("src").getTextContent().trim();

            return new HostService.NameUrl(imgTitle.isEmpty() ? hostService.getDefaultImageName(imgUrl) : imgTitle, imgUrl);
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
