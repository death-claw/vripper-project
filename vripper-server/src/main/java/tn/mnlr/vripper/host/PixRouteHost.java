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
public class PixRouteHost extends Host {

    private static final String host = "pixroute.com";
    private static final String IMG_XPATH = "//img[@id='imgpreview']";

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
        Document doc = getResponse(url, context).getDocument();

        Node imgNode;
        try {
            log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            imgNode = xpathService.getAsNode(doc, IMG_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        if (imgNode == null) {
            throw new HostException("Failed to locate image");
        }

        try {
            log.debug(String.format("Resolving name and image url for %s", url));

            imageFileData.setImageUrl(imgNode.getAttributes().getNamedItem("src").getTextContent().trim());
            imageFileData.setImageName(imgNode.getAttributes().getNamedItem("alt").getTextContent().trim());
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
