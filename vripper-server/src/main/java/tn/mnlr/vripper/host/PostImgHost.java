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

import java.util.Optional;

@Service
public class PostImgHost extends Host {

    private static final Logger logger = LoggerFactory.getLogger(PostImgHost.class);

    private static final String host = "postimg.cc";
    private static final String TITLE_XPATH = "//span[contains(@class,'imagename')]";
    private static final String IMG_XPATH = "//a[@id='download']";

    public PostImgHost() {
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
        Document doc = getResponse(url, context).getDocument();

        Node urlNode, titleNode;
        try {
            logger.debug(String.format("Looking for xpath expression %s in %s", TITLE_XPATH, url));
            titleNode = xpathService.getAsNode(doc, TITLE_XPATH);

            logger.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            urlNode = xpathService.getAsNode(doc, IMG_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        try {
            logger.debug(String.format("Resolving name and image url for %s", url));
            String imgTitle = Optional.ofNullable(titleNode).map(node -> node.getTextContent().trim()).orElseGet(() -> getDefaultImageName(url));

            imageFileData.setImageUrl(urlNode.getAttributes().getNamedItem("href").getTextContent().trim());
            imageFileData.setImageName(imgTitle);
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
