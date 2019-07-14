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
public class ImageZillaHost extends Host {

    private static final Logger logger = LoggerFactory.getLogger(ImageZillaHost.class);

    private static final String host = "imagezilla.net";
    public static final String IMG_XPATH = "//img[@id='photo']";

    @Autowired
    private ConnectionManager cm;

    @Override
    protected String getHost() {
        return host;
    }

    @Override
    protected void setNameAndUrl(final String url, final ImageFileData imageFileData) throws HostException {

        Document doc = getResponse(url).getDocument();

        String title;
        try {
            logger.info(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            Node titleNode = xpathService.getAsNode(doc, IMG_XPATH).getAttributes().getNamedItem("title");
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
            imageFileData.setImageUrl(url.replace("show", "images"));
            imageFileData.setImageName(title);
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
