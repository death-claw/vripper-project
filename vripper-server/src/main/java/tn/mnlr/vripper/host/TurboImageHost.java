package tn.mnlr.vripper.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.q.ImageFileData;
import tn.mnlr.vripper.services.ConnectionManager;

@Service
public class TurboImageHost extends Host {

    private static final Logger logger = LoggerFactory.getLogger(TurboImageHost.class);

    private static final String host = "turboimagehost.com";
    public static final String TITLE_XPATH = "//div[contains(@class,'titleFullS')]/h1";
    public static final String IMG_XPATH = "//img[@id='uImage']";

    @Autowired
    private ConnectionManager cm;

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getLookup() {
        return host;
    }

    @Override
    protected void setNameAndUrl(final String url, final ImageFileData imageFileData) throws HostException {

        Document doc = getResponse(url).getDocument();

        String title;
        try {
            logger.info(String.format("Looking for xpath expression %s in %s", TITLE_XPATH, url));
            Node titleNode = xpathService.getAsNode(doc, TITLE_XPATH);
            logger.info(String.format("Resolving name for %s", url));
            if(titleNode != null) {
                title = titleNode.getTextContent().trim();
            } else {
                title = null;
            }
        } catch (XpathException e) {
            throw new HostException(e);
        }

        if(title == null || title.isEmpty()) {
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
