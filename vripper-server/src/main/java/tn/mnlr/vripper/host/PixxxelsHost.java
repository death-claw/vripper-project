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
public class PixxxelsHost extends Host {

    private static final Logger logger = LoggerFactory.getLogger(PixxxelsHost.class);

    private static final String host = "pixxxels.cc";
    private static final String IMG_XPATH = "//*[@id='download']";
    private static final String TITLE_XPATH = "//*[contains(@class,'imagename')]";

    public PixxxelsHost() {
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
    protected void setNameAndUrl(final String _url, final ImageFileData imageFileData, final HttpClientContext context) throws HostException {

        String url = _url.replace("http://", "https://");
        Response resp = getResponse(url, context);
        Document doc = resp.getDocument();

        Node imgNode, titleNode;
        try {
            logger.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            imgNode = xpathService.getAsNode(doc, IMG_XPATH);

            logger.debug(String.format("Looking for xpath expression %s in %s", TITLE_XPATH, url));
            titleNode = xpathService.getAsNode(doc, TITLE_XPATH);

        } catch (XpathException e) {
            throw new HostException(e);
        }

        try {
            logger.debug(String.format("Resolving name and image url for %s", url));
            String imgTitle = titleNode.getTextContent().trim();
            String imgUrl = imgNode.getAttributes().getNamedItem("href").getTextContent().trim();

            imageFileData.setImageUrl(imgUrl);
            imageFileData.setImageName(imgTitle.isEmpty() ? imgUrl.substring(imgUrl.lastIndexOf('/') + 1) : imgTitle);
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
