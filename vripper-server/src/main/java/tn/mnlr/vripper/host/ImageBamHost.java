package tn.mnlr.vripper.host;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.HtmlProcessorException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.q.ImageFileData;
import tn.mnlr.vripper.services.ConnectionManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class ImageBamHost extends Host {

    private static final Logger logger = LoggerFactory.getLogger(ImageBamHost.class);

    private static final String host = "imagebam.com";
    public static final String CONTINUE_BUTTON_XPATH = "//a[@title='Continue to your image']";
    public static final String IMG_XPATH = "//img[@class='image']";

    @Autowired
    private ConnectionManager cm;

    @Override
    protected String getHost() {
        return host;
    }

    @Override
    protected void setNameAndUrl(final String url, final ImageFileData imageFileData) throws HostException {

        Response response = getResponse(url);
        Document doc = response.getDocument();
        Header[] headers = response.getHeaders();
        String cookies = Arrays.asList(headers)
                .stream()
                .filter(e -> e.getName().toLowerCase().contains("Set-Cookie".toLowerCase()))
                .map(e -> e.getValue().split(";")[0].trim())
                .collect(Collectors.joining("; "));

        Node contDiv;
        try {
            logger.info(String.format("Looking for xpath expression %s in %s", CONTINUE_BUTTON_XPATH, url));
            contDiv = xpathService.getAsNode(doc, CONTINUE_BUTTON_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        if (contDiv != null) {
            logger.info(String.format("Getting cookies to use for %s", url));
            logger.info(String.format("Click button found for %s", url));
            HttpClient client = cm.getClient().build();
            HttpGet httpGet = cm.buildHttpGet(url);
            httpGet.addHeader("Referer", url);
            httpGet.addHeader("Cookie", cookies);
            logger.info(String.format("Requesting %s", httpGet));
            try (CloseableHttpResponse res = (CloseableHttpResponse) client.execute(httpGet)) {
                String s = EntityUtils.toString(res.getEntity());
                logger.debug(String.format("%s response is:%n%s", httpGet, s));
                logger.debug(String.format("Cleaning response for %s", httpGet));
                doc = htmlProcessorService.clean(s);
                EntityUtils.consumeQuietly(res.getEntity());
            } catch (IOException | HtmlProcessorException e) {
                throw new HostException(e);
            }
        }

        Node imgNode;
        try {
            logger.info(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            imgNode = xpathService.getAsNode(doc, IMG_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        try {
            logger.info(String.format("Resolving name and image url for %s", url));
            String imgTitle = imgNode.getAttributes().getNamedItem("id").getTextContent().trim();
            String imgUrl = imgNode.getAttributes().getNamedItem("src").getTextContent().trim();

            imageFileData.setImageUrl(imgUrl);
            imageFileData.setImageName(imgTitle.isEmpty() ? imgUrl.substring(imgUrl.lastIndexOf('/') + 1) : imgTitle);
        } catch(Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
