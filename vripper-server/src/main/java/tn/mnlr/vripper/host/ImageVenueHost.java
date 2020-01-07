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

import java.net.URI;

@Service
public class ImageVenueHost extends Host {

    private static final Logger logger = LoggerFactory.getLogger(ImageVenueHost.class);

    private static final String host = "imagevenue.com";
    private static final String CONTINUE_BUTTON_XPATH = "//a[@title='Continue to your image']";
    private static final String IMG_XPATH = "//img[@id='thepic']";

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

        //Sadly, they do not support https.
        //If they add such support in the future,
        //then we should automatically adapt the URL here as done elsewhere.

        Response resp = getResponse(url, context);
        Document doc = resp.getDocument();

        try {
            logger.debug(String.format("Looking for xpath expression %s in %s", CONTINUE_BUTTON_XPATH, url));
            if(xpathService.getAsNode(doc, CONTINUE_BUTTON_XPATH) != null) {
                //Button detected. No need to actually click it, just make the call again.
                resp = getResponse(url, context);
                doc = resp.getDocument();
            }
        } catch (XpathException e) {
            throw new HostException(e);
        }

        Node imgNode;
        try {
            logger.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            imgNode = xpathService.getAsNode(doc, IMG_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        if (imgNode == null) {
            throw new HostException("Failed to locate image");
        }

        try {
            logger.debug(String.format("Resolving name and image url for %s", url));
            String imgTitle = imgNode.getAttributes().getNamedItem("alt").getTextContent().trim();
            String imgUrl = imgNode.getAttributes().getNamedItem("src").getTextContent().trim();

            URI baseUri = new URI(url);
            imageFileData.setImageUrl(new URI(baseUri.getScheme(), baseUri.getHost(), '/'+imgUrl, null).toString());
            imageFileData.setImageName(imgTitle.isEmpty() ? imgUrl.substring(imgUrl.lastIndexOf('/') + 1) : imgTitle);
        } catch(Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
