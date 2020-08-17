package tn.mnlr.vripper.host;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.protocol.HttpClientContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.services.HostService;
import tn.mnlr.vripper.services.XpathService;

import java.util.Optional;

@Service
@Slf4j
public class ImageTwistHost extends Host {

    private static final String IMG_XPATH = "//img[contains(@class, 'img')]";
    private static final String host = "imagetwist.com";

    private final HostService hostService;
    private final XpathService xpathService;

    @Autowired
    public ImageTwistHost(HostService hostService, XpathService xpathService) {
        this.hostService = hostService;
        this.xpathService = xpathService;
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

        Node imgNode;
        try {
            log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            imgNode = xpathService.getAsNode(doc, IMG_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        try {
            log.debug(String.format("Resolving name and image url for %s", url));
            String imgTitle = Optional.ofNullable(imgNode.getAttributes().getNamedItem("alt")).map(Node::getTextContent).map(String::trim).orElse(null);
            String imgUrl = imgNode.getAttributes().getNamedItem("src").getTextContent().trim();

            return new HostService.NameUrl(imgTitle, imgUrl);
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
