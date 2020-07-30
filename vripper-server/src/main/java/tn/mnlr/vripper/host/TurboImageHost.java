package tn.mnlr.vripper.host;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.protocol.HttpClientContext;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.q.ImageFileData;

@Service
@Slf4j
public class TurboImageHost extends Host {

    private static final String host = "turboimagehost.com";
    private static final String TITLE_XPATH = "//div[contains(@class,'titleFullS')]/h1";
    private static final String IMG_XPATH = "//img[@id='uImage']";

    public TurboImageHost() {
        super();
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
    protected void setNameAndUrl(final String url, final ImageFileData imageFileData, final HttpClientContext context) throws HostException {

        Document doc = getResponse(url, context).getDocument();

        String title;
        try {
            log.debug(String.format("Looking for xpath expression %s in %s", TITLE_XPATH, url));
            Node titleNode = xpathService.getAsNode(doc, TITLE_XPATH);
            log.debug(String.format("Resolving name for %s", url));
            if (titleNode != null) {
                title = titleNode.getTextContent().trim();
            } else {
                title = null;
            }
        } catch (XpathException e) {
            throw new HostException(e);
        }

        if (title == null || title.isEmpty()) {
            title = getDefaultImageName(url);
        }

        try {
            Node urlNode = xpathService.getAsNode(doc, IMG_XPATH);
            imageFileData.setImageUrl(urlNode.getAttributes().getNamedItem("src").getTextContent().trim());
            imageFileData.setImageName(title);
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
