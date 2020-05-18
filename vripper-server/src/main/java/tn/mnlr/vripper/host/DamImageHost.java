package tn.mnlr.vripper.host;

import org.apache.http.client.protocol.HttpClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.q.ImageFileData;

@Service
public class DamImageHost extends Host {

    private static final String IMG_XPATH = "//img[contains(@class, 'centred')]";
    private static final Logger logger = LoggerFactory.getLogger(DamImageHost.class);
    private static final String host = "damimage.com";

    public DamImageHost() {
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

        Node imgNode;
        try {
            logger.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            imgNode = xpathService.getAsNode(doc, IMG_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        try {
            logger.debug(String.format("Resolving name and image url for %s", url));
            //String imgTitle = Optional.ofNullable(imgNode.getAttributes().getNamedItem("alt")).map(Node::getTextContent).map(String::trim).orElse(null);
            String imgUrl = imgNode.getAttributes().getNamedItem("src").getTextContent().trim();

            imageFileData.setImageUrl(imgUrl);
            imageFileData.setImageName(imgUrl.substring(imgUrl.lastIndexOf('/') + 1));
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
