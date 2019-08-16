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
public class PixhostHost extends Host {

    private static final Logger logger = LoggerFactory.getLogger(PixhostHost.class);

    private static final String host = "pixhost.to";
    public static final String IMG_XPATH = "//img[@id='image']";

    @Autowired
    private ConnectionManager cm;

    @Override
    public String getHost() {
        return host;
    }

    @Override
    protected void setNameAndUrl(final String url, final ImageFileData imageFileData) throws HostException {

        Document doc = getResponse(url).getDocument();

        Node imgNode;
        try {
            logger.info(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            imgNode = xpathService.getAsNode(doc, IMG_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        try {
            logger.info(String.format("Resolving name and image url for %s", url));
            String imgTitle = imgNode.getAttributes().getNamedItem("alt").getTextContent().trim();
            String imgUrl = imgNode.getAttributes().getNamedItem("src").getTextContent().trim();

            imageFileData.setImageUrl(imgUrl);
            imageFileData.setImageName(imgTitle.substring(imgTitle.indexOf('_') + 1));
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
